package com.rock.pixelplay.ui;

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
    private val overlayHandler = Handler(Looper.getMainLooper())
    var hideOverlayRunnable: Runnable? =Runnable {
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
        updateHandler = Handler(mainLooper)
        initPlayerUI(UI_IDLE)
        initPlayerInstance()
        lb.playerOverlay.back.setOnClickListener {
            finish()
        }
        if (intent.hasExtra("video_item")) initWithVideoItem()
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
        lb.playerOverlay.root.setOnClickListener {
            hidePlayerOverlay()
        }
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
                lb.playerOverlay.playPauseToggleBtn.visibility = View.GONE
                lb.playerOverlay.progressBar.progress = 0
            }

            UI_ENDED -> {
                lb.playerOverlay.playPauseToggleBtn.setImageResource(R.drawable.baseline_loop_24)
            }
        }

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

    override fun onStop() {
        super.onStop()
        player.stop()
        var position = player.currentPosition
        var duration = player.duration
        videoItemG.lastPlayed = VideoUtils().getVideoDuration(position.toString())
        videoItemG.playedPercentage = ((position * 100) / duration).toFloat()
        if (!videoItemG.title.isEmpty()) {
            HistoryHelper(this).addVideo(videoItemG)
        }
    }

    private fun resetHideTimer() {
        overlayHandler.removeCallbacks(hideOverlayRunnable!!)
        overlayHandler.postDelayed(hideOverlayRunnable!!, 5000)
    }

}

