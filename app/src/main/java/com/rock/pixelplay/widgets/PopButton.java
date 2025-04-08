package com.rock.pixelplay.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class PopButton extends ImageButton {
    private OnClickListener clickListener;

    public PopButton(Context context) {
        super(context);
        init();
    }

    public PopButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PopButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public PopButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();

    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        clickListener = l;
    }

    private void init() {
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    animate().scaleX(0.95F).scaleY(0.95F).setDuration(100).start();
                }else if (event.getAction() == MotionEvent.ACTION_UP) {
                    animate().scaleX(1F).scaleY(1F).setDuration(100).start();
                    if (clickListener != null) {
                        clickListener.onClick(v);
                    }
                }
                return true;
            }
        });
    }
}
