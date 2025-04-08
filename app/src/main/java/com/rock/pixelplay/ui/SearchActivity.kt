package com.rock.pixelplay.ui

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rock.pixelplay.R
import com.rock.pixelplay.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {
    private lateinit var lb: ActivitySearchBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lb = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(lb.getRoot())
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        lb.searchInput.requestFocus()
        inputMethodManager.showSoftInput(lb.searchInput, InputMethodManager.SHOW_IMPLICIT)
        lb.searchBox.post {
            val parentWidth = (lb.searchBox.parent as View).width

            val animator = ValueAnimator.ofInt(0, parentWidth)
            animator.duration = 300
            animator.addUpdateListener {
                val value = it.animatedValue as Int
                lb.searchBox.layoutParams.width = value
                lb.searchBox.requestLayout()
            }
            animator.start()
        }

    }

    override fun onBackPressed() {
        lb.searchBox.post {
            val parentWidth = (lb.searchBox.parent as View).width

            val animator = ValueAnimator.ofInt( parentWidth,0)
            animator.duration = 300
            animator.addUpdateListener {
                val value = it.animatedValue as Int
                lb.searchBox.layoutParams.width = value
                lb.searchBox.requestLayout()
                if (value <= 0) {
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            }

            animator.start()
        }
        super.onBackPressed()
    }
}