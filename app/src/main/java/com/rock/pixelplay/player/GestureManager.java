package com.rock.pixelplay.player;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.ui.PlayerView;

public class GestureManager {
    private final PlayerView playerView;
    private final AppCompatActivity activity;

    public GestureManager(AppCompatActivity activity, PlayerView playerView) {
        this.playerView = playerView;
        this.activity = activity;
    }

    int initialX = 0;
    int initialY = 0;

    @SuppressLint("ClickableViewAccessibility")
    public void init() {

        playerView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = (int) event.getX();
                    initialY = (int) event.getY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (event.getPointerCount() == 1) {
                        int x = (int) event.getX();
                        int y = (int) event.getY();
                        int dx = x - initialX;
                        int dy = y - initialY;
                        if (dy > 0) {
                            //moved up
                            changeBrightness(0.02f, 1);
                        }
                        if (dy < 0) {
                            //moved down
                            changeBrightness(0.02f, -1);
                        }
                        initialX = (int) event.getX();
                        initialY = (int) event.getY();
                    }
                    return true;
                default:
                    return false;
            }
        });
    }

    void changeBrightness(float value, int type) {
        WindowManager.LayoutParams layout = activity.getWindow().getAttributes();
        if (type == 1) {
            layout.screenBrightness = activity.getWindow().getAttributes().screenBrightness + Math.max(0.0f, Math.min(1.0f, value));
        } else if (type == -1) {
            layout.screenBrightness = activity.getWindow().getAttributes().screenBrightness - Math.max(0.0f, Math.min(1.0f, value));
        } activity.getWindow().setAttributes(layout);
    }
}
