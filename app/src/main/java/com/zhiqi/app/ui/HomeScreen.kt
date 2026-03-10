package com.zhiqi.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhiqi.app.data.DailyIndicatorEntity
import com.zhiqi.app.data.DailyIndicatorRepository
import com.zhiqi.app.data.RecordEntity
import com.zhiqi.app.data.RecordRepository
import com.zhiqi.app.security.PinManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PERIOD_STATUS_KEY = "月经状态"
private const val PERIOD_STARTED = "start"
private const val PERIOD_ENDED = "end"
private const val MAX_PERIOD_DAYS = 10

private enum class HomePeriodAction {
    START,
    END
}

private enum class HomeDayMarker {
    NONE,
    PREDICTED,
    ACTUAL
}

private data class HomeRingSegment(
    val label: String,
    val days: Int,
    val color: Color
)

private data class HomeCycleOverview(
    val configured: Boolean,
    val cycleLength: Int,
    val periodLength: Int,
    val daysToNext: Int,
    val expectedStartMillis: Long,
    val cycleDay: Int,
    val fertileWindowText: String,
    val phaseTitle: String,
    val phaseDescription: String,
    val ringSegments: List<HomeRingSegment>
)

private data class HomeLogEntry(
    val title: String,
    val value: String,
    val metricKey: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color
)

private data class HomeWeekDayUi(
    val dateMillis: Long,
    val dayNumber: Int,
    val isToday: Boolean,
    val marker: HomeDayMarker
)

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
    val allIndicators by indicatorRepository.allIndicators().collectAsState(initial = emptyList())
    val visibleRecords = if (pinManager.isHidden()) emptyList() else allRecords
    val records = applyFilters(visibleRecords, filterState)
    val todayIndicators by indicatorRepository.indicatorsByDate(dayKey(System.currentTimeMillis()))
        .collectAsState(initial = emptyList())

    val cycleManager = remember { CycleSettingsManager(context) }
    val cycleTip = remember(cycleSettingsVersion, records, allIndicators) {
        buildCycleTip(cycleManager, records, allIndicators)
    }
    val cycleOverview = remember(cycleSettingsVersion, allIndicators) {
        buildHomeCycleOverview(cycleManager, allIndicators)
    }
    val weekDays = remember(cycleSettingsVersion, allIndicators, cycleOverview.expectedStartMillis) {
        buildHomeWeekDays(cycleManager, allIndicators, cycleOverview)
    }
    val todayLogs = remember(todayIndicators) {
        buildTodayLogEntries(todayIndicators)
    }
    val todayText = remember {
        SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date())
    }

    GlassBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HomeGreetingHeader(
                    todayText = todayText,
                    onBellClick = onOpenInsights
                )
            }

            item {
                HomeCycleRingCard(
                    overview = cycleOverview,
                    onPrimaryClick = {
                        if (cycleOverview.configured) onOpenInsights() else onOpenCycleSettings()
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeInfoTipCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Favorite,
                        iconTint = Color(0xFFE876A6),
                        title = "易孕窗口",
                        text = if (cycleOverview.configured) {
                            "本周期窗口：${cycleOverview.fertileWindowText}"
                        } else {
                            "完成周期设置后会自动显示。"
                        }
                    )
                    HomeInfoTipCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Lightbulb,
                        iconTint = Color(0xFFE6B24F),
                        title = "健康提醒",
                        text = cycleTip.tip
                    )
                }
            }

            item {
                HomeTodayLogsCard(
                    entries = todayLogs,
                    onOpen = onOpenInsights,
                    onAddRecord = onAddRecord
                )
            }

            item {
                HomeWeekCalendarCard(
                    days = weekDays,
                    onOpen = onOpenInsights
                )
            }

            item {
                Box(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun HomeGreetingHeader(
    todayText: String,
    onBellClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "你好，欢迎回来",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF5A4A72),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "今天：$todayText",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF756A86)
            )
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color(0xFFEDE5F5), CircleShape)
                .clickable(onClick = onBellClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "提醒",
                tint = Color(0xFF9E86C5)
            )
        }
    }
}

