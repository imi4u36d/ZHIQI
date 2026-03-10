package com.zhiqi.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import com.zhiqi.app.data.RecordEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class ProtectionUi(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

private val protectionOptions = listOf(
    ProtectionUi("避孕套", Icons.Filled.Shield, Color(0xFFE08DB4)),
    ProtectionUi("短效避孕药", Icons.Filled.LocalHospital, Color(0xFFC47CA2)),
    ProtectionUi("长效避孕", Icons.Filled.Security, Color(0xFFF0A35E)),
    ProtectionUi("体外", Icons.Filled.DirectionsRun, Color(0xFFE39A32)),
    ProtectionUi("无防护", Icons.Filled.WarningAmber, Color(0xFFE05B66)),
    ProtectionUi("其他", Icons.Filled.Edit, Color(0xFF7C8798))
)

@Composable
fun RecordSheet(
    initialRecord: RecordEntity? = null,
    initialTimeMillis: Long = System.currentTimeMillis(),
    entryContext: String? = null,
    onSave: (RecordEntity) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(initialRecord?.type ?: if (entryContext == "爱爱") "同房" else null) }
    var protections by remember {
        mutableStateOf(initialRecord?.protections?.split("|")?.filter { it.isNotBlank() }?.toSet() ?: emptySet())
    }
    var otherProtection by remember { mutableStateOf(initialRecord?.otherProtection ?: "") }
    var note by remember { mutableStateOf(initialRecord?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTimeMillis by remember { mutableStateOf(initialRecord?.timeMillis ?: initialTimeMillis) }
    val scrollState = rememberScrollState()

    val title = when {
        initialRecord != null -> "编辑记录"
        !entryContext.isNullOrBlank() -> "今日${entryContext}记录"
        else -> "新增记录"
    }

    fun submit() {
        if (type == null) {
            error = "请选择行为类型"
            return
        }
        if (protections.isEmpty()) {
            error = "请选择防护措施"
            return
        }
        val oneYearAgo = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000
        if (selectedTimeMillis !in oneYearAgo..System.currentTimeMillis()) {
            error = "记录时间需在最近一年内"
            return
        }
        error = null
        onSave(
            RecordEntity(
                id = initialRecord?.id ?: 0,
                type = type!!,
                protections = protections.joinToString("|"),
                otherProtection = otherProtection.ifBlank { null },
                timeMillis = selectedTimeMillis,
                note = note.ifBlank { null }
            )
        )
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "取消",
                style = MaterialTheme.typography.titleMedium,
                color = ZhiQiTokens.TextSecondary,
                modifier = Modifier.noRippleClickable(onCancel)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = ZhiQiTokens.TextPrimary
            )
            Text(
                text = "确定",
                style = MaterialTheme.typography.titleMedium,
                color = ZhiQiTokens.Primary,
                modifier = Modifier.noRippleClickable { submit() }
            )
        }

        if (entryContext != "爱爱") {
            SectionCard(highlighted = false) {
                Text("行为", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SelectButton("同房", selected = type == "同房", modifier = Modifier.weight(1f)) { type = "同房" }
                    SelectButton("导管", selected = type == "导管", modifier = Modifier.weight(1f)) { type = "导管" }
                }
            }
        }

        SectionCard(
            highlighted = entryContext == "流量" || entryContext == "颜色" || entryContext == "痛经" || entryContext == "导管"
        ) {
            Text("措施", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            protectionOptions.chunked(3).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { item ->
                        ProtectionCircleButton(
                            item = item,
                            selected = protections.contains(item.name),
                            modifier = Modifier.weight(1f)
                        ) {
                            protections = if (protections.contains(item.name)) protections - item.name else protections + item.name
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            if (protections.contains("其他")) {
                OutlinedTextField(
                    value = otherProtection,
                    onValueChange = { if (it.length <= 10) otherProtection = it },
                    label = { Text("其他说明（10字内）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SectionCard(highlighted = entryContext == "记录具体时间") {
            Text("时间", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            DateTimeCard(
                selectedTimeMillis = selectedTimeMillis,
                onPick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedTimeMillis }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            cal.set(Calendar.YEAR, year)
                            cal.set(Calendar.MONTH, month)
                            cal.set(Calendar.DAY_OF_MONTH, day)
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    cal.set(Calendar.HOUR_OF_DAY, hour)
                                    cal.set(Calendar.MINUTE, minute)
                                    cal.set(Calendar.MILLISECOND, 0)
                                    selectedTimeMillis = cal.timeInMillis
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            )
        }

        SectionCard(highlighted = entryContext == "日记" || entryContext == "心情" || entryContext == "症状") {
            Text("备注", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 10) note = it },
                label = {
                    Text(
                        when (entryContext) {
                            "心情" -> "记录一下当前情绪"
                            "症状" -> "补充身体感受"
                            else -> "备注（10字内）"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
        }

    }
}

@Composable
private fun SectionCard(
    highlighted: Boolean,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZhiQiTokens.Surface, RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = if (highlighted) ZhiQiTokens.BorderStrong else ZhiQiTokens.Border,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
private fun SelectButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (selected) ZhiQiTokens.PrimaryStrong else ZhiQiTokens.SurfaceSoft
    val fg = if (selected) Color.White else ZhiQiTokens.TextSecondary
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(14.dp))
            .noRippleClickable(onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = fg, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
private fun DateTimeCard(
    selectedTimeMillis: Long,
    onPick: () -> Unit
) {
    val dateText = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(selectedTimeMillis))
    val timeText = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(selectedTimeMillis))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        TimeToken(icon = Icons.Filled.CalendarMonth, text = dateText, modifier = Modifier.weight(1f))
        TimeToken(icon = Icons.Filled.AccessTime, text = timeText, modifier = Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(10.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZhiQiTokens.AccentSoft, RoundedCornerShape(14.dp))
            .noRippleClickable(onPick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("选择日期与时间", color = ZhiQiTokens.Primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TimeToken(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(ZhiQiTokens.SurfaceSoft, RoundedCornerShape(14.dp))
            .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = text, tint = ZhiQiTokens.Primary, modifier = Modifier.size(18.dp))
        Text(text, color = ZhiQiTokens.TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProtectionCircleButton(
    item: ProtectionUi,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) item.color.copy(alpha = 0.16f) else ZhiQiTokens.SurfaceSoft
    val border = if (selected) item.color else ZhiQiTokens.Border
    val textColor = if (selected) item.color else ZhiQiTokens.TextSecondary

    Column(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .noRippleClickable(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(bg, CircleShape)
                .border(1.dp, border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, contentDescription = item.name, tint = textColor)
        }
        Text(
            text = item.name,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
