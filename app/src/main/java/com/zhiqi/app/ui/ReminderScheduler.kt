package com.zhiqi.app.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zhiqi.app.MainActivity
import com.zhiqi.app.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderPrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun reminderTime(): String = prefs.getString(KEY_TIME, DEFAULT_TIME) ?: DEFAULT_TIME

    fun hideSensitiveWords(): Boolean = prefs.getBoolean(KEY_HIDE_SENSITIVE, true)

    fun save(enabled: Boolean, time: String) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_TIME, normalizeTime(time))
            .apply()
    }

    fun saveEnabled(enabled: Boolean) {
        save(enabled, reminderTime())
    }

    fun saveTime(time: String) {
        save(isEnabled(), time)
    }

    fun saveSensitiveHidden(hidden: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_SENSITIVE, hidden).apply()
    }

    private fun normalizeTime(time: String): String {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 21
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return String.format("%02d:%02d", hour, minute)
    }

    companion object {
        private const val PREFS_NAME = "zhiqi_reminder"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_TIME = "time"
        private const val KEY_HIDE_SENSITIVE = "hide_sensitive_words"
        private const val DEFAULT_TIME = "21:00"
    }
}

object ReminderScheduler {
    private const val WORK_NAME = "zhiqi_daily_reminder_work"
    private const val CHANNEL_ID = "zhiqi_daily_reminder_channel"
    private const val CHANNEL_NAME = "安全提醒"
    private const val NOTIFICATION_ID = 10086

    fun schedule(context: Context, reminderTime: String) {
        ensureChannel(context)
        val initialDelay = computeInitialDelayMillis(reminderTime)
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "每日安全提醒"
        }
        manager.createNotificationChannel(channel)
    }

    private fun computeInitialDelayMillis(reminderTime: String): Long {
        val parts = reminderTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 21
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(1_000L)
    }

    class DailyReminderWorker(
        appContext: Context,
        params: WorkerParameters
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            val prefs = ReminderPrefsManager(applicationContext)
            if (!prefs.isEnabled()) return Result.success()
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return Result.success()
            }

            ensureChannel(applicationContext)
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return Result.retry()

            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_default)
                .setContentTitle(if (prefs.hideSensitiveWords()) "知期 · 今日提醒" else "知期 · 安全提醒")
                .setContentText(
                    if (prefs.hideSensitiveWords()) {
                        "记得完成今天的健康记录。"
                    } else {
                        "记得记录今天的状态并做好防护。"
                    }
                )
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            manager.notify(NOTIFICATION_ID, notification)
            return Result.success()
        }
    }
}