@Composable
private fun HomeCycleRingCard(
    overview: HomeCycleOverview,
    onPrimaryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0x0FFFFFFF), Color(0x14E9DCEA), Color.Transparent)
                ),
                shape = RoundedCornerShape(30.dp)
            )
            .clickable(onClick = onPrimaryClick)
            .padding(top = 8.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(352.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ringStroke = 30.dp.toPx()
                val ringGap = 2.2f
                val diameter = size.minDimension - ringStroke - 8.dp.toPx()
                val topLeft = Offset(
                    x = (size.width - diameter) / 2f,
                    y = (size.height - diameter) / 2f
                )
                val arcSize = Size(diameter, diameter)

                drawCircle(
                    color = Color(0x22CCBDD3),
                    radius = diameter / 2f + ringStroke * 0.64f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )

                var startAngle = 145f
                val total = overview.cycleLength.coerceAtLeast(1)
                val availableSweep = 360f - ringGap * overview.ringSegments.size
                overview.ringSegments.forEach { segment ->
                    val sweep = availableSweep * (segment.days.toFloat() / total.toFloat())
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = ringStroke, cap = StrokeCap.Round)
                    )
                    startAngle += sweep + ringGap
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.InvertColors,
                        contentDescription = "今天",
                        tint = Color(0xFFE57EA8),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "今天",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6D6180)
                )
            }

            Column(
                modifier = Modifier
                    .size(236.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = Color(0x33B8A2C2),
                        spotColor = Color(0x33B8A2C2)
                    )
                    .background(Color(0xFFFDFBFE), CircleShape)
                    .border(1.dp, Color(0x26DCCEE0), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "下次月经",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF64577A),
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .padding(vertical = 8.dp)
                        .height(1.dp)
                        .background(Color(0x22A99CB8))
                )

                if (overview.configured) {
                    val delayDays = abs(overview.daysToNext)
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (overview.daysToNext >= 0) "还有" else "已推迟",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF6A5F7E)
                        )
                        Text(
                            text = delayDays.toString(),
                            fontSize = 64.sp,
                            lineHeight = 64.sp,
                            color = Color(0xFF5E4C7B),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "天",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF6A5F7E),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .padding(vertical = 8.dp)
                            .height(1.dp)
                            .background(Color(0x1AA99CB8))
                    )

                    Text(
                        text = "预计：${formatMonthDay(overview.expectedStartMillis)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF7A6E8E)
                    )
                } else {
                    Text(
                        text = "请先完成周期设置",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF6D6380)
                    )
                    Text(
                        text = "设置后可自动生成提醒",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9288A3)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
                    .background(Color(0x10FFFFFF), RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                overview.ringSegments.take(3).forEach { segment ->
                    Text(
                        text = segment.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFF7F3FB),
                        modifier = Modifier
                            .background(segment.color.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        if (overview.configured) {
            Text(
                text = "当前阶段：${overview.phaseTitle} · 周期第${overview.cycleDay}天",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7A6F8D)
            )
        }
    }
}

@Composable
private fun HomeInfoTipCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    text: String
) {
    Column(
        modifier = modifier
            .glassCard()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF5E4F75)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0x1DA89CB6))
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF6D6382)
        )
    }
}

@Composable
private fun HomeTodayLogsCard(
    entries: List<HomeLogEntry>,
    onOpen: () -> Unit,
    onAddRecord: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "今日记录",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF5F4F76)
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "查看",
                tint = Color(0xFFA596B7),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onOpen)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF7F2FA), RoundedCornerShape(16.dp))
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            entries.forEachIndexed { index, entry ->
                HomeLogChip(
                    entry = entry,
                    modifier = Modifier.weight(1f),
                    onClick = { onAddRecord(entry.metricKey) }
                )
                if (index != entries.lastIndex) {
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(Color(0x1FA79CB6))
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeLogChip(
    entry: HomeLogEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = entry.title,
            tint = entry.tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = " ${entry.title}：${entry.value}",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5F5575),
            maxLines = 1
        )
    }
}

