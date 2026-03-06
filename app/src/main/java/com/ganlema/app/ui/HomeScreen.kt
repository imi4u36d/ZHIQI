package com.ganlema.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ganlema.app.data.DailyIndicatorEntity
import com.ganlema.app.data.DailyIndicatorRepository
import com.ganlema.app.data.RecordEntity
import com.ganlema.app.data.RecordRepository
import com.ganlema.app.security.PinManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    repository: RecordRepository,
    indicatorRepository: DailyIndicatorRepository,
    pinManager: PinManager,
    cycleSettingsVersion: Int,
    filterState: FilterState,
    onAddRecord: (String?) -> Unit,
    onOpenInsights: () -> Unit,
    onOpenCycleSettings: () -> Unit
) {
    val context = LocalContext.current
    val allRecords by repository.records().collectAsState(initial = emptyList())
    val visibleRecords = if (pinManager.isHidden()) emptyList() else allRecords
    val records = applyFilters(visibleRecords, filterState)
    val todayIndicators by indicatorRepository.indicatorsByDate(dayKey(System.currentTimeMillis()))
        .collectAsState(initial = emptyList())
    val recentLoveRecord = remember(records.size) {
        val monthAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        records.firstOrNull { it.type == "同房" && it.timeMillis >= monthAgo }
    }
    val lovePrefs = remember { context.getSharedPreferences("home_privacy", android.content.Context.MODE_PRIVATE) }
    var hideLoveCard by remember { mutableStateOf(lovePrefs.getBoolean("hide_love_card", false)) }

    val cycleManager = remember { CycleSettingsManager(context) }
    var cycleRefreshToken by remember { mutableIntStateOf(0) }
    val cycleTip = remember(cycleSettingsVersion, cycleRefreshToken, records.size) { buildCycleTip(cycleManager) }

    GlassBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CycleHeroCard(
                cycleTip = cycleTip,
                onPrimaryClick = {
                    if (cycleTip.needSetup) onOpenCycleSettings() else onOpenInsights()
                }
            )
            if (!cycleTip.overdueWarning.isNullOrBlank()) {
                OverdueWarningCard(text = cycleTip.overdueWarning)
            }
            CycleQuickActionCard(
                cycleTip = cycleTip,
                onRecordFlow = { onAddRecord("流量") },
                onCycleStarted = {
                    if (cycleManager.isConfigured()) {
                        cycleManager.setLastPeriodStartMillis(System.currentTimeMillis())
                        cycleRefreshToken += 1
                        onAddRecord("流量")
                    } else {
                        onOpenCycleSettings()
                    }
                },
                onCycleNotStarted = { cycleRefreshToken += 1 }
            )
            if (todayIndicators.isNotEmpty()) {
                TodayIndicatorCard(indicators = todayIndicators)
            }
            if (recentLoveRecord != null) {
                LoveRecordCard(
                    record = recentLoveRecord,
                    hidden = hideLoveCard,
                    onToggleHidden = {
                        hideLoveCard = !hideLoveCard
                        lovePrefs.edit().putBoolean("hide_love_card", hideLoveCard).apply()
                    }
                )
            }
        }
    }
}

