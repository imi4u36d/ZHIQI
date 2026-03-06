package com.ganlema.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ganlema.app.data.DailyIndicatorEntity
import com.ganlema.app.data.DailyIndicatorRepository
import com.ganlema.app.data.RecordEntity
import com.ganlema.app.data.RecordRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val PERIOD_STATUS_KEY = "月经状态"
private const val PERIOD_STARTED = "start"
private const val PERIOD_ENDED = "end"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    repository: RecordRepository,
    indicatorRepository: DailyIndicatorRepository,
    cycleSettingsVersion: Int,
    onAddRecord: (String?) -> Unit,
    onSelectDateForEntry: (Long) -> Unit,
    onSaveIndicator: suspend (DailyIndicatorEntity) -> Unit,
    onCycleChanged: () -> Unit
) {
    val context = LocalContext.current
    val cycleManager = remember { CycleSettingsManager(context) }
    val records by repository.records().collectAsState(initial = emptyList())
    val allIndicators by indicatorRepository.allIndicators().collectAsState(initial = emptyList())
    var monthOffset by remember { mutableIntStateOf(0) }
    var showAnalysisSheet by remember { mutableStateOf(false) }
    var selectedDateMillis by remember(cycleSettingsVersion) { mutableLongStateOf(startOfDay(System.currentTimeMillis())) }

    val monthState = remember(monthOffset, cycleSettingsVersion, records, allIndicators, selectedDateMillis) {
        buildCycleMonthState(
            cycleManager = cycleManager,
            monthOffset = monthOffset,
            records = records,
            indicators = allIndicators,
            selectedDateMillis = selectedDateMillis
        )
    }
    val cycleInsight = remember(cycleSettingsVersion, allIndicators, records) {
        buildCycleInsight(cycleManager, allIndicators)
    }
    val selectedDateKey = remember(selectedDateMillis) { dayKey(selectedDateMillis) }
    val selectedIndicators = remember(allIndicators, selectedDateKey) {
        allIndicators.filter { it.dateKey == selectedDateKey }
    }
    val selectedLoveRecord = remember(records, selectedDateKey) {
        records.firstOrNull { it.type == "同房" && dayKey(it.timeMillis) == selectedDateKey }
    }
    val statusUi = remember(selectedDateMillis, allIndicators, cycleSettingsVersion) {
        buildPeriodStatusUi(
            cycleManager = cycleManager,
            indicators = allIndicators,
            selectedDateMillis = selectedDateMillis
        )
    }

    GlassBackground {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                RecordCalendarHeader(
                    title = monthState.title,
                    onPrev = { monthOffset -= 1 },
                    onNext = { monthOffset += 1 }
                )
            }
            item { CalendarWeekHeader() }
            item {
                CalendarGrid(
                    monthState = monthState,
                    onSelectDate = { selectedDateMillis = it }
                )
            }
            item { CalendarLegend() }
            item {
                InsightActionCard(onClick = { showAnalysisSheet = true })
            }
            item {
                PeriodStatusSection(
                    selectedDateMillis = selectedDateMillis,
                    statusUi = statusUi,
                    onConfirm = {
                        val normalized = startOfDay(selectedDateMillis)
                        val saveStart = statusUi.cardTitle == "月经来了"
                        onSaveIndicator(
                            DailyIndicatorEntity(
                                dateKey = dayKey(normalized),
                                metricKey = PERIOD_STATUS_KEY,
                                optionValue = if (saveStart) PERIOD_STARTED else PERIOD_ENDED,
                                displayLabel = if (saveStart) "月经来了" else "月经走了",
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        if (saveStart) {
                            cycleManager.saveAll(
                                cycleLengthDays = cycleManager.cycleLengthDays(),
                                periodLengthDays = cycleManager.periodLengthDays(),
                                lastPeriodStartMillis = normalized
                            )
                        }
                        onCycleChanged()
                    }
                )
            }
            item {
                RecordEntryList(
                    onAddRecord = { entry ->
                        onSelectDateForEntry(selectedDateMillis)
                        onAddRecord(entry)
                    },
                    indicators = selectedIndicators,
                    loveRecord = selectedLoveRecord
                )
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
    }

    if (showAnalysisSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAnalysisSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            CycleInsightSheet(
                insight = cycleInsight,
                onClose = { showAnalysisSheet = false }
            )
        }
    }
}

@Composable
private fun RecordCalendarHeader(
    title: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.ChevronLeft,
            contentDescription = "上个月",
            tint = Color(0xFF2F3440),
            modifier = Modifier.noRippleClickable(onPrev)
        )
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2F3440))
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = "下个月",
            tint = Color(0xFF2F3440),
            modifier = Modifier.noRippleClickable(onNext)
        )
    }
}

