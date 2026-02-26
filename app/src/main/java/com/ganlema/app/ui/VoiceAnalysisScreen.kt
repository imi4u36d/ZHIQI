package com.ganlema.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

private const val SAMPLE_INTERVAL_MS = 100L
private const val MERGE_GAP_SECONDS = 10
private const val MERGE_GAP_SAMPLES = (MERGE_GAP_SECONDS * 1000 / SAMPLE_INTERVAL_MS).toInt()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAnalysisScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val historyStore = remember { VoiceHistoryStore(context) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(hasAnyLocationPermission(context))
    }

    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var sampleJob by remember { mutableStateOf<Job?>(null) }
    var amplitudes by remember { mutableStateOf<List<Float>>(emptyList()) }
    var report by remember { mutableStateOf<VoiceReport?>(null) }
    var recordingStartMillis by remember { mutableStateOf<Long?>(null) }
    var recordingEndMillis by remember { mutableStateOf<Long?>(null) }
    var pendingSave by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    var showHistory by remember { mutableStateOf(false) }
    var histories by remember { mutableStateOf(historyStore.load()) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted || hasAnyLocationPermission(context)
    }

    fun stopRecordingAndAnalyze() {
        sampleJob?.cancel()
        sampleJob = null

        val currentRecorder = recorder
        recorder = null
        isRecording = false

        if (currentRecorder != null) {
            try {
                currentRecorder.stop()
            } catch (_: Exception) {
            }
            currentRecorder.release()
        }

        recordingEndMillis = System.currentTimeMillis()
        report = analyzeVoice(
            samples = amplitudes,
            startMillis = recordingStartMillis,
            endMillis = recordingEndMillis
        )
        pendingSave = amplitudes.isNotEmpty()
    }

    fun startRecording() {
        if (!hasAudioPermission) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        report = null
        pendingSave = false
        amplitudes = emptyList()
        recordingStartMillis = System.currentTimeMillis()
        recordingEndMillis = null

        val outputFile = "${context.cacheDir.absolutePath}/voice_${System.currentTimeMillis()}.m4a"
        val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile)
                prepare()
                start()
            }
        } catch (_: Exception) {
            newRecorder.release()
            return
        }

        recorder = newRecorder
        isRecording = true

        sampleJob = scope.launch {
            while (isActive && isRecording) {
                val amp = (recorder?.maxAmplitude ?: 0).toFloat()
                val normalized = (amp / 32767f).coerceIn(0f, 1f)
                amplitudes = (amplitudes + normalized).takeLast(6000)
                delay(SAMPLE_INTERVAL_MS)
            }
        }
    }

    fun deleteCurrentResult() {
        report = null
        pendingSave = false
        amplitudes = emptyList()
        recordingStartMillis = null
        recordingEndMillis = null
    }

    fun saveCurrentResult() {
        val currentReport = report ?: return
        val startMs = recordingStartMillis ?: return
        val endMs = recordingEndMillis ?: return
        if (saving) return

        saving = true
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        scope.launch(Dispatchers.IO) {
            val location = resolveLocationLabel(context, hasAnyLocationPermission(context))
            val storedPoints = downsample(amplitudes, 900)
            val item = VoiceHistoryItem(
                id = System.currentTimeMillis(),
                savedAtMillis = System.currentTimeMillis(),
                startMillis = startMs,
                endMillis = endMs,
                locationLabel = location,
                amplitudes = storedPoints,
                climaxSegments = currentReport.climaxSegments.map { it.startIndex to it.endIndex },
                peakPercent = currentReport.peakPercent,
                peakSecond = currentReport.peakSecond,
                activePercent = currentReport.activePercent,
                climaxCount = currentReport.climaxCount,
                climaxTotalSecond = currentReport.climaxTotalSecond,
                longestClimaxSecond = currentReport.longestClimaxSecond,
                summary = currentReport.summary,
                maleSummary = currentReport.maleInsight.summary,
                femaleSummary = currentReport.femaleInsight.summary
            )
            historyStore.append(item)

            val loaded = historyStore.load()
            scope.launch {
                histories = loaded
                pendingSave = false
                saving = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sampleJob?.cancel()
            try {
                recorder?.stop()
            } catch (_: Exception) {
            }
            recorder?.release()
        }
    }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VoiceHero(
                isRecording = isRecording,
                hasAudioPermission = hasAudioPermission,
                startLabel = formatTime(recordingStartMillis),
                endLabel = if (isRecording) "--:--:--" else formatTime(recordingEndMillis),
                showPersistActions = pendingSave,
                saving = saving,
                onStart = { startRecording() },
                onStop = { stopRecordingAndAnalyze() },
                onSave = { saveCurrentResult() },
                onDelete = { deleteCurrentResult() },
                onRequestPermission = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                onOpenHistory = {
                    histories = historyStore.load()
                    showHistory = true
                }
            )

            VoiceChartCard(
                title = if (isRecording) "实时声线变化" else "声音折线图",
                points = amplitudes,
                highlightSegments = if (isRecording) emptyList() else report?.climaxSegments ?: emptyList(),
                recording = isRecording
            )

            VoiceReportCard(report = report, hasData = amplitudes.isNotEmpty() && !isRecording)
        }
    }

    if (showHistory) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            VoiceHistorySheet(
                histories = histories,
                onDeleteItem = { id ->
                    historyStore.delete(id)
                    histories = historyStore.load()
                }
            )
        }
    }
}

