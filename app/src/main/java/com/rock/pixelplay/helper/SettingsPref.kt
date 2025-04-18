package com.rock.pixelplay.helper

import android.content.Context
import android.content.SharedPreferences

object SettingsPref {
    private const val PREF_NAME = "app_settings"
    private const val KEY_THEME = "theme"
    private const val KEY_SWIPE = "swipe_enabled"

    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getTheme(context: Context): Int =
        prefs(context).getInt(KEY_THEME, THEME_SYSTEM)

    fun setTheme(context: Context, theme: Int) {
        prefs(context).edit().putInt(KEY_THEME, theme).apply()
    }

    fun isSwipeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SWIPE, true)

    fun setSwipeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SWIPE, enabled).apply()
    }
}
