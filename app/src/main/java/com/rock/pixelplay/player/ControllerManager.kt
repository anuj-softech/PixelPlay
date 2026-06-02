package com.rock.pixelplay.player

import android.R.attr.duration
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.toColorInt
import com.rock.pixelplay.R
import com.rock.pixelplay.helper.AnimationUtils
import com.rock.pixelplay.helper.VideoUtils
import com.rock.pixelplay.ui.PlayerActivity
import com.rock.pixelplay.widgets.PlayerSettings


public fun PlayerActivity.initProgressBar() {
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
    lb.playerOverlay.rotate.setOnClickListener {
        val rotation = getWindowManager().getDefaultDisplay().getRotation()
        val newOrientation = if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setRequestedOrientation(newOrientation)
        com.rock.pixelplay.helper.SettingsPref.setPlayerOrientation(this, newOrientation)
    }
}

public fun PlayerActivity.hidePlayerOverlay() {
    if (overlayShowing) {
        lb.playerOverlay.root.post { ->
            overlayShowing = false
            AnimationUtils().translateY(lb.playerOverlay.titleBar, 0, -100)
            AnimationUtils().translateY(lb.playerOverlay.progressContainer, 0, 100)
            AnimationUtils().fadeOut(lb.playerOverlay.root)
        }
    }
}

public fun PlayerActivity.showPlayerOverlay() {
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

public fun PlayerActivity.resetHideTimer() {
    overlayHandler.removeCallbacks(hideOverlayRunnable!!)
    overlayHandler.postDelayed(hideOverlayRunnable!!, 5000)
}

@SuppressLint("ClickableViewAccessibility")
public fun PlayerActivity.initOptions() {
    lb.playerOverlay.options.setOnClickListener {
        val dialogView = PlayerSettings(this)
        val windowManager = this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.RIGHT

        windowManager.addView(dialogView, layoutParams)
        val slideIn: Animation? = android.view.animation.AnimationUtils.loadAnimation(
            this,
            R.anim.slide_in_right
        )
        dialogView.getLB().main.startAnimation(slideIn)
        val currentNightMode = dimView != null
        val currentSpeed = player.playbackParameters.speed
        val config = PlayerSettingConfig(currentNightMode, currentSpeed)
        settupOptionsDialog(dialogView, windowManager, config)
        dialogView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                closeDialog(dialogView, windowManager)
                true
            } else {
                false
            }
        }
    }
}

private fun PlayerActivity.settupOptionsDialog(
    dialogView: PlayerSettings,
    windowManager: WindowManager,
    config: PlayerSettingConfig
) {
    dialogView.configure(config, object : PlayerSettingsListener {
        override fun onSettingsChanged(config: PlayerSettingConfig) {
            applyNightOverlay(config.nightMode)
            player.setPlaybackSpeed(config.playbackSpeed)
        }

        override fun onClose() {
            closeDialog(dialogView, windowManager)
        }
    })

    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (dialogView.isShown) {
                val slideOut: Animation? = android.view.animation.AnimationUtils.loadAnimation(
                    lb.root.context,
                    R.anim.slide_out_right
                )
                dialogView.getLB().main.startAnimation(slideOut)
                dialogView.getLB().main.postDelayed({ windowManager.removeView(dialogView) }, 290)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    })
}

private fun PlayerActivity.closeDialog(
    dialogView: PlayerSettings,
    windowManager: WindowManager
) {
    val slideOut: Animation? =
        android.view.animation.AnimationUtils.loadAnimation(lb.root.context, R.anim.slide_out_right)
    dialogView.getLB().main.startAnimation(slideOut)
    dialogView.getLB().main.postDelayed({ windowManager.removeView(dialogView) }, 90)
}

fun PlayerActivity.applyNightOverlay(enable: Boolean) {
    val decor = (this as? Activity)?.window?.decorView as? ViewGroup ?: return

    if (enable) {
        if (dimView == null) {
            dimView = View(this).apply {
                setBackgroundColor("#80000000".toColorInt())
                isClickable = false
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            decor.addView(dimView)
        }
    } else {
        dimView?.let {
            decor.removeView(it)
            dimView = null
        }
    }
}