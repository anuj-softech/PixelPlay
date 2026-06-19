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

    fun getPlayerOrientation(context: Context): Int =
        prefs(context).getInt("player_orientation", -1)

    fun setPlayerOrientation(context: Context, orientation: Int) {
        prefs(context).edit().putInt("player_orientation", orientation).apply()
    }

    fun getWhisperModelPath(context: Context): String? =
        prefs(context).getString("whisper_model_path", null)

    fun setWhisperModelPath(context: Context, path: String?) {
        prefs(context).edit().putString("whisper_model_path", path).apply()
    }

    fun getWhisperAccuracyMode(context: Context): Int =
        prefs(context).getInt("whisper_accuracy_mode", 1)

    fun setWhisperAccuracyMode(context: Context, mode: Int) {
        prefs(context).edit().putInt("whisper_accuracy_mode", mode).apply()
    }

    fun isAiSubtitleEnabled(context: Context): Boolean =
        prefs(context).getBoolean("whisper_ai_subtitle_enabled", false)

    fun setAiSubtitleEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("whisper_ai_subtitle_enabled", enabled).apply()
    }
}
