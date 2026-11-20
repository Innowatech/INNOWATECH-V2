package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ModuleCategory
import com.example.model.ModuleId
import com.example.ui.ServiceToolViewModel
import com.example.ui.components.AdminPinDialog
import com.example.ui.components.ChangelogDialog
import com.example.ui.components.InnotechTopBar
import com.example.ui.components.KofferQtyModal
import com.example.ui.components.LightboxModal
import com.example.ui.components.PinChipBar
import com.example.ui.components.TechnicianLoginDialog
import com.example.ui.screens.AdminConsoleModal
import com.example.ui.screens.AnwendungModuleHost
import com.example.ui.screens.CategoryModuleListScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HilfsmittelModuleHost
import com.example.ui.screens.KatalogModuleHost
import com.example.ui.screens.KofferPickerModal
import com.example.ui.screens.ParameterModuleHost
import com.example.ui.screens.SettingsModalDialog
import com.example.ui.theme.InnotechServiceTheme
import com.example.ui.theme.InnotechTheme

private const val ROUTE_OVERVIEW = "overview"
private const val ROUTE_ANWENDUNG = "anwendung"
private const val ROUTE_PARAMETER = "parameter"
private const val ROUTE_TOOLS_CATALOG = "tools_catalog"

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ServiceToolViewModel = viewModel()
            InnotechServiceApp(viewModel = viewModel)
        }
    }
}

