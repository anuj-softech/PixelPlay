package com.rock.pixelplay.ui;

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import com.rock.pixelplay.R
import com.rock.pixelplay.databinding.ActivityPlayerBinding
import com.rock.pixelplay.helper.HistoryHelper
import com.rock.pixelplay.helper.Loader
import com.rock.pixelplay.helper.VideoUtils
import com.rock.pixelplay.model.VideoItem
import com.rock.pixelplay.player.GestureManager
import com.rock.pixelplay.player.UI_BUFFERING
import com.rock.pixelplay.player.UI_ENDED
import com.rock.pixelplay.player.UI_IDLE
import com.rock.pixelplay.player.UI_PAUSED
import com.rock.pixelplay.player.UI_PLAYING
import com.rock.pixelplay.player.hidePlayerOverlay
import com.rock.pixelplay.player.initOptions
import com.rock.pixelplay.player.initProgressBar
import com.rock.pixelplay.player.manageState
import com.rock.pixelplay.player.setupTrackButton
import com.rock.pixelplay.player.showPlayerOverlay
import com.squareup.moshi.Moshi
import java.text.SimpleDateFormat
import java.util.Date

class PlayerActivity : AppCompatActivity() {

    public var dimView: View? = null
    lateinit var lb: ActivityPlayerBinding
    lateinit var player: ExoPlayer
    lateinit var updateHandler: Handler
    lateinit var updateRunnable: Runnable;
    lateinit var videoItemG: VideoItem
    var overlayShowing: Boolean = true
    var isUserSeeking: Boolean = false
    val overlayHandler = Handler(Looper.getMainLooper())
    var hideOverlayRunnable: Runnable? = Runnable {
        hidePlayerOverlay()
    }
    lateinit var loader: Loader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lb = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(lb.root);
        ViewCompat.setOnApplyWindowInsetsListener(lb.root, { v, insets ->
            var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        });
        loader = Loader(this, R.drawable.lucide_loader)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)
        updateHandler = Handler(mainLooper)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        initPlayerUI(UI_IDLE)
        initPlayerInstance()
        lb.playerOverlay.back.setOnClickListener {
            finish()
        }
        if (intent.hasExtra("video_item")) initWithVideoItem()

        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val rawUri = intent.data!!
            val isLocal = rawUri.scheme == "file" || rawUri.scheme == "content"
            if (rawUri.scheme == "content") {
                Log.e("Content", rawUri.toString())
                applicationContext.grantUriPermission(
                    packageName,
                    rawUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                try {
                    contentResolver.takePersistableUriPermission(
                        rawUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    Log.e("VideoPlayer", "Failed to persist URI permission: $e")
                }

            }
            val actualUri = if (rawUri.scheme == "pixelplay") {
                rawUri.toString().replace("pixelplay://", "https://").toUri()
            } else rawUri

            val thumbnail = if (isLocal) {
                actualUri.toString()
            } else {
                " "
            }

            val videoItem = VideoItem(
                title = actualUri.lastPathSegment ?: "Unknown",
                path = actualUri.toString(),
                dateAdded = System.currentTimeMillis(),
                duration = VideoUtils().getVideoDuration(this, actualUri.toString()),
                thumbnail = thumbnail,
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

    @OptIn(UnstableApi::class)
    private fun initPlayerFromPath(path: String?) {
        Log.e("Path", path.toString())
        if (path?.contains(".m3u") == true || path?.contains(".vdo") == true || path?.contains(".hls") == true) {
            val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
            val hlsMediaSource =
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(path))
            player.setMediaSource(hlsMediaSource, videoItemG.lastPlayedMs)

            Log.e("M3u", path.toString() + " " + videoItemG.lastPlayedMs)
            lb.playerView.player = player
            player.prepare()
            player.play()
            loader.startLoading()
        } else {
            var mediaItem = MediaItem.fromUri(path.toString())
            player.setMediaItem(mediaItem, videoItemG.lastPlayedMs)
            Log.e("Path", path.toString() + " " + videoItemG.lastPlayedMs)
            lb.playerView.player = player
            player.prepare()
            player.play()
        }

        val gestureManager = GestureManager(this, lb.playerView, player);
        gestureManager.init();
        lb.playerView.setOnClickListener {
            overlayShowing = !(lb.playerOverlay.root.isGone)
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

    public fun initPlayerUI(state: Int) {
        setupTrackButton()
        lb.playerOverlay.root.setOnClickListener {
            hidePlayerOverlay()
        }
        initProgressBar();
        initOptions();
        lb.playerOverlay.playPauseToggleBtn.setOnClickListener {
            overlayShowing = !(lb.playerOverlay.root.isGone)
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
        lb.playerOverlay.playPauseToggleBtn.visibility = View.VISIBLE

        manageState(state)

    }


    //TODO set spinner options

    private fun initPlayerInstance() {
        player = ExoPlayer.Builder(this).build();
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
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

                val endat = System.currentTimeMillis() + player.duration - player.currentPosition;
                var date = Date(endat)
                var dateFormat = SimpleDateFormat("hh:mm a");
                var strDate = dateFormat.format(date);
                lb.playerOverlay.endAt.text = "End at " + strDate;

            }
        }
    }

    override fun onStop() {
        super.onStop()
        player.pause()
        var position = player.currentPosition
        var duration = player.duration
        videoItemG.lastPlayedMs = position
        videoItemG.duration = VideoUtils().getVideoDuration(duration.toString())
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

    override fun onDestroy() {
        player.release();
        finish()
        super.onDestroy()
    }

}


