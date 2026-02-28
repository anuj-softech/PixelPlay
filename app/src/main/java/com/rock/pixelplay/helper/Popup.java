package com.rock.pixelplay.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.rock.pixelplay.R;

public class Popup {

    private final PopupWindow popupWindow;
    private final View popupView;
    private ImageView popupImageView;
    private TextView popupText;
    private final Handler handler = new Handler();
    private boolean isShowing = false;

    private final Runnable dismissRunnable = this::stopPopup;

    @SuppressLint("InflateParams")
    public Popup(@NonNull Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        popupView = createLoaderView(context);
        popupWindow = new PopupWindow(popupView,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(false);
        popupWindow.setTouchable(false);
        popupWindow.setFocusable(false);
    }

    private View createLoaderView(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(50, 50, 50, 50);
        layout.setBackground(context.getDrawable(R.drawable.round_bg));
        layout.setGravity(Gravity.CENTER_VERTICAL);

        popupImageView = new ImageView(context);
        popupImageView.setLayoutParams(new LinearLayout.LayoutParams(40, 40));
        popupImageView.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        layout.addView(popupImageView);

        popupText = new TextView(context);
        popupText.setTextColor(Color.WHITE);
        popupText.setTextSize(20);
        layout.addView(popupText);

        return layout;
    }

    public void showPopup(@NonNull View anchor, @DrawableRes int loaderImageRes, @NonNull String text) {
        popupImageView.setImageResource(loaderImageRes);
        popupText.setText(text);

        if (!isShowing) {
            popupWindow.showAtLocation(anchor, Gravity.CENTER, 0, -50);
            isShowing = true;
        }

        handler.removeCallbacks(dismissRunnable);
        handler.postDelayed(dismissRunnable, 800);
    }

    public void stopPopup() {
        if (isShowing) {
            popupWindow.dismiss();
            isShowing = false;
        }
    }

    public boolean isShowing() {
        return isShowing;
    }
}
