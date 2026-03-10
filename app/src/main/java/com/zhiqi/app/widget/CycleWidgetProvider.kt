package com.zhiqi.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.zhiqi.app.MainActivity
import com.zhiqi.app.R
import com.zhiqi.app.ui.CycleSettingsManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CycleWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        refresh(context)
    }

    companion object {
        fun refresh(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CycleWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) return
            updateWidgets(context, appWidgetManager, appWidgetIds)
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val state = buildCycleWidgetState(CycleSettingsManager(context.applicationContext))
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            appWidgetIds.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_cycle).apply {
                    setTextViewText(R.id.widget_title, state.title)
                    setTextViewText(R.id.widget_phase, state.phase)
                    setTextViewText(R.id.widget_tip, state.tip)
                    setTextColor(R.id.widget_brand, state.theme.brandColor)
                    setTextColor(R.id.widget_title, state.theme.titleColor)
                    setTextColor(R.id.widget_phase, state.theme.phaseColor)
                    setTextColor(R.id.widget_tip, state.theme.tipColor)
                    setInt(R.id.widget_accent_bar, "setBackgroundColor", state.theme.accentColor)
                    setInt(R.id.widget_title_badge_bg, "setColorFilter", state.theme.badgeBgColor)
                    setInt(R.id.widget_flower_shell, "setColorFilter", state.theme.flowerShellColor)
                    setImageViewResource(R.id.widget_flower, state.theme.flowerResId)
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}

private data class CycleWidgetState(
    val title: String,
    val phase: String,
    val tip: String,
    val theme: CycleWidgetTheme
)

private data class CycleWidgetTheme(
    val accentColor: Int,
    val brandColor: Int,
    val titleColor: Int,
    val phaseColor: Int,
    val tipColor: Int,
    val badgeBgColor: Int,
    val flowerShellColor: Int,
    val flowerResId: Int
)

private enum class CycleStage {
    SETUP,
    INVALID,
    MENSTRUAL,
    OVERDUE,
    EXPECTED_START,
    OVULATION,
    FERTILE,
    FOLLICULAR,
    LUTEAL
}

private fun buildCycleWidgetState(manager: CycleSettingsManager): CycleWidgetState {
    if (!manager.isConfigured() || manager.lastPeriodStartMillis() <= 0L) {
        return CycleWidgetState(
            title = "周期建议",
            phase = "未设置生理周期",
            tip = "先在 App 设置最近一次经期开始日，小组件会每天给你不同提醒。",
            theme = themeFor(CycleStage.SETUP)
        )
    }

    val cycleLength = manager.cycleLengthDays()
    val periodLength = manager.periodLengthDays()
    val dayMillis = 24L * 60L * 60L * 1000L
    val todayStart = startOfDay(System.currentTimeMillis())
    val anchorStart = startOfDay(manager.lastPeriodStartMillis())
    if (anchorStart > todayStart) {
        return CycleWidgetState(
            title = "周期建议",
            phase = "周期设置异常",
            tip = "最近一次经期开始日不能晚于今天，请进入 App 重新设置。",
            theme = themeFor(CycleStage.INVALID)
        )
    }

    val daysSinceLastStart = ((todayStart - anchorStart) / dayMillis).toInt()
    val expectedNextStart = anchorStart + cycleLength * dayMillis
    val ovulationDay = (cycleLength - 14).coerceAtLeast(periodLength + 1).coerceAtMost(cycleLength - 1)
    val ovulationOffset = ovulationDay - 1
    val fertileStartOffset = (ovulationOffset - 5).coerceAtLeast(periodLength)
    val fertileEndOffset = (ovulationOffset + 1).coerceAtMost(cycleLength - 1)
    val overdueDays = (daysSinceLastStart - cycleLength).coerceAtLeast(0)

    return when {
        daysSinceLastStart in 0 until periodLength -> {
            val dayInPeriod = daysSinceLastStart + 1
            CycleWidgetState(
                title = "今日周期提醒",
                phase = "经期（第${dayInPeriod}天）",
                tip = "建议温和沟通，注意情绪安抚与休息。",
                theme = themeFor(CycleStage.MENSTRUAL)
            )
        }

        daysSinceLastStart > cycleLength -> CycleWidgetState(
            title = "今日周期提醒",
            phase = "月经推迟${overdueDays}天",
            tip = if (overdueDays >= 7) {
                "已经明显超出预计周期，若近期有行为记录，建议尽快验孕或关注身体变化。"
            } else {
                "月经已较预计开始日推迟，建议先观察并留意身体状态。"
            },
            theme = themeFor(CycleStage.OVERDUE)
        )

        daysSinceLastStart == cycleLength -> CycleWidgetState(
            title = "今日周期提醒",
            phase = "预计经期开始日",
            tip = "按设置周期推算，今天是下一次月经预计开始日。",
            theme = themeFor(CycleStage.EXPECTED_START)
        )

        daysSinceLastStart in fertileStartOffset..fertileEndOffset -> {
            val isOvulationDay = daysSinceLastStart == ovulationOffset
            CycleWidgetState(
                title = "今日周期提醒",
                phase = if (isOvulationDay) "排卵日" else "排卵期",
                tip = "当前受孕概率相对更高，建议重点关注避孕措施与身体变化。",
                theme = themeFor(if (isOvulationDay) CycleStage.OVULATION else CycleStage.FERTILE)
            )
        }

        daysSinceLastStart < fertileStartOffset -> CycleWidgetState(
            title = "今日周期提醒",
            phase = "卵泡期",
            tip = "预计排卵日约在${formatMonthDay(anchorStart + ovulationOffset * dayMillis)}。",
            theme = themeFor(CycleStage.FOLLICULAR)
        )

        else -> CycleWidgetState(
            title = "今日周期提醒",
            phase = "黄体期",
            tip = "当前已进入排卵后阶段，预计下次月经在${formatMonthDay(expectedNextStart)}开始。",
            theme = themeFor(CycleStage.LUTEAL)
        )
    }
}

