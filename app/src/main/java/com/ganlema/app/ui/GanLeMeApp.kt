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
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ganlema.app.data.DatabaseProvider
import com.ganlema.app.data.RecordRepository
import com.ganlema.app.security.AppLockManager
import com.ganlema.app.security.PinManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GanLeMeApp(lockManager: AppLockManager) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val pinManager = remember { PinManager(context) }
    val repository = remember {
        RecordRepository(DatabaseProvider.get(context).recordDao())
    }

    var showRecordSheet by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var detailRecordId by remember { mutableStateOf<Long?>(null) }
    var filterState by remember { mutableStateOf(FilterState()) }
    var cyclePickerRequest by remember { mutableStateOf(0) }
    var showSplash by remember { mutableStateOf(true) }
    val isUnlocked by lockManager.isUnlocked.collectAsState()
    val bottomRoutes = remember { setOf("home", "insights", "voice", "me") }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
        } else if (!isUnlocked) {
            UnlockScreen(
                pinManager = pinManager,
                onUnlocked = { lockManager.unlock() }
            )
        } else {
            val navBackStack by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStack?.destination?.route ?: "home"
            Scaffold(
                bottomBar = {
                    if (bottomRoutes.contains(currentRoute)) {
                        AppBottomBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("home") {
                    HomeScreen(
                        repository = repository,
                        pinManager = pinManager,
                        filterState = filterState,
                        onAddRecord = { showRecordSheet = true },
                        onOpenCycleSettings = {
                            cyclePickerRequest += 1
                            navController.navigate("me") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenDetail = { id ->
                            detailRecordId = id
                            showDetailSheet = true
                        }
                    )
                }
                    composable("insights") {
                        InsightsScreen(repository = repository)
                    }
                    composable("me") {
                        MeScreen(
                            repository = repository,
                            pinManager = pinManager,
                            onOpenDisguise = { navController.navigate("disguise") },
                            onOpenSettings = { navController.navigate("settings") },
                            cyclePickerRequest = cyclePickerRequest
                        )
                    }
                    composable("voice") {
                        VoiceAnalysisScreen()
                    }
                    composable("settings") {
                        SettingsScreen(
                            repository = repository,
                            pinManager = pinManager,
                            onOpenDisguise = { navController.navigate("disguise") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("filters") {
                        FilterScreen(
                            initialState = filterState,
                            onApply = { state ->
                                filterState = state
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("disguise") {
                        AppDisguiseScreen(
                            onBack = { navController.popBackStack() }
                        )
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
                    onSave = { record ->
                        scope.launch {
                            repository.add(record)
                            showRecordSheet = false
                        }
                    },
                    onCancel = { showRecordSheet = false }
                )
            }
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
        Triple("insights", "洞察", Icons.Filled.AutoGraph),
        Triple("voice", "录音", Icons.Filled.Mic),
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
                        tint = if (selected) Color(0xFF5E5CE6) else Color(0xFF8A8F98),
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
