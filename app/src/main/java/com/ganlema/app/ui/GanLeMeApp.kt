package com.ganlema.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ganlema.app.data.DatabaseProvider
import com.ganlema.app.data.DailyIndicatorEntity
import com.ganlema.app.data.DailyIndicatorRepository
import com.ganlema.app.data.RecordRepository
import com.ganlema.app.security.AppLockManager
import com.ganlema.app.security.PinManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GanLeMeApp(lockManager: AppLockManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pinManager = remember { PinManager(context) }
    val database = remember { DatabaseProvider.get(context) }
    val repository = remember {
        RecordRepository(database.recordDao())
    }
    val indicatorRepository = remember {
        DailyIndicatorRepository(database.dailyIndicatorDao())
    }
    val allRecords by repository.records().collectAsState(initial = emptyList())
    val allIndicators by indicatorRepository.allIndicators().collectAsState(initial = emptyList())

    var showRecordSheet by remember { mutableStateOf(false) }
    var showIndicatorSheet by remember { mutableStateOf(false) }
    var recordEntryContext by remember { mutableStateOf<String?>(null) }
    var recordEntryDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var detailRecordId by remember { mutableStateOf<Long?>(null) }
    var filterState by remember { mutableStateOf(FilterState()) }
    var showCycleSheet by remember { mutableStateOf(false) }
    var showCycleSavedDialog by remember { mutableStateOf(false) }
    var cycleSettingsVersion by remember { mutableStateOf(0) }
    var showSplash by remember { mutableStateOf(true) }
    var currentRoute by remember { mutableStateOf("home") }
    val isUnlocked by lockManager.isUnlocked.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
        } else if (!isUnlocked) {
            UnlockScreen(
                pinManager = pinManager,
                onUnlocked = { lockManager.unlock() }
            )
        } else {
            Scaffold(
                bottomBar = {
                    AppBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route -> currentRoute = route }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (currentRoute) {
                        "home" -> {
                    HomeScreen(
                        repository = repository,
                        indicatorRepository = indicatorRepository,
                        pinManager = pinManager,
                        cycleSettingsVersion = cycleSettingsVersion,
                        filterState = filterState,
                        onAddRecord = { entry ->
                            recordEntryContext = entry
                            recordEntryDateMillis = System.currentTimeMillis()
                            if (entry == "爱爱") {
                                showRecordSheet = true
                            } else {
                                showIndicatorSheet = true
                            }
                        },
                        onOpenCycleSettings = { showCycleSheet = true },
                        onOpenInsights = {
                            currentRoute = "insights"
                        }
                    )
                        }
                        "insights" -> {
                        InsightsScreen(
                            repository = repository,
                            indicatorRepository = indicatorRepository,
                            cycleSettingsVersion = cycleSettingsVersion,
                            onAddRecord = { entry ->
                                recordEntryContext = entry
                                if (entry == "爱爱") {
                                    showRecordSheet = true
                                } else {
                                    showIndicatorSheet = true
                                }
                            },
                            onSelectDateForEntry = { dateMillis ->
                                recordEntryDateMillis = dateMillis
                            },
                            onSaveIndicator = { indicator ->
                                indicatorRepository.save(indicator)
                            },
                            onCycleChanged = {
                                cycleSettingsVersion += 1
                            }
                        )
                        }
                        else -> {
                        MeScreen(
                            repository = repository,
                            indicatorRepository = indicatorRepository,
                            pinManager = pinManager,
                            cycleSettingsVersion = cycleSettingsVersion,
                            onOpenCycleSettings = { showCycleSheet = true }
                        )
                    }
                    }
                }
            }
        }
 
        if (showRecordSheet && isUnlocked && !showSplash) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showRecordSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                RecordSheet(
                    initialRecord = allRecords.firstOrNull {
                        it.type == "同房" &&
                            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it.timeMillis)) ==
                            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(recordEntryDateMillis))
                    },
                    initialTimeMillis = recordEntryDateMillis,
                    entryContext = recordEntryContext,
                    onSave = { record ->
                        scope.launch {
                            if (record.id == 0L) repository.add(record) else repository.update(record)
                            recordEntryContext = null
                            showRecordSheet = false
                        }
                    },
                    onCancel = {
                        recordEntryContext = null
                        showRecordSheet = false
                    }
                )
            }
        }

        if (showIndicatorSheet && isUnlocked && !showSplash && recordEntryContext != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = {
                    recordEntryContext = null
                    showIndicatorSheet = false
                },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                IndicatorSheet(
                    metricKey = recordEntryContext!!,
                    targetDateMillis = recordEntryDateMillis,
                    initialIndicator = allIndicators.firstOrNull {
                        it.metricKey == recordEntryContext &&
                            it.dateKey == java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(recordEntryDateMillis))
                    },
                    onSave = { indicator ->
                        scope.launch {
                            indicatorRepository.save(indicator)
                            recordEntryContext = null
                            showIndicatorSheet = false
                        }
                    },
                    onCancel = {
                        recordEntryContext = null
                        showIndicatorSheet = false
                    }
                )
            }
        }

        if (showCycleSheet && isUnlocked && !showSplash) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showCycleSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                CycleSettingsSheet(
                    onSave = {
                        cycleSettingsVersion += 1
                        showCycleSavedDialog = true
                        showCycleSheet = false
                    },
                    onCancel = { showCycleSheet = false }
                )
            }
        }

        if (showCycleSavedDialog && isUnlocked && !showSplash) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showCycleSavedDialog = false },
                title = { androidx.compose.material3.Text("保存成功") },
                text = { androidx.compose.material3.Text("生理周期已更新，首页提醒已刷新。") },
                confirmButton = {
                    androidx.compose.material3.Button(onClick = { showCycleSavedDialog = false }) {
                        androidx.compose.material3.Text("确定")
                    }
                }
            )
        }

        if (showDetailSheet && detailRecordId != null && isUnlocked && !showSplash) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = {
                    showDetailSheet = false
                    detailRecordId = null
                },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                RecordDetailSheet(
                    repository = repository,
                    recordId = detailRecordId!!,
                    onClose = {
                        showDetailSheet = false
                        detailRecordId = null
                    }
                )
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        Triple("home", "首页", Icons.Filled.Home),
        Triple("insights", "记录", Icons.Filled.CalendarMonth),
        Triple("me", "我的", Icons.Filled.Person)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 0.dp, vertical = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (route, label, icon) ->
                val selected = currentRoute == route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .noRippleClickable { onNavigate(route) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) Color(0xFFD66A9A) else Color(0xFF8A8F98),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

data class FilterState(
    val types: Set<String> = emptySet(),
    val protections: Set<String> = emptySet()
)