@Composable
private fun VoiceHero(
    isRecording: Boolean,
    hasAudioPermission: Boolean,
    startLabel: String,
    endLabel: String,
    showPersistActions: Boolean,
    saving: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.GraphicEq, contentDescription = "录音分析", tint = Color(0xFF5E5CE6))
                Text(
                    text = "录音分析",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2F3440)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isRecording) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFE76472), shape = CircleShape)
                        )
                        Text(
                            text = " 录音中",
                            color = Color(0xFFE76472),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                ActionChip(
                    text = "记录",
                    color = Color(0xFF5E5CE6),
                    icon = Icons.Filled.History,
                    onClick = onOpenHistory
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!hasAudioPermission) {
                ActionChip(text = "授予麦克风权限", color = Color(0xFF5E5CE6), onClick = onRequestPermission)
            } else if (isRecording) {
                ActionChip(text = "停止并生成报告", color = Color(0xFFE76472), icon = Icons.Filled.StopCircle, onClick = onStop)
            } else {
                ActionChip(text = "开始录音", color = Color(0xFF5E5CE6), icon = Icons.Filled.Mic, onClick = onStart)
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("开始 $startLabel", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A8F98))
                Text("结束 $endLabel", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A8F98))
                if (showPersistActions) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionChip(
                            text = if (saving) "保存中" else "保存记录",
                            color = Color(0xFF2A9A7A),
                            onClick = onSave
                        )
                        ActionChip(
                            text = "删除记录",
                            color = Color(0xFFE76472),
                            onClick = onDelete
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    text: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .noRippleClickable(onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = text, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(5.dp))
        }
        Text(text = text, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun VoiceChartCard(
    title: String,
    points: List<Float>,
    highlightSegments: List<Segment>,
    recording: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(Color(0xFFF8F9FC), RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            if (points.size < 2) {
                Text(
                    text = if (recording) "正在采集声音..." else "开始录音后将显示声音曲线",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF949AA5)
                )
            } else {
                VoiceLineChart(points = points, highlightSegments = highlightSegments)
            }
        }
    }
}

