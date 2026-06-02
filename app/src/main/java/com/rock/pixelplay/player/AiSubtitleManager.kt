package com.rock.pixelplay.player

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.C
import androidx.media3.common.Player
import com.rock.pixelplay.R
import com.rock.pixelplay.helper.SettingsPref
import com.rock.pixelplay.ui.PlayerActivity
import com.whispercpp.java.whisper.WhisperContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future

enum class SegmentStatus {
    GENERATING,
    SUCCESS,
    ERROR
}

class AiSubtitleSegment(
    val startMs: Long,
    val endMs: Long,
    var text: String,
    var status: SegmentStatus
)

interface AiSubtitleInitCallback {
    fun onReady()
    fun onError(error: String)
}

fun PlayerActivity.initAiSubtitle() {
    if (whisperContext != null || isAiSubtitleInitializing) return
    isAiSubtitleInitializing = true
    if (player.playWhenReady) {
        isPausedForGeneration = true
        player.pause()
    }

    val path = SettingsPref.getWhisperModelPath(this)
    if (path == null) {
        isAiSubtitleInitializing = false
        Toast.makeText(this, "AI Model not downloaded!", Toast.LENGTH_LONG).show()
        SettingsPref.setAiSubtitleEnabled(this, false)
        if (isPausedForGeneration) {
            isPausedForGeneration = false
            player.play()
        }
        return
    }

    val callback = object : AiSubtitleInitCallback {
        override fun onReady() {
            isAiSubtitleInitializing = false
            subtitleExecutor = Executors.newSingleThreadExecutor()

            val mainLayout = findViewById<FrameLayout>(R.id.main)
            if (aiSubtitleView == null) {
                aiSubtitleView = layoutInflater.inflate(R.layout.view_ai_subtitle_view, mainLayout, false)
                mainLayout.addView(aiSubtitleView, 1)
            }

            val params = player.trackSelectionParameters.buildUpon()
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            player.trackSelectionParameters = params.build()

            startSubtitleSyncLoop()

            if (isPausedForGeneration) {
                isPausedForGeneration = false
                player.play()
            }
        }

        override fun onError(error: String) {
            isAiSubtitleInitializing = false
            Toast.makeText(this@initAiSubtitle, "Failed to load AI Model: $error", Toast.LENGTH_LONG).show()
            SettingsPref.setAiSubtitleEnabled(this@initAiSubtitle, false)
            if (isPausedForGeneration) {
                isPausedForGeneration = false
                player.play()
            }
        }
    }

    Thread {
        try {
            val context = WhisperContext.createContextFromFile(path)
            runOnUiThread {
                whisperContext = context
                callback.onReady()
            }
        } catch (e: Exception) {
            runOnUiThread {
                callback.onError(e.message ?: "Unknown error")
            }
        }
    }.start()
}

fun PlayerActivity.startSubtitleSyncLoop() {
    aiSubtitleRunnable?.let { updateHandler.removeCallbacks(it) }
    aiSubtitleRunnable = object : Runnable {
        override fun run() {
            if (whisperContext == null) return
            val currentPos = player.currentPosition
            val curSeg = (currentPos / 10000).toInt()

            val minSeg = curSeg
            val maxSeg = curSeg + 5

            for (index in activeTasks.keys) {
                if (index < minSeg || index > maxSeg) {
                    activeTasks[index]?.cancel(true)
                    activeTasks.remove(index)
                    subtitleCache.remove(index)
                }
            }

            val segment = subtitleCache[curSeg]
            if (segment == null) {
                if (player.playWhenReady) {
                    isPausedForGeneration = true
                    player.pause()
                }
                showGeneratingUI()
                generateSegment(curSeg)
            } else {
                when (segment.status) {
                    SegmentStatus.GENERATING -> {
                        if (player.playWhenReady) {
                            isPausedForGeneration = true
                            player.pause()
                        }
                        showGeneratingUI()
                    }
                    SegmentStatus.SUCCESS -> {
                        showSubtitleText(segment.text, isError = false)
                        if (isPausedForGeneration) {
                            isPausedForGeneration = false
                            player.play()
                        }
                    }
                    SegmentStatus.ERROR -> {
                        showSubtitleText(segment.text, isError = true)
                        if (isPausedForGeneration) {
                            isPausedForGeneration = false
                            player.play()
                        }
                    }
                }
            }

            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val isCpuHigh = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                powerManager.currentThermalStatus >= android.os.PowerManager.THERMAL_STATUS_MODERATE
            } else {
                powerManager.isPowerSaveMode
            }

            if (!isCpuHigh) {
                for (i in (curSeg + 1)..maxSeg) {
                    if (!subtitleCache.containsKey(i)) {
                        generateSegment(i)
                    }
                }
            }

            updateHandler.postDelayed(this, 100)
        }
    }
    updateHandler.post(aiSubtitleRunnable as Runnable)

    player.addListener(object : Player.Listener {
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                aiSubtitleRunnable?.let {
                    updateHandler.removeCallbacks(it)
                    updateHandler.post(it)
                }
            }
        }
    })
}

