package com.ganlema.app.ui

import android.content.Context

class CycleSettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("cycle_settings", Context.MODE_PRIVATE)

    fun isConfigured(): Boolean = prefs.getBoolean(KEY_CONFIGURED, false)
    fun cycleLengthDays(): Int = prefs.getInt(KEY_CYCLE_LENGTH, 28)
    fun periodLengthDays(): Int = prefs.getInt(KEY_PERIOD_LENGTH, 5)
    fun lastPeriodStartMillis(): Long = prefs.getLong(KEY_LAST_PERIOD_START, 0L)

    fun setCycleLengthDays(value: Int) {
        prefs.edit().putInt(KEY_CYCLE_LENGTH, value.coerceIn(21, 45)).apply()
    }

    fun setPeriodLengthDays(value: Int) {
        prefs.edit().putInt(KEY_PERIOD_LENGTH, value.coerceIn(2, 10)).apply()
    }

    fun setLastPeriodStartMillis(value: Long) {
        prefs.edit().putLong(KEY_LAST_PERIOD_START, value).apply()
    }

    fun saveAll(cycleLengthDays: Int, periodLengthDays: Int, lastPeriodStartMillis: Long) {
        prefs.edit()
            .putInt(KEY_CYCLE_LENGTH, cycleLengthDays.coerceIn(21, 45))
            .putInt(KEY_PERIOD_LENGTH, periodLengthDays.coerceIn(2, 10))
            .putLong(KEY_LAST_PERIOD_START, lastPeriodStartMillis)
            .putBoolean(KEY_CONFIGURED, true)
            .apply()
    }

    companion object {
        private const val KEY_CONFIGURED = "configured"
        private const val KEY_CYCLE_LENGTH = "cycle_length"
        private const val KEY_PERIOD_LENGTH = "period_length"
        private const val KEY_LAST_PERIOD_START = "last_period_start"
    }
}