@Composable
private fun VoiceLineChart(points: List<Float>, highlightSegments: List<Segment>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (points.isNotEmpty() && highlightSegments.isNotEmpty()) {
            val stepX = width / (points.size - 1).coerceAtLeast(1)
            highlightSegments.forEach { segment ->
                val safeStart = segment.startIndex.coerceIn(0, points.lastIndex)
                val safeEnd = segment.endIndex.coerceIn(0, points.lastIndex)
                val startX = safeStart * stepX
                val endX = safeEnd * stepX
                drawRect(
                    color = Color(0x22FF8A65),
                    topLeft = Offset(startX, 0f),
                    size = Size((endX - startX).coerceAtLeast(4f), height)
                )
            }
        }

        drawLine(Color(0xFFE9ECF4), Offset(0f, height * 0.2f), Offset(width, height * 0.2f), strokeWidth = 1f)
        drawLine(Color(0xFFE9ECF4), Offset(0f, height * 0.5f), Offset(width, height * 0.5f), strokeWidth = 1f)
        drawLine(Color(0xFFE9ECF4), Offset(0f, height * 0.8f), Offset(width, height * 0.8f), strokeWidth = 1f)

        val stepX = width / (points.size - 1).coerceAtLeast(1)
        val linePath = Path()
        val fillPath = Path()

        points.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - (value.coerceIn(0f, 1f) * height)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (index == points.lastIndex) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        drawPath(path = fillPath, color = Color(0x225E5CE6))
        drawPath(path = linePath, color = Color(0xFF5E5CE6))
    }
}

@Composable
private fun VoiceReportCard(report: VoiceReport?, hasData: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("过程分析报告", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))

        if (!hasData || report == null) {
            Text("结束录音后将生成峰值、稳定度和高潮段分析。", color = Color(0xFF8A8F98))
            return
        }

        ReportLine("峰值强度", "${report.peakPercent}%")
        ReportLine("峰值时间", "${report.peakSecond}s")
        ReportLine("开始时间", report.startTime)
        ReportLine("结束时间", report.endTime)
        ReportLine("活跃占比", "${report.activePercent}%")
        ReportLine("高潮段数量", "${report.climaxCount} 段")
        ReportLine("高潮总时长", "${report.climaxTotalSecond}s")
        ReportLine("最长高潮段", "${report.longestClimaxSecond}s")

        Text("高潮段时间轴", color = Color(0xFF8A8F98), style = MaterialTheme.typography.labelMedium)
        ClimaxTimeline(segments = report.climaxSegments, totalSamples = report.totalSampleCount)

        Text(report.summary, color = Color(0xFF5D6470), style = MaterialTheme.typography.bodyMedium)

        Text("男/女声分析（基于响度曲线估计，非声纹识别）", color = Color(0xFF8A8F98), style = MaterialTheme.typography.labelMedium)
        GenderInsightCard(title = "男声模型", insight = report.maleInsight)
        GenderInsightCard(title = "女声模型", insight = report.femaleInsight)
    }
}

@Composable
private fun VoiceHistorySheet(
    histories: List<VoiceHistoryItem>,
    onDeleteItem: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(620.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("录音历史记录", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F3440))

        if (histories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无记录", color = Color(0xFF9BA1AD))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(histories, key = { it.id }) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.startMillis)),
                                color = Color(0xFF2F3440),
                                fontWeight = FontWeight.SemiBold
                            )
                            ActionChip(text = "删除", color = Color(0xFFE76472)) { onDeleteItem(item.id) }
                        }
                        Text("地点：${item.locationLabel}", color = Color(0xFF66707D), style = MaterialTheme.typography.bodySmall)
                        ReportLine("高潮段", "${item.climaxCount} 段")
                        ReportLine("高潮总时长", "${item.climaxTotalSecond}s")
                        Text(item.summary, color = Color(0xFF5D6470), style = MaterialTheme.typography.bodySmall)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(Color(0xFFF8F9FC), RoundedCornerShape(10.dp))
                                .padding(6.dp)
                        ) {
                            val segments = item.climaxSegments.map { Segment(it.first, it.second) }
                            if (item.amplitudes.size < 2) {
                                Text("无波形数据", modifier = Modifier.align(Alignment.Center), color = Color(0xFF9BA1AD))
                            } else {
                                VoiceLineChart(points = item.amplitudes, highlightSegments = segments)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun ReportLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF8A8F98), style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Color(0xFF2F3440), fontWeight = FontWeight.SemiBold)
    }
}

