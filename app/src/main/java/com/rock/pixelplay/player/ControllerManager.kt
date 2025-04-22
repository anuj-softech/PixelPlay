package com.rock.pixelplay.player

import android.R.attr.duration
import android.widget.SeekBar
import com.rock.pixelplay.helper.AnimationUtils
import com.rock.pixelplay.helper.VideoUtils
import com.rock.pixelplay.ui.PlayerActivity

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