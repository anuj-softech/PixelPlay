package com.rock.pixelplay

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.rock.pixelplay.helper.DiskBitmapCache
import com.rock.pixelplay.helper.SettingsPref

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        val theme = SettingsPref.getTheme(this)
        DiskBitmapCache.init(applicationContext)
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                2 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