@Composable
private fun CalendarWeekHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) {
        listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = Color(0xFFB4B8C2),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    monthState: CycleMonthState,
    onSelectDate: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
    ) {
        monthState.days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        modifier = Modifier.weight(1f),
                        onSelect = onSelectDate
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CycleCalendarDay,
    modifier: Modifier = Modifier,
    onSelect: (Long) -> Unit
) {
    val state = day.phaseState
    val isFuture = !day.isBlank && day.dateMillis > startOfDay(System.currentTimeMillis())
    val background = when {
        day.isBlank -> Color.Transparent
        day.isToday -> Color(0xFFE95A92)
        state == PhaseState.ACTUAL_PERIOD -> Color(0xFFFF7EAE)
        state == PhaseState.PREDICTED_PERIOD -> Color(0xFFFCEAF1)
        state == PhaseState.OVULATION_DAY -> Color(0xFFF3E6FF)
        else -> Color.Transparent
    }
    val numberColor = when {
        day.isBlank -> Color.Transparent
        day.isToday -> Color.White
        state == PhaseState.ACTUAL_PERIOD -> Color.White
        state == PhaseState.PREDICTED_PERIOD -> Color(0xFFE6A9C0)
        state == PhaseState.FERTILE || state == PhaseState.OVULATION_DAY -> Color(0xFFC18CF2)
        else -> Color(0xFF7ED48C)
    }.let { if (isFuture) it.copy(alpha = 0.42f) else it }

    Box(
        modifier = modifier
            .height(52.dp)
            .padding(horizontal = 1.dp, vertical = 0.5.dp)
            .background(background)
            .border(
                width = if (day.isSelected && !day.isBlank) 1.5.dp else 0.dp,
                color = if (day.isSelected) Color(0xFFD66A9A) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .then(
                if (!day.isBlank && !isFuture) Modifier.noRippleClickable { onSelect(day.dateMillis) } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!day.isBlank) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (day.isToday) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Color(0xFFFFD95B), CircleShape)
                    )
                } else {
                    Spacer(modifier = Modifier.height(5.dp))
                }
                Text(
                    text = day.dayNumber.toString(),
                    color = numberColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (day.isToday || day.isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
                Text(
                    text = if (day.isToday) "今天" else "",
                    color = if (day.isToday) Color.White.copy(alpha = 0.92f) else Color.Transparent,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
                if (state == PhaseState.OVULATION_DAY) {
                    Text("✿", color = Color(0xFFC18CF2), style = MaterialTheme.typography.labelSmall)
                } else if (day.hasRecord) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(
                                if (day.isToday) Color.White else if (day.isSelected) Color(0xFFD66A9A) else Color(0xFFFF6EA8),
                                CircleShape
                            )
                    )
                } else {
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }
        }
    }
}