fun PlayerActivity.generateSegment(segIndex: Int) {
    if (subtitleCache.containsKey(segIndex)) return

    val startMs = segIndex * 10000L
    val segment = AiSubtitleSegment(startMs, startMs + 10000L, "", SegmentStatus.GENERATING)
    subtitleCache[segIndex] = segment

    val executor = subtitleExecutor ?: return
    val path = videoItemG.path ?: return

    val future = executor.submit {
        try {
            if (Thread.currentThread().isInterrupted) return@submit

            val audioFloats = decodeAudioSegment(this, path, startMs, 10000L)
            if (audioFloats == null || audioFloats.isEmpty()) {
                segment.status = SegmentStatus.ERROR
                segment.text = "AI Subtitle Error:\nModel failed to process audio."
                runOnUiThread { updateSubtitleDisplay() }
                return@submit
            }

            val context = whisperContext
            if (context == null) {
                segment.status = SegmentStatus.ERROR
                segment.text = "AI Subtitle Error:\nModel context is not initialized."
                runOnUiThread { updateSubtitleDisplay() }
                return@submit
            }

            val text = context.transcribeData(audioFloats)
            segment.text = text
            segment.status = SegmentStatus.SUCCESS
            runOnUiThread { updateSubtitleDisplay() }
        } catch (e: Exception) {
            segment.status = SegmentStatus.ERROR
            segment.text = "AI Subtitle Error:\n${e.message ?: "Model failed to process audio."}"
            runOnUiThread { updateSubtitleDisplay() }
        } finally {
            activeTasks.remove(segIndex)
        }
    }

    activeTasks[segIndex] = future
}

fun PlayerActivity.updateSubtitleDisplay() {
    val currentPos = player.currentPosition
    val curSeg = (currentPos / 10000).toInt()
    val segment = subtitleCache[curSeg]
    if (segment != null) {
        when (segment.status) {
            SegmentStatus.GENERATING -> {
                showGeneratingUI()
            }
            SegmentStatus.SUCCESS -> {
                showSubtitleText(segment.text, isError = false)
                if (isPausedForGeneration) {
                    isPausedForGeneration = false
                    player.play()
                }
            }
            SegmentStatus.ERROR -> {
                showSubtitleText(segment.text, isError = true)
                if (isPausedForGeneration) {
                    isPausedForGeneration = false
                    player.play()
                }
            }
        }
    }
}

fun PlayerActivity.showSubtitleText(text: String, isError: Boolean) {
    val view = aiSubtitleView ?: return
    val root = view.findViewById<View>(R.id.ai_subtitle_root) ?: return
    val textView = view.findViewById<TextView>(R.id.ai_subtitle_text) ?: return

    if (isError) {
        root.setBackgroundColor(android.graphics.Color.BLACK)
        textView.setTextColor(android.graphics.Color.RED)
    } else {
        root.setBackgroundColor(android.graphics.Color.parseColor("#99000000"))
        textView.setTextColor(android.graphics.Color.WHITE)
    }

    textView.text = text
    if (text.isBlank() || overlayShowing) {
        view.visibility = View.GONE
    } else {
        view.visibility = View.VISIBLE
    }
}

fun PlayerActivity.showGeneratingUI() {
    val view = aiSubtitleView ?: return
    val root = view.findViewById<View>(R.id.ai_subtitle_root) ?: return
    val textView = view.findViewById<TextView>(R.id.ai_subtitle_text) ?: return

    root.setBackgroundColor(android.graphics.Color.parseColor("#22000000"))
    textView.setTextColor(android.graphics.Color.WHITE)
    textView.text = "Generating subtitles..."
    if (overlayShowing) {
        view.visibility = View.GONE
    } else {
        view.visibility = View.VISIBLE
    }
}

