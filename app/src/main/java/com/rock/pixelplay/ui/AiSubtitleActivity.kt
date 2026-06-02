package com.rock.pixelplay.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rock.pixelplay.R
import com.rock.pixelplay.ai.voice.ModelsManager
import com.rock.pixelplay.databinding.ActivityAiSubtitleBinding
import com.rock.pixelplay.helper.SettingsPref

class AiSubtitleActivity : AppCompatActivity() {
    private lateinit var lb: ActivityAiSubtitleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lb = ActivityAiSubtitleBinding.inflate(layoutInflater)
        setContentView(lb.root)

        ViewCompat.setOnApplyWindowInsetsListener(lb.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        lb.back.setOnClickListener {
            finish()
        }

        updateModelStatus()

        lb.actionButton.setOnClickListener {
            if (!ModelsManager.isModelDownloaded(this)) {
                startModelDownload()
            }
        }

        lb.deleteButton.setOnClickListener {
            ModelsManager.deleteModel(this)
            SettingsPref.setWhisperModelPath(this, null)
            updateModelStatus()
        }

        val currentAccuracy = SettingsPref.getWhisperAccuracyMode(this)
        lb.accuracySlider.progress = currentAccuracy
        updateAccuracyDesc(currentAccuracy)

        lb.accuracySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                SettingsPref.setWhisperAccuracyMode(this@AiSubtitleActivity, progress)
                updateAccuracyDesc(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateModelStatus() {
        val isDownloaded = ModelsManager.isModelDownloaded(this)
        val tempFile = java.io.File(filesDir, "${ModelsManager.MODEL_FILENAME}.tmp")
        val hasPartialDownload = tempFile.exists() && tempFile.length() > 0

        if (isDownloaded) {
            lb.deleteButton.visibility = View.VISIBLE
            lb.actionButton.visibility = View.GONE
            lb.modelStatusBadge.text = "Downloaded"
            lb.modelStatusBadge.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary_500))
        } else {
            lb.deleteButton.visibility = View.GONE
            lb.actionButton.visibility = View.VISIBLE
            lb.actionButton.text = if (hasPartialDownload) "Resume" else "Download"
            lb.actionButton.isEnabled = true
            lb.actionButton.alpha = 1.0f
            lb.modelStatusBadge.text = if (hasPartialDownload) "Paused" else "Not Downloaded"
            lb.modelStatusBadge.backgroundTintList = ColorStateList.valueOf(getColor(android.R.color.darker_gray))
        }
        lb.downloadProgressContainer.visibility = View.GONE
    }

    private fun startModelDownload() {
        lb.actionButton.isEnabled = false
        lb.actionButton.alpha = 0.5f
        lb.deleteButton.visibility = View.GONE
        lb.downloadProgressContainer.visibility = View.VISIBLE
        lb.modelStatusBadge.text = "Downloading..."
        lb.modelStatusBadge.backgroundTintList = ColorStateList.valueOf(getColor(R.color.secondary_500))

        ModelsManager.downloadModel(this, object : ModelsManager.DownloadCallback {
            override fun onProgress(downloaded: Long, total: Long) {
                runOnUiThread {
                    val downloadedMb = downloaded.toFloat() / (1024 * 1024)
                    val totalMb = total.toFloat() / (1024 * 1024)
                    val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0

                    lb.downloadProgress.progress = progress
                    lb.downloadPercent.text = "$progress%"
                    lb.downloadSizeText.text = String.format("%.1f MB / %.1f MB", downloadedMb, totalMb)
                }
            }

            override fun onComplete(file: java.io.File) {
                runOnUiThread {
                    SettingsPref.setWhisperModelPath(this@AiSubtitleActivity, file.absolutePath)
                    updateModelStatus()
                    Toast.makeText(this@AiSubtitleActivity, "Model downloaded successfully!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(e: Exception) {
                runOnUiThread {
                    updateModelStatus()
                    Toast.makeText(this@AiSubtitleActivity, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun updateAccuracyDesc(progress: Int) {
        lb.accuracyDesc.text = when (progress) {
            0 -> "Speed Optimized (Uses 1 CPU thread)"
            1 -> "Balanced Mode (Uses 2 CPU threads)"
            2 -> "Accuracy Optimized (Uses 4 CPU threads)"
            else -> ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ModelsManager.cancelDownload()
    }
}
