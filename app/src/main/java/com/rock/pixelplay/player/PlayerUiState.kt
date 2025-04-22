package com.rock.pixelplay.player

import android.view.WindowManager
import com.rock.pixelplay.R
import com.rock.pixelplay.ui.PlayerActivity

var UI_PAUSED = 1
var UI_PLAYING = 2
var UI_BUFFERING = 3
var UI_ERROR = 4
var UI_IDLE = 5
var UI_ENDED = 6

public fun PlayerActivity.manageState(state: Int) {
    when (state) {
        UI_PAUSED -> {
            lb.playerOverlay.playPauseToggleBtn.setImageResource(R.drawable.iconoir_play_solid)
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (Math.abs(player.duration - player.currentPosition) < 100) {
                initPlayerUI(UI_ENDED)
            }
        }

        UI_PLAYING -> {
            loader.stopLoading()
            lb.playerOverlay.playPauseToggleBtn.setImageResource(R.drawable.ic_round_pause)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            //lb.playerOverlay.playPauseToggleBtn.visibility = View.VISIBLE
            hidePlayerOverlay()
        }

        UI_BUFFERING -> {
            loader.startLoading()
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            //lb.playerOverlay.playPauseToggleBtn.visibility = View.GONE
            hidePlayerOverlay()
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
