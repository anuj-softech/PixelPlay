package com.rock.pixelplay.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.rock.pixelplay.R;
import com.rock.pixelplay.helper.Popup;
import com.rock.pixelplay.helper.SettingsPref;

public class GestureManager {
    private final PlayerView playerView;
    private final AppCompatActivity activity;
    private final ExoPlayer player;
    private final AudioManager audioManager;

    private int initialX, initialY;

    private long downTime;
    private float baseBrightness = -1f;
    private boolean verticleSwipeEnabled = false;
    private boolean horizontalSwipeEnabled = false;

    WindowManager.LayoutParams layout;

    float currentBrightness;
    int currentVolume;
    private long currentDuration;
    private long newDuration = 0;

    public GestureManager(AppCompatActivity activity, PlayerView playerView, ExoPlayer player) {
        this.playerView = playerView;
        this.activity = activity;
        this.player = player;
        this.audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        layout = activity.getWindow().getAttributes();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void init() {
        Popup popup = new Popup(activity);
        GestureDetector gestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float x = e.getX();
                if (x < playerView.getWidth() / 2f) {
                    player.seekTo(Math.max(0, player.getCurrentPosition() - 10000));
                } else {
                    player.seekTo(player.getCurrentPosition() + 10000);
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                player.setPlaybackSpeed(2f);
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                playerView.performClick();
                return true;
            }
        });

        playerView.setOnTouchListener((v, event) -> {
            if(gestureDetector.onTouchEvent(event)){
                return true;
            }
            if(!SettingsPref.INSTANCE.isSwipeEnabled(activity)){
                return false;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = (int) event.getX();
                    initialY = (int) event.getY();
                    downTime = System.currentTimeMillis();
                    baseBrightness = -1f;
                    player.setPlaybackSpeed(1f);
                    currentBrightness = layout.screenBrightness;
                    currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    currentDuration = player.getCurrentPosition();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    int x = (int) event.getX();
                    int y = (int) event.getY();
                    int dx = x - initialX;
                    int dy = initialY - y;

                    verticleSwipeEnabled = ((Math.abs(dy) > 50 && Math.abs(dx) < 100) || verticleSwipeEnabled) && !horizontalSwipeEnabled ;
                    horizontalSwipeEnabled = ((Math.abs(dx) > 50 && Math.abs(dy) < 100)|| horizontalSwipeEnabled) && !(verticleSwipeEnabled);

                    float factor = (float) (dy * 3) / playerView.getHeight();

                    if (Math.abs(dy) > Math.abs(dx) && verticleSwipeEnabled) {
                        if (x < playerView.getWidth() / 2) {
                            float newBrightness = setBrightnessBySwipe(factor);
                            popup.showPopup(playerView,R.drawable.brightness_icon, (int) (newBrightness * 100) + "%");
                        } else {
                            float newVolume = setVolumeBySwipe(factor);
                            popup.showPopup(playerView,R.drawable.volume_icon, (int) newVolume + "%");
                        }
                    } else if(horizontalSwipeEnabled){
                        factor = (float) (dx * 10) / playerView.getWidth();
                        long duration = setDurationBySwipe(factor);
                        popup.showPopup(playerView,R.drawable.move_x, getDurationInString(duration) + "");

                    }

                    return true;

                case MotionEvent.ACTION_UP:
                    player.setPlaybackSpeed(1f);
                    verticleSwipeEnabled = false;
                    if(horizontalSwipeEnabled){
                        player.seekTo(newDuration);
                    }
                    horizontalSwipeEnabled = false;
                    return true;

                default:
                    return false;
            }
        });
    }



    private float setVolumeBySwipe(float factor) {

        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int newVolume = Math.max(0, Math.min(maxVolume, (int)(currentVolume + 5*factor)));
        Log.d("Volume", "New volume: " + newVolume +" "+ factor);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
        return (newVolume * 100f / maxVolume);
    }

    private float setBrightnessBySwipe(float factor) {
        float newBrightness = Math.max(0.01f, Math.min(1.0f, currentBrightness + 0.1f * factor));
        Log.d("Brightness", "New brightness: " + newBrightness +" "+factor);
        layout.screenBrightness = newBrightness;
        activity.getWindow().setAttributes(layout);
        return newBrightness;
    }
    private long setDurationBySwipe(float factor) {
        newDuration = (long) Math.max(0, Math.min(player.getDuration(), currentDuration + player.getDuration() * factor/10));
        Log.d("Duration", "New Duration: " + newDuration +" "+factor);

        return newDuration;
    }
    @SuppressLint("DefaultLocale")
    private String getDurationInString(long duration) {
        long seconds = duration / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
