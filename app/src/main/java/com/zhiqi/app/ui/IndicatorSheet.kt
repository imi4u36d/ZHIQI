package com.zhiqi.app.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import com.zhiqi.app.data.DailyIndicatorEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IndicatorSheet(
    metricKey: String,
    targetDateMillis: Long = System.currentTimeMillis(),
    initialIndicator: DailyIndicatorEntity? = null,
    onSave: (DailyIndicatorEntity) -> Unit,
    onCancel: () -> Unit
) {
    val accent = metricAccent(metricKey)
    val options = metricOptions(metricKey)
    var selected by remember(metricKey, initialIndicator?.optionValue) {
        mutableStateOf(initialIndicator?.optionValue ?: options.firstOrNull()?.value)
    }
    var selectedLabel by remember(metricKey, initialIndicator?.displayLabel) {
        mutableStateOf(initialIndicator?.displayLabel ?: options.firstOrNull()?.label ?: "")
    }
    var customValue by remember(metricKey, initialIndicator?.optionValue) {
        mutableStateOf(
            when (metricKey) {
                "体温" -> initialIndicator?.optionValue ?: ""
                "体重" -> initialIndicator?.optionValue ?: ""
                "日记" -> initialIndicator?.optionValue ?: ""
                else -> ""
            }
        )
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("取消", color = ZhiQiTokens.TextSecondary, modifier = Modifier.noRippleClickable(onCancel))
            Text("今日${metricTitle(metricKey)}", style = MaterialTheme.typography.titleLarge, color = ZhiQiTokens.TextPrimary)
            Text(
                "确定",
                color = accent,
                modifier = Modifier.noRippleClickable {
                    val value = when (metricKey) {
                        "体温", "体重", "日记" -> customValue.trim().takeIf { it.isNotBlank() } ?: return@noRippleClickable
                        else -> selected ?: return@noRippleClickable
                    }
                    val label = when (metricKey) {
                        "体温" -> "${value}°C"
                        "体重" -> "${value} kg"
                        "日记" -> value
                        else -> selectedLabel
                    }
                    onSave(
                        DailyIndicatorEntity(
                            dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(targetDateMillis)),
                            metricKey = metricKey,
                            optionValue = value,
                            displayLabel = label,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            )
        }

        if (metricKey == "体温" || metricKey == "体重" || metricKey == "日记") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(metricTitle(metricKey), style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
                OutlinedTextField(
                    value = customValue,
                    onValueChange = {
                        customValue = when (metricKey) {
                            "日记" -> it.take(80)
                            "体温" -> it.filter { ch -> ch.isDigit() || ch == '.' }.take(4)
                            "体重" -> it.filter { ch -> ch.isDigit() || ch == '.' }.take(5)
                            else -> it
                        }
                    },
                    label = {
                        Text(
                            when (metricKey) {
                                "体温" -> "输入体温，例如 36.5"
                                "体重" -> "输入体重，例如 52.3"
                                else -> "输入今天的日记内容"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (metricKey == "日记") 4 else 1
                )
            }
        } else if (metricKey != "爱爱") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(metricTitle(metricKey), style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
                options.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { option ->
                            IndicatorChoice(
                                text = option.label,
                                selected = selected == option.value,
                                accent = accent,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selected = option.value
                                    selectedLabel = option.label
                                }
                            )
                        }
                        repeat(3 - row.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndicatorChoice(
    text: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(if (selected) accent.copy(alpha = 0.15f) else ZhiQiTokens.Surface, RoundedCornerShape(18.dp))
            .border(1.dp, if (selected) accent else ZhiQiTokens.Border, RoundedCornerShape(18.dp))
            .noRippleClickable(onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) accent else ZhiQiTokens.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
