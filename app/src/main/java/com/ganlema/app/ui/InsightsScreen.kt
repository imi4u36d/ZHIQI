package com.ganlema.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ganlema.app.data.RecordEntity
import com.ganlema.app.data.RecordRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(repository: RecordRepository) {
    val records by repository.records().collectAsState(initial = emptyList())
    val stats = computeStats(records)
    val showContent = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { showContent.value = true }

    GlassBackground {
        AnimatedVisibility(
            visible = showContent.value,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 })
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    InsightsHero(total = stats.total, protectionRate = stats.protectionRate)
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard("近30天", stats.last30.toString(), Modifier.weight(1f), Color(0xFFEFF4FF), Color(0xFF3F6FD9))
                        StatCard("连续天数", stats.streak.toString(), Modifier.weight(1f), Color(0xFFF0FFFA), Color(0xFF2A9A7A))
                    }
                }
                item {
                    TrendCard(last7 = stats.last7, max = stats.last7Max)
                }
                item {
                    AdviceCard(rate = stats.protectionRate)
                }
                if (stats.recent.isNotEmpty()) {
                    item {
                        Text(
                            "最近记录",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF303542)
                        )
                    }
                }
                items(stats.recent) { rec ->
                    RecentRecordCard(rec)
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
            }
        }
    }
}

@Composable
private fun InsightsHero(total: Int, protectionRate: Int) {
    val gradient = Brush.linearGradient(
        listOf(Color(0xFF6D7BFF), Color(0xFF8E72FF), Color(0xFF9D8BFF))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .glassCard()
            .background(gradient)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("行为洞察", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("总记录", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                    Text(total.toString(), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("防护覆盖率", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                    Text("$protectionRate%", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier,
    bg: Color,
    valueColor: Color
) {
    Column(
        modifier = modifier
            .glassCard()
            .background(bg)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = Color(0xFF7F8895), style = MaterialTheme.typography.labelMedium)
        Text(value, color = valueColor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TrendCard(last7: List<Pair<String, Int>>, max: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("近7天趋势", style = MaterialTheme.typography.titleMedium, color = Color(0xFF303542))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(Color(0xFFF8F9FC), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            TrendLineChart(last7 = last7, max = max)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            last7.forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, color = Color(0xFF8A8F98), style = MaterialTheme.typography.labelSmall)
                    Text(value.toString(), color = Color(0xFF5E5CE6), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AdviceCard(rate: Int) {
    val title: String
    val tip: String
    val levelColor: Color
    if (rate < 60) {
        title = "建议提升防护"
        tip = "近阶段防护覆盖率偏低，建议提高防护记录频率，并结合周期提醒。"
        levelColor = Color(0xFFE76472)
    } else {
        title = "当前状态稳定"
        tip = "防护习惯较稳定，建议继续保持当前节奏，按周复盘趋势变化。"
        levelColor = Color(0xFF2A9A7A)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(levelColor.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("!", color = levelColor, fontWeight = FontWeight.Bold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF303542))
            Text(tip, color = Color(0xFF656D78), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TrendLineChart(last7: List<Pair<String, Int>>, max: Int) {
    val points = last7.map { it.second }
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (points.isEmpty()) return@Canvas
        val width = size.width
        val height = size.height
        val gridColor = Color(0xFFE7EAF2)

        drawLine(gridColor, Offset(0f, height * 0.2f), Offset(width, height * 0.2f), 1f)
        drawLine(gridColor, Offset(0f, height * 0.5f), Offset(width, height * 0.5f), 1f)
        drawLine(gridColor, Offset(0f, height * 0.8f), Offset(width, height * 0.8f), 1f)

        val stepX = width / (points.size - 1).coerceAtLeast(1)
        val linePath = Path()
        val fillPath = Path()
        points.forEachIndexed { index, value ->
            val ratio = if (max <= 0) 0f else value.toFloat() / max.toFloat()
            val x = index * stepX
            val y = height - (ratio * (height * 0.9f)) - (height * 0.05f)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            drawCircle(Color(0xFF6D7BFF), radius = 4f, center = Offset(x, y))
            if (index == points.lastIndex) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        drawPath(fillPath, Color(0x226D7BFF))
        drawPath(linePath, Color(0xFF5E5CE6))
    }
}

@Composable
private fun RecentRecordCard(rec: RecordEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF6D7BFF), shape = CircleShape)
            )
            Text(
                text = "  " + SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(rec.timeMillis)),
                color = Color(0xFF8A8F98),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(rec.type, color = Color(0xFF303542), fontWeight = FontWeight.SemiBold)

        val tags = rec.protections.split("|").filter { it.isNotBlank() }
        if (tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.take(3).forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEFEFFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(tag, color = Color(0xFF5E5CE6), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (!rec.note.isNullOrBlank()) {
            Text("备注：${rec.note}", color = Color(0xFF656D78), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private data class InsightStats(
    val total: Int,
    val last30: Int,
    val streak: Int,
    val protectionRate: Int,
    val last7: List<Pair<String, Int>>,
    val last7Max: Int,
    val recent: List<RecordEntity>
)

private fun computeStats(records: List<RecordEntity>): InsightStats {
    val now = System.currentTimeMillis()
    val day = 24L * 60 * 60 * 1000
    val last30Cut = now - day * 30
    val last30 = records.count { it.timeMillis >= last30Cut }

    val unprotected = records.count { it.protections.split("|").contains("无防护") }
    val protectionRate = if (records.isEmpty()) 0 else (((records.size - unprotected).toFloat() / records.size) * 100).roundToInt()

    val dateSet = records.map { dayKey(it.timeMillis) }.toSet()
    var streak = 0
    val cal = Calendar.getInstance()
    while (true) {
        val key = dayKey(cal.timeInMillis)
        if (dateSet.contains(key)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
    }

    val last7 = mutableListOf<Pair<String, Int>>()
    val cal7 = Calendar.getInstance()
    for (i in 6 downTo 0) {
        cal7.timeInMillis = now - i * day
        val key = dayKey(cal7.timeInMillis)
        val count = records.count { dayKey(it.timeMillis) == key }
        val label = SimpleDateFormat("E", Locale.getDefault()).format(Date(cal7.timeInMillis))
        last7 += label to count
    }

    return InsightStats(
        total = records.size,
        last30 = last30,
        streak = streak,
        protectionRate = protectionRate,
        last7 = last7,
        last7Max = last7.maxOfOrNull { it.second } ?: 0,
        recent = records.take(6)
    )
}

private fun dayKey(timeMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeMillis))
}