@Composable
fun InnotechServiceApp(viewModel: ServiceToolViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(ROUTE_OVERVIEW) }
    var pendingDictationTarget by remember { mutableStateOf("service") }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim().orEmpty()
            if (spoken.isNotEmpty()) {
                if (pendingDictationTarget == "kunden") {
                    viewModel.updateKnDraft { draft ->
                        val sep = if (draft.text.isNotEmpty() && !draft.text.endsWith(' ') && !draft.text.endsWith('\n')) " " else ""
                        draft.copy(text = draft.text + sep + spoken)
                    }
                } else {
                    viewModel.appendServiceNoteLine(spoken)
                }
                viewModel.emitToast("Diktat übernommen.")
            }
        }
    }

    val startVoiceDictation: (String) -> Unit = { target ->
        pendingDictationTarget = target
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bitte sprechen…")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            viewModel.emitToast("Spracheingabe wird auf diesem Gerät nicht unterstützt.")
        }
    }

    val navTabs = remember {
        listOf(
            NavTab(
                route = ROUTE_OVERVIEW,
                labelRes = R.string.nav_overview,
                selectedIcon = Icons.Filled.GridView,
                unselectedIcon = Icons.Outlined.GridView,
                testTag = "nav_overview"
            ),
            NavTab(
                route = ROUTE_ANWENDUNG,
                labelRes = R.string.nav_anwendung,
                selectedIcon = Icons.Filled.Calculate,
                unselectedIcon = Icons.Outlined.Calculate,
                testTag = "nav_anwendung"
            ),
            NavTab(
                route = ROUTE_PARAMETER,
                labelRes = R.string.nav_parameter,
                selectedIcon = Icons.Filled.Tune,
                unselectedIcon = Icons.Outlined.Tune,
                testTag = "nav_parameter"
            ),
            NavTab(
                route = ROUTE_TOOLS_CATALOG,
                labelRes = R.string.nav_tools_catalog,
                selectedIcon = Icons.Filled.Build,
                unselectedIcon = Icons.Outlined.Build,
                testTag = "nav_tools_catalog"
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val keepAwake = state.prefs.keepScreenAwake || state.ausliterRunning || state.ledTorchOn || state.screenTorchOn
    DisposableEffect(activity, keepAwake) {
        if (keepAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler(enabled = state.activeModule != null) {
        viewModel.closeActiveModule()
    }

    val pinnedModules = remember(state.prefs.pinnedTabIds) {
        state.prefs.pinnedTabIds.mapNotNull { ModuleId.fromId(it) }
    }

    InnotechServiceTheme(darkTheme = state.prefs.darkMode) {
        val colors = InnotechTheme.colors
        val activeMod = state.activeModule
        val topTitle = activeMod?.title ?: stringResource(R.string.brand_title)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    Column {
                        InnotechTopBar(
                            title = topTitle,
                            activeUserKuerzel = state.prefs.activeUserKuerzel,
                            activeModule = activeMod,
                            isPinned = activeMod != null && pinnedModules.contains(activeMod),
                            darkTheme = state.prefs.darkMode,
                            showBack = activeMod != null,
                            onBackClick = viewModel::closeActiveModule,
                            onUserClick = viewModel::openLoginDialog,
                            onTogglePin = { activeMod?.let { viewModel.togglePin(it) } },
                            onToggleTheme = { viewModel.setDarkMode(!state.prefs.darkMode) },
                            onOpenSettings = viewModel::openSettings
                        )
                        PinChipBar(
                            pinnedModules = pinnedModules,
                            activeModule = activeMod,
                            onSelectModule = viewModel::openModule,
                            onUnpinModule = viewModel::unpinTab
                        )
                    }
                },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            navTabs.forEach { tab ->
                                val selected = activeMod == null && selectedTab == tab.route
                                val label = stringResource(tab.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        viewModel.closeActiveModule()
                                        selectedTab = tab.route
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = colors.onSignalYellow,
                                        indicatorColor = colors.signalYellow,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            navTabs.forEach { tab ->
                                val selected = activeMod == null && selectedTab == tab.route
                                val label = stringResource(tab.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        viewModel.closeActiveModule()
                                        selectedTab = tab.route
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label, maxLines = 1) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = colors.onSignalYellow,
                                        indicatorColor = colors.signalYellow
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (activeMod != null) {
                            when (activeMod.defaultCategory) {
                                ModuleCategory.ANWENDUNG -> AnwendungModuleHost(
                                    module = activeMod,
                                    inputs = state.calcInputs,
                                    viewModel = viewModel,
                                    onBack = viewModel::closeActiveModule
                                )
                                ModuleCategory.PARAMETER -> ParameterModuleHost(
                                    module = activeMod,
                                    state = state,
                                    viewModel = viewModel,
                                    onBack = viewModel::closeActiveModule
                                )
                                ModuleCategory.HILFSMITTEL -> HilfsmittelModuleHost(
                                    module = activeMod,
                                    state = state,
                                    viewModel = viewModel,
                                    onStartVoiceDictation = startVoiceDictation,
                                    onBack = viewModel::closeActiveModule
                                )
                                ModuleCategory.KATALOG_CHECKLISTEN -> KatalogModuleHost(
                                    module = activeMod,
                                    state = state,
                                    viewModel = viewModel,
                                    onBack = viewModel::closeActiveModule
                                )
                            }
                        } else {
                            when (selectedTab) {
                                ROUTE_ANWENDUNG -> CategoryModuleListScreen(
                                    categories = listOf(ModuleCategory.ANWENDUNG),
                                    state = state,
                                    onOpenModule = viewModel::openModule
                                )
                                ROUTE_PARAMETER -> CategoryModuleListScreen(
                                    categories = listOf(ModuleCategory.PARAMETER),
                                    state = state,
                                    onOpenModule = viewModel::openModule
                                )
                                ROUTE_TOOLS_CATALOG -> CategoryModuleListScreen(
                                    categories = listOf(ModuleCategory.HILFSMITTEL, ModuleCategory.KATALOG_CHECKLISTEN),
                                    state = state,
                                    onOpenModule = viewModel::openModule
                                )
                                else -> DashboardScreen(
                                    state = state,
                                    onSearchChange = viewModel::updateDashboardSearch,
                                    onOpenModule = viewModel::openModule
                                )
                            }
                        }
                    }
                }
            }

            if (state.screenTorchOn) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .clickable { viewModel.turnOffScreenTorch() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Display-Notlicht aktiv",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                        Button(
                            onClick = viewModel::turnOffScreenTorch,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0D47A1),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(width = 220.dp, height = 56.dp)
                        ) {
                            Text("Notlicht ausschalten")
                        }
                    }
                }
            }
        }

        if (state.kofferPickerOpen) {
            KofferPickerModal(state = state, viewModel = viewModel)
        }

        state.pendingKofferQty?.let { pending ->
            KofferQtyModal(
                pending = pending,
                onDismiss = viewModel::dismissKofferQtyDialog,
                onConfirm = viewModel::confirmKofferNachorderQty
            )
        }

        state.lightboxContent?.let { lb ->
            LightboxModal(
                content = lb,
                onSelectKofferPin = viewModel::selectPinInLightbox,
                onDismiss = viewModel::closeLightbox
            )
        }

        if (state.isSettingsOpen) {
            SettingsModalDialog(state = state, viewModel = viewModel)
        }

        if (state.isLoginDialogOpen) {
            TechnicianLoginDialog(
                users = state.users,
                activeUserKuerzel = state.prefs.activeUserKuerzel,
                errorMessage = state.loginError,
                onDismiss = viewModel::closeLoginDialog,
                onLogin = viewModel::submitTechnicianLogin,
                onLogout = viewModel::logoutTechnician
            )
        }

        if (state.isAdminAuthDialogOpen) {
            AdminPinDialog(
                errorMessage = state.adminAuthError,
                onDismiss = viewModel::closeAdminAuthDialog,
                onConfirm = viewModel::submitAdminPin
            )
        }

        if (state.isAdminConsoleOpen) {
            AdminConsoleModal(state = state, viewModel = viewModel)
        }

        if (state.isChangelogOpen) {
            ChangelogDialog(
                changelogText = state.changelogText,
                onDismiss = viewModel::closeChangelog
            )
        }
    }
}
