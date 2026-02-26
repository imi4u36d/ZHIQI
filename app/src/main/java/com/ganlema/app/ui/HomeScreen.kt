package com.ganlema.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    pinManager: PinManager,
    filterState: FilterState,
    onAddRecord: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenCycleSettings: () -> Unit
) {
    val context = LocalContext.current
    val records by repository.records().collectAsState(initial = emptyList())
    val visibleRecords = if (pinManager.isHidden()) emptyList() else records
    val filteredRecords = applyFilters(visibleRecords, filterState)
    val recordCountByDate = filteredRecords.groupingBy { dateKey(it.timeMillis) }.eachCount()
    val showContent = remember { mutableStateOf(false) }
    val selectedDateKey = remember { mutableStateOf<String?>(null) }

    val cycleManager = remember { CycleSettingsManager(context) }
    val cycleTip = buildCycleTip(cycleManager)

    LaunchedEffect(Unit) { showContent.value = true }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showContent.value,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 })
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CycleTipCard(cycleTip = cycleTip, onOpenCycleSettings = onOpenCycleSettings)

                    Box(modifier = Modifier.glassCard().padding(12.dp)) {
                        CalendarGrid(
                            recordCountByDate = recordCountByDate,
                            selectedDateKey = selectedDateKey.value,
                            highlightTodayAsPeriod = cycleTip.isMenstrualNow,
                            onOpenDetail = onOpenDetail,
                            onSelectDate = { selectedDateKey.value = it },
                            records = filteredRecords
                        )
                    }

                    RecordEntryCard(onAddRecord = onAddRecord)
                }
            }
        }
    }
}

@Composable
private fun CycleTipCard(cycleTip: CycleTip, onOpenCycleSettings: () -> Unit) {
    val gradient = Brush.linearGradient(listOf(Color(0xFFF4F7FF), Color(0xFFFFF4F8)))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .background(gradient)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(cycleTip.title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))
        Text(cycleTip.phase, color = Color(0xFF5E5CE6), fontWeight = FontWeight.SemiBold)
        Text(cycleTip.tip, color = Color(0xFF5D6470), style = MaterialTheme.typography.bodyMedium)
        if (!cycleTip.overdueWarning.isNullOrBlank()) {
            Text(
                cycleTip.overdueWarning,
                color = Color(0xFFE76472),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (cycleTip.needSetup) {
            Text(
                text = "去设置生理周期，获取更准确建议",
                color = Color(0xFF5E5CE6),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.noRippleClickable { onOpenCycleSettings() }
            )
        }
    }
}