fun PlayerActivity.stopAiSubtitle() {
    aiSubtitleRunnable?.let {
        updateHandler.removeCallbacks(it)
        aiSubtitleRunnable = null
    }

    for ((_, future) in activeTasks) {
        future.cancel(true)
    }
    activeTasks.clear()

    subtitleExecutor?.shutdownNow()
    subtitleExecutor = null

    val ctx = whisperContext
    if (ctx != null) {
        Thread {
            try {
                ctx.release()
            } catch (e: Exception) {}
        }.start()
        whisperContext = null
    }

    subtitleCache.clear()

    aiSubtitleView?.let {
        val mainLayout = findViewById<FrameLayout>(R.id.main)
        mainLayout.removeView(it)
        aiSubtitleView = null
    }

    try {
        val params = player.trackSelectionParameters.buildUpon()
        params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        player.trackSelectionParameters = params.build()
    } catch (e: Exception) {}
}

fun decodeAudioSegment(
    context: Context,
    path: String,
    startMs: Long,
    durationMs: Long
): FloatArray? {
    val extractor = MediaExtractor()
    try {
        if (path.startsWith("content://")) {
            extractor.setDataSource(context, Uri.parse(path), null)
        } else {
            extractor.setDataSource(path)
        }
    } catch (e: Exception) {
        return null
    }

    var audioTrackIndex = -1
    for (i in 0 until extractor.trackCount) {
        val format = extractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
        if (mime.startsWith("audio/")) {
            audioTrackIndex = i
            break
        }
    }
    if (audioTrackIndex == -1) {
        extractor.release()
        return null
    }

    extractor.selectTrack(audioTrackIndex)
    val format = extractor.getTrackFormat(audioTrackIndex)
    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
    val codec = MediaCodec.createDecoderByType(mime)
    codec.configure(format, null, null, 0)
    codec.start()

    val startUs = startMs * 1000L
    val endUs = (startMs + durationMs) * 1000L
    extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

    val bufferInfo = MediaCodec.BufferInfo()
    val shortBuffers = ArrayList<ShortArray>()
    var sampleRate = 44100
    var channelCount = 2
    var sawInputEOS = false
    var sawOutputEOS = false

    try {
        while (!sawOutputEOS && extractor.sampleTime < endUs && extractor.sampleTime >= 0) {
            if (!sawInputEOS) {
                val inputBufferIndex = codec.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: break
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEOS = true
                    } else {
                        codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null) {
                    val outFormat = codec.getOutputFormat(outputBufferIndex)
                    sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                    channelCount = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount)

                    val shortBuffer = outputBuffer.asShortBuffer()
                    val chunk = ShortArray(shortBuffer.remaining())
                    shortBuffer.get(chunk)
                    shortBuffers.add(chunk)
                }
                codec.releaseOutputBuffer(outputBufferIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val outFormat = codec.outputFormat
                sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                channelCount = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount)
            }
        }
    } catch (e: Exception) {
    } finally {
        try {
            codec.stop()
            codec.release()
        } catch (e: Exception) {}
        extractor.release()
    }

    if (shortBuffers.isEmpty()) return null

    var totalSize = 0
    for (arr in shortBuffers) {
        totalSize += arr.size
    }
    val mergedShorts = ShortArray(totalSize)
    var offset = 0
    for (arr in shortBuffers) {
        System.arraycopy(arr, 0, mergedShorts, offset, arr.size)
        offset += arr.size
    }

    val totalFrames = totalSize / channelCount
    val monoShorts = ShortArray(totalFrames)
    for (i in 0 until totalFrames) {
        var sum = 0
        for (c in 0 until channelCount) {
            val idx = i * channelCount + c
            if (idx < totalSize) {
                sum += mergedShorts[idx]
            }
        }
        monoShorts[i] = (sum / channelCount).toShort()
    }

    val ratio = sampleRate.toDouble() / 16000.0
    val outputLength = (monoShorts.size / ratio).toInt()
    if (outputLength <= 0) return null
    val resampledFloats = FloatArray(outputLength)
    for (i in 0 until outputLength) {
        val srcIndex = i * ratio
        val index = srcIndex.toInt()
        val frac = srcIndex - index
        if (index + 1 < monoShorts.size) {
            val s0 = monoShorts[index].toFloat() / 32768f
            val s1 = monoShorts[index + 1].toFloat() / 32768f
            resampledFloats[i] = s0 + frac.toFloat() * (s1 - s0)
        } else if (index < monoShorts.size) {
            resampledFloats[i] = monoShorts[index].toFloat() / 32768f
        }
    }

    return resampledFloats
}