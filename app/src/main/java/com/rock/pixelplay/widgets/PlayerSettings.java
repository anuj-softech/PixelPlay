package com.rock.pixelplay.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rock.pixelplay.databinding.PlayerSettingsBinding;
import com.rock.pixelplay.player.PlayerSettingConfig;
import com.rock.pixelplay.player.PlayerSettingsListener;

public class PlayerSettings extends LinearLayout {

    private @NonNull PlayerSettingsBinding lb;
    private PlayerSettingConfig config;
    private PlayerSettingsListener listener;

    public PlayerSettings(Context context) {
        super(context);
        init();
    }

    public PlayerSettings(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PlayerSettings(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public PlayerSettings(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void configure(PlayerSettingConfig config, PlayerSettingsListener listener) {
        this.config = config;
        this.listener = listener;
        if (config != null) {
            lb.nightModeSwitch.setChecked(config.nightMode);
            lb.aiSubtitleSwitch.setChecked(config.aiSubtitles);
            lb.speedSlider.setValue(config.playbackSpeed);
            lb.speedtxt.setText(String.format("%.1fx", config.playbackSpeed));
        }
    }

    private void init() {
        lb = PlayerSettingsBinding.inflate(LayoutInflater.from(this.getContext()), this, true);

        lb.nightModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (config != null) {
                config.nightMode = isChecked;
                if (listener != null) {
                    listener.onSettingsChanged(config);
                }
            }
        });
        lb.aiSubtitleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (config != null) {
                config.aiSubtitles = isChecked;
                if (listener != null) {
                    listener.onSettingsChanged(config);
                }
            }
        });
        lb.speedSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (config != null) {
                config.playbackSpeed = value;
                if (listener != null) {
                    listener.onSettingsChanged(config);
                }
            }
            lb.speedtxt.setText(String.format("%.1fx", value));
        });
        lb.closeDialog.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClose();
            }
        });
    }

    public @NonNull PlayerSettingsBinding getLB() {
        return lb;
    }
}
