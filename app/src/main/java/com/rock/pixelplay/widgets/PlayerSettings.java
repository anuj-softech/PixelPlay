package com.rock.pixelplay.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rock.pixelplay.databinding.PlayerSettingsBinding;

public class PlayerSettings extends LinearLayout {

    private @NonNull PlayerSettingsBinding lb;
    private OnProgressChangeListener progressChangeListener;
    private OnCommandListener commandListener;

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

    public void setProgressChangeListener(OnProgressChangeListener listener) {
        this.progressChangeListener = listener;
    }
    public void setCommandListener(OnCommandListener listener) {
        this.commandListener = listener;
    }

    private void init() {
        lb = PlayerSettingsBinding.inflate(LayoutInflater.from(this.getContext()), this, true);

        lb.nightModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (commandListener != null) commandListener.onNightMode(isChecked);
        });
        lb.speedSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (progressChangeListener != null) progressChangeListener.onProgressChange(value);
            lb.speedtxt.setText(String.format("%.1fx", value));
        });
        lb.closeDialog.setOnClickListener(v -> {
            if (commandListener != null) commandListener.onClose();
            Log.e("close", "close");
        });

    }

    public @NonNull PlayerSettingsBinding getLB() {
        return lb;
    }

    public interface OnProgressChangeListener {
        void onProgressChange(float progress);
    }
    public interface OnCommandListener {
        void onNightMode(boolean enable);
        void onClose();
    }
}
