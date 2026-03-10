package com.zhiqi.app.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked = _isUnlocked.asStateFlow()

    fun unlock() {
        _isUnlocked.value = true
        prefs.edit().putLong(KEY_LAST_UNLOCK, System.currentTimeMillis()).apply()
    }

    fun lock() {
        _isUnlocked.value = false
    }

    fun onAppBackgrounded() {
        prefs.edit().putLong(KEY_LAST_BG, System.currentTimeMillis()).apply()
    }

    fun onAppForegrounded() {
        val lastBg = prefs.getLong(KEY_LAST_BG, 0L)
        if (lastBg == 0L) return
        val elapsed = System.currentTimeMillis() - lastBg
        if (elapsed > LOCK_TIMEOUT_MS) {
            lock()
        }
    }

    companion object {
        private const val PREFS_NAME = "zhiqi_lock"
        private const val KEY_LAST_BG = "last_bg"
        private const val KEY_LAST_UNLOCK = "last_unlock"
        private const val LOCK_TIMEOUT_MS = 5 * 60 * 1000L
    }
}
