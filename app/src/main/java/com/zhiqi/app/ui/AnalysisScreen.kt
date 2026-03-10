package com.zhiqi.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhiqi.app.data.DailyIndicatorEntity
import com.zhiqi.app.data.RecordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ANALYSIS_WINDOW_DAYS = 30
private const val PERIOD_STATUS_KEY = "月经状态"
private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
private const val MAX_PIE_SLICES = 6

@Composable
fun AnalysisScreen(
    records: List<RecordEntity>,
    indicators: List<DailyIndicatorEntity>
) {
    val state = remember(records, indicators) {
        buildAnalysisOverview(records, indicators)
    }

    GlassBackground {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { AnalysisHeaderCard(summary = state.summary) }
            if (state.slices.isEmpty()) {
                item { EmptyAnalysisCard() }
            } else {
                item { PieOverviewCard(state = state) }
                item { ComprehensiveInsightCard(insight = state.insight) }
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
    }
}

@Composable
private fun AnalysisHeaderCard(summary: AnalysisSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("分析概览", style = MaterialTheme.typography.titleLarge, color = ZhiQiTokens.TextPrimary)
        Text(
            text = "${summary.startLabel} - ${summary.endLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = ZhiQiTokens.TextSecondary
        )
        Text(
            text = "最近${summary.totalDays}天共记录 ${summary.totalEntries} 条，活跃记录 ${summary.activeDays} 天。",
            style = MaterialTheme.typography.bodyMedium,
            color = ZhiQiTokens.TextSecondary
        )
    }
}

@Composable
private fun EmptyAnalysisCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("暂无可分析数据", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
        Text(
            "先在记录页持续记录几天，分析页会自动生成指标占比和综合结论。",
            style = MaterialTheme.typography.bodyMedium,
            color = ZhiQiTokens.TextSecondary
        )
    }
}

@Composable
private fun PieOverviewCard(state: AnalysisOverviewState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("指标占比", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                var startAngle = -90f
                state.slices.forEach { slice ->
                    val sweep = (slice.ratio * 360f).coerceAtLeast(0f)
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true
                    )
                    startAngle += sweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("总记录", style = MaterialTheme.typography.bodySmall, color = ZhiQiTokens.TextSecondary)
                Text(
                    "${state.summary.totalEntries}",
                    style = MaterialTheme.typography.titleLarge,
                    color = ZhiQiTokens.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.slices.forEach { slice ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(slice.color, CircleShape)
                        )
                        Text(
                            text = " ${slice.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZhiQiTokens.TextSecondary
                        )
                    }
                    Text(
                        text = "${slice.count} 条 (${(slice.ratio * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZhiQiTokens.TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ComprehensiveInsightCard(insight: ComprehensiveInsight) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("综合分析", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
        Text(
            text = insight.conclusion,
            style = MaterialTheme.typography.bodyMedium,
            color = ZhiQiTokens.TextSecondary
        )
        Text(
            text = "建议：${insight.suggestion}",
            style = MaterialTheme.typography.bodyMedium,
            color = ZhiQiTokens.TextSecondary
        )
    }
}

private fun buildAnalysisOverview(
    records: List<RecordEntity>,
    indicators: List<DailyIndicatorEntity>
): AnalysisOverviewState {
    val endDay = analysisStartOfDay(System.currentTimeMillis())
    val startDay = endDay - (ANALYSIS_WINDOW_DAYS - 1L) * DAY_MILLIS

    val indicatorWindow = indicators.filter {
        it.metricKey != PERIOD_STATUS_KEY && analysisParseDayKey(it.dateKey) in startDay..endDay
    }
    val recordWindow = records.filter { analysisStartOfDay(it.timeMillis) in startDay..endDay }

    val countByMetric = linkedMapOf<String, Int>()

    indicatorWindow.forEach { indicator ->
        val key = indicator.metricKey
        countByMetric[key] = (countByMetric[key] ?: 0) + 1
    }
    recordWindow.forEach { record ->
        val key = if (record.type == "同房") "爱爱" else record.type
        countByMetric[key] = (countByMetric[key] ?: 0) + 1
    }

    val totalEntries = countByMetric.values.sum()
    val activeDays = buildSet {
        indicatorWindow.forEach { add(it.dateKey) }
        recordWindow.forEach { add(analysisDayKey(it.timeMillis)) }
    }.size

    val summary = AnalysisSummary(
        startLabel = analysisFormatMonthDay(startDay),
        endLabel = analysisFormatMonthDay(endDay),
        totalDays = ANALYSIS_WINDOW_DAYS,
        totalEntries = totalEntries,
        activeDays = activeDays
    )

    val slices = buildPieSlices(countByMetric)
    val insight = buildComprehensiveInsight(
        summary = summary,
        slices = slices,
        indicatorWindow = indicatorWindow
    )

    return AnalysisOverviewState(
        summary = summary,
        slices = slices,
        insight = insight
    )
}

private fun buildPieSlices(countByMetric: Map<String, Int>): List<PieSlice> {
    val total = countByMetric.values.sum()
    if (total <= 0) return emptyList()

    val sorted = countByMetric.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }

    val primary = sorted.take(MAX_PIE_SLICES)
    val extra = sorted.drop(MAX_PIE_SLICES)
    val slices = mutableListOf<PieSlice>()

    primary.forEach { entry ->
        val key = entry.key
        val label = metricTitle(key)
        slices += PieSlice(
            label = label,
            count = entry.value,
            ratio = entry.value.toFloat() / total.toFloat(),
            color = metricAccent(key)
        )
    }
    if (extra.isNotEmpty()) {
        val count = extra.sumOf { it.value }
        slices += PieSlice(
            label = "其他",
            count = count,
            ratio = count.toFloat() / total.toFloat(),
            color = Color(0xFFC4CAD5)
        )
    }
    return slices
}