private fun themeFor(stage: CycleStage): CycleWidgetTheme {
    return when (stage) {
        CycleStage.SETUP -> CycleWidgetTheme(
            accentColor = color("#FFC4A4B6"),
            brandColor = color("#FF8A6D7D"),
            titleColor = color("#FF7B5D6D"),
            phaseColor = color("#FF4E3A47"),
            tipColor = color("#FF6B5662"),
            badgeBgColor = color("#FFF3EAF0"),
            flowerShellColor = color("#FFF6EAF0"),
            flowerResId = R.drawable.ic_widget_flower_setup
        )

        CycleStage.INVALID -> CycleWidgetTheme(
            accentColor = color("#FFE08D62"),
            brandColor = color("#FF91614B"),
            titleColor = color("#FF8A5B44"),
            phaseColor = color("#FF5E3E30"),
            tipColor = color("#FF735244"),
            badgeBgColor = color("#FFFDEFE6"),
            flowerShellColor = color("#FFFDEFE7"),
            flowerResId = R.drawable.ic_widget_flower_overdue
        )

        CycleStage.MENSTRUAL -> CycleWidgetTheme(
            accentColor = color("#FFE56C98"),
            brandColor = color("#FF9C4F72"),
            titleColor = color("#FF8D4566"),
            phaseColor = color("#FFAE3969"),
            tipColor = color("#FF714C5E"),
            badgeBgColor = color("#FFFCE7EF"),
            flowerShellColor = color("#FFFDE7F0"),
            flowerResId = R.drawable.ic_widget_flower_menstrual
        )

        CycleStage.OVERDUE -> CycleWidgetTheme(
            accentColor = color("#FFAE88C5"),
            brandColor = color("#FF6F6389"),
            titleColor = color("#FF675C83"),
            phaseColor = color("#FF695F86"),
            tipColor = color("#FF5E5A73"),
            badgeBgColor = color("#FFEDEAF8"),
            flowerShellColor = color("#FFEFEAF9"),
            flowerResId = R.drawable.ic_widget_flower_overdue
        )

        CycleStage.EXPECTED_START -> CycleWidgetTheme(
            accentColor = color("#FFEA7BA2"),
            brandColor = color("#FF936177"),
            titleColor = color("#FF8A596F"),
            phaseColor = color("#FFB14572"),
            tipColor = color("#FF735262"),
            badgeBgColor = color("#FFFDE8F0"),
            flowerShellColor = color("#FFFEEAF2"),
            flowerResId = R.drawable.ic_widget_flower_expected
        )

        CycleStage.OVULATION -> CycleWidgetTheme(
            accentColor = color("#FFE9B15B"),
            brandColor = color("#FF95743C"),
            titleColor = color("#FF8A6C38"),
            phaseColor = color("#FFC2822D"),
            tipColor = color("#FF796343"),
            badgeBgColor = color("#FFFFF3E1"),
            flowerShellColor = color("#FFFFF5E5"),
            flowerResId = R.drawable.ic_widget_flower_ovulation
        )

        CycleStage.FERTILE -> CycleWidgetTheme(
            accentColor = color("#FF9A7AED"),
            brandColor = color("#FF6D5BA3"),
            titleColor = color("#FF65569A"),
            phaseColor = color("#FF684FBF"),
            tipColor = color("#FF61587D"),
            badgeBgColor = color("#FFF1ECFF"),
            flowerShellColor = color("#FFF3EEFF"),
            flowerResId = R.drawable.ic_widget_flower_fertile
        )

        CycleStage.FOLLICULAR -> CycleWidgetTheme(
            accentColor = color("#FF5CB37E"),
            brandColor = color("#FF4E7D5E"),
            titleColor = color("#FF4A7859"),
            phaseColor = color("#FF2D8B57"),
            tipColor = color("#FF4A6857"),
            badgeBgColor = color("#FFE8F8ED"),
            flowerShellColor = color("#FFE9F9EF"),
            flowerResId = R.drawable.ic_widget_flower_follicular
        )

        CycleStage.LUTEAL -> CycleWidgetTheme(
            accentColor = color("#FFF09A68"),
            brandColor = color("#FF8D6246"),
            titleColor = color("#FF845C42"),
            phaseColor = color("#FFC47440"),
            tipColor = color("#FF74563E"),
            badgeBgColor = color("#FFFFF0E8"),
            flowerShellColor = color("#FFFFF1E9"),
            flowerResId = R.drawable.ic_widget_flower_luteal
        )
    }
}

private fun color(value: String): Int = Color.parseColor(value)

private fun startOfDay(timeMillis: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

private fun formatMonthDay(timeMillis: Long): String {
    return SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(timeMillis))
}
