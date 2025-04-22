package com.rock.pixelplay.helper;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;


public class Loader {

    private Dialog loadingDialog;
    private boolean isLoading = false;
    private ObjectAnimator rotation;

    public Loader(@NonNull Context context, @DrawableRes int loaderImageRes) {
        // Initialize the dialog
        loadingDialog = new Dialog(context);
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setCancelable(false);
        //make this dialog not touchablke and non flocusable all ow back view to get focus
        Window window = loadingDialog.getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        loadingDialog.setContentView(createLoaderView(context, loaderImageRes));

    }

    private View createLoaderView(Context context, @DrawableRes int loaderImageRes) {
        // Create a parent LinearLayout to hold the loader
        LinearLayout loaderLayout = new LinearLayout(context);
        loaderLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        loaderLayout.setOrientation(LinearLayout.VERTICAL);

        // Create the ImageView for the loader
        ImageView loaderImageView = new ImageView(context);
        loaderImageView.setLayoutParams(new LinearLayout.LayoutParams(180, 180));
        loaderImageView.setImageResource(loaderImageRes);
        loaderImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        loaderImageView.setColorFilter(new PorterDuffColorFilter(android.graphics.Color.WHITE, PorterDuff.Mode.SRC_IN));


        rotation = ObjectAnimator.ofFloat(loaderImageView, "rotation", 0f, 360f);
        rotation.setDuration(2000); // Duration of one rotation in milliseconds
        rotation.setInterpolator(new LinearInterpolator());
        rotation.setRepeatCount(ObjectAnimator.INFINITE); // Infinite rotation


        // Add the loader image to the layout
        loaderLayout.addView(loaderImageView);
        return loaderLayout;
    }

    public void startLoading() {
        if (!isLoading) {
            loadingDialog.show();
            if(rotation!=null) rotation.start();
            isLoading = true;
        }
    }

    public void stopLoading() {
        if (isLoading) {
            if(rotation!=null) rotation.cancel();
            loadingDialog.dismiss();
            isLoading = false;
        }
    }

    public boolean isLoading() {
        return isLoading;
    }
}
