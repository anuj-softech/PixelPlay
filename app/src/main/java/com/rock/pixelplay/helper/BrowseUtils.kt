package com.rock.pixelplay.helper

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.rock.pixelplay.model.VideoItem

class BrowseUtils(private val context: Context) {
    public fun countVideosInPath(path: String): Int {
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Video.Media._ID)
        val selection = "${MediaStore.Video.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("$path%")

        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        val count = cursor?.count ?: 0
        cursor?.close()
        return count
    }

    public fun countAllVideos(listener:onResultInterface) {

        val projection = arrayOf(MediaStore.Video.Media._ID)
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        val count = cursor?.count ?: 0
        cursor?.close()
        listener.onValue(count)
    }


    public fun getLatestVideos(context: Context): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()
        val contentResolver = context.contentResolver
        val videoUtils = VideoUtils()
        val uri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Video.Media.MIME_TYPE} IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        val selectionArgs = arrayOf(
            "video/x-matroska",  // MKV
            "video/webm",         // WebM
            "video/mp4",          // MP4
            "video/3gpp",         // 3GP
            "video/avi",          // AVI (may need custom support)
            "video/quicktime",    // MOV
            "video/x-flv",        // FLV
            "video/mpeg",         // MPEG
            "video/x-ms-wmv",     // WMV
            "video/x-msvideo",    // AVI
            "video/ogg"           // OGG
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            var count = 0
            while (cursor.moveToNext() && count < 15) {
                val title = cursor.getString(titleColumn)
                val path = cursor.getString(pathColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val duration = videoUtils.getVideoDuration(context, path)
                val thumbnail = path
                videos.add(
                    VideoItem(
                        title,
                        path,
                        dateAdded,
                        duration,
                        thumbnail,
                        lastPlayed = "00:00:00",
                        playedPercentage = 0F
                    )
                )
                count++
            }
        }

        return videos

    }
}

public interface onResultInterface {
    fun onValue(count: Int)
}
