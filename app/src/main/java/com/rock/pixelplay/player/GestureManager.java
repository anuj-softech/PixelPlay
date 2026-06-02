package com.rock.pixelplay.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;
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
    private float smoothedDx = 0f;
    private float smoothedDy = 0f;
    private boolean isLeftSide = false;
    private float scaleFactor = 1.0f;
    private ScaleGestureDetector scaleGestureDetector;

    private float startX, startY;
    private boolean isSwipeActive = false;
    private boolean is2xActive = false;
    private boolean isStableTouch = false;

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

        scaleGestureDetector = new ScaleGestureDetector(activity, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @OptIn(markerClass = UnstableApi.class)
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 4.0f));
                View videoSurfaceView = playerView.getVideoSurfaceView();
                if (videoSurfaceView != null) {
                    disableClipping(videoSurfaceView);
                    videoSurfaceView.setScaleX(scaleFactor);
                    videoSurfaceView.setScaleY(scaleFactor);
                } else {
                    disableClipping(playerView);
                    playerView.setScaleX(scaleFactor);
                    playerView.setScaleY(scaleFactor);
                }
                popup.showPopup(playerView, R.drawable.swipe, String.format(java.util.Locale.US, "%.1fx", scaleFactor));
                return true;
            }
        });

        GestureDetector gestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                handleDoubleTap(e, popup);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                playerView.performClick();
                return true;
            }
        });

        playerView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            if (event.getPointerCount() > 1) {
                verticleSwipeEnabled = false;
                horizontalSwipeEnabled = false;
                isSwipeActive = false;
                is2xActive = false;
                return true;
            }
            if (!isSwipeActive && !is2xActive) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
            }
            if (!SettingsPref.INSTANCE.isSwipeEnabled(activity)) {
                return false;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    downTime = System.currentTimeMillis();
                    isSwipeActive = false;
                    is2xActive = false;
                    isStableTouch = true;
                    player.setPlaybackSpeed(1f);
                    verticleSwipeEnabled = false;
                    horizontalSwipeEnabled = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    long elapsedTime = System.currentTimeMillis() - downTime;
                    float dx = event.getX() - startX;
                    float dy = event.getY() - startY;
                    double distance = Math.sqrt(dx * dx + dy * dy);

                    if (elapsedTime < 200) {
                        if (distance > 10) {
                            isStableTouch = false;
                        }
                        return true;
                    }

                    if (isStableTouch) {
                        if (elapsedTime >= 400 && elapsedTime <= 500) {
                            if (!isSwipeActive && !is2xActive && distance > 80) {
                                isSwipeActive = true;
                                handleSwipeStart(event);
                            }
                        }
                        if (elapsedTime > 500 && !isSwipeActive && !is2xActive && distance <= 80) {
                            is2xActive = true;
                            player.setPlaybackSpeed(2f);
                            popup.showPopup(playerView, R.drawable.fastforward, "2x");
                        }
                    } else {
                        if (!isSwipeActive && !is2xActive && distance > 80) {
                            isSwipeActive = true;
                            handleSwipeStart(event);
                        }
                    }

                    if (isSwipeActive) {
                        handleActionMove(event, popup);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    player.setPlaybackSpeed(1f);
                    if (isSwipeActive && horizontalSwipeEnabled) {
                        player.seekTo(newDuration);
                    }
                    isSwipeActive = false;
                    is2xActive = false;
                    verticleSwipeEnabled = false;
                    horizontalSwipeEnabled = false;
                    return true;

                default:
                    return false;
            }
        });
    }

    private void handleDoubleTap(MotionEvent e, Popup popup) {
        float x = e.getX();
        float width = playerView.getWidth();
        if (x > width * 0.35f && x < width * 0.65f) {
            if (player.isPlaying()) {
                player.pause();
                popup.showPopup(playerView, R.drawable.ic_round_pause, "");
            } else {
                player.play();
                popup.showPopup(playerView, R.drawable.iconoir_play_solid, "");
            }
        } else if (x < width / 2f) {
            player.seekTo(Math.max(0, player.getCurrentPosition() - 10000));
            popup.showPopup(playerView, R.drawable.back10, " 10s");
        } else {
            player.seekTo(player.getCurrentPosition() + 10000);
            popup.showPopup(playerView, R.drawable.next10, " 10s");
        }
    }

    private void handleSwipeStart(MotionEvent event) {
        initialX = (int) event.getX();
        initialY = (int) event.getY();
        baseBrightness = -1f;
        isLeftSide = event.getX() < playerView.getWidth() / 2f;
        currentBrightness = layout.screenBrightness;
        if (currentBrightness < 0) {
            currentBrightness = 0.5f;
        }
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        currentDuration = player.getCurrentPosition();
        newDuration = currentDuration;
        smoothedDx = 0f;
        smoothedDy = 0f;
        verticleSwipeEnabled = false;
        horizontalSwipeEnabled = false;
    }

    private void handleActionMove(MotionEvent event, Popup popup) {
        if (is2xActive) {
            return;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();
        float rawDx = x - initialX;
        float rawDy = initialY - y;

        if (!verticleSwipeEnabled && !horizontalSwipeEnabled) {
            if (Math.abs(rawDy) > 80 || Math.abs(rawDx) > 80) {
                if (Math.abs(rawDy) > Math.abs(rawDx)) {
                    verticleSwipeEnabled = true;
                    initialY = y;
                    initialX = x;
                    smoothedDy = 0f;
                    smoothedDx = 0f;
                } else {
                    horizontalSwipeEnabled = true;
                    initialY = y;
                    initialX = x;
                    smoothedDy = 0f;
                    smoothedDx = 0f;
                }
            }
        }

        if (verticleSwipeEnabled) {
            float targetDy = initialY - y;
            smoothedDy = smoothedDy + 0.25f * (targetDy - smoothedDy);
            float factor = smoothedDy / playerView.getHeight();
            if (isLeftSide) {
                float newBrightness = setBrightnessBySwipe(factor);
                popup.showPopup(playerView, R.drawable.brightness_icon, (int) (newBrightness * 100) + "%");
            } else {
                float newVolume = setVolumeBySwipe(factor);
                popup.showPopup(playerView, R.drawable.volume_icon, (int) newVolume + "%");
            }
        } else if (horizontalSwipeEnabled) {
            float targetDx = x - initialX;
            smoothedDx = smoothedDx + 0.25f * (targetDx - smoothedDx);
            float factor = smoothedDx / playerView.getWidth();
            long duration = setDurationBySwipe(factor);
            long change = duration - currentDuration;
            popup.showPopup(playerView, R.drawable.move_x, formatRelativeTime(change));
        }
    }

    private String formatRelativeTime(long change) {
        if (change == 0) {
            return "No Change";
        }
        String sign = change > 0 ? "+" : "-";
        long diffSec = Math.round(Math.abs(change) / 1000.0);
        long h = diffSec / 3600;
        long m = (diffSec % 3600) / 60;
        long s = diffSec % 60;
        StringBuilder sb = new StringBuilder();
        sb.append(sign).append(" ");
        if (h > 0) {
            sb.append(h).append("hr ");
        }
        if (m > 0) {
            sb.append(m).append("min ");
        }
        if (s > 0 || (h == 0 && m == 0)) {
            sb.append(s).append("sec");
        }
        return sb.toString().trim();
    }


    private float setVolumeBySwipe(float factor) {
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int newVolume = Math.max(0, Math.min(maxVolume, (int) (currentVolume + maxVolume * factor * 1.5f)));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
        return (newVolume * 100f / maxVolume);
    }

    private float setBrightnessBySwipe(float factor) {
        float newBrightness = Math.max(0.01f, Math.min(1.0f, currentBrightness + factor * 1.5f));
        layout.screenBrightness = newBrightness;
        activity.getWindow().setAttributes(layout);
        return newBrightness;
    }

    private long setDurationBySwipe(float factor) {
        long duration = player.getDuration();
        if (duration <= 0) {
            duration = 1;
        }
        long maxSeekDuration = Math.max(30000L, Math.min(duration / 2, 300000L));
        newDuration = Math.max(0, Math.min(duration, currentDuration + (long) (maxSeekDuration * factor * 1.5f)));
        return newDuration;
    }

    private void disableClipping(View view) {
        if (view == null) return;
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            viewGroup.setClipChildren(false);
            viewGroup.setClipToPadding(false);
        }
        android.view.ViewParent parent = view.getParent();
        if (parent instanceof View) {
            disableClipping((View) parent);
        }
    }
}
