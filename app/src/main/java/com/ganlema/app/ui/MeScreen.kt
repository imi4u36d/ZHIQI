package com.ganlema.app.ui

import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ganlema.app.data.RecordRepository
import com.ganlema.app.data.DailyIndicatorRepository
import com.ganlema.app.security.CryptoManager
import com.ganlema.app.security.PinManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    repository: RecordRepository,
    indicatorRepository: DailyIndicatorRepository,
    pinManager: PinManager,
    cycleSettingsVersion: Int,
    onOpenCycleSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cycleManager = remember { CycleSettingsManager(context) }
    val backupManager = remember { BackupManager(context, repository, cycleManager, pinManager) }
    val records by repository.records().collectAsState(initial = emptyList())

    var remind by remember { mutableStateOf(false) }
    var clearStep by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val reminderTime = remember { mutableStateOf("21:00") }

    var showUnlockSheet by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isWorking = true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { backupManager.exportTo(uri) } }
                .onSuccess { summary ->
                    message = "已导出 ${summary.recordCount} 条记录备份"
                }
                .onFailure { error ->
                    message = error.message ?: "导出失败"
                }
            isWorking = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingImportUri = uri
    }

    GlassBackground {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ProfileHeader(
                    recordCount = records.size,
                    cycleConfigured = cycleManager.isConfigured(),
                    pinConfigured = pinManager.isPinSet()
                )
            }

            item {
                SectionCard(title = "隐私与周期") {
                    MenuRow(
                        icon = Icons.Filled.CalendarMonth,
                        title = "生理期设置",
                        subtitle = cycleSummary(cycleManager, cycleSettingsVersion),
                        onClick = onOpenCycleSettings
                    )
                    MenuRow(
                        icon = Icons.Filled.Lock,
                        title = "解锁方式",
                        subtitle = if (pinManager.isPinSet()) "已设置数字密码" else "未设置密码",
                        onClick = { showUnlockSheet = true }
                    )
                    ReminderRow(
                        remind = remind,
                        reminderTime = reminderTime.value,
                        onToggle = { remind = it },
                        onPickTime = {
                            val parts = reminderTime.value.split(":")
                            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 21
                            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(context, { _, h: Int, m: Int ->
                                reminderTime.value = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                            }, hour, minute, true).show()
                        }
                    )
                }
            }

            item {
                SectionCard(title = "数据管理") {
                    MenuRow(
                        icon = Icons.Filled.Download,
                        title = "数据导出",
                        subtitle = if (isWorking) "处理中..." else "导出记录、周期设置和解锁密码",
                        enabled = !isWorking,
                        onClick = {
                            exportLauncher.launch(defaultBackupFileName())
                        }
                    )
                    MenuRow(
                        icon = Icons.Filled.Upload,
                        title = "数据导入",
                        subtitle = if (isWorking) "处理中..." else "从备份文件恢复并覆盖本地数据",
                        enabled = !isWorking,
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }
                    )
                    DataStatusBlock(
                        recordCount = records.size,
                        cycleConfigured = cycleManager.isConfigured(),
                        pinConfigured = pinManager.isPinSet()
                    )
                }
            }

            item {
                SectionCard(title = "危险操作") {
                    Text(
                        text = "一键清除所有数据",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clearStep = 1 }
                            .padding(vertical = 12.dp)
                    )
                }
            }

            if (message.isNotBlank()) {
                item {
                    Text(
                        text = message,
                        color = Color(0xFF4D5765),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("导入备份") },
            text = { Text("导入会覆盖当前本地记录、周期设置和解锁密码，是否继续？") },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportUri ?: return@Button
                        pendingImportUri = null
                        isWorking = true
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) { backupManager.importFrom(uri) } }
                                .onSuccess { summary ->
                                    message = "导入完成，恢复 ${summary.recordCount} 条记录"
                                }
                                .onFailure { error ->
                                    message = error.message ?: "导入失败"
                                }
                            isWorking = false
                        }
                    }
                ) { Text("继续导入") }
            },
            dismissButton = {
                Button(onClick = { pendingImportUri = null }) { Text("取消") }
            }
        )
    }

    if (clearStep in 1..3) {
        val title = when (clearStep) {
            1 -> "确认清除所有数据？"
            2 -> "再次确认，数据不可恢复"
            else -> "最后一次确认，是否继续？"
        }
        AlertDialog(
            onDismissRequest = { clearStep = 0 },
            title = { Text(title) },
            text = { Text("此操作会清除所有记录、密码和设置，且无法恢复。") },
            confirmButton = {
                Button(onClick = {
                    if (clearStep < 3) {
                        clearStep += 1
                    } else {
                        CoroutineScope(Dispatchers.IO).launch { repository.clearAll() }
                        CoroutineScope(Dispatchers.IO).launch { indicatorRepository.clearAll() }
                        pinManager.clearAll()
                        CryptoManager(context).clearAll()
                        cycleManager.restoreSnapshot(null)
                        clearStep = 0
                        message = "数据已清除"
                    }
                }) { Text("继续") }
            },
            dismissButton = {
                Button(onClick = { clearStep = 0 }) { Text("取消") }
            }
        )
    }

    if (showUnlockSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var oldPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = { showUnlockSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("解锁方式", style = MaterialTheme.typography.titleMedium)
                if (pinManager.isPinSet()) {
                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) oldPin = it },
                        label = { Text("当前密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
                    label = { Text("新密码（4-6位数字）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
                    label = { Text("确认新密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )

                if (pinError != null) {
                    Text(pinError!!, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        if (pinManager.isPinSet() && !pinManager.verifyPin(oldPin)) {
                            pinError = "当前密码错误"
                            return@Button
                        }
                        if (newPin.length < 4) {
                            pinError = "新密码至少4位"
                            return@Button
                        }
                        if (newPin != confirmPin) {
                            pinError = "两次输入不一致"
                            return@Button
                        }
                        pinManager.setPin(newPin)
                        pinError = null
                        message = "解锁密码已更新"
                        showUnlockSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("保存") }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    recordCount: Int,
    cycleConfigured: Boolean,
    pinConfigured: Boolean
) {
    val brush = Brush.linearGradient(listOf(Color(0xFFFFD5C2), Color(0xFFF2A3A0), Color(0xFFD66A9A)))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .background(brush, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.24f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "我的", tint = Color.White)
                }
                Column {
                    Text("我的", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("本地优先的数据与隐私管理", color = Color.White.copy(alpha = 0.92f))
                }
            }
            Text(
                text = if (pinConfigured) "已加密" else "未设密码",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryPill(label = "记录", value = recordCount.toString(), modifier = Modifier.weight(1f))
            SummaryPill(label = "周期", value = if (cycleConfigured) "已配置" else "未配置", modifier = Modifier.weight(1f))
            SummaryPill(label = "安全", value = if (pinConfigured) "开启" else "关闭", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))
            content()
        }
    )
}

@Composable
private fun ReminderRow(
    remind: Boolean,
    reminderTime: String,
    onToggle: (Boolean) -> Unit,
    onPickTime: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFFF8E4EA), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = "提醒", tint = Color(0xFFD66A9A))
            }
            Column {
                Text("安全提醒", color = Color(0xFF2F3440), fontWeight = FontWeight.SemiBold)
                Text(
                    if (remind) "每日 $reminderTime" else "当前未开启",
                    color = Color(0xFF7C8593),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (remind) {
                Text(
                    text = reminderTime,
                    color = Color(0xFFD66A9A),
                    modifier = Modifier.clickable { onPickTime() }
                )
            }
            Switch(checked = remind, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun DataStatusBlock(
    recordCount: Int,
    cycleConfigured: Boolean,
    pinConfigured: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F4F6), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("当前数据概况", color = Color(0xFF2F3440), fontWeight = FontWeight.SemiBold)
        Text("记录数：$recordCount", color = Color(0xFF697180), style = MaterialTheme.typography.bodySmall)
        Text("周期配置：${if (cycleConfigured) "已保存" else "未保存"}", color = Color(0xFF697180), style = MaterialTheme.typography.bodySmall)
        Text("解锁密码：${if (pinConfigured) "已设置" else "未设置"}", color = Color(0xFF697180), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clickable(enabled = enabled && onClick != null) { onClick?.invoke() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFFF8E4EA), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = Color(0xFFD66A9A))
            }
            Column {
                Text(title, color = Color(0xFF2F3440), fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, color = Color(0xFF7C8593), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = title,
            tint = if (enabled) Color(0xFFB3B8C2) else Color(0xFFD0D4DA)
        )
    }
}

@Suppress("UNUSED_PARAMETER")
private fun cycleSummary(cycleManager: CycleSettingsManager, cycleSettingsVersion: Int): String {
    if (!cycleManager.isConfigured()) return "未设置"
    val lastStart = cycleManager.lastPeriodStartMillis()
    val dateText = if (lastStart > 0L) {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(lastStart))
    } else {
        "未记录"
    }
    return "${cycleManager.cycleLengthDays()}天周期 · 最近 $dateText"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleSettingsSheet(
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val cycleManager = remember { CycleSettingsManager(context) }
    var cycleDays by remember { mutableStateOf(cycleManager.cycleLengthDays()) }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = cycleManager.lastPeriodStartMillis().takeIf { it > 0L } ?: System.currentTimeMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "取消",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF666D79),
                modifier = Modifier.clickable { onCancel() }.padding(vertical = 6.dp)
            )
            Text("生理周期", style = MaterialTheme.typography.titleLarge, color = Color(0xFF2F3440))
            Text(
                text = "保存",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFD66A9A),
                modifier = Modifier.clickable {
                    val selected = pickerState.selectedDateMillis ?: System.currentTimeMillis()
                    cycleManager.saveAll(
                        cycleLengthDays = cycleDays,
                        periodLengthDays = cycleManager.periodLengthDays(),
                        lastPeriodStartMillis = selected
                    )
                    onSave()
                }.padding(vertical = 6.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFFBFD), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFF2DCE6), RoundedCornerShape(24.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("最近一次开始日", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))
            Text("仅可选择今天及更早的日期", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A8F98))
            DatePicker(
                state = pickerState,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = Color(0xFFEF5D95),
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = Color(0xFFD66A9A),
                    todayContentColor = Color(0xFFD66A9A)
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFFBFD), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFF2DCE6), RoundedCornerShape(24.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("周期天数", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))
                    Text("左右滑动选择 21 到 45 天", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A8F98))
                }
                Text("$cycleDays 天", color = Color(0xFFD66A9A), fontWeight = FontWeight.Bold)
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items((21..45).toList()) { day ->
                    Box(
                        modifier = Modifier
                            .background(
                                if (day == cycleDays) Color(0xFFFFE5EF) else Color.White,
                                RoundedCornerShape(18.dp)
                            )
                            .border(
                                1.dp,
                                if (day == cycleDays) Color(0xFFEF5D95) else Color(0xFFE8E3E8),
                                RoundedCornerShape(18.dp)
                            )
                            .clickable { cycleDays = day }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$day",
                            color = if (day == cycleDays) Color(0xFFD66A9A) else Color(0xFF5E6674),
                            fontWeight = if (day == cycleDays) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                val selected = pickerState.selectedDateMillis ?: System.currentTimeMillis()
                cycleManager.saveAll(
                    cycleLengthDays = cycleDays,
                    periodLengthDays = cycleManager.periodLengthDays(),
                    lastPeriodStartMillis = selected
                )
                onSave()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存设置")
        }
    }
}

private fun defaultBackupFileName(): String {
    val time = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
    return "ganlema-backup-$time.json"
}