private data class VoiceReport(
    val peakPercent: Int,
    val peakSecond: String,
    val startTime: String,
    val endTime: String,
    val activePercent: Int,
    val climaxCount: Int,
    val climaxTotalSecond: String,
    val longestClimaxSecond: String,
    val climaxSegments: List<Segment>,
    val totalSampleCount: Int,
    val maleInsight: GenderInsight,
    val femaleInsight: GenderInsight,
    val summary: String
)

private data class GenderInsight(
    val climaxCount: Int,
    val climaxTotalSecond: String,
    val longestClimaxSecond: String,
    val summary: String
)

private data class Segment(val startIndex: Int, val endIndex: Int) {
    val length: Int get() = (endIndex - startIndex + 1).coerceAtLeast(0)
}

private fun analyzeVoice(
    samples: List<Float>,
    startMillis: Long?,
    endMillis: Long?
): VoiceReport {
    if (samples.isEmpty()) {
        val emptyInsight = GenderInsight(0, "0.0", "0.0", "样本不足，无法形成有效判断。")
        return VoiceReport(
            peakPercent = 0,
            peakSecond = "0.0",
            startTime = formatTime(startMillis),
            endTime = formatTime(endMillis),
            activePercent = 0,
            climaxCount = 0,
            climaxTotalSecond = "0.0",
            longestClimaxSecond = "0.0",
            climaxSegments = emptyList(),
            totalSampleCount = 0,
            maleInsight = emptyInsight,
            femaleInsight = emptyInsight,
            summary = "本次录音有效数据不足，建议延长录音时长。"
        )
    }

    val peak = samples.maxOrNull() ?: 0f
    val peakIndex = samples.indexOfFirst { it == peak }.coerceAtLeast(0)
    val average = samples.average().toFloat()
    val activePercent = ((samples.count { it > 0.25f }.toFloat() / samples.size) * 100f).toInt()

    val variance = samples.map { (it - average) * (it - average) }.average().toFloat()
    val stability = 1f - sqrt(variance).coerceIn(0f, 1f)

    val threshold = (average + 0.25f).coerceIn(0.4f, 0.85f)
    val segments = detectSegments(
        samples = samples,
        threshold = threshold,
        mergeGapSamples = MERGE_GAP_SAMPLES,
        minSegmentSamples = 3
    )
    val count = segments.size
    val climaxTotalSecond = formatSeconds(segments.sumOf { it.length } * (SAMPLE_INTERVAL_MS / 1000f))
    val longestClimaxSecond = formatSeconds((segments.maxOfOrNull { it.length } ?: 0) * (SAMPLE_INTERVAL_MS / 1000f))

    val maleInsight = buildGenderInsight(
        samples = samples,
        threshold = (average + 0.20f).coerceIn(0.35f, 0.78f),
        label = "男声"
    )
    val femaleInsight = buildGenderInsight(
        samples = samples,
        threshold = (average + 0.30f).coerceIn(0.45f, 0.9f),
        label = "女声"
    )

    val summary = when {
        count >= 3 -> "声音能量起伏明显，出现多段连续高能区间，过程节奏偏活跃。"
        count >= 1 -> "检测到清晰的高能片段，过程存在明显高潮段。"
        peak > 0.65f -> "存在瞬时高峰，但连续性较弱，整体节奏中等。"
        else -> "整体声音变化较平稳，高潮特征不明显。"
    } + " 稳定度约 ${(stability * 100f).toInt()}%。"

    return VoiceReport(
        peakPercent = (peak * 100f).toInt(),
        peakSecond = String.format(Locale.getDefault(), "%.1f", peakIndex * (SAMPLE_INTERVAL_MS / 1000f)),
        startTime = formatTime(startMillis),
        endTime = formatTime(endMillis),
        activePercent = activePercent,
        climaxCount = count,
        climaxTotalSecond = climaxTotalSecond,
        longestClimaxSecond = longestClimaxSecond,
        climaxSegments = segments,
        totalSampleCount = samples.size,
        maleInsight = maleInsight,
        femaleInsight = femaleInsight,
        summary = summary
    )
}