@Composable
private fun RecordEntryCard(onAddRecord: () -> Unit) {
    val gradient = Brush.linearGradient(
        listOf(Color(0xFF6D7BFF), Color(0xFF8E72FF), Color(0xFF9D8BFF))
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .glassCard()
            .background(gradient)
            .padding(14.dp)
            .noRippleClickable { onAddRecord() }
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Text("马上记录", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("记录当下状态，生成趋势分析", color = Color.White.copy(alpha = 0.92f))
                Text("去记录", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    recordCountByDate: Map<String, Int>,
    selectedDateKey: String?,
    highlightTodayAsPeriod: Boolean,
    records: List<RecordEntity>,
    onSelectDate: (String) -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    val days = generateMonthCells()
    val monthText = SimpleDateFormat("yyyy年MM月", Locale.getDefault()).format(Date())
    val todayKey = dateKey(System.currentTimeMillis())

    Column {
        Text(monthText, style = MaterialTheme.typography.titleMedium, color = Color(0xFF313642))
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(it, color = Color(0xFF9AA1AD), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().height(300.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(days) { day ->
                if (day.key == null) {
                    Spacer(modifier = Modifier.size(38.dp))
                } else {
                    val hasRecord = (recordCountByDate[day.key] ?: 0) > 0
                    val selected = selectedDateKey == day.key
                    val menstrualToday = highlightTodayAsPeriod && day.key == todayKey
                    Column(
                        modifier = Modifier
                            .size(38.dp)
                            .noRippleClickable {
                                onSelectDate(day.key)
                                val record = records.firstOrNull { dateKey(it.timeMillis) == day.key }
                                if (record != null) onOpenDetail(record.id)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    when {
                                        selected -> Color(0xFF5E5CE6)
                                        hasRecord -> Color(0xFFEDEBFF)
                                        else -> Color.Transparent
                                    },
                                    shape = CircleShape
                                )
                                .then(if (hasRecord && !selected) Modifier.border(1.dp, Color(0xFF8A84FF), CircleShape) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.label,
                                color = when {
                                    selected -> Color.White
                                    menstrualToday -> Color(0xFFE64A59)
                                    hasRecord -> Color(0xFF5E5CE6)
                                    else -> Color(0xFF525866)
                                },
                                fontWeight = if (selected || hasRecord) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(
                                    if (hasRecord) if (selected) Color.White else Color(0xFF5E5CE6) else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

private data class CycleTip(
    val title: String,
    val phase: String,
    val tip: String,
    val needSetup: Boolean,
    val isMenstrualNow: Boolean,
    val overdueWarning: String? = null
)

private fun buildCycleTip(manager: CycleSettingsManager): CycleTip {
    if (!manager.isConfigured() || manager.lastPeriodStartMillis() <= 0L) {
        return CycleTip(
            title = "周期建议",
            phase = "未设置生理周期",
            tip = "设置周期后可获得安全期、排卵期、黄体期建议和怀孕概率提示。",
            needSetup = true,
            isMenstrualNow = false,
            overdueWarning = null
        )
    }

    val cycleLength = manager.cycleLengthDays()
    val periodLength = manager.periodLengthDays()
    val dayMillis = 24L * 60 * 60 * 1000
    val offset = ((System.currentTimeMillis() - manager.lastPeriodStartMillis()) / dayMillis).toInt().coerceAtLeast(0)
    val dayInCycle = (offset % cycleLength) + 1
    val ovulationDay = (cycleLength - 14).coerceAtLeast(periodLength + 1)
    val overdueWarning = if (offset > cycleLength) {
        "已超出周期天数，可能怀孕，请及时留意。"
    } else {
        null
    }

    return when {
        dayInCycle <= periodLength -> CycleTip(
            title = "今日周期提醒",
            phase = "经期（第${dayInCycle}天）",
            tip = "建议温和沟通，注意情绪安抚与休息，避免刺激性安排；怀孕概率较低。",
            needSetup = false,
            isMenstrualNow = true,
            overdueWarning = overdueWarning
        )
        dayInCycle in (ovulationDay - 2)..(ovulationDay + 1) -> CycleTip(
            title = "今日周期提醒",
            phase = "排卵期",
            tip = "怀孕概率较高，建议重视避孕措施，并关注情绪与压力波动。",
            needSetup = false,
            isMenstrualNow = false,
            overdueWarning = overdueWarning
        )
        dayInCycle > ovulationDay + 1 -> CycleTip(
            title = "今日周期提醒",
            phase = "黄体期",
            tip = "可能出现情绪敏感或易躁，建议减少冲突、规律作息；怀孕概率中低。",
            needSetup = false,
            isMenstrualNow = false,
            overdueWarning = overdueWarning
        )
        else -> CycleTip(
            title = "今日周期提醒",
            phase = "卵泡期",
            tip = "状态通常更稳定，建议保持规律作息和适度运动；注意基础避孕。",
            needSetup = false,
            isMenstrualNow = false,
            overdueWarning = overdueWarning
        )
    }
}

private data class DayItem(val key: String?, val label: String)

private fun generateMonthCells(): List<DayItem> {
    val now = Calendar.getInstance()
    val year = now.get(Calendar.YEAR)
    val month = now.get(Calendar.MONTH)

    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val firstDayOfWeek = ((calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = mutableListOf<DayItem>()

    repeat(firstDayOfWeek) { cells.add(DayItem(null, "")) }
    for (day in 1..daysInMonth) {
        calendar.set(Calendar.DAY_OF_MONTH, day)
        cells.add(DayItem(key = dateKey(calendar.timeInMillis), label = day.toString()))
    }
    while (cells.size % 7 != 0) cells.add(DayItem(null, ""))

    return cells
}

private fun dateKey(timeMillis: Long): String {
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
