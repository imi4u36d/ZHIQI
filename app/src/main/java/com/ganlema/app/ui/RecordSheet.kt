package com.ganlema.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.ganlema.app.data.RecordEntity
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
    ProtectionUi("避孕套", Icons.Filled.Shield, Color(0xFF4F86F7)),
    ProtectionUi("短效避孕药", Icons.Filled.LocalHospital, Color(0xFF2DAA8A)),
    ProtectionUi("长效避孕", Icons.Filled.Security, Color(0xFF6E6CE5)),
    ProtectionUi("体外", Icons.Filled.DirectionsRun, Color(0xFFE39A32)),
    ProtectionUi("无防护", Icons.Filled.WarningAmber, Color(0xFFE05B66)),
    ProtectionUi("其他", Icons.Filled.Edit, Color(0xFF7C8798))
)

@Composable
fun RecordSheet(
    initialRecord: RecordEntity? = null,
    onSave: (RecordEntity) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(initialRecord?.type) }
    var protections by remember {
        mutableStateOf(initialRecord?.protections?.split("|")?.filter { it.isNotBlank() }?.toSet() ?: emptySet())
    }
    var otherProtection by remember { mutableStateOf(initialRecord?.otherProtection ?: "") }
    var note by remember { mutableStateOf(initialRecord?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    var selectedTimeMillis by remember { mutableStateOf(initialRecord?.timeMillis ?: System.currentTimeMillis()) }

    Column(
        modifier = Modifier
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (initialRecord == null) "新增记录" else "编辑记录",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF2F3440)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("行为类型", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SelectButton("同房", selected = type == "同房", modifier = Modifier.weight(1f)) { type = "同房" }
                SelectButton("导管", selected = type == "导管", modifier = Modifier.weight(1f)) { type = "导管" }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("防护措施（可多选）", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))

            protectionOptions.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { item ->
                        ProtectionButton(
                            item = item,
                            selected = protections.contains(item.name),
                            modifier = Modifier.weight(1f)
                        ) {
                            protections = if (protections.contains(item.name)) protections - item.name else protections + item.name
                        }
                    }
                }
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("记录时间", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF6F7FA), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE2E5EC), RoundedCornerShape(12.dp))
                    .noRippleClickable {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimeMillis }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                cal.set(Calendar.YEAR, y)
                                cal.set(Calendar.MONTH, m)
                                cal.set(Calendar.DAY_OF_MONTH, d)
                                TimePickerDialog(
                                    context,
                                    { _, hh, mm ->
                                        cal.set(Calendar.HOUR_OF_DAY, hh)
                                        cal.set(Calendar.MINUTE, mm)
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
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("点击选择日期和时间", color = Color(0xFF8A8F98), style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(selectedTimeMillis)),
                        color = Color(0xFF2F3440),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 10) note = it },
                label = { Text("备注（10字内）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SelectButton("取消", selected = false, modifier = Modifier.weight(1f), onClick = onCancel)
            Button(
                onClick = {
                    if (type == null) {
                        error = "请选择行为类型"
                        return@Button
                    }
                    if (protections.isEmpty()) {
                        error = "请选择防护措施"
                        return@Button
                    }

                    val oneYearAgo = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000
                    if (selectedTimeMillis !in oneYearAgo..System.currentTimeMillis()) {
                        error = "记录时间需在最近一年内"
                        return@Button
                    }

                    val record = RecordEntity(
                        id = initialRecord?.id ?: 0,
                        type = type!!,
                        protections = protections.joinToString("|"),
                        otherProtection = otherProtection.ifBlank { null },
                        timeMillis = selectedTimeMillis,
                        note = note.ifBlank { null }
                    )
                    onSave(record)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5E5CE6),
                    contentColor = Color.White
                )
            ) {
                Text("保存")
            }
        }
    }
}

@Composable
private fun SelectButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (selected) Color(0xFF5E5CE6) else Color(0xFFF4F5F8)
    val fg = if (selected) Color.White else Color(0xFF4A5160)
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(12.dp))
            .noRippleClickable(onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = fg, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
private fun ProtectionButton(
    item: ProtectionUi,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) item.color.copy(alpha = 0.15f) else Color(0xFFF6F7FA)
    val border = if (selected) item.color else Color(0xFFE2E5EC)
    val textColor = if (selected) item.color else Color(0xFF576072)

    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .noRippleClickable(onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, contentDescription = item.name, tint = textColor)
        Text(
            text = item.name,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