private fun buildGenderInsight(samples: List<Float>, threshold: Float, label: String): GenderInsight {
    val segments = detectSegments(
        samples = samples,
        threshold = threshold,
        mergeGapSamples = MERGE_GAP_SAMPLES,
        minSegmentSamples = 3
    )
    val count = segments.size
    val total = formatSeconds(segments.sumOf { it.length } * (SAMPLE_INTERVAL_MS / 1000f))
    val longest = formatSeconds((segments.maxOfOrNull { it.length } ?: 0) * (SAMPLE_INTERVAL_MS / 1000f))
    val summary = when {
        count >= 3 -> "${label}模型下高能段明显，连续性较好。"
        count >= 1 -> "${label}模型下检测到有效高能片段。"
        else -> "${label}模型下未形成连续高潮段。"
    }
    return GenderInsight(count, total, longest, summary)
}

private fun detectSegments(
    samples: List<Float>,
    threshold: Float,
    mergeGapSamples: Int,
    minSegmentSamples: Int
): List<Segment> {
    if (samples.size < 3) return emptyList()

    val smoothed = smooth(samples, radius = 2)
    val peakThreshold = (threshold + 0.08f).coerceIn(0.35f, 0.92f)
    val peaks = detectPeaks(smoothed, peakThreshold)

    val baseSegments = if (peaks.isNotEmpty()) {
        val clusters = clusterByGap(peaks, mergeGapSamples)
        val extend = 12
        clusters.map { cluster ->
            val start = (cluster.first() - extend).coerceAtLeast(0)
            val end = (cluster.last() + extend).coerceAtMost(samples.lastIndex)
            Segment(start, end)
        }
    } else {
        detectThresholdRuns(smoothed, threshold, minSegmentSamples)
    }

    return mergeAndFilterSegments(baseSegments, mergeGapSamples, minSegmentSamples)
}

private fun smooth(samples: List<Float>, radius: Int): List<Float> {
    val out = MutableList(samples.size) { 0f }
    samples.indices.forEach { i ->
        var sum = 0f
        var count = 0
        val start = (i - radius).coerceAtLeast(0)
        val end = (i + radius).coerceAtMost(samples.lastIndex)
        for (j in start..end) {
            sum += samples[j]
            count++
        }
        out[i] = if (count == 0) 0f else sum / count
    }
    return out
}

private fun detectPeaks(samples: List<Float>, peakThreshold: Float): List<Int> {
    val peaks = mutableListOf<Int>()
    var lastPeak = -1000
    for (i in 1 until samples.lastIndex) {
        val prev = samples[i - 1]
        val cur = samples[i]
        val next = samples[i + 1]
        if (cur >= prev && cur > next && cur >= peakThreshold && i - lastPeak >= 3) {
            peaks += i
            lastPeak = i
        }
    }
    return peaks
}

private fun clusterByGap(points: List<Int>, gap: Int): List<List<Int>> {
    if (points.isEmpty()) return emptyList()
    val clusters = mutableListOf<MutableList<Int>>()
    var current = mutableListOf(points.first())
    points.drop(1).forEach { idx ->
        if (idx - current.last() <= gap) current += idx
        else {
            clusters += current
            current = mutableListOf(idx)
        }
    }
    clusters += current
    return clusters
}

private fun detectThresholdRuns(samples: List<Float>, threshold: Float, minSegmentSamples: Int): List<Segment> {
    val segments = mutableListOf<Segment>()
    var start = -1
    samples.forEachIndexed { index, value ->
        if (value >= threshold) {
            if (start < 0) start = index
        } else if (start >= 0) {
            val end = index - 1
            if (end - start + 1 >= minSegmentSamples) segments += Segment(start, end)
            start = -1
        }
    }
    if (start >= 0) {
        val end = samples.lastIndex
        if (end - start + 1 >= minSegmentSamples) segments += Segment(start, end)
    }
    return segments
}

