package com.zhiqi.app.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.NumberPicker
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.zhiqi.app.data.RecordRepository
import com.zhiqi.app.data.DailyIndicatorRepository
import com.zhiqi.app.security.CryptoManager
import com.zhiqi.app.security.PinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
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
    val reminderPrefs = remember { ReminderPrefsManager(context) }
    val backupManager = remember { BackupManager(context, repository, indicatorRepository, cycleManager, pinManager) }
    val records by repository.records().collectAsState(initial = emptyList())
    val pinConfigured by pinManager.pinConfigured.collectAsState()
    val passwordEnabled by pinManager.passwordEnabled.collectAsState()

    var remind by remember { mutableStateOf(reminderPrefs.isEnabled()) }
    var clearStep by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val reminderTime = remember { mutableStateOf(reminderPrefs.reminderTime()) }

    var showUnlockSheet by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            remind = true
            reminderPrefs.saveEnabled(true)
            ReminderScheduler.schedule(context, reminderTime.value)
            message = "安全提醒已开启"
        } else {
            remind = false
            reminderPrefs.saveEnabled(false)
            ReminderScheduler.cancel(context)
            message = "未授予通知权限，提醒未开启"
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isWorking = true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { backupManager.exportTo(uri) } }
                .onSuccess { summary ->
                    message = "已导出 ${summary.recordCount} 条记录、${summary.indicatorCount} 条指标"
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
                    pinConfigured = pinConfigured && passwordEnabled
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
                    PasswordToggleRow(
                        enabled = passwordEnabled,
                        pinConfigured = pinConfigured,
                        onToggle = { enabled ->
                            pinManager.setPasswordEnabled(enabled)
                            if (!enabled) {
                                showUnlockSheet = false
                                message = "密码功能已关闭"
                            } else {
                                message = if (pinConfigured) {
                                    "密码功能已开启"
                                } else {
                                    "密码功能已开启，请先设置解锁密码"
                                }
                            }
                        }
                    )
                    MenuRow(
                        icon = Icons.Filled.Lock,
                        title = "解锁方式",
                        subtitle = when {
                            !passwordEnabled -> "密码功能已关闭"
                            pinConfigured -> "已设置数字密码"
                            else -> "未设置密码"
                        },
                        enabled = passwordEnabled,
                        onClick = { showUnlockSheet = true }
                    )
                    ReminderRow(
                        remind = remind,
                        reminderTime = reminderTime.value,
                        onToggle = { enabled ->
                            if (enabled) {
                                val needsPermission =
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                if (needsPermission) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    remind = true
                                    reminderPrefs.saveEnabled(true)
                                    ReminderScheduler.schedule(context, reminderTime.value)
                                    message = "安全提醒已开启"
                                }
                            } else {
                                remind = false
                                reminderPrefs.saveEnabled(false)
                                ReminderScheduler.cancel(context)
                                message = "安全提醒已关闭"
                            }
                        },
                        onPickTime = {
                            val parts = reminderTime.value.split(":")
                            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 21
                            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(context, { _, h: Int, m: Int ->
                                reminderTime.value = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                reminderPrefs.saveTime(reminderTime.value)
                                if (remind) {
                                    ReminderScheduler.schedule(context, reminderTime.value)
                                    message = "提醒时间已更新"
                                }
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
                        subtitle = if (isWorking) "处理中..." else "导出记录、指标和周期设置（不含密码）",
                        enabled = !isWorking,
                        onClick = {
                            exportLauncher.launch(defaultBackupFileName())
                        }
                    )
                    MenuRow(
                        icon = Icons.Filled.Upload,
                        title = "数据导入",
                        subtitle = if (isWorking) "处理中..." else "从备份文件恢复并覆盖本地记录与周期",
                        enabled = !isWorking,
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }
                    )
                    DataStatusBlock(
                        recordCount = records.size,
                        cycleConfigured = cycleManager.isConfigured(),
                        pinConfigured = pinConfigured,
                        passwordEnabled = passwordEnabled
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
                        color = ZhiQiTokens.TextSecondary,
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
            text = { Text("导入会覆盖当前本地记录、指标和周期设置。旧版备份可能同时覆盖密码，是否继续？") },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportUri ?: return@Button
                        pendingImportUri = null
                        isWorking = true
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) { backupManager.importFrom(uri) } }
                                .onSuccess { summary ->
                                    message = "导入完成，恢复 ${summary.recordCount} 条记录、${summary.indicatorCount} 条指标"
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
                        isWorking = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repository.clearAll()
                                indicatorRepository.clearAll()
                                pinManager.clearAll()
                                CryptoManager(context).clearAll()
                                cycleManager.restoreSnapshot(null)
                            }
                            reminderPrefs.save(false, reminderTime.value)
                            ReminderScheduler.cancel(context)
                            remind = false
                            clearStep = 0
                            message = "数据已清除"
                            isWorking = false
                        }
                    }
                }) { Text("继续") }
            },
            dismissButton = {
                Button(onClick = { clearStep = 0 }) { Text("取消") }
            }
        )
    }

    if (showUnlockSheet && passwordEnabled) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var oldPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = { showUnlockSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("解锁方式", style = MaterialTheme.typography.titleMedium)
                if (pinConfigured) {
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
                        if (pinConfigured && !pinManager.verifyPin(oldPin)) {
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
    val cardShape = RoundedCornerShape(24.dp)
    val brush = Brush.linearGradient(
        listOf(
            ZhiQiTokens.AccentSoft,
            ZhiQiTokens.PrimarySoft,
            ZhiQiTokens.AccentStrongerSoft
        )
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush, cardShape)
            .border(1.dp, ZhiQiTokens.BorderStrong, cardShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(ZhiQiTokens.AccentStrongerSoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "我的", tint = ZhiQiTokens.Primary)
                }
                Column {
                    Text(
                        "我的",
                        color = ZhiQiTokens.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "本地优先的数据与隐私管理",
                        color = ZhiQiTokens.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Text(
                text = if (pinConfigured) "已加密" else "未设密码",
                color = ZhiQiTokens.Primary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(start = 10.dp)
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
            .background(ZhiQiTokens.Surface.copy(alpha = 0.94f), RoundedCornerShape(18.dp))
            .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(value, color = ZhiQiTokens.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
            Text(title, style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
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
                    .background(ZhiQiTokens.PrimarySoft, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = "提醒", tint = ZhiQiTokens.Primary)
            }
            Column {
                Text("安全提醒", color = ZhiQiTokens.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (remind) "每日 $reminderTime" else "当前未开启",
                    color = ZhiQiTokens.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (remind) {
                Text(
                    text = reminderTime,
                    color = ZhiQiTokens.Primary,
                    modifier = Modifier.clickable { onPickTime() }
                )
            }
            Switch(checked = remind, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun PasswordToggleRow(
    enabled: Boolean,
    pinConfigured: Boolean,
    onToggle: (Boolean) -> Unit
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
                    .background(ZhiQiTokens.PrimarySoft, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, contentDescription = "密码解锁", tint = ZhiQiTokens.Primary)
            }
            Column {
                Text("密码解锁", color = ZhiQiTokens.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = when {
                        !enabled -> "默认关闭"
                        pinConfigured -> "已开启"
                        else -> "已开启，请设置密码"
                    },
                    color = ZhiQiTokens.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun DataStatusBlock(
    recordCount: Int,
    cycleConfigured: Boolean,
    pinConfigured: Boolean,
    passwordEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZhiQiTokens.SurfaceSoft, RoundedCornerShape(18.dp))
            .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("当前数据概况", color = ZhiQiTokens.TextPrimary, style = MaterialTheme.typography.bodyMedium)
        Text("记录数：$recordCount", color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text("周期配置：${if (cycleConfigured) "已保存" else "未保存"}", color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text("密码功能：${if (passwordEnabled) "开启" else "关闭"}", color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text("解锁密码：${if (pinConfigured) "已设置" else "未设置"}", color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
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
                    .background(ZhiQiTokens.PrimarySoft, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = ZhiQiTokens.Primary)
            }
            Column {
                Text(title, color = ZhiQiTokens.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
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

private enum class CycleSettingsEditor {
    LAST_START_DATE,
    CYCLE_LENGTH,
    PERIOD_LENGTH
}

private data class CycleDateParts(
    val year: Int,
    val month: Int,
    val day: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleSettingsSheet(
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val cycleManager = remember { CycleSettingsManager(context) }
    val initialCycleDays = remember { cycleManager.cycleLengthDays() }
    val initialPeriodDays = remember { cycleManager.periodLengthDays() }
    val initialDateMillis = remember {
        cycleManager.lastPeriodStartMillis().takeIf { it > 0L } ?: System.currentTimeMillis()
    }
    val todayDateParts = remember { cycleDatePartsFromMillis(System.currentTimeMillis()) }
    val initialDateParts = remember {
        normalizeCycleDateParts(cycleDatePartsFromMillis(initialDateMillis), todayDateParts)
    }

    var cycleDays by remember { mutableStateOf(initialCycleDays) }
    var periodDays by remember { mutableStateOf(initialPeriodDays) }
    var selectedDateParts by remember { mutableStateOf(initialDateParts) }
    var expandedEditor by remember { mutableStateOf<CycleSettingsEditor?>(null) }
    val selectedDateMillis = cycleDatePartsToMillis(selectedDateParts)
    val initialDateText = formatCycleDate(initialDateMillis)
    val selectedDateText = formatCycleDate(selectedDateMillis)
    val hasChanges = cycleDays != initialCycleDays ||
        periodDays != initialPeriodDays ||
        selectedDateText != initialDateText

    fun saveCycleSettings() {
        cycleManager.saveAll(
            cycleLengthDays = cycleDays,
            periodLengthDays = periodDays,
            lastPeriodStartMillis = selectedDateMillis
        )
        onSave()
    }

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
            Text("生理周期", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
            Box(modifier = Modifier.size(40.dp))
        }

        CycleSummaryCard(
            cycleDays = cycleDays,
            periodDays = periodDays,
            lastPeriodStartMillis = selectedDateMillis
        )

        SectionCard(title = "基础设置") {
            CycleSettingRow(
                title = "最近一次开始日",
                value = formatCycleDate(selectedDateMillis),
                onClick = {
                    expandedEditor = if (expandedEditor == CycleSettingsEditor.LAST_START_DATE) {
                        null
                    } else {
                        CycleSettingsEditor.LAST_START_DATE
                    }
                }
            )
            if (expandedEditor == CycleSettingsEditor.LAST_START_DATE) {
                Text(
                    "上下滚动选择年月日",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZhiQiTokens.TextSecondary
                )
                CycleDateWheelPicker(
                    value = selectedDateParts,
                    maxValue = todayDateParts,
                    onChange = { selectedDateParts = it }
                )
            }

            CycleSettingRow(
                title = "周期天数",
                value = "$cycleDays 天",
                onClick = {
                    expandedEditor = if (expandedEditor == CycleSettingsEditor.CYCLE_LENGTH) {
                        null
                    } else {
                        CycleSettingsEditor.CYCLE_LENGTH
                    }
                }
            )
            if (expandedEditor == CycleSettingsEditor.CYCLE_LENGTH) {
                Text(
                    "左右滑动选择 21 到 45 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZhiQiTokens.TextSecondary
                )
                DayValuePicker(
                    valueRange = 21..45,
                    selectedValue = cycleDays,
                    onSelect = { cycleDays = it }
                )
            }

            CycleSettingRow(
                title = "经期天数",
                value = "$periodDays 天",
                onClick = {
                    expandedEditor = if (expandedEditor == CycleSettingsEditor.PERIOD_LENGTH) {
                        null
                    } else {
                        CycleSettingsEditor.PERIOD_LENGTH
                    }
                }
            )
            if (expandedEditor == CycleSettingsEditor.PERIOD_LENGTH) {
                Text(
                    "左右滑动选择 2 到 10 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZhiQiTokens.TextSecondary
                )
                DayValuePicker(
                    valueRange = 2..10,
                    selectedValue = periodDays,
                    onSelect = { periodDays = it }
                )
            }

            Text(
                text = "恢复推荐值",
                color = ZhiQiTokens.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        cycleDays = 28
                        periodDays = 5
                    }
                    .padding(vertical = 10.dp)
            )
        }

        Button(
            onClick = { saveCycleSettings() },
            enabled = hasChanges,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (hasChanges) "保存设置" else "已保存")
        }
    }
}

@Composable
private fun CycleSummaryCard(
    cycleDays: Int,
    periodDays: Int,
    lastPeriodStartMillis: Long
) {
    val nextStart = calculateNextCycleStart(lastPeriodStartMillis, cycleDays)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "${cycleDays}天周期 · 经期${periodDays}天",
            style = MaterialTheme.typography.titleMedium,
            color = ZhiQiTokens.TextPrimary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "最近开始：${formatCycleDate(lastPeriodStartMillis)}",
            style = MaterialTheme.typography.bodySmall,
            color = ZhiQiTokens.TextSecondary
        )
        Text(
            text = "下次预计：${formatCycleMonthDay(nextStart)}",
            style = MaterialTheme.typography.bodySmall,
            color = ZhiQiTokens.TextSecondary
        )
    }
}

@Composable
private fun CycleSettingRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = title,
                tint = Color(0xFFB3B8C2)
            )
        }
    }
}

@Composable
private fun CycleDateWheelPicker(
    value: CycleDateParts,
    maxValue: CycleDateParts,
    onChange: (CycleDateParts) -> Unit
) {
    val yearRange = 1900..maxValue.year
    val monthMax = if (value.year == maxValue.year) maxValue.month else 12
    val monthRange = 1..monthMax
    val dayMaxByMonth = maxDayOfMonth(value.year, value.month)
    val dayMax = if (value.year == maxValue.year && value.month == maxValue.month) {
        minOf(dayMaxByMonth, maxValue.day)
    } else {
        dayMaxByMonth
    }
    val dayRange = 1..dayMax

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CycleWheelColumn(
            label = "年",
            value = value.year,
            range = yearRange,
            onValueChange = { year ->
                onChange(normalizeCycleDateParts(value.copy(year = year), maxValue))
            },
            modifier = Modifier.weight(1.5f)
        )
        CycleWheelColumn(
            label = "月",
            value = value.month,
            range = monthRange,
            onValueChange = { month ->
                onChange(normalizeCycleDateParts(value.copy(month = month), maxValue))
            },
            modifier = Modifier.weight(1f)
        )
        CycleWheelColumn(
            label = "日",
            value = value.day,
            range = dayRange,
            onValueChange = { day ->
                onChange(normalizeCycleDateParts(value.copy(day = day), maxValue))
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CycleWheelColumn(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = ZhiQiTokens.TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ZhiQiTokens.Surface, RoundedCornerShape(14.dp))
                .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(14.dp))
        ) {
            AndroidView(
                factory = { context ->
                    NumberPicker(context).apply {
                        descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                        wrapSelectorWheel = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(146.dp),
                update = { picker ->
                    val safeRange = if (range.first <= range.last) range else value..value
                    if (picker.minValue != safeRange.first || picker.maxValue != safeRange.last) {
                        picker.minValue = safeRange.first
                        picker.maxValue = safeRange.last
                    }
                    val normalizedValue = value.coerceIn(safeRange.first, safeRange.last)
                    if (picker.value != normalizedValue) {
                        picker.value = normalizedValue
                    }
                    picker.setOnValueChangedListener { _, _, newValue ->
                        if (newValue != value) onValueChange(newValue)
                    }
                }
            )
        }
    }
}

@Composable
private fun DayValuePicker(
    valueRange: IntRange,
    selectedValue: Int,
    onSelect: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(valueRange.toList()) { day ->
            Box(
                modifier = Modifier
                    .background(
                        if (day == selectedValue) ZhiQiTokens.PrimarySoft else ZhiQiTokens.Surface,
                        RoundedCornerShape(18.dp)
                    )
                    .border(
                        1.dp,
                        if (day == selectedValue) ZhiQiTokens.PrimaryStrong else ZhiQiTokens.Border,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelect(day) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$day",
                    color = if (day == selectedValue) ZhiQiTokens.Primary else Color(0xFF5E6674),
                    fontWeight = if (day == selectedValue) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

private fun cycleDatePartsFromMillis(timeMillis: Long): CycleDateParts {
    val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return CycleDateParts(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH)
    )
}

private fun cycleDatePartsToMillis(parts: CycleDateParts): Long {
    val calendar = Calendar.getInstance()
    calendar.clear()
    calendar.set(parts.year, parts.month - 1, parts.day, 12, 0, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun normalizeCycleDateParts(
    parts: CycleDateParts,
    maxValue: CycleDateParts
): CycleDateParts {
    val year = parts.year.coerceIn(1900, maxValue.year)
    var month = parts.month.coerceIn(1, 12)
    if (year == maxValue.year) {
        month = month.coerceAtMost(maxValue.month)
    }
    var day = parts.day.coerceAtLeast(1)
    day = day.coerceAtMost(maxDayOfMonth(year, month))
    if (year == maxValue.year && month == maxValue.month) {
        day = day.coerceAtMost(maxValue.day)
    }
    return CycleDateParts(year = year, month = month, day = day)
}

private fun maxDayOfMonth(year: Int, month: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.clear()
    calendar.set(year, month - 1, 1)
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun formatCycleDate(timeMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeMillis))
}

private fun formatCycleMonthDay(timeMillis: Long): String {
    return SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(timeMillis))
}

private fun calculateNextCycleStart(lastPeriodStartMillis: Long, cycleDays: Int): Long {
    return lastPeriodStartMillis + cycleDays * 24L * 60L * 60L * 1000L
}

private fun defaultBackupFileName(): String {
    val time = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
    return "zhiqi-backup-$time.json"
}
