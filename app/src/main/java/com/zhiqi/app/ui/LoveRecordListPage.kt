package com.zhiqi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhiqi.app.data.RecordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoveRecordListPage(
    targetDateMillis: Long,
    records: List<RecordEntity>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (RecordEntity) -> Unit
) {
    val targetDateKey = remember(targetDateMillis) { loveDayKey(targetDateMillis) }
    val loveRecords = remember(records, targetDateKey) {
        records
            .asSequence()
            .filter { it.type == "同房" && loveDayKey(it.timeMillis) == targetDateKey }
            .sortedBy { it.timeMillis }
            .toList()
    }

    GlassBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LoveRecordTopBar(
                    targetDateMillis = targetDateMillis,
                    onBack = onBack
                )
            }
            itemsIndexed(loveRecords, key = { _, record -> record.id }) { index, record ->
                LoveRecordCard(
                    index = index + 1,
                    record = record,
                    onClick = { onEdit(record) }
                )
            }
            item {
                AddLoveRecordCard(onClick = onAdd)
            }
        }
    }
}

@Composable
private fun LoveRecordTopBar(
    targetDateMillis: Long,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = ZhiQiTokens.TextPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .noRippleClickable(onBack)
            )
            Text(
                text = "爱爱记录",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                color = ZhiQiTokens.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(24.dp))
        }
        Text(
            text = formatDateTitle(targetDateMillis),
            style = MaterialTheme.typography.bodySmall,
            color = ZhiQiTokens.TextSecondary
        )
    }
}

@Composable
private fun LoveRecordCard(
    index: Int,
    record: RecordEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .noRippleClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "第${index}次  ${formatTime(record.timeMillis)}",
            style = MaterialTheme.typography.titleLarge,
            color = ZhiQiTokens.TextPrimary
        )
        Text(
            text = formatProtectionText(record),
            style = MaterialTheme.typography.titleMedium,
            color = ZhiQiTokens.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AddLoveRecordCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .noRippleClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(ZhiQiTokens.Primary.copy(alpha = 0.08f), CircleShape)
                .border(1.dp, ZhiQiTokens.Primary.copy(alpha = 0.32f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "添加爱爱记录",
                tint = ZhiQiTokens.Primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = "添加爱爱记录",
            style = MaterialTheme.typography.titleLarge,
            color = ZhiQiTokens.Primary,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

private fun formatDateTitle(timeMillis: Long): String {
    return SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(timeMillis))
}

private fun formatTime(timeMillis: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))
}

private fun formatProtectionText(record: RecordEntity): String {
    val protections = record.protections
        .split("|")
        .map { normalizeProtectionDisplay(it) }
        .filter { it.isNotBlank() }
    if (protections.isNotEmpty()) return protections.joinToString(" / ")
    return record.otherProtection?.trim().orEmpty().ifBlank { "未记录措施" }
}

private fun normalizeProtectionDisplay(name: String): String {
    return when (name.trim()) {
        "体外排" -> "体外排精"
        "紧急避" -> "紧急避孕药"
        "短效避" -> "短效避孕药"
        "长效避" -> "长效避孕药"
        "其他措" -> "其他措施"
        else -> name.trim()
    }
}

private fun loveDayKey(timeMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeMillis))
}
