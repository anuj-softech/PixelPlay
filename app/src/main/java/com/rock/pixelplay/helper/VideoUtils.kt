package com.rock.pixelplay.helper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
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

    public fun searchVideos(context: Context, query: String): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Video.Media.TITLE} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val dateAddedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                var count = 0
                while (cursor.moveToNext() && count < 5) {
                    val title = cursor.getString(titleColumn)
                    val path = cursor.getString(pathColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val duration = getVideoDuration(context, path) // your util method
                    val thumbnail = path

                    videos.add(
                        VideoItem(
                            title = title,
                            path = path,
                            dateAdded = dateAdded,
                            duration = duration,
                            thumbnail = thumbnail,
                            lastPlayed = "00:00:00",
                            playedPercentage = 0f
                        )
                    )
                    count++
                }
            }

        return videos
    }

}
