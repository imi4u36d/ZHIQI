package com.zhiqi.app.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zhiqi.app.data.RecordRepository
import com.zhiqi.app.security.CryptoManager
import com.zhiqi.app.security.PinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun SettingsScreen(
    repository: RecordRepository,
    pinManager: PinManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reminderPrefs = remember { ReminderPrefsManager(context) }
    var remind by remember { mutableStateOf(reminderPrefs.isEnabled()) }
    var clearStep by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    val reminderTime = remember { mutableStateOf(reminderPrefs.reminderTime()) }
    val pinConfigured by pinManager.pinConfigured.collectAsState()
    val passwordEnabled by pinManager.passwordEnabled.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            remind = true
            reminderPrefs.saveEnabled(true)
            ReminderScheduler.schedule(context, reminderTime.value)
            message = "提醒已开启"
        } else {
            remind = false
            reminderPrefs.saveEnabled(false)
            ReminderScheduler.cancel(context)
            message = "未授予通知权限，提醒未开启"
        }
    }

    GlassBackground {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("设置", style = MaterialTheme.typography.titleLarge)

            Column(modifier = Modifier.glassCard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("密码功能")
                        Text(
                            text = if (passwordEnabled) "已开启" else "默认关闭",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Switch(
                        checked = passwordEnabled,
                        onCheckedChange = { enabled ->
                            pinManager.setPasswordEnabled(enabled)
                            message = if (enabled) {
                                if (pinConfigured) "密码功能已开启" else "密码功能已开启，请到我的页面设置密码"
                            } else {
                                "密码功能已关闭"
                            }
                        }
                    )
                }
                SettingRow(
                    title = "解锁方式",
                    subtitle = when {
                        !passwordEnabled -> "密码功能已关闭"
                        pinConfigured -> "请到我的页面修改密码"
                        else -> "请到我的页面设置密码"
                    }
                )
            }

            Column(modifier = Modifier.glassCard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("避孕提醒")
                        if (remind) Text("每日 ${reminderTime.value}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = remind,
                        onCheckedChange = { enabled ->
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
                                }
                            } else {
                                remind = false
                                reminderPrefs.saveEnabled(false)
                                ReminderScheduler.cancel(context)
                            }
                        }
                    )
                }
                if (remind) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("提醒时间")
                        Text(
                            reminderTime.value,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.clickable {
                                val parts = reminderTime.value.split(":")
                                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 21
                                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                TimePickerDialog(context, { _, h, m ->
                                    reminderTime.value = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                    reminderPrefs.saveTime(reminderTime.value)
                                    ReminderScheduler.schedule(context, reminderTime.value)
                                }, hour, minute, true).show()
                            }
                        )
                    }
                }
            }

            Text(
                text = "一键清除所有数据",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable { clearStep = 1 }
            )

            Text(
                text = "返回",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable { onBack() }
            )

            if (message.isNotBlank()) {
                Text(message, color = MaterialTheme.colorScheme.secondary)
            }
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
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repository.clearAll()
                                pinManager.clearAll()
                                CryptoManager(context).clearAll()
                            }
                            reminderPrefs.saveEnabled(false)
                            ReminderScheduler.cancel(context)
                            remind = false
                            clearStep = 0
                            message = "数据已清除"
                        }
                    }
                }) { Text("继续") }
            },
            dismissButton = {
                Button(onClick = { clearStep = 0 }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable(enabled = onClick != null) {
            onClick?.invoke()
        },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title)
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Text(">", color = MaterialTheme.colorScheme.secondary)
    }
}
