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
import com.rock.pixelplay.ui.PlayerActivity
import com.rock.pixelplay.widgets.PopButton

fun PlayerActivity.showCaptionSelectorDialog(context: Context, player: ExoPlayer) {
    val dialogView = LayoutInflater.from(context)
        .inflate(R.layout.dialog_track_selector, null)

    val recyclerView = dialogView.findViewById<RecyclerView>(R.id.captionRecyclerView)
    val closeButton = dialogView.findViewById<PopButton>(R.id.closeButton)
    val title = dialogView.findViewById<TextView>(R.id.title)
    title.text = getString(R.string.select_captions);
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )
    layoutParams.gravity = Gravity.BOTTOM
    windowManager.addView(dialogView, layoutParams)

    val tracks = mutableListOf<Pair<String, TrackSelectionOverride?>>()

    tracks.add("No Caption" to null)

    var selectedIndex: Int = 0

    val trackGroups = player.currentTracks.groups
    trackGroups.forEach { group ->
        if (group.type == C.TRACK_TYPE_TEXT) {
            for (i in 0 until group.length) {
                val trackName = group.getTrackFormat(i).language ?: "Unknown"
                if (group.isTrackSelected(i)) {
                    selectedIndex = i + 1 // +1 to account for "No Caption" at index 0
                }
                tracks.add(trackName to TrackSelectionOverride(group.mediaTrackGroup, i))
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

    closeButton.setOnClickListener { windowManager.removeView(dialogView) }
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
    title.text = getString(R.string.select_audio);
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )
    layoutParams.gravity = Gravity.BOTTOM
    windowManager.addView(dialogView, layoutParams)

    val tracks = mutableListOf<Pair<String, TrackSelectionOverride?>>()
    tracks.add("No Audio" to null)

    var selectedIndex = 0
    val trackGroups = player.currentTracks.groups
    trackGroups.forEach { group ->
        if (group.type == C.TRACK_TYPE_AUDIO) {
            for (i in 0 until group.length) {
                val trackName = group.getTrackFormat(i).language ?: "Unknown"
                if (group.isTrackSelected(i)) {
                    selectedIndex = i + 1
                }
                tracks.add(trackName to TrackSelectionOverride(group.mediaTrackGroup, i))
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

