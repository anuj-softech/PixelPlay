package com.rock.pixelplay.ui;

import android.R.attr.duration
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rock.pixelplay.R
import com.rock.pixelplay.databinding.ActivityPlayerBinding
import com.rock.pixelplay.helper.AnimationUtils
import com.rock.pixelplay.helper.HistoryHelper
import com.rock.pixelplay.helper.VideoUtils
import com.rock.pixelplay.model.VideoItem
import com.squareup.moshi.Moshi

class PlayerActivity : AppCompatActivity() {

    lateinit var lb: ActivityPlayerBinding
    lateinit var player: ExoPlayer
    var UI_PAUSED = 1
    var UI_PLAYING = 2
    var UI_BUFFERING = 3
    var UI_ERROR = 4
    var UI_IDLE = 5
    var UI_ENDED = 6
    lateinit var updateHandler: Handler
    lateinit var updateRunnable: Runnable;
    lateinit var videoItemG: VideoItem
    var overlayShowing: Boolean = true
    var isUserSeeking: Boolean = false
    private val overlayHandler = Handler(Looper.getMainLooper())
    var hideOverlayRunnable: Runnable? = Runnable {
        hidePlayerOverlay()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lb = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(lb.root);
        ViewCompat.setOnApplyWindowInsetsListener(lb.root, { v, insets ->
            var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        });
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)
        updateHandler = Handler(mainLooper)
        initPlayerUI(UI_IDLE)
        initPlayerInstance()
        lb.playerOverlay.back.setOnClickListener {
            finish()
        }
        if (intent.hasExtra("video_item")) initWithVideoItem()
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val videoUri = intent.data!!
            val videoItem = VideoItem(
                title = videoUri.lastPathSegment ?: "Unknown",
                path = videoUri.toString(),
                dateAdded = System.currentTimeMillis(),
                duration = VideoUtils().getVideoDuration(this, videoUri.toString()),
                thumbnail = videoUri.toString(),
                lastPlayed = "00:00:00",
                playedPercentage = 0f
            )
            videoItemG = videoItem
            lb.playerOverlay.title.text = videoItem.title
            initPlayerFromPath(videoItem.path)
            return
        }
    }

    private fun initWithVideoItem() {
        val moshi = Moshi.Builder().build()
        val json = intent.getStringExtra("video_item")
        val adapter = moshi.adapter(VideoItem::class.java)
        val videoItem = adapter.fromJson(json)
        videoItemG = videoItem!!
        lb.playerOverlay.title.text = videoItem?.title
        initPlayerFromPath(videoItem?.path)
    }

    private fun initPlayerFromPath(path: String?) {
        var mediaItem = MediaItem.fromUri(path.toString())
        player.setMediaItem(mediaItem)
        lb.playerView.player = player
        player.prepare()
        player.play()
        lb.playerView.setOnClickListener {
            showPlayerOverlay()
        }
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    initPlayerUI(UI_PLAYING)
                } else {
                    initPlayerUI(UI_PAUSED)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE -> initPlayerUI(UI_IDLE)
                    Player.STATE_BUFFERING -> initPlayerUI(UI_BUFFERING)
                    Player.STATE_READY -> println("Player is ready")
                    Player.STATE_ENDED -> initPlayerUI(UI_ENDED)
                }
            }
        })
        updateRunnable = Runnable {
            updateInfo();
            updateHandler.postDelayed(updateRunnable, 1000);
        }
        updateHandler.postDelayed(updateRunnable, 1000);
    }

    private fun initPlayerUI(state: Int) {
        setupTrackButton()
        lb.playerOverlay.root.setOnClickListener {
            hidePlayerOverlay()
        }
        initProgressBar();
        lb.playerOverlay.playPauseToggleBtn.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
        lb.playerOverlay.playPauseToggleBtn.visibility = View.VISIBLE
        when (state) {
            UI_PAUSED -> {
                lb.playerOverlay.playPauseToggleBtn.setImageResource(R.drawable.iconoir_play_solid)
                showPlayerOverlay()
            }

            UI_PLAYING -> {
                lb.playerOverlay.playPauseToggleBtn.setImageResource(R.drawable.ic_round_pause)
                hidePlayerOverlay()
            }

            UI_BUFFERING -> {
                lb.playerOverlay.playPauseToggleBtn.setImageResource(R.drawable.ic_round_pause)
            }

            UI_ERROR -> {
                lb.playerOverlay.playPauseToggleBtn.setImageResource(R.drawable.ic_round_pause)
            }

            UI_IDLE -> {
            }

            UI_ENDED -> {
                lb.playerOverlay.playPauseToggleBtn.setImageResource(R.drawable.baseline_loop_24)
                lb.playerOverlay.playPauseToggleBtn.setOnClickListener { player.seekTo(0);player.play() }
            }
        }

    }

    private fun setupTrackButton() {
        lb.playerOverlay.caption.setOnClickListener {
            showCaptionSelectorDialog(this, player)
        }
        lb.playerOverlay.audio.setOnClickListener {
            showAudioSelectorDialog(this, player)
        }
    }

    private fun initProgressBar() {
        lb.playerOverlay.progressBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    seekBar?.let {
                        val percentage = it.progress.toFloat() / it.max
                        val position = (player.getDuration() * percentage).toLong()
                        lb.playerOverlay.duration.text = buildString {
                            append(VideoUtils().getVideoDuration(position.toString()))
                            append(" / ")
                            append(VideoUtils().getVideoDuration(duration.toString()))
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                seekBar?.let {
                    val percentage = it.progress.toFloat() / it.max
                    val newPosition = (player.getDuration() * percentage).toLong()
                    player.seekTo(newPosition)
                }
            }
        })

    }

    private fun hidePlayerOverlay() {
        if (overlayShowing) {
            lb.playerOverlay.root.post { ->
                overlayShowing = false
                AnimationUtils().translateY(lb.playerOverlay.titleBar, 0, -100)
                AnimationUtils().translateY(lb.playerOverlay.progressContainer, 0, 100)
                AnimationUtils().fadeOut(lb.playerOverlay.root)
            }
        }
    }

    private fun showPlayerOverlay() {
        if (!overlayShowing) {
            lb.playerOverlay.root.post { ->
                overlayShowing = true
                AnimationUtils().translateY(lb.playerOverlay.titleBar, -100, 0)
                AnimationUtils().translateY(lb.playerOverlay.progressContainer, 100, 0)
                AnimationUtils().fadeIn(lb.playerOverlay.root)
            }
        }
        resetHideTimer()
    }

    private fun initPlayerInstance() {
        player = ExoPlayer.Builder(this).build();
    }

    private fun updateInfo() {
        if (player.isPlaying) {
            var position = player.currentPosition
            var duration = player.duration

            if (!isUserSeeking) {
                lb.playerOverlay.duration.text = buildString {
                    append(VideoUtils().getVideoDuration(position.toString()))
                    append(" / ")
                    append(VideoUtils().getVideoDuration(duration.toString()))
                }
                val maxProg = lb.playerOverlay.progressBar.max
                lb.playerOverlay.progressBar.progress =
                    (player.currentPosition.times(maxProg).div(duration)).toInt()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        player.pause()
        var position = player.currentPosition
        var duration = player.duration
        videoItemG.lastPlayed = VideoUtils().getVideoDuration(position.toString())
        videoItemG.playedPercentage = ((position * 100) / duration).toFloat()

        Log.e("Position", position.toString())
        if (!videoItemG.title.isEmpty()) {
            Log.e("Duration", duration.toString())
            Log.e("Percentage", videoItemG.lastPlayed)
            Log.e("Percentage", videoItemG.playedPercentage.toString())
            HistoryHelper(this).addVideo(videoItemG)
        }
    }

    private fun resetHideTimer() {
        overlayHandler.removeCallbacks(hideOverlayRunnable!!)
        overlayHandler.postDelayed(hideOverlayRunnable!!, 5000)
    }

    fun showCaptionSelectorDialog(context: Context, player: ExoPlayer) {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.dialog_caption_selector, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.captionRecyclerView)
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)

        val dialog = MaterialAlertDialogBuilder(
            context)
            .setView(dialogView)
            .create()

        val tracks = mutableListOf<Pair<String, TrackSelectionOverride?>>()

        tracks.add("No Caption" to null)

        var selectedIndex : Int = 0

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
        recyclerView.adapter = TrackAdapter(tracks,selectedIndex) { override ->
            val params = player.trackSelectionParameters.buildUpon()
            if (override == null) {
                params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else {
                params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                params.addOverride(override)
            }
            player.trackSelectionParameters = params.build()
            dialog.dismiss()
        }

        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    fun showAudioSelectorDialog(context: Context, player: ExoPlayer) {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.dialog_caption_selector, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.captionRecyclerView)
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)

        val dialog = MaterialAlertDialogBuilder(
            context)
            .setView(dialogView)
            .create()

        val tracks = mutableListOf<Pair<String, TrackSelectionOverride?>>()

        tracks.add("No Audio" to null)

        var selectedIndex : Int = 0

        val trackGroups = player.currentTracks.groups
        trackGroups.forEach { group ->
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val trackName = group.getTrackFormat(i).language ?: "Unknown"
                    if (group.isTrackSelected(i)) {
                        selectedIndex = i + 1 // +1 to account for "No Audio" at index 0
                    }
                    tracks.add(trackName to TrackSelectionOverride(group.mediaTrackGroup, i))
                }
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = TrackAdapter(tracks,selectedIndex) { override ->
            val params = player.trackSelectionParameters.buildUpon()
            if (override == null) {
                params.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            } else {
                params.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                params.addOverride(override)
            }
            player.trackSelectionParameters = params.build()
            dialog.dismiss()
        }

        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    class TrackAdapter(
        private val tracks: List<Pair<String, TrackSelectionOverride?>>,
        private val selectedIndex: Int,
        private val onCaptionClick: (TrackSelectionOverride?) -> Unit
    ) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            TrackViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_single_choice, parent, false)
            )

        override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
            holder.textView.text = tracks[position].first
            holder.itemView.isSelected = position == selectedIndex
            if(position == selectedIndex){ holder.textView.isChecked = true }
            holder.itemView.setOnClickListener { onCaptionClick(tracks[position].second) }
        }

        override fun getItemCount() = tracks.size

        class TrackViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: CheckedTextView = view.findViewById(android.R.id.text1)
        }
    }
}