@Composable
private fun TodayIndicatorCard(indicators: List<DailyIndicatorEntity>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("今日已记录", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            indicators.take(4).forEach { indicator ->
                val shape = RoundedCornerShape(18.dp)
                val accent = metricAccent(indicator.metricKey)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(accent.copy(alpha = 0.12f), shape)
                        .border(1.dp, accent.copy(alpha = 0.2f), shape)
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(metricTitle(indicator.metricKey), color = accent, style = MaterialTheme.typography.labelMedium)
                    Text(indicator.displayLabel, color = Color(0xFF2F3440), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun LoveRecordCard(
    record: RecordEntity,
    hidden: Boolean,
    onToggleHidden: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(0xFFFCEAF3), CircleShape)
                    .noRippleClickable(onToggleHidden),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = "切换显示",
                    tint = Color(0xFFD66A9A),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (!hidden) {
            Text("爱爱记录", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))
            Text(
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.timeMillis)),
                color = Color(0xFF8E95A0),
                style = MaterialTheme.typography.bodySmall
            )
            if (record.protections.isNotBlank()) {
                Text(
                    "措施：${record.protections.replace("|", " / ")}",
                    color = Color(0xFF5D6573),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!record.note.isNullOrBlank()) {
                Text("备注：${record.note}", color = Color(0xFF5D6573), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CycleHeroCard(
    cycleTip: CycleTip,
    onPrimaryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFFF47BA8), Color(0xFFEF4F90))
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = cycleTip.heroTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = cycleTip.heroSubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFFFE8F1)
            )
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFD6E5), RoundedCornerShape(22.dp))
                    .noRippleClickable(onPrimaryClick)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (cycleTip.needSetup) "去设置" else "查看详情",
                    color = Color(0xFFD85D90),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Box(
            modifier = Modifier
                .size(128.dp)
                .background(Color(0xFFFFD7E7), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "怀孕几率",
                    color = Color(0xFFB84876),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = cycleTip.pregnancyChance,
                    color = Color(0xFFB84876),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OverdueWarningCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(text = text, color = Color(0xFFD96582), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CycleQuickActionCard(
    cycleTip: CycleTip,
    onRecordFlow: () -> Unit,
    onCycleStarted: () -> Unit,
    onCycleNotStarted: () -> Unit
) {
    if (cycleTip.isMenstrualNow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFFFFEEF4), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "流量",
                        tint = Color(0xFFEF5D95)
                    )
                }
                Column {
                    Text("流量", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))
                    Text("记录今天的经量变化", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A8F98))
                }
            }

            QuickPrimaryButton(text = "记录", onClick = onRecordFlow)
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("是否来月经", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))
                Text(
                    text = if (cycleTip.needSetup) "先确认今天是否为经期开始日" else "如已开始，可立即切换到经期记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A8F98)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickSecondaryButton(text = "否", onClick = onCycleNotStarted)
                QuickPrimaryButton(text = "是", onClick = onCycleStarted)
            }
        }
    }
}

@Composable
private fun QuickPrimaryButton(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color(0xFFEF5D95), RoundedCornerShape(22.dp))
            .noRippleClickable(onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Text(text = text, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun QuickSecondaryButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(Color(0xFFFFEEF4), RoundedCornerShape(22.dp))
            .noRippleClickable(onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color(0xFFD66A9A), style = MaterialTheme.typography.titleMedium)
    }
}

private data class CycleTip(
    val title: String,
    val phase: String,
    val tip: String,
    val needSetup: Boolean,
    val isMenstrualNow: Boolean,
    val overdueWarning: String? = null,
    val heroTitle: String,
    val heroSubtitle: String,
    val pregnancyChance: String
)

