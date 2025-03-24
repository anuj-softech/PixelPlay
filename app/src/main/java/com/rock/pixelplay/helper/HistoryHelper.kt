package com.rock.pixelplay.helper

import android.content.Context
import com.rock.pixelplay.model.VideoItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class HistoryHelper(context: Context) {

    private val prefs = context.getSharedPreferences("video_history_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().build()
    private val videoListType = Types.newParameterizedType(List::class.java, VideoItem::class.java)

    private var videoHistory: MutableList<VideoItem> = loadHistory()

    // Retrieve the list of video items (read-only)
    fun getHistory(): List<VideoItem> = videoHistory.toList()

    // Add a video, ensuring no duplicates, and save
    fun addVideo(video: VideoItem) {
        // Remove existing video with the same path
        videoHistory.removeAll { it.path == video.path }
        // Add new video at the top
        videoHistory.add(0, video)
        saveHistory()
    }

    // Remove a video by path and save
    fun removeVideoByPath(path: String) {
        videoHistory.removeAll { it.path == path }
        saveHistory()
    }

    // Clear the entire history and save
    fun clearHistory() {
        videoHistory.clear()
        saveHistory()
    }

    // Load history from SharedPreferences
    private fun loadHistory(): MutableList<VideoItem> {
        val json = prefs.getString("video_history", null) ?: return mutableListOf()
        val adapter = moshi.adapter<List<VideoItem>>(videoListType)
        return adapter.fromJson(json)?.toMutableList() ?: mutableListOf()
    }

    // Save history to SharedPreferences
    private fun saveHistory() {
        val adapter = moshi.adapter<List<VideoItem>>(videoListType)
        val json = adapter.toJson(videoHistory)
        prefs.edit().putString("video_history", json).apply()
    }
}