@Composable
private fun HomeWeekCalendarCard(
    days: List<HomeWeekDayUi>,
    onOpen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "周期日历",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF5F4F76)
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "查看",
                tint = Color(0xFFA596B7),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onOpen)
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF7B708F),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEach { day ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .background(
                            color = if (day.isToday) Color(0xFFD4B8F2).copy(alpha = 0.34f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (day.isToday) "今天" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7553A8)
                    )
                    Text(
                        text = day.dayNumber.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (day.isToday) Color(0xFF6F4AA3) else Color(0xFF6E637F),
                        fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Medium
                    )
                    if (day.marker != HomeDayMarker.NONE) {
                        Icon(
                            imageVector = Icons.Filled.InvertColors,
                            contentDescription = "经期标记",
                            tint = if (day.marker == HomeDayMarker.ACTUAL) Color(0xFFE978A8) else Color(0xFFF1A7C7),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Box(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

private fun buildHomeCycleOverview(
    manager: CycleSettingsManager,
    indicators: List<DailyIndicatorEntity>
): HomeCycleOverview {
    val defaultSegments = listOf(
        HomeRingSegment(label = "经期", days = 5, color = Color(0xFFF178A9)),
        HomeRingSegment(label = "卵泡期", days = 9, color = Color(0xFFF594BD)),
        HomeRingSegment(label = "易孕窗", days = 6, color = Color(0xFF9BCE63)),
        HomeRingSegment(label = "排卵期", days = 8, color = Color(0xFFA88AE6))
    )

    if (!manager.isConfigured() || manager.lastPeriodStartMillis() <= 0L) {
        return HomeCycleOverview(
            configured = false,
            cycleLength = 28,
            periodLength = 5,
            daysToNext = 0,
            expectedStartMillis = 0L,
            cycleDay = 0,
            fertileWindowText = "未设置",
            phaseTitle = "未设置",
            phaseDescription = "请先完成周期设置",
            ringSegments = defaultSegments
        )
    }

    val dayMillis = 24L * 60L * 60L * 1000L
    val cycleLength = manager.cycleLengthDays().coerceAtLeast(21)
    val periodLength = manager.periodLengthDays().coerceIn(2, MAX_PERIOD_DAYS)
    val configuredStart = startOfDay(manager.lastPeriodStartMillis())
    val actualRanges = buildActualPeriodRanges(manager, indicators)
    val anchorStart = resolveAnchorStart(configuredStart, actualRanges)
    val todayStart = startOfDay(System.currentTimeMillis())
    val daysSince = ((todayStart - anchorStart) / dayMillis).toInt().coerceAtLeast(0)
    val cycleDay = (daysSince % cycleLength) + 1
    val expectedStart = anchorStart + cycleLength * dayMillis
    val daysToNext = ((expectedStart - todayStart) / dayMillis).toInt()

    val ovulationDay = (cycleLength - 14).coerceAtLeast(periodLength + 1).coerceAtMost(cycleLength - 1)
    val ovulationOffset = ovulationDay - 1
    val fertileStart = (ovulationOffset - 5).coerceAtLeast(periodLength)
    val fertileEnd = (ovulationOffset + 1).coerceAtMost(cycleLength - 1)

    val follicularDays = (fertileStart - periodLength).coerceAtLeast(1)
    val fertileDays = (fertileEnd - fertileStart + 1).coerceAtLeast(1)
    val lutealDays = (cycleLength - periodLength - follicularDays - fertileDays).coerceAtLeast(1)

    val phaseTitle = when {
        cycleDay <= periodLength -> "经期"
        cycleDay - 1 in fertileStart..fertileEnd -> if (cycleDay - 1 == ovulationOffset) "排卵日" else "易孕窗口"
        cycleDay - 1 < fertileStart -> "卵泡期"
        else -> "黄体期"
    }

    val phaseDescription = when (phaseTitle) {
        "经期" -> "注意休息，观察身体状态。"
        "排卵日" -> "今天接近排卵高点，注意防护。"
        "易孕窗口" -> "当前处于易孕窗口，建议更重视措施。"
        "卵泡期" -> "身体状态通常更平稳。"
        else -> "容易疲劳，建议规律作息。"
    }

    val fertileStartMillis = anchorStart + fertileStart * dayMillis
    val fertileEndMillis = anchorStart + fertileEnd * dayMillis
    val fertileWindowText = "${formatMonthDay(fertileStartMillis)} - ${formatMonthDay(fertileEndMillis)}"

    val segments = listOf(
        HomeRingSegment(label = "经期", days = periodLength, color = Color(0xFFF178A9)),
        HomeRingSegment(label = "卵泡期", days = follicularDays, color = Color(0xFFF594BD)),
        HomeRingSegment(label = "易孕窗", days = fertileDays, color = Color(0xFF9BCE63)),
        HomeRingSegment(label = "排卵期", days = lutealDays, color = Color(0xFFA88AE6))
    )

    return HomeCycleOverview(
        configured = true,
        cycleLength = cycleLength,
        periodLength = periodLength,
        daysToNext = daysToNext,
        expectedStartMillis = expectedStart,
        cycleDay = cycleDay,
        fertileWindowText = fertileWindowText,
        phaseTitle = phaseTitle,
        phaseDescription = phaseDescription,
        ringSegments = segments
    )
}

private fun buildHomeWeekDays(
    manager: CycleSettingsManager,
    indicators: List<DailyIndicatorEntity>,
    overview: HomeCycleOverview
): List<HomeWeekDayUi> {
    val dayMillis = 24L * 60L * 60L * 1000L
    val today = startOfDay(System.currentTimeMillis())

    val weekStartCalendar = Calendar.getInstance().apply {
        timeInMillis = today
        firstDayOfWeek = Calendar.SUNDAY
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    }
    val weekStart = startOfDay(weekStartCalendar.timeInMillis)

    val actualDays = buildSet {
        buildActualPeriodRanges(manager, indicators).forEach { range ->
            var cursor = range.startMillis
            while (cursor <= range.endMillis) {
                add(cursor)
                cursor += dayMillis
            }
        }
    }

    val predictedDays = buildSet {
        if (overview.configured && overview.expectedStartMillis > 0L) {
            repeat(overview.periodLength.coerceAtLeast(1)) { offset ->
                add(startOfDay(overview.expectedStartMillis + offset * dayMillis))
            }
        }
    }

    return (0..6).map { index ->
        val dateMillis = weekStart + index * dayMillis
        val marker = when {
            dateMillis in actualDays -> HomeDayMarker.ACTUAL
            dateMillis in predictedDays -> HomeDayMarker.PREDICTED
            else -> HomeDayMarker.NONE
        }
        HomeWeekDayUi(
            dateMillis = dateMillis,
            dayNumber = Calendar.getInstance().apply { timeInMillis = dateMillis }.get(Calendar.DAY_OF_MONTH),
            isToday = dateMillis == today,
            marker = marker
        )
    }
}

private fun buildTodayLogEntries(indicators: List<DailyIndicatorEntity>): List<HomeLogEntry> {
    fun valueFor(metricKey: String, fallback: String): String {
        val raw = indicators.firstOrNull { it.metricKey == metricKey }?.displayLabel?.trim().orEmpty()
        if (raw.isBlank()) return fallback
        return if (raw.length > 7) "${raw.take(7)}…" else raw
    }

    return listOf(
        HomeLogEntry(
            title = "流量",
            value = valueFor("流量", "未记录"),
            metricKey = "流量",
            icon = Icons.Filled.InvertColors,
            tint = Color(0xFFAB87DE)
        ),
        HomeLogEntry(
            title = "心情",
            value = valueFor("心情", "未记录"),
            metricKey = "心情",
            icon = Icons.Filled.SentimentSatisfied,
            tint = Color(0xFFE6B556)
        ),
        HomeLogEntry(
            title = "睡眠",
            value = valueFor("好习惯", "未记录"),
            metricKey = "好习惯",
            icon = Icons.Filled.Hotel,
            tint = Color(0xFF8A7DD2)
        )
    )
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
    val pregnancyChance: String,
    val pregnancyConfidence: String,
    val pregnancyDetail: String
)

private data class HomePeriodRange(val startMillis: Long, val endMillis: Long)

private fun buildCycleTip(
    manager: CycleSettingsManager,
    records: List<RecordEntity>,
    indicators: List<DailyIndicatorEntity>
): CycleTip {
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
            pregnancyChance = "--",
            pregnancyConfidence = "无估算",
            pregnancyDetail = "设置后可显示动态估算。"
        )
    }

    val cycleLength = manager.cycleLengthDays()
    val periodLength = manager.periodLengthDays()
    val dayMillis = 24L * 60 * 60 * 1000
    val todayStart = startOfDay(System.currentTimeMillis())
    val configuredStart = startOfDay(manager.lastPeriodStartMillis())
    val actualRanges = buildActualPeriodRanges(manager, indicators)
    val anchorStart = resolveAnchorStart(configuredStart, actualRanges)
    val currentRange = actualRanges.firstOrNull { todayStart in it.startMillis..it.endMillis }

    if (anchorStart > todayStart) {
        return CycleTip(
            title = "周期建议",
            phase = "周期设置异常",
            tip = "最近一次经期开始日不能晚于今天，请重新设置。",
            needSetup = true,
            isMenstrualNow = false,
            overdueWarning = null,
            heroTitle = "请修正周期日期",
            heroSubtitle = "当前设置的开始日比今天更晚，无法推断周期状态。",
            pregnancyChance = "--",
            pregnancyConfidence = "无估算",
            pregnancyDetail = "请先修正最近一次开始日。"
        )
    }

    val daysSinceLastStart = ((todayStart - anchorStart) / dayMillis).toInt()
    val expectedNextStart = anchorStart + cycleLength * dayMillis
    val ovulationDay = (cycleLength - 14).coerceAtLeast(periodLength + 1).coerceAtMost(cycleLength - 1)
    val ovulationOffset = ovulationDay - 1
    val fertileStartOffset = (ovulationOffset - 5).coerceAtLeast(periodLength)
    val fertileEndOffset = (ovulationOffset + 1).coerceAtMost(cycleLength - 1)
    val overdueDays = (daysSinceLastStart - cycleLength).coerceAtLeast(0)
    val dayMillisLong = 24L * 60L * 60L * 1000L
    val recentLoveRecords = records.filter {
        it.type == "同房" && startOfDay(it.timeMillis) in (todayStart - 6 * dayMillisLong)..todayStart
    }
    val chancePercent = estimatePregnancyChancePercent(
        todayStart = todayStart,
        lastStart = anchorStart,
        cycleLength = cycleLength,
        periodLength = periodLength,
        records = records
    )
    val pregnancyChance = formatPregnancyChance(
        chancePercent
    )
    val pregnancyConfidence = if (recentLoveRecords.isEmpty()) "仅周期估算" else "周期+记录估算"
    val pregnancyDetail = buildPregnancyDetail(
        daysSinceLastStart = daysSinceLastStart,
        cycleLength = cycleLength,
        recentLoveRecords = recentLoveRecords,
        chancePercent = chancePercent
    )

    return when {
        currentRange != null -> {
            val dayInPeriod = ((todayStart - currentRange.startMillis) / dayMillis).toInt() + 1
            CycleTip(
                title = "今日周期提醒",
                phase = "经期（第${dayInPeriod}天）",
                tip = "建议温和沟通，注意情绪安抚与休息。",
                needSetup = false,
                isMenstrualNow = true,
                overdueWarning = null,
                heroTitle = "经期第${dayInPeriod}天",
                heroSubtitle = "当前处于经期，预计本次经期在${formatMonthDay(currentRange.endMillis)}结束。",
                pregnancyChance = pregnancyChance,
                pregnancyConfidence = pregnancyConfidence,
                pregnancyDetail = pregnancyDetail
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
            pregnancyChance = pregnancyChance,
            pregnancyConfidence = pregnancyConfidence,
            pregnancyDetail = pregnancyDetail
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
            pregnancyChance = pregnancyChance,
            pregnancyConfidence = pregnancyConfidence,
            pregnancyDetail = pregnancyDetail
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
                pregnancyChance = pregnancyChance,
                pregnancyConfidence = pregnancyConfidence,
                pregnancyDetail = pregnancyDetail
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
            heroSubtitle = "预计排卵日约在${formatMonthDay(anchorStart + ovulationOffset * dayMillis)}，当前仍在排卵前阶段。",
            pregnancyChance = pregnancyChance,
            pregnancyConfidence = pregnancyConfidence,
            pregnancyDetail = pregnancyDetail
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
            pregnancyChance = pregnancyChance,
            pregnancyConfidence = pregnancyConfidence,
            pregnancyDetail = pregnancyDetail
        )
    }
}

private fun estimatePregnancyChancePercent(
    todayStart: Long,
    lastStart: Long,
    cycleLength: Int,
    periodLength: Int,
    records: List<RecordEntity>
): Int {
    val phaseOnlyPercent = fertilityPercentForDate(
        dateMillis = todayStart,
        lastStart = lastStart,
        cycleLength = cycleLength,
        periodLength = periodLength
    )
    val dayMillis = 24L * 60L * 60L * 1000L
    val recentLoveRecords = records.filter { record ->
        record.type == "同房" && startOfDay(record.timeMillis) in (todayStart - 6 * dayMillis)..todayStart
    }
    if (recentLoveRecords.isEmpty()) return phaseOnlyPercent

    val combinedRisk = recentLoveRecords.fold(0.0) { accumulated, record ->
        val dailyFertility = fertilityPercentForDate(
            dateMillis = startOfDay(record.timeMillis),
            lastStart = lastStart,
            cycleLength = cycleLength,
            periodLength = periodLength
        ) / 100.0
        val protectionFactor = protectionRiskFactor(record.protections)
        val eventRisk = (dailyFertility * protectionFactor).coerceIn(0.0, 0.95)
        1 - ((1 - accumulated) * (1 - eventRisk))
    }

    // Keep a small cycle baseline while letting protection-adjusted behavior dominate.
    val baseline = (phaseOnlyPercent / 100.0) * 0.25
    val blended = baseline + (1 - baseline) * combinedRisk
    return (blended * 100).roundToInt().coerceIn(1, 95)
}

private fun fertilityPercentForDate(
    dateMillis: Long,
    lastStart: Long,
    cycleLength: Int,
    periodLength: Int
): Int {
    val dayMillis = 24L * 60L * 60L * 1000L
    val diffDays = ((startOfDay(dateMillis) - startOfDay(lastStart)) / dayMillis).toInt()
    if (diffDays < 0) return 1

    val cycleDay = diffDays % cycleLength
    val ovulationOffset = (cycleLength - 14 - 1).coerceAtLeast(periodLength).coerceAtMost(cycleLength - 1)
    val fertileStart = (ovulationOffset - 5).coerceAtLeast(periodLength)
    val fertileEnd = (ovulationOffset + 1).coerceAtMost(cycleLength - 1)

    return when {
        cycleDay < periodLength -> (2 + cycleDay).coerceAtMost(6)
        cycleDay == ovulationOffset -> 32
        cycleDay in fertileStart..fertileEnd -> {
            when (abs(cycleDay - ovulationOffset)) {
                1 -> 26
                2 -> 21
                3 -> 16
                4 -> 12
                5 -> 9
                else -> 18
            }
        }
        cycleDay < fertileStart -> {
            val span = (fertileStart - periodLength).coerceAtLeast(1)
            val progress = (cycleDay - periodLength).toFloat() / span.toFloat()
            (5 + progress * 6f).roundToInt()
        }
        else -> {
            val lutealSpan = (cycleLength - fertileEnd - 1).coerceAtLeast(1)
            val progress = (cycleDay - fertileEnd).toFloat() / lutealSpan.toFloat()
            (10 - progress * 7f).roundToInt()
        }
    }.coerceIn(1, 35)
}

private fun protectionRiskFactor(protectionsRaw: String): Double {
    val protections = protectionsRaw.split("|").filter { it.isNotBlank() }.toSet()
    if (protections.isEmpty()) return 1.0
    if ("无防护" in protections || "无措施" in protections) return 1.0

    val factors = protections.mapNotNull { protection ->
        when (protection) {
            "避孕套" -> 0.13
            "短效避孕药" -> 0.08
            "长效避孕", "长效避孕药", "节育环" -> 0.02
            "紧急避孕药" -> 0.18
            "体外", "体外排精", "未射精" -> 0.45
            "其他" -> 0.65
            else -> null
        }
    }
    if (factors.isEmpty()) return 0.75

    val strongest = factors.minOrNull() ?: 0.75
    val hasStackedMethods = factors.size >= 2
    val stackedFactor = if (hasStackedMethods) strongest * 0.8 else strongest
    return stackedFactor.coerceIn(0.01, 1.0)
}

private fun formatPregnancyChance(percent: Int): String {
    return if (percent <= 1) "<1%" else "$percent%"
}

private fun buildPregnancyDetail(
    daysSinceLastStart: Int,
    cycleLength: Int,
    recentLoveRecords: List<RecordEntity>,
    chancePercent: Int
): String {
    val protectionSummary = recentLoveRecords
        .flatMap { it.protections.split("|") }
        .filter { it.isNotBlank() }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?: "无近7天行为记录"
    return "估算依据：周期第${daysSinceLastStart + 1}天/$cycleLength，近7天行为${recentLoveRecords.size}次，主要措施：$protectionSummary（已纳入折算），估算值${formatPregnancyChance(chancePercent)}。"
}

private fun buildActualPeriodRanges(
    manager: CycleSettingsManager,
    indicators: List<DailyIndicatorEntity>
): List<HomePeriodRange> {
    val dayMillis = 24L * 60L * 60L * 1000L
    val periodLength = manager.periodLengthDays().coerceIn(1, MAX_PERIOD_DAYS)
    val starts = indicators
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_STARTED }
        .map { parseDayKey(it.dateKey) }
        .filter { it > 0L }
        .toMutableList()
    val ends = indicators
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_ENDED }
        .map { parseDayKey(it.dateKey) }
        .filter { it > 0L }
        .sorted()
    val configuredStart = startOfDay(manager.lastPeriodStartMillis())
    if (configuredStart > 0L && configuredStart !in starts) {
        starts += configuredStart
    }
    val sortedStarts = starts.distinct().sorted()
    if (sortedStarts.isEmpty()) return emptyList()

    return sortedStarts.mapIndexed { index, start ->
        val nextStart = sortedStarts.getOrNull(index + 1)
        val hardMaxEnd = start + (MAX_PERIOD_DAYS - 1L) * dayMillis
        val defaultEnd = start + (periodLength - 1L) * dayMillis
        val explicitEnd = ends.firstOrNull { end ->
            end >= start && end <= hardMaxEnd && (nextStart == null || end < nextStart)
        }
        val resolvedEnd = if (explicitEnd != null) {
            listOfNotNull(explicitEnd, hardMaxEnd, nextStart?.minus(dayMillis)).minOrNull() ?: explicitEnd
        } else {
            listOfNotNull(defaultEnd, hardMaxEnd, nextStart?.minus(dayMillis)).minOrNull() ?: defaultEnd
        }
        HomePeriodRange(startMillis = start, endMillis = resolvedEnd)
    }
}

private fun resolveAnchorStart(configuredStart: Long, ranges: List<HomePeriodRange>): Long {
    val latestActualStart = ranges.maxByOrNull { it.startMillis }?.startMillis ?: 0L
    return maxOf(configuredStart, latestActualStart)
}

private fun resolveHomePeriodAction(
    manager: CycleSettingsManager,
    indicators: List<DailyIndicatorEntity>,
    targetDateMillis: Long
): HomePeriodAction {
    val normalized = startOfDay(targetDateMillis)
    val dayMillis = 24L * 60L * 60L * 1000L
    val latestStart = indicators
        .asSequence()
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_STARTED }
        .map { parseDayKey(it.dateKey) }
        .filter { it in 1L..normalized }
        .maxOrNull()
        ?: manager.lastPeriodStartMillis().takeIf { it > 0L }?.let(::startOfDay)
    val latestEnd = indicators
        .asSequence()
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_ENDED }
        .map { parseDayKey(it.dateKey) }
        .filter { it in 1L..normalized }
        .maxOrNull()
    val hasOpenPeriod =
        latestStart != null &&
            (latestEnd == null || latestEnd < latestStart) &&
            normalized in latestStart..(latestStart + (MAX_PERIOD_DAYS - 1) * dayMillis)
    return if (hasOpenPeriod) HomePeriodAction.END else HomePeriodAction.START
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

private fun parseDayKey(dateKey: String): Long {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey)?.time?.let(::startOfDay) ?: 0L
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
