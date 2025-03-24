package com.rock.pixelplay.helper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.rock.pixelplay.model.VideoItem
import com.rock.pixelplay.ui.PlayerActivity
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit

class VideoUtils {
    fun getVideoDuration(context: Context, path: String?): String {
        if (path.isNullOrEmpty()) return "00:00:00"

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            val durationStr =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }
        return "00:00:00"
    }

    fun getVideoDuration(durationStr: String): String {
        val durationMs = durationStr?.toLongOrNull() ?: 0L

        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun getVideoThumbnail(videoPath: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoPath)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    public fun playInApp(context: Context, videoItem: VideoItem) {

        val intent = Intent(context, PlayerActivity::class.java)
        val moshi = Moshi.Builder().build()
        val jsonAdapter = moshi.adapter(VideoItem::class.java)
        val json = jsonAdapter.toJson(videoItem)
        intent.putExtra("video_item", json)
        context.startActivity(intent)
        HistoryHelper(context).addVideo(videoItem)

    }
}
