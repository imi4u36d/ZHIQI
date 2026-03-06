package com.ganlema.app.ui

import android.app.TimePickerDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ganlema.app.data.RecordRepository
import com.ganlema.app.security.CryptoManager
import com.ganlema.app.security.PinManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(
    repository: RecordRepository,
    pinManager: PinManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var remind by remember { mutableStateOf(false) }
    var clearStep by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    val reminderTime = remember { mutableStateOf("21:00") }

    GlassBackground {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("设置", style = MaterialTheme.typography.titleLarge)

            Column(modifier = Modifier.glassCard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingRow(title = "解锁方式", subtitle = "请到我的页面设置")
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
                    Switch(checked = remind, onCheckedChange = { remind = it })
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
                        CoroutineScope(Dispatchers.IO).launch {
                            repository.clearAll()
                        }
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