@Composable
private fun CalendarLegend() {
    val items = listOf(
        LegendItem("月经期", Color(0xFFFF7EAE)),
        LegendItem("预测经期", Color(0xFFFCEAF1)),
        LegendItem("排卵期", Color(0xFFD7B3FF)),
        LegendItem("排卵日", Color(0xFFC18CF2))
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(item.color, RoundedCornerShape(3.dp))
                )
                Text(
                    text = " ${item.label}",
                    color = Color(0xFF7D8390),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun InsightActionCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFFF1EEF3), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFFFFE8F0), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AutoGraph, contentDescription = "规律解读", tint = Color(0xFFD66A9A))
            }
            Column {
                Text("月经规律解读", color = Color(0xFFD66A9A), style = MaterialTheme.typography.titleMedium)
                Text("对比上次，本次经期有哪些异常？", color = Color(0xFF8A8F98), style = MaterialTheme.typography.bodySmall)
            }
        }
        Box(
            modifier = Modifier
                .background(Color(0xFFEF5D95), RoundedCornerShape(18.dp))
                .noRippleClickable(onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("去查看", color = Color.White, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun PeriodStatusSection(
    selectedDateMillis: Long,
    statusUi: PeriodStatusUi,
    onConfirm: suspend () -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFFF1EEF3), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "${formatMonthDay(selectedDateMillis)}记录",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF2F3440)
        )
        Text(
            text = "当前判断：${statusUi.phaseLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A8F98)
        )
        PeriodToggleCard(
            title = statusUi.cardTitle,
            active = statusUi.active,
            onYes = { scope.launch { onConfirm() } }
        )
    }
}

@Composable
private fun PeriodToggleCard(
    title: String,
    active: Boolean,
    onYes: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFCFD), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFF2EEF2), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleButton(text = "是", active = active, onClick = onYes)
            ToggleButton(text = "否", active = !active, onClick = {})
        }
    }
}

@Composable
private fun ToggleButton(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (active) Color(0xFFEF5D95) else Color(0xFFF1F1F4),
                RoundedCornerShape(14.dp)
            )
            .noRippleClickable(onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (active) Color.White else Color(0xFF9BA1AA), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun RecordEntryList(
    onAddRecord: (String?) -> Unit,
    indicators: List<DailyIndicatorEntity>,
    loveRecord: RecordEntity?
) {
    val items = listOf(
        RecordEntryItem("爱爱", Icons.Filled.Favorite),
        RecordEntryItem("症状", Icons.Filled.HealthAndSafety),
        RecordEntryItem("心情", Icons.Filled.Mood),
        RecordEntryItem("白带", Icons.Filled.InvertColors),
        RecordEntryItem("体温", Icons.Filled.Thermostat),
        RecordEntryItem("体重", Icons.Filled.Palette),
        RecordEntryItem("日记", Icons.Filled.Schedule),
        RecordEntryItem("好习惯", Icons.Filled.ThumbUp),
        RecordEntryItem("便便", Icons.Filled.Palette),
        RecordEntryItem("计划", Icons.Filled.Today)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFFF1EEF3), RoundedCornerShape(24.dp))
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFFEEF4), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = item.title, tint = Color(0xFFEF5D95))
                    }
                    Column {
                        Text(item.title, color = Color(0xFF2F3440), style = MaterialTheme.typography.titleMedium)
                        val saved = indicators.firstOrNull { it.metricKey == item.title }
                        if (item.title == "爱爱" && loveRecord != null) {
                            Text(
                                "已记录 · ${loveRecord.protections.replace("|", " / ")}",
                                color = Color(0xFF8E95A0),
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else if (saved != null) {
                            Text(saved.displayLabel, color = Color(0xFF8E95A0), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .border(1.dp, Color(0xFFF4B4CD), CircleShape)
                        .noRippleClickable { onAddRecord(item.title) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = item.title, tint = Color(0xFFD66A9A))
                }
            }
            if (index != items.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(start = 68.dp)
                        .background(Color(0xFFF2F0F4))
                )
            }
        }
    }
}

private data class RecordEntryItem(val title: String, val icon: ImageVector)
private data class LegendItem(val label: String, val color: Color)
private data class CycleInsight(
    val summaryTitle: String,
    val summaryText: String,
    val cycleLengthText: String,
    val nextPeriodText: String,
    val fertileText: String,
    val guidance: List<String>,
    val segments: List<CycleSegment>
)
private data class CycleSegment(val label: String, val color: Color, val days: Int)
private data class CycleMonthState(
    val title: String,
    val days: List<CycleCalendarDay>
)
private data class CycleCalendarDay(
    val dateMillis: Long,
    val dayNumber: Int,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isBlank: Boolean,
    val hasRecord: Boolean,
    val phaseState: PhaseState
)
private data class PeriodStatusUi(
    val phaseLabel: String,
    val cardTitle: String,
    val active: Boolean
)