private fun mergeAndFilterSegments(
    segments: List<Segment>,
    mergeGapSamples: Int,
    minSegmentSamples: Int
): List<Segment> {
    if (segments.isEmpty()) return emptyList()
    val sorted = segments.sortedBy { it.startIndex }
    val merged = mutableListOf<Segment>()
    var current = sorted.first()
    sorted.drop(1).forEach { next ->
        val gap = next.startIndex - current.endIndex - 1
        current = if (gap <= mergeGapSamples) {
            Segment(current.startIndex, maxOf(current.endIndex, next.endIndex))
        } else {
            merged += current
            next
        }
    }
    merged += current
    return merged.filter { it.length >= minSegmentSamples }
}

private fun downsample(points: List<Float>, maxPoints: Int): List<Float> {
    if (points.size <= maxPoints) return points
    val step = points.size.toFloat() / maxPoints.toFloat()
    return (0 until maxPoints).map { idx ->
        val src = (idx * step).toInt().coerceIn(0, points.lastIndex)
        points[src]
    }
}

private fun hasAnyLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

@SuppressLint("MissingPermission")
private fun resolveLocationLabel(context: Context, hasPermission: Boolean): String {
    if (!hasPermission) return "位置权限未授权"

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return "未知位置"
    val providers = runCatching { manager.getProviders(true) }.getOrElse { emptyList() }
    var best: android.location.Location? = null

    providers.forEach { provider ->
        val loc = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        if (loc != null && (best == null || loc.time > best!!.time)) {
            best = loc
        }
    }

    return if (best == null) {
        "未知位置"
    } else {
        val lat = best!!.latitude
        val lon = best!!.longitude
        val address = reverseGeocode(context, lat, lon)
        address ?: String.format(Locale.getDefault(), "%.4f, %.4f", lat, lon)
    }
}

private fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String? {
    return runCatching {
        val geocoder = Geocoder(context, Locale.getDefault())
        val result = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() ?: return@runCatching null

        val admin = result.adminArea.orEmpty()
        val subAdmin = result.subAdminArea.orEmpty()
        val locality = result.locality.orEmpty()
        val subLocality = result.subLocality.orEmpty()
        val thoroughfare = result.thoroughfare.orEmpty()

        val pieces = listOf(admin, subAdmin, locality, subLocality, thoroughfare)
            .filter { it.isNotBlank() }
            .distinct()
        if (pieces.isEmpty()) null else pieces.joinToString("")
    }.getOrNull()
}

private fun formatTime(millis: Long?): String {
    if (millis == null || millis <= 0L) return "--:--:--"
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))
}

private fun formatSeconds(second: Float): String {
    return String.format(Locale.getDefault(), "%.1f", second)
}

@Composable
private fun GenderInsightCard(title: String, insight: GenderInsight) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FC), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, color = Color(0xFF5E5CE6), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        ReportLine("高潮段", "${insight.climaxCount} 段")
        ReportLine("高潮总时长", "${insight.climaxTotalSecond}s")
        ReportLine("最长高潮段", "${insight.longestClimaxSecond}s")
        Text(insight.summary, color = Color(0xFF656D78), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ClimaxTimeline(segments: List<Segment>, totalSamples: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(Color(0xFFF8F9FC), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (segments.isEmpty() || totalSamples <= 1) {
            Text(
                "未检测到高潮段",
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF9BA1AD),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val barHeight = size.height * 0.5f
                val top = (size.height - barHeight) / 2f
                drawRect(color = Color(0xFFE8EBF3), topLeft = Offset(0f, top), size = Size(width, barHeight))
                segments.forEach { segment ->
                    val startX = segment.startIndex.toFloat() / (totalSamples - 1).coerceAtLeast(1) * width
                    val endX = segment.endIndex.toFloat() / (totalSamples - 1).coerceAtLeast(1) * width
                    drawRect(
                        color = Color(0xFFFF8A65),
                        topLeft = Offset(startX, top),
                        size = Size((endX - startX).coerceAtLeast(4f), barHeight)
                    )
                }
            }
        }
    }
}
