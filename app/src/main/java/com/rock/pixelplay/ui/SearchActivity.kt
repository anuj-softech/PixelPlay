package com.rock.pixelplay.ui

import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.rock.pixelplay.R
import com.rock.pixelplay.adapter.SearchAdapter
import com.rock.pixelplay.databinding.ActivitySearchBinding
import com.rock.pixelplay.helper.Loader
import com.rock.pixelplay.helper.VideoUtils
import java.util.Locale

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

        addSearchLogic();
        lb.back.setOnClickListener {
            finish()
        }

    }

    private fun addSearchLogic() {
        lb.searchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val query = lb.searchInput.text.toString().trim()
                if (query.isNotEmpty()) {
                    val loader : Loader = Loader(this,R.drawable.baseline_loop_24);
                    loader.startLoading();
                    val results = VideoUtils().searchVideos(this, query)
                    loader.stopLoading();
                    if(results.isNotEmpty()){
                        lb.searchPlaceholder.visibility = View.GONE
                        lb.searchRv.visibility = View.VISIBLE
                        lb.searchRv.layoutManager = LinearLayoutManager(this)
                        lb.searchRv.adapter = SearchAdapter(this, results)
                    }else{
                        lb.searchStatus.text = "No results found"
                        lb.searchPlaceholder.visibility = View.VISIBLE
                        lb.searchRv.visibility = View.GONE
                    }
                }
                true
            } else {
                false
            }
        }

        lb.micButton.setOnClickListener { v->
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak something...")
            }
            try {
                startActivityForResult(intent, 1001)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Speech not supported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            lb.searchInput.setText(result?.get(0) ?: "")
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