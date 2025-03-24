package com.rock.pixelplay.helper

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View

class AnimationUtils {
    fun translateY(view: View, i1: Int, i2: Int) {
        val valueAnimator = ValueAnimator.ofInt(i1, i2).apply {
            duration = 300
            addUpdateListener { v ->
                val value = v.animatedValue as Int
                view.translationY = value.toFloat()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator) {
                }

                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
        valueAnimator.start();
    }


    fun translateX(view: View, i1: Int, i2: Int) {
        val valueAnimator = ValueAnimator.ofInt(i1, i2).apply {
            duration = 300
            addUpdateListener { v ->
                val value = v.animatedValue as Int
                view.translationX = value.toFloat()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator) {

                }

                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
        valueAnimator.start();
    }



    fun fadeOut(view: View) {
        ValueAnimator.ofFloat(1F, 0F).apply {
            duration = 300
            addUpdateListener { v ->
                val value = v.animatedValue as Float
                view.alpha = value
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                }
            })
            start()
        }
    }

    fun fadeIn(view: View) {
        view.alpha = 0f
        view.visibility = View.VISIBLE

        ValueAnimator.ofFloat(0F, 1F).apply {
            duration = 300
            addUpdateListener { v ->
                val value = v.animatedValue as Float
                view.alpha = value
            }
            start()
        }
    }


}
