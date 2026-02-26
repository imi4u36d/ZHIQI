package com.ganlema.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val ALIAS_DEFAULT = "com.ganlema.app.AliasDefault"
private const val ALIAS_TOOL = "com.ganlema.app.AliasTool"
private const val ALIAS_NOTE = "com.ganlema.app.AliasNote"

@Composable
fun AppDisguiseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }

    GlassBackground {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("应用隐藏与伪装", style = MaterialTheme.typography.titleLarge)
            Text("选择一个图标与名称，或隐藏桌面图标。隐藏后需在系统设置中重新启用。")

            AnimatedVisibility(visible = show, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 })) {
                Column(modifier = Modifier.glassCard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        setAlias(context, ALIAS_DEFAULT)
                        message = "已切换为默认图标"
                    }) { Text("默认：干了么") }

                    Button(onClick = {
                        setAlias(context, ALIAS_TOOL)
                        message = "已切换为工具图标"
                    }) { Text("工具") }

                    Button(onClick = {
                        setAlias(context, ALIAS_NOTE)
                        message = "已切换为笔记图标"
                    }) { Text("笔记") }

                    Button(onClick = {
                        disableAllLaunchers(context)
                        message = "已隐藏桌面图标，请在系统设置中重新启用"
                    }) { Text("隐藏桌面图标") }
                }
            }

            if (message.isNotBlank()) {
                Text(message, color = MaterialTheme.colorScheme.secondary)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onBack) { Text("完成") }
            }
        }
    }
}

private fun setAlias(context: Context, alias: String) {
    val pm = context.packageManager
    listOf(ALIAS_DEFAULT, ALIAS_TOOL, ALIAS_NOTE).forEach {
        val state = if (it == alias) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        pm.setComponentEnabledSetting(
            ComponentName(context, it),
            state,
            PackageManager.DONT_KILL_APP
        )
    }
}

private fun disableAllLaunchers(context: Context) {
    val pm = context.packageManager
    listOf(ALIAS_DEFAULT, ALIAS_TOOL, ALIAS_NOTE).forEach {
        pm.setComponentEnabledSetting(
            ComponentName(context, it),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