private enum class PhaseState {
    NONE,
    ACTUAL_PERIOD,
    PREDICTED_PERIOD,
    FERTILE,
    OVULATION_DAY
}

@Composable
private fun CycleInsightSheet(
    insight: CycleInsight,
    onClose: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("月经规律解读", style = MaterialTheme.typography.titleLarge, color = Color(0xFF2F3440))
                Text("关闭", color = Color(0xFFD66A9A), modifier = Modifier.noRippleClickable(onClose))
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF7FA), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFF3DEE8), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(insight.summaryTitle, style = MaterialTheme.typography.titleLarge, color = Color(0xFFD66A9A))
                Text(insight.summaryText, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF616875))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                InsightMetricCard("平均周期", insight.cycleLengthText, Modifier.weight(1f))
                InsightMetricCard("预计下次", insight.nextPeriodText, Modifier.weight(1f))
                InsightMetricCard("易孕窗口", insight.fertileText, Modifier.weight(1f))
            }
        }
        item { CycleSegmentChart(insight.segments) }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFF1EEF3), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("温和建议", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))
                insight.guidance.forEach { tip ->
                    Text("• $tip", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF656D78))
                }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun InsightMetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFF1EEF3), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9AA1AD))
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CycleSegmentChart(segments: List<CycleSegment>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFFF1EEF3), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("周期结构示意", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val totalDays = segments.sumOf { it.days }.coerceAtLeast(1)
            Row(modifier = Modifier.fillMaxWidth()) {
                segments.forEach { segment ->
                    val fraction = segment.days.toFloat() / totalDays.toFloat()
                    Box(
                        modifier = Modifier
                            .weight(fraction)
                            .height(16.dp)
                            .background(segment.color, RoundedCornerShape(10.dp))
                    )
                    if (segment != segments.last()) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }
        segments.forEach { segment ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(segment.color, CircleShape)
                    )
                    Text(" ${segment.label}", color = Color(0xFF5F6775), style = MaterialTheme.typography.bodyMedium)
                }
                Text("${segment.days} 天", color = Color(0xFF2F3440), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun buildCycleMonthState(
    cycleManager: CycleSettingsManager,
    monthOffset: Int,
    records: List<RecordEntity>,
    indicators: List<DailyIndicatorEntity>,
    selectedDateMillis: Long
): CycleMonthState {
    val monthCalendar = Calendar.getInstance().apply {
        add(Calendar.MONTH, monthOffset)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val title = SimpleDateFormat("M月", Locale.getDefault()).format(Date(monthCalendar.timeInMillis))
    val cycleLength = cycleManager.cycleLengthDays()
    val periodLength = cycleManager.periodLengthDays()
    val actualRanges = buildActualPeriodRanges(cycleManager, indicators)
    val lastStart = resolveAnchorStart(cycleManager, actualRanges)
    val hasCycle = cycleManager.isConfigured() && lastStart > 0L
    val dayRecordKeys = (records.map { dayKey(it.timeMillis) } + indicators.map { it.dateKey }).toSet()
    val todayKey = dayKey(System.currentTimeMillis())
    val selectedKey = dayKey(selectedDateMillis)

    val firstDayOfWeek = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1
    val maxDay = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val days = mutableListOf<CycleCalendarDay>()

    repeat(firstDayOfWeek) {
        days += CycleCalendarDay(0L, 0, false, false, true, false, PhaseState.NONE)
    }

    for (day in 1..maxDay) {
        monthCalendar.set(Calendar.DAY_OF_MONTH, day)
        val time = startOfDay(monthCalendar.timeInMillis)
        val key = dayKey(time)
        days += CycleCalendarDay(
            dateMillis = time,
            dayNumber = day,
            isToday = key == todayKey,
            isSelected = key == selectedKey,
            isBlank = false,
            hasRecord = dayRecordKeys.contains(key),
            phaseState = phaseForDate(
                dateMillis = time,
                cycleLength = cycleLength,
                periodLength = periodLength,
                lastStartMillis = lastStart,
                hasCycle = hasCycle,
                actualRanges = actualRanges
            )
        )
    }

    while (days.size % 7 != 0) {
        days += CycleCalendarDay(0L, 0, false, false, true, false, PhaseState.NONE)
    }

    return CycleMonthState(title = title, days = days)
}

private fun buildPeriodStatusUi(
    cycleManager: CycleSettingsManager,
    indicators: List<DailyIndicatorEntity>,
    selectedDateMillis: Long
): PeriodStatusUi {
    val normalized = startOfDay(selectedDateMillis)
    val actualRanges = buildActualPeriodRanges(cycleManager, indicators)
    val dateIndicators = indicators.filter { it.dateKey == dayKey(normalized) && it.metricKey == PERIOD_STATUS_KEY }
    val hasStartOnDay = dateIndicators.any { it.optionValue == PERIOD_STARTED }
    val hasEndOnDay = dateIndicators.any { it.optionValue == PERIOD_ENDED }
    val latestStart = indicators
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_STARTED }
        .map { parseDayKey(it.dateKey) }
        .filter { it in 1L..normalized }
        .maxOrNull()
        ?: cycleManager.lastPeriodStartMillis().takeIf { it > 0L }?.let(::startOfDay)
    val latestEnd = indicators
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_ENDED }
        .map { parseDayKey(it.dateKey) }
        .filter { it in 1L..normalized }
        .maxOrNull()
    val dayMillis = 24L * 60L * 60L * 1000L
    val hasStartWithinTenDays = latestStart != null &&
        normalized >= latestStart &&
        ((normalized - latestStart) / dayMillis).toInt() <= 10
    val periodStillOpen = latestStart != null && (latestEnd == null || latestEnd < latestStart)
    val shouldShowEndCard = when {
        hasEndOnDay -> false
        hasStartOnDay -> true
        periodStillOpen && hasStartWithinTenDays -> true
        else -> false
    }
    val cardTitle = if (shouldShowEndCard) "月经走了" else "月经来了"
    val isCurrentActionSaved = when (cardTitle) {
        "月经走了" -> hasEndOnDay
        else -> hasStartOnDay
    }

    return PeriodStatusUi(
        phaseLabel = cyclePhaseLabel(
            dateMillis = normalized,
            cycleLength = cycleManager.cycleLengthDays(),
            periodLength = cycleManager.periodLengthDays(),
            lastStartMillis = resolveAnchorStart(cycleManager, actualRanges),
            hasCycle = cycleManager.isConfigured() && resolveAnchorStart(cycleManager, actualRanges) > 0L,
            actualRanges = actualRanges
        ),
        cardTitle = cardTitle,
        active = isCurrentActionSaved
    )
}

private data class ActualPeriodRange(val startMillis: Long, val endMillis: Long)

private fun buildActualPeriodRanges(
    cycleManager: CycleSettingsManager,
    indicators: List<DailyIndicatorEntity>
): List<ActualPeriodRange> {
    val dayMillis = 24L * 60L * 60L * 1000L
    val periodLength = cycleManager.periodLengthDays().coerceAtLeast(1)
    val startEvents = indicators
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_STARTED }
        .map { parseDayKey(it.dateKey) }
        .toMutableList()
    val endEvents = indicators
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_ENDED }
        .map { parseDayKey(it.dateKey) }
        .sorted()
    val managerStart = startOfDay(cycleManager.lastPeriodStartMillis())
    if (cycleManager.isConfigured() && managerStart > 0L && managerStart !in startEvents) {
        startEvents += managerStart
    }
    val starts = startEvents.distinct().sorted()
    val ranges = mutableListOf<ActualPeriodRange>()

    starts.forEachIndexed { index, start ->
        val nextStart = starts.getOrNull(index + 1)
        val defaultEnd = start + (periodLength - 1) * dayMillis
        val explicitEnd = endEvents.firstOrNull { it >= start && (nextStart == null || it < nextStart) }
        val cappedEnd = listOfNotNull(
            defaultEnd,
            explicitEnd,
            nextStart?.minus(dayMillis)
        ).minOrNull() ?: defaultEnd
        ranges += ActualPeriodRange(startMillis = start, endMillis = cappedEnd)
    }

    return ranges
}

