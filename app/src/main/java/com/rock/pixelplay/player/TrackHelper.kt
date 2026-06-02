package com.rock.pixelplay.player

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rock.pixelplay.R
import com.rock.pixelplay.adapter.TrackAdapter
import com.rock.pixelplay.adapter.TrackItem
import com.rock.pixelplay.ui.PlayerActivity
import com.rock.pixelplay.widgets.PopButton

fun PlayerActivity.showCaptionSelectorDialog(context: Context, player: ExoPlayer) {
    val dialogView = LayoutInflater.from(context)
        .inflate(R.layout.dialog_track_selector, null)

    val recyclerView = dialogView.findViewById<RecyclerView>(R.id.captionRecyclerView)
    val closeButton = dialogView.findViewById<PopButton>(R.id.closeButton)
    val title = dialogView.findViewById<TextView>(R.id.title)
    title.text = getString(R.string.select_captions)
    val density = context.resources.displayMetrics.density
    val widthPx = (380 * density).toInt()
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val layoutParams = WindowManager.LayoutParams(
        widthPx,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_DIM_BEHIND,
        PixelFormat.TRANSLUCENT
    )
    layoutParams.dimAmount = 0.5f
    layoutParams.gravity = Gravity.CENTER
    windowManager.addView(dialogView, layoutParams)

    val tracks = mutableListOf<TrackItem>()
    tracks.add(TrackItem("No Caption", "Disable captions", null))

    var selectedIndex = 0

    val trackGroups = player.currentTracks.groups
    trackGroups.forEach { group ->
        if (group.type == C.TRACK_TYPE_TEXT) {
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val langCode = format.language ?: "Unknown"
                val displayLanguage = format.language?.let { java.util.Locale(it).displayLanguage } ?: "Unknown Language"
                val titleText = format.label ?: displayLanguage
                val attributes = mutableListOf<String>()
                attributes.add("Language Code: $langCode")
                if ((format.selectionFlags and C.SELECTION_FLAG_FORCED) != 0) {
                    attributes.add("Forced")
                }
                if ((format.selectionFlags and C.SELECTION_FLAG_DEFAULT) != 0) {
                    attributes.add("Default")
                }
                if ((format.roleFlags and C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND) != 0) {
                    attributes.add("CC")
                }
                if (format.id != null) {
                    attributes.add("ID: ${format.id}")
                }
                val subtitleText = attributes.joinToString(" | ")

                if (group.isTrackSelected(i)) {
                    selectedIndex = tracks.size
                }
                tracks.add(TrackItem(titleText, subtitleText, TrackSelectionOverride(group.mediaTrackGroup, i)))
            }
        }
    }

    recyclerView.layoutManager = LinearLayoutManager(context)
    recyclerView.adapter = TrackAdapter(tracks, selectedIndex) { override ->
        val params = player.trackSelectionParameters.buildUpon()
        if (override == null) {
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            params.addOverride(override)
        }
        player.trackSelectionParameters = params.build()
        windowManager.removeView(dialogView)
    }

    closeButton.setOnClickListener {
        windowManager.removeView(dialogView)
    }
}

fun PlayerActivity.showAudioSelectorDialog(context: Context, player: ExoPlayer) {
    val dialogView = LayoutInflater.from(context)
        .inflate(R.layout.dialog_track_selector, null)

    val recyclerView = dialogView.findViewById<RecyclerView>(R.id.captionRecyclerView)
    val closeButton = dialogView.findViewById<PopButton>(R.id.closeButton)
    val title = dialogView.findViewById<TextView>(R.id.title)
    title.text = getString(R.string.select_audio)
    val density = context.resources.displayMetrics.density
    val widthPx = (380 * density).toInt()
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val layoutParams = WindowManager.LayoutParams(
        widthPx,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_DIM_BEHIND,
        PixelFormat.TRANSLUCENT
    )
    layoutParams.dimAmount = 0.5f
    layoutParams.gravity = Gravity.CENTER
    windowManager.addView(dialogView, layoutParams)

    val tracks = mutableListOf<TrackItem>()
    tracks.add(TrackItem("No Audio", "Disable audio", null))

    var selectedIndex = 0
    val trackGroups = player.currentTracks.groups
    trackGroups.forEach { group ->
        if (group.type == C.TRACK_TYPE_AUDIO) {
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val langCode = format.language ?: "Unknown"
                val displayLanguage = format.language?.let { java.util.Locale(it).displayLanguage } ?: "Unknown Language"
                val titleText = format.label ?: displayLanguage
                val attributes = mutableListOf<String>()
                if (format.bitrate > 0) {
                    attributes.add("${format.bitrate / 1000} kbps")
                }
                if (format.channelCount > 0) {
                    val channels = when (format.channelCount) {
                        1 -> "Mono"
                        2 -> "Stereo"
                        6 -> "5.1 Surround"
                        8 -> "7.1 Surround"
                        else -> "${format.channelCount} Ch"
                    }
                    attributes.add(channels)
                }
                if (format.sampleRate > 0) {
                    attributes.add("${format.sampleRate / 1000.0} kHz")
                }
                attributes.add("Code: $langCode")
                if (format.id != null) {
                    attributes.add("ID: ${format.id}")
                }
                val subtitleText = attributes.joinToString(" | ")

                if (group.isTrackSelected(i)) {
                    selectedIndex = tracks.size
                }
                tracks.add(TrackItem(titleText, subtitleText, TrackSelectionOverride(group.mediaTrackGroup, i)))
            }
        }
    }

    recyclerView.layoutManager = LinearLayoutManager(context)
    recyclerView.adapter = TrackAdapter(tracks, selectedIndex) { override ->
        val params = player.trackSelectionParameters.buildUpon()
        if (override == null) {
            params.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
        } else {
            params.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            params.addOverride(override)
        }
        player.trackSelectionParameters = params.build()
        windowManager.removeView(dialogView)
    }

    closeButton.setOnClickListener {
        windowManager.removeView(dialogView)
    }
}

fun PlayerActivity.setupTrackButton() {
    lb.playerOverlay.caption.setOnClickListener {
        showCaptionSelectorDialog(this, player)
    }
    lb.playerOverlay.audio.setOnClickListener {
        showAudioSelectorDialog(this, player)
    }
}