private fun buildCycleTip(manager: CycleSettingsManager): CycleTip {
    if (!manager.isConfigured() || manager.lastPeriodStartMillis() <= 0L) {
        return CycleTip(
            title = "周期建议",
            phase = "未设置生理周期",
            tip = "设置周期后可获得安全期、排卵期、黄体期建议和怀孕概率提示。",
            needSetup = true,
            isMenstrualNow = false,
            overdueWarning = null,
            heroTitle = "记录周期",
            heroSubtitle = "设置最近一次经期开始日，首页会自动展示经期状态和提醒。",
            pregnancyChance = "--"
        )
    }

    val cycleLength = manager.cycleLengthDays()
    val periodLength = manager.periodLengthDays()
    val dayMillis = 24L * 60 * 60 * 1000
    val todayStart = startOfDay(System.currentTimeMillis())
    val lastStart = startOfDay(manager.lastPeriodStartMillis())

    if (lastStart > todayStart) {
        return CycleTip(
            title = "周期建议",
            phase = "周期设置异常",
            tip = "最近一次经期开始日不能晚于今天，请重新设置。",
            needSetup = true,
            isMenstrualNow = false,
            overdueWarning = null,
            heroTitle = "请修正周期日期",
            heroSubtitle = "当前设置的开始日比今天更晚，无法推断周期状态。",
            pregnancyChance = "--"
        )
    }

    val daysSinceLastStart = ((todayStart - lastStart) / dayMillis).toInt()
    val expectedNextStart = lastStart + cycleLength * dayMillis
    val expectedPeriodEnd = lastStart + (periodLength - 1L) * dayMillis
    val ovulationDay = (cycleLength - 14).coerceAtLeast(periodLength + 1).coerceAtMost(cycleLength - 1)
    val ovulationOffset = ovulationDay - 1
    val fertileStartOffset = (ovulationOffset - 5).coerceAtLeast(periodLength)
    val fertileEndOffset = (ovulationOffset + 1).coerceAtMost(cycleLength - 1)
    val overdueDays = (daysSinceLastStart - cycleLength).coerceAtLeast(0)

    return when {
        daysSinceLastStart < periodLength -> {
            val dayInPeriod = daysSinceLastStart + 1
            CycleTip(
                title = "今日周期提醒",
                phase = "经期（第${dayInPeriod}天）",
                tip = "建议温和沟通，注意情绪安抚与休息。",
                needSetup = false,
                isMenstrualNow = true,
                overdueWarning = null,
                heroTitle = "经期第${dayInPeriod}天",
                heroSubtitle = "当前处于经期，预计本次经期在${formatMonthDay(expectedPeriodEnd)}结束。",
                pregnancyChance = "3.2%"
            )
        }

        daysSinceLastStart > cycleLength -> CycleTip(
            title = "今日周期提醒",
            phase = "月经推迟${overdueDays}天",
            tip = if (overdueDays >= 7) {
                "已经明显超出预计周期，若近期有行为记录，建议尽快验孕或关注身体变化。"
            } else {
                "月经已较预计开始日推迟，建议先观察并留意身体状态。"
            },
            needSetup = false,
            isMenstrualNow = false,
            overdueWarning = "距离上次月经开始已${daysSinceLastStart}天，已超过预计${overdueDays}天。",
            heroTitle = "距离上次开始已${daysSinceLastStart}天",
            heroSubtitle = "预计本次月经应在${formatMonthDay(expectedNextStart)}开始，目前处于超期状态。",
            pregnancyChance = if (overdueDays >= 7) "需排查" else "待观察"
        )

        daysSinceLastStart == cycleLength -> CycleTip(
            title = "今日周期提醒",
            phase = "预计经期开始日",
            tip = "按设置周期推算，今天是下一次月经的预计开始日，可留意是否来潮。",
            needSetup = false,
            isMenstrualNow = false,
            overdueWarning = null,
            heroTitle = "距离上次开始已${daysSinceLastStart}天",
            heroSubtitle = "今天是预计经期开始日，如未开始可继续观察1到2天。",
            pregnancyChance = "待观察"
        )

        daysSinceLastStart in fertileStartOffset..fertileEndOffset -> {
            val fertileDay = daysSinceLastStart + 1
            val isOvulationDay = daysSinceLastStart == ovulationOffset
            CycleTip(
                title = "今日周期提醒",
                phase = if (isOvulationDay) "排卵日" else "排卵期",
                tip = "当前受孕概率相对更高，建议重点关注避孕措施与身体变化。",
                needSetup = false,
                isMenstrualNow = false,
                overdueWarning = null,
                heroTitle = if (isOvulationDay) "排卵日" else "排卵期",
                heroSubtitle = if (isOvulationDay) {
                    "今天接近排卵高点，属于本周期的重点关注日。"
                } else {
                    "当前是易孕窗口期第${fertileDay - fertileStartOffset}天，建议重视防护。"
                },
                pregnancyChance = if (isOvulationDay) "高" else "较高"
            )
        }

        daysSinceLastStart < fertileStartOffset -> CycleTip(
            title = "今日周期提醒",
            phase = "卵泡期",
            tip = "身体状态通常更平稳，适合保持原有节奏和适度运动。",
            needSetup = false,
            isMenstrualNow = false,
            overdueWarning = null,
            heroTitle = "卵泡期",
            heroSubtitle = "预计排卵日约在${formatMonthDay(lastStart + ovulationOffset * dayMillis)}，当前仍在排卵前阶段。",
            pregnancyChance = "中低"
        )

        else -> CycleTip(
            title = "今日周期提醒",
            phase = "黄体期",
            tip = "可能更容易疲惫或情绪敏感，建议规律休息并减少高压安排。",
            needSetup = false,
            isMenstrualNow = false,
            overdueWarning = null,
            heroTitle = "黄体期",
            heroSubtitle = "当前已进入排卵后阶段，预计下次月经在${formatMonthDay(expectedNextStart)}开始。",
            pregnancyChance = "较低"
        )
    }
}

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

private fun dayKey(timeMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeMillis))
}

private fun applyFilters(records: List<RecordEntity>, state: FilterState): List<RecordEntity> {
    if (state.types.isEmpty() && state.protections.isEmpty()) return records
    return records.filter { record ->
        val typeOk = state.types.isEmpty() || state.types.contains(record.type)
        val protections = record.protections.split("|").filter { it.isNotBlank() }.toSet()
        val protectionOk = state.protections.isEmpty() || protections.any { state.protections.contains(it) }
        typeOk && protectionOk
    }
}
