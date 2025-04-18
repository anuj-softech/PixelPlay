package com.rock.pixelplay.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rock.pixelplay.databinding.ActivitySettingsBinding
import com.rock.pixelplay.helper.SettingsPref

class SettingsActivity : AppCompatActivity() {
    private lateinit var lb: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lb = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(lb.root)

        ViewCompat.setOnApplyWindowInsetsListener(lb.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        lb.back.setOnClickListener { finish() }

        val themeLabels = arrayOf("System Default", "Light", "Dark")
        val swipeLabels = arrayOf("Enabled", "Disabled")

        lb.themeSubtitle.text = themeLabels[SettingsPref.getTheme(this)]
        lb.swipeSubtitle.text = swipeLabels[if (SettingsPref.isSwipeEnabled(this)) 0 else 1]

        lb.themeSetting.setOnClickListener {
            val current = SettingsPref.getTheme(this)
            MaterialAlertDialogBuilder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(themeLabels, current) { dialog, which ->
                    SettingsPref.setTheme(this, which)
                    lb.themeSubtitle.text = themeLabels[which]
                    dialog.dismiss()
                    AppCompatDelegate.setDefaultNightMode(
                        when (which) {
                            0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            1 -> AppCompatDelegate.MODE_NIGHT_NO
                            2 -> AppCompatDelegate.MODE_NIGHT_YES
                            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        }
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        lb.swipeGestureSetting.setOnClickListener {
            val current = if (SettingsPref.isSwipeEnabled(this)) 0 else 1
            MaterialAlertDialogBuilder(this)
                .setTitle("Swipe Gestures")
                .setSingleChoiceItems(swipeLabels, current) { dialog, which ->
                    val enabled = which == 0
                    SettingsPref.setSwipeEnabled(this, enabled)
                    lb.swipeSubtitle.text = swipeLabels[which]
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