private fun buildComprehensiveInsight(
    summary: AnalysisSummary,
    slices: List<PieSlice>,
    indicatorWindow: List<DailyIndicatorEntity>
): ComprehensiveInsight {
    if (summary.totalEntries == 0 || slices.isEmpty()) {
        return ComprehensiveInsight(
            conclusion = "当前记录样本不足，暂时无法形成稳定趋势判断。",
            suggestion = "建议先连续记录 7 到 14 天，再查看综合分析结果。"
        )
    }

    val top = slices.maxByOrNull { it.count }!!
    val continuity = summary.activeDays.toFloat() / summary.totalDays.toFloat()

    val moodRecords = indicatorWindow.filter { it.metricKey == "心情" }
    val moodWaveRate = if (moodRecords.isEmpty()) 0f else {
        moodRecords.count { it.optionValue in setOf("sensitive", "irritable", "sad") }.toFloat() / moodRecords.size.toFloat()
    }

    val symptomRecords = indicatorWindow.filter { it.metricKey == "症状" }
    val symptomRate = if (symptomRecords.isEmpty()) 0f else {
        symptomRecords.count { it.optionValue != "none" }.toFloat() / symptomRecords.size.toFloat()
    }

    val conclusion = buildString {
        append("最近${summary.totalDays}天以${top.label}记录为主，占比${(top.ratio * 100).toInt()}%。")
        append(
            when {
                continuity >= 0.6f -> "整体记录连续性较好。"
                continuity >= 0.3f -> "记录连续性中等。"
                else -> "记录连续性偏低。"
            }
        )
        when {
            moodWaveRate >= 0.5f && symptomRate >= 0.5f -> append("情绪波动与身体不适记录都偏高。")
            moodWaveRate >= 0.5f -> append("情绪波动记录偏多。")
            symptomRate >= 0.5f -> append("身体不适记录偏多。")
            else -> append("整体状态相对平稳。")
        }
    }

    val suggestion = when {
        continuity < 0.3f -> "建议固定一个每日记录时间，提升连续性后分析会更准确。"
        moodWaveRate >= 0.5f && symptomRate >= 0.5f -> "建议优先保证睡眠和休息，减少高强度安排，并持续观察 1 到 2 周。"
        moodWaveRate >= 0.5f -> "建议增加放松活动和规律作息，记录触发情绪波动的场景。"
        symptomRate >= 0.5f -> "建议减少刺激性饮食，记录不适触发因素，必要时咨询专业医生。"
        else -> "建议继续保持当前记录节奏，每周回看一次变化。"
    }

    return ComprehensiveInsight(
        conclusion = conclusion,
        suggestion = suggestion
    )
}

private fun analysisDayKey(timeMillis: Long): String = DAY_KEY_FORMAT.format(Date(timeMillis))

private fun analysisParseDayKey(key: String): Long {
    return runCatching { DAY_KEY_FORMAT.parse(key)?.time ?: 0L }.getOrDefault(0L)
}

private fun analysisStartOfDay(timeMillis: Long): Long {
    return runCatching {
        DAY_KEY_FORMAT.parse(DAY_KEY_FORMAT.format(Date(timeMillis)))?.time ?: timeMillis
    }.getOrDefault(timeMillis)
}

private fun analysisFormatMonthDay(timeMillis: Long): String = MONTH_DAY_FORMAT.format(Date(timeMillis))

private data class AnalysisOverviewState(
    val summary: AnalysisSummary,
    val slices: List<PieSlice>,
    val insight: ComprehensiveInsight
)

private data class AnalysisSummary(
    val startLabel: String,
    val endLabel: String,
    val totalDays: Int,
    val totalEntries: Int,
    val activeDays: Int
)

private data class PieSlice(
    val label: String,
    val count: Int,
    val ratio: Float,
    val color: Color
)

private data class ComprehensiveInsight(
    val conclusion: String,
    val suggestion: String
)

private val DAY_KEY_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val MONTH_DAY_FORMAT = SimpleDateFormat("M/d", Locale.getDefault())
