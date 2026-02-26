package com.ganlema.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FilterScreen(
    initialState: FilterState,
    onApply: (FilterState) -> Unit,
    onBack: () -> Unit
) {
    var filterType by remember { mutableStateOf(initialState.types) }
    var filterProtection by remember { mutableStateOf(initialState.protections) }
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }

    GlassBackground {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("条件筛选", style = MaterialTheme.typography.titleLarge)

            AnimatedVisibility(visible = show, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 })) {
                Column(modifier = Modifier.glassCard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("行为类型")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = filterType.contains("同房"), onCheckedChange = { checked ->
                            filterType = if (checked) filterType + "同房" else filterType - "同房"
                        })
                        Text("同房")
                        Spacer(modifier = Modifier.weight(1f))
                        Checkbox(checked = filterType.contains("导管"), onCheckedChange = { checked ->
                            filterType = if (checked) filterType + "导管" else filterType - "导管"
                        })
                        Text("导管")
                    }
                }
            }

            AnimatedVisibility(visible = show, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })) {
                Column(modifier = Modifier.glassCard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("防护措施")
                    listOf("避孕套", "短效避孕药", "长效避孕", "体外", "无防护").forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = filterProtection.contains(item), onCheckedChange = { checked ->
                                filterProtection = if (checked) filterProtection + item else filterProtection - item
                            })
                            Text(item)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onBack, modifier = Modifier.weight(1f)) { Text("返回") }
                Button(
                    onClick = { onApply(FilterState(filterType, filterProtection)) },
                    modifier = Modifier.weight(1f)
                ) { Text("应用") }
            }
        }
    }
}
