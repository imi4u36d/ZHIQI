package com.ganlema.app.ui

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.ganlema.app.security.CryptoManager
import com.ganlema.app.security.PinManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    repository: RecordRepository,
    pinManager: PinManager,
    onOpenDisguise: () -> Unit,
    onOpenSettings: () -> Unit,
    cyclePickerRequest: Int
) {
    val context = LocalContext.current
    var remind by remember { mutableStateOf(false) }
    var clearStep by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    val reminderTime = remember { mutableStateOf("21:00") }
    var show by remember { mutableStateOf(false) }

    var showCycleSheet by remember { mutableStateOf(false) }
    var showCycleSavedDialog by remember { mutableStateOf(false) }
    var showUnlockSheet by remember { mutableStateOf(false) }
    val cycleManager = remember { CycleSettingsManager(context) }

    LaunchedEffect(Unit) { show = true }
    LaunchedEffect(cyclePickerRequest) {
        if (cyclePickerRequest > 0) showCycleSheet = true
    }

    GlassBackground {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileHeader()

            AnimatedVisibility(visible = show, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 7 })) {
                Column(modifier = Modifier.glassCard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MenuRow(
                        icon = Icons.Filled.CalendarMonth,
                        title = "生理期设置",
                        subtitle = "选择日期",
                        onClick = { showCycleSheet = true }
                    )
                    MenuRow(icon = Icons.Filled.Notifications, title = "设置", subtitle = "提醒与偏好", onClick = onOpenSettings)
                    MenuRow(icon = Icons.Filled.Palette, title = "应用伪装", subtitle = "图标与名称", onClick = onOpenDisguise)
                    MenuRow(icon = Icons.Filled.Lock, title = "解锁方式", subtitle = "修改密码", onClick = { showUnlockSheet = true })
                }
            }

            AnimatedVisibility(visible = show, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 })) {
                Column(modifier = Modifier.glassCard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Notifications, contentDescription = "提醒")
                            Column {
                                Text("安全提醒")
                                if (remind) Text("每日 ${reminderTime.value}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Switch(checked = remind, onCheckedChange = { remind = it })
                    }
                    if (remind) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("提醒时间", color = MaterialTheme.colorScheme.secondary)
                            Text(
                                reminderTime.value,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    val parts = reminderTime.value.split(":")
                                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 21
                                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    TimePickerDialog(context, { _, h, m ->
                                        reminderTime.value = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                    }, hour, minute, true).show()
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = show, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 })) {
                Column(modifier = Modifier.glassCard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MenuRow(icon = Icons.Filled.Storage, title = "数据导出")
                    MenuRow(icon = Icons.Filled.Storage, title = "数据导入")
                    Text(
                        text = "一键清除所有数据",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { clearStep = 1 }
                    )
                }
            }

            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.secondary)
        }
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
                        pinManager.clearAll()
                        CryptoManager(context).clearAll()
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

    if (showCycleSavedDialog) {
        AlertDialog(
            onDismissRequest = { showCycleSavedDialog = false },
            title = { Text("保存成功") },
            text = { Text("生理期日期已保存。") },
            confirmButton = { Button(onClick = { showCycleSavedDialog = false }) { Text("确定") } }
        )
    }

    if (showCycleSheet) {
        var cycleDays by remember { mutableStateOf(cycleManager.cycleLengthDays()) }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = cycleManager.lastPeriodStartMillis().takeIf { it > 0L } ?: System.currentTimeMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCycleSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("选择最近一次生理期开始日期", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("周期天数", color = Color(0xFF8A8F98))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("-", modifier = Modifier.noRippleClickable { cycleDays = (cycleDays - 1).coerceAtLeast(21) }, color = Color(0xFF5E5CE6))
                        Text("${cycleDays}天", fontWeight = FontWeight.SemiBold, color = Color(0xFF2F3440))
                        Text("+", modifier = Modifier.noRippleClickable { cycleDays = (cycleDays + 1).coerceAtMost(45) }, color = Color(0xFF5E5CE6))
                    }
                }
                DatePicker(state = pickerState, showModeToggle = false)
                Button(
                    onClick = {
                        val selected = pickerState.selectedDateMillis ?: System.currentTimeMillis()
                        cycleManager.saveAll(
                            cycleLengthDays = cycleDays,
                            periodLengthDays = cycleManager.periodLengthDays(),
                            lastPeriodStartMillis = selected
                        )
                        showCycleSheet = false
                        showCycleSavedDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("保存") }
            }
        }
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
private fun ProfileHeader() {
    val brush = Brush.linearGradient(listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6)))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .background(brush)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("我", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("我的", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("隐私优先 · 本地加密", color = Color.White.copy(alpha = 0.9f))
            }
        }
        Text("安全", color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title)
                if (subtitle != null) Text(subtitle, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = "arrow", tint = MaterialTheme.colorScheme.secondary)
    }
}