private fun cyclePhaseLabel(
    dateMillis: Long,
    cycleLength: Int,
    periodLength: Int,
    lastStartMillis: Long,
    hasCycle: Boolean,
    actualRanges: List<ActualPeriodRange>
): String {
    return when (phaseForDate(dateMillis, cycleLength, periodLength, lastStartMillis, hasCycle, actualRanges)) {
        PhaseState.ACTUAL_PERIOD -> "经期"
        PhaseState.PREDICTED_PERIOD -> "预测经期"
        PhaseState.FERTILE -> "排卵期"
        PhaseState.OVULATION_DAY -> "排卵日"
        else -> "周期中"
    }
}

private fun phaseForDate(
    dateMillis: Long,
    cycleLength: Int,
    periodLength: Int,
    lastStartMillis: Long,
    hasCycle: Boolean,
    actualRanges: List<ActualPeriodRange>
): PhaseState {
    val normalized = startOfDay(dateMillis)
    if (actualRanges.any { normalized in it.startMillis..it.endMillis }) {
        return PhaseState.ACTUAL_PERIOD
    }
    if (!hasCycle || lastStartMillis <= 0L) return PhaseState.NONE

    val dayMillis = 24L * 60L * 60L * 1000L
    val lastStart = startOfDay(lastStartMillis)
    val diffDays = ((normalized - lastStart) / dayMillis).toInt()
    if (diffDays < 0) return PhaseState.NONE

    val cycleDay = diffDays % cycleLength
    val cycleIndex = diffDays / cycleLength
    if (normalized >= startOfDay(System.currentTimeMillis()) && cycleIndex >= 1 && cycleDay in 0 until periodLength) {
        return PhaseState.PREDICTED_PERIOD
    }

    val ovulationOffset = (cycleLength - 14 - 1).coerceAtLeast(periodLength).coerceAtMost(cycleLength - 1)
    val fertileStart = (ovulationOffset - 5).coerceAtLeast(periodLength)
    val fertileEnd = (ovulationOffset + 1).coerceAtMost(cycleLength - 1)
    return when {
        cycleDay == ovulationOffset -> PhaseState.OVULATION_DAY
        cycleDay in fertileStart..fertileEnd -> PhaseState.FERTILE
        else -> PhaseState.NONE
    }
}

