package com.gameclock.referee

import android.content.Context

object GameClockPrefs {
    private const val PREFS_NAME = "gameclock"
    private const val KEY_QUARTER_SEC = "quarter_duration_sec"

    private const val DEFAULT_QUARTER_SEC = 15 * 60
    private const val MIN_QUARTER_SEC = 10
    private const val MAX_QUARTER_SEC = 99 * 60 + 59

    fun getQuarterDurationSec(context: Context): Int {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getInt(KEY_QUARTER_SEC, DEFAULT_QUARTER_SEC).coerceIn(MIN_QUARTER_SEC, MAX_QUARTER_SEC)
    }

    fun setQuarterDurationSec(context: Context, seconds: Int) {
        val v = seconds.coerceIn(MIN_QUARTER_SEC, MAX_QUARTER_SEC)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_QUARTER_SEC, v)
            .apply()
    }
}