private fun buildCycleInsight(
    cycleManager: CycleSettingsManager,
    indicators: List<DailyIndicatorEntity>
): CycleInsight {
    val actualRanges = buildActualPeriodRanges(cycleManager, indicators)
    if (!cycleManager.isConfigured() || cycleManager.lastPeriodStartMillis() <= 0L) {
        return CycleInsight(
            summaryTitle = "尚未形成完整解读",
            summaryText = "设置最近一次生理期开始日和周期天数后，这里会生成经期、排卵期和下次月经的结构化解读。",
            cycleLengthText = "--",
            nextPeriodText = "--",
            fertileText = "--",
            guidance = listOf(
                "先完成生理周期设置，记录会更准确。",
                "若周期波动明显，建议连续记录 2 到 3 个周期后再判断。",
                "如果经期长期推迟或异常疼痛，建议及时咨询医生。"
            ),
            segments = listOf(
                CycleSegment("经期", Color(0xFFEF5D95), 5),
                CycleSegment("卵泡期", Color(0xFF8CD99D), 9),
                CycleSegment("排卵期", Color(0xFFC58EFF), 6),
                CycleSegment("黄体期", Color(0xFFF6D9E5), 8)
            )
        )
    }

    val cycleLength = cycleManager.cycleLengthDays()
    val periodLength = cycleManager.periodLengthDays()
    val lastStart = resolveAnchorStart(cycleManager, actualRanges)
    val today = startOfDay(System.currentTimeMillis())
    val dayMillis = 24L * 60L * 60L * 1000L
    val nextPeriod = lastStart + cycleLength * dayMillis
    val ovulationOffset = (cycleLength - 14 - 1).coerceAtLeast(periodLength).coerceAtMost(cycleLength - 1)
    val fertileStart = (ovulationOffset - 5).coerceAtLeast(periodLength)
    val fertileEnd = (ovulationOffset + 1).coerceAtMost(cycleLength - 1)
    val fertileLength = (fertileEnd - fertileStart + 1).coerceAtLeast(1)
    val follicularLength = (fertileStart - periodLength).coerceAtLeast(1)
    val lutealLength = (cycleLength - fertileEnd - 1).coerceAtLeast(1)
    val phase = cyclePhaseLabel(
        dateMillis = today,
        cycleLength = cycleLength,
        periodLength = periodLength,
        lastStartMillis = lastStart,
        hasCycle = true,
        actualRanges = actualRanges
    )
    val latestActualRange = actualRanges.maxByOrNull { it.startMillis }
    val recordedPeriodLength = latestActualRange?.let {
        ((it.endMillis - it.startMillis) / dayMillis).toInt() + 1
    }

    val summaryText = when {
        phase == "经期" -> "当前处于经期阶段，建议以休息、保暖和温和记录为主。若本次经期较平时明显提前、延后或持续时间变化较大，可继续观察下个周期。"
        phase == "排卵期" || phase == "排卵日" -> "当前已经进入易孕窗口，受孕概率相对更高。如果近期有行为记录，建议重点留意防护方式和身体变化。"
        today > nextPeriod -> "本周期已超过预计长度，建议先观察经期是否启动。若持续推迟且伴随胸胀、疲惫或其他变化，可考虑进一步排查。"
        else -> "从当前设置来看，周期长度处于常见区间，今天不属于经期高峰。继续保持稳定记录，有助于后续识别节律变化。"
    }

    val guidance = buildList {
        add("你当前设置为 $cycleLength 天周期、约 $periodLength 天经期，作为基础推算模板。")
        add("预计下次月经开始时间为 ${formatMonthDay(nextPeriod)}，建议提前 2 到 3 天留意身体感受。")
        if (recordedPeriodLength != null) {
            add("最近一次实际记录经期约 $recordedPeriodLength 天，可和默认经期长度一起观察是否稳定。")
        }
        if (phase == "排卵期" || phase == "排卵日") {
            add("当前位于易孕窗口，若暂无怀孕计划，建议重点关注避孕措施是否稳定。")
        } else {
            add("若后续出现连续 2 个周期以上明显提前或推迟，建议重新校正周期或咨询医生。")
        }
    }

    return CycleInsight(
        summaryTitle = "当前属于$phase",
        summaryText = summaryText,
        cycleLengthText = "$cycleLength 天",
        nextPeriodText = formatMonthDay(nextPeriod),
        fertileText = "${fertileLength} 天窗口",
        guidance = guidance,
        segments = listOf(
            CycleSegment("经期", Color(0xFFEF5D95), periodLength),
            CycleSegment("卵泡期", Color(0xFF8CD99D), follicularLength),
            CycleSegment("排卵期", Color(0xFFC58EFF), fertileLength),
            CycleSegment("黄体期", Color(0xFFF6D9E5), lutealLength)
        )
    )
}

private fun resolveAnchorStart(
    cycleManager: CycleSettingsManager,
    actualRanges: List<ActualPeriodRange>
): Long {
    val latestRangeStart = actualRanges.maxByOrNull { it.startMillis }?.startMillis ?: 0L
    return maxOf(startOfDay(cycleManager.lastPeriodStartMillis()), latestRangeStart)
}

private fun startOfDay(timeMillis: Long): Long {
    if (timeMillis <= 0L) return 0L
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

private fun dayKey(timeMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeMillis))
}

private fun parseDayKey(dateKey: String): Long {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey)?.time?.let(::startOfDay) ?: 0L
}

private fun formatMonthDay(timeMillis: Long): String {
    return SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(timeMillis))
}
