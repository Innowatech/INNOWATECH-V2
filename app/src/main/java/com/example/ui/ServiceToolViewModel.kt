package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.data.InnowatechCalculations
import com.example.data.InnowatechRepository
import com.example.data.PersistedAppState
import com.example.model.AnlageInfo
import com.example.model.CalculatorInputs
import com.example.model.DashboardDensity
import com.example.model.EnthaerterChecklistState
import com.example.model.EnthaerterInfo
import com.example.model.InnowatechDatabases
import com.example.model.KofferItem
import com.example.model.KundenNotizDraft
import com.example.model.KundenNotizEntry
import com.example.model.LightboxContent
import com.example.model.ModuleCategory
import com.example.model.ModuleConfig
import com.example.model.ModuleId
import com.example.model.SavedEnthaerterRecord
import com.example.model.SondeInfo
import com.example.model.UserAccount
import com.example.util.SoundAndVibrateHelper
import com.example.util.TorchController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class KofferQtyPending(
    val boxId: String,
    val item: KofferItem,
    val closePickerOnConfirm: Boolean
)

data class ServiceToolUiState(
    val prefs: PersistedAppState = PersistedAppState(),
    val databases: InnowatechDatabases = InnowatechDatabases(),
    val users: Map<String, UserAccount> = emptyMap(),
    val orderedCategories: List<ModuleCategory> = InnowatechRepository.defaultCategoryOrder,
    val modules: List<ModuleConfig> = InnowatechRepository.defaultModules,
    val activeModule: ModuleId? = null,
    val dashboardSearchQuery: String = "",
    val calcInputs: CalculatorInputs = CalculatorInputs(),
    // Ausliter timer
    val ausliterTimeLeftSec: Int = 60,
    val ausliterRunning: Boolean = false,
    // Torch
    val ledTorchOn: Boolean = false,
    val screenTorchOn: Boolean = false,
    // Koffer state
    val kofferSelectedBoxId: String = "alle",
    val kofferSearchQuery: String = "",
    val kofferActivePinNr: String = "",
    val kofferPickerOpen: Boolean = false,
    val kofferPickerBoxId: String = "alle",
    val kofferPickerSearchQuery: String = "",
    val kofferPickerActivePinNr: String = "",
    val pendingKofferQty: KofferQtyPending? = null,
    // Lightbox & Dialogs
    val lightboxContent: LightboxContent? = null,
    val isSettingsOpen: Boolean = false,
    val isLoginDialogOpen: Boolean = false,
    val isAdminAuthDialogOpen: Boolean = false,
    val isAdminConsoleOpen: Boolean = false,
    val isChangelogOpen: Boolean = false,
    val loginError: String? = null,
    val adminAuthError: String? = null,
    val changelogText: String = InnowatechRepository.CHANGELOG_FALLBACK
)

class ServiceToolViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferences(application.applicationContext)
    private val appContext = application.applicationContext

    private val defaultDatabases: InnowatechDatabases = InnowatechRepository.loadDatabasesFromAssets(appContext)
    private val defaultUsers: Map<String, UserAccount> = InnowatechRepository.loadUsersFromAssets(appContext)
    private val changelog: String = InnowatechRepository.loadChangelogFromAssets(appContext)

    private val _uiState = MutableStateFlow(
        ServiceToolUiState(
            databases = defaultDatabases,
            users = defaultUsers,
            changelogText = changelog
        )
    )
    val uiState: StateFlow<ServiceToolUiState> = _uiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private var ausliterTimerJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.state.collect { saved ->
                val dbs = saved.customDatabasesJson?.let {
                    runCatching { InnowatechRepository.parseDatabasesJson(it) }.getOrNull()
                } ?: defaultDatabases

                val userMap = saved.customUsersJson?.let {
                    runCatching { InnowatechRepository.parseUsersJson(it) }.getOrNull()
                } ?: defaultUsers

                val orderedCats = buildList {
                    saved.categoryOrderKeys.forEach { key ->
                        ModuleCategory.entries.find { it.key == key }?.let { if (!contains(it)) add(it) }
                    }
                    InnowatechRepository.defaultCategoryOrder.forEach { if (!contains(it)) add(it) }
                }

                val activeUser = saved.activeUserKuerzel?.let { userMap[it] }
                val allowedSet = activeUser?.modules?.toSet()

                val orderedModIds = buildList {
                    saved.moduleOrderIds.forEach { id ->
                        ModuleId.fromId(id)?.let { if (!contains(it)) add(it) }
                    }
                    ModuleId.entries.forEach { if (!contains(it)) add(it) }
                }

                val builtModules = orderedModIds.map { mod ->
                    val visible = !saved.hiddenModuleIds.contains(mod.id)
                    val allowed = allowedSet == null || allowedSet.contains(mod.id)
                    ModuleConfig(module = mod, visible = visible, userAllowed = allowed)
                }

                _uiState.update { cur ->
                    val prefillKuerzel = if (saved.enthaerterDraft.kuerzel.isBlank() && !saved.activeUserKuerzel.isNullOrBlank()) {
                        saved.enthaerterDraft.copy(kuerzel = saved.activeUserKuerzel)
                    } else {
                        saved.enthaerterDraft
                    }
                    cur.copy(
                        prefs = saved.copy(enthaerterDraft = prefillKuerzel),
                        databases = dbs,
                        users = userMap,
                        orderedCategories = orderedCats,
                        modules = builtModules
                    )
                }
            }
        }
    }

    fun emitToast(msg: String) {
        _toastEvents.tryEmit(msg)
    }

    // Navigation & Modules
    fun openModule(moduleId: ModuleId) {
        val modCfg = _uiState.value.modules.find { it.module == moduleId }
        if (modCfg != null && !modCfg.userAllowed) {
            emitToast("${moduleId.title} ist für dieses Kürzel nicht freigeschaltet.")
            return
        }
        _uiState.update { it.copy(activeModule = moduleId) }
    }

    fun closeActiveModule() {
        _uiState.update { it.copy(activeModule = null) }
    }

    fun updateDashboardSearch(query: String) {
        _uiState.update { it.copy(dashboardSearchQuery = query) }
    }

    // Pinning (max 5)
    fun togglePin(moduleId: ModuleId) {
        val current = _uiState.value.prefs.pinnedTabIds.toMutableList()
        if (current.contains(moduleId.id)) {
            current.remove(moduleId.id)
            emitToast("Tab gelöst.")
        } else {
            if (current.size >= 5) {
                emitToast("Maximal 5 Tabs anpinnbar – bitte erst einen lösen.")
                return
            }
            current.add(moduleId.id)
            emitToast("Tab angepinnt (${current.size}/5).")
        }
        viewModelScope.launch { preferences.setPinnedTabs(current) }
    }

    fun unpinTab(moduleId: ModuleId) {
        val next = _uiState.value.prefs.pinnedTabIds.filter { it != moduleId.id }
        viewModelScope.launch { preferences.setPinnedTabs(next) }
    }

    // Settings
    fun openSettings() = _uiState.update { it.copy(isSettingsOpen = true) }
    fun closeSettings() = _uiState.update { it.copy(isSettingsOpen = false) }
    fun openChangelog() = _uiState.update { it.copy(isChangelogOpen = true) }
    fun closeChangelog() = _uiState.update { it.copy(isChangelogOpen = false) }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferences.setDarkMode(enabled) }
    }

    fun setDashboardDensity(density: DashboardDensity) {
        viewModelScope.launch { preferences.setDashboardDensity(density) }
    }

    fun setShowDashboardIcons(show: Boolean) {
        viewModelScope.launch { preferences.setShowDashboardIcons(show) }
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        viewModelScope.launch { preferences.setKeepScreenAwake(enabled) }
    }

    fun setTimerBeep(enabled: Boolean) {
        viewModelScope.launch { preferences.setTimerBeep(enabled) }
    }

    fun setPersistCalcInputs(enabled: Boolean) {
        viewModelScope.launch { preferences.setPersistCalcInputs(enabled) }
    }

    fun moveCategory(category: ModuleCategory, delta: Int) {
        val list = _uiState.value.orderedCategories.toMutableList()
        val idx = list.indexOf(category)
        val target = idx + delta
        if (idx < 0 || target !in list.indices) return
        list.removeAt(idx)
        list.add(target, category)
        viewModelScope.launch { preferences.setCategoryOrder(list.map { it.key }) }
    }

    fun toggleModuleVisibility(moduleId: ModuleId) {
        val curMods = _uiState.value.modules
        val activeCount = curMods.count { it.visible }
        val hidden = _uiState.value.prefs.hiddenModuleIds.toMutableSet()
        if (hidden.contains(moduleId.id)) {
            hidden.remove(moduleId.id)
        } else {
            if (activeCount <= 1) {
                emitToast("Es muss mindestens ein Modul aktiv bleiben!")
                return
            }
            hidden.add(moduleId.id)
        }
        viewModelScope.launch { preferences.setHiddenModules(hidden) }
    }

    fun moveModuleInCategory(moduleId: ModuleId, delta: Int) {
        val all = _uiState.value.modules.map { it.module }.toMutableList()
        val cat = moduleId.defaultCategory
        val sameCatIndices = all.mapIndexedNotNull { i, m -> if (m.defaultCategory == cat) i else null }
        val posInCat = sameCatIndices.indexOfFirst { all[it] == moduleId }
        val targetPosInCat = posInCat + delta
        if (posInCat < 0 || targetPosInCat !in sameCatIndices.indices) return
        val i1 = sameCatIndices[posInCat]
        val i2 = sameCatIndices[targetPosInCat]
        val tmp = all[i1]
        all[i1] = all[i2]
        all[i2] = tmp
        viewModelScope.launch { preferences.setModuleOrder(all.map { it.id }) }
    }

    // Auth & Admin
    fun openLoginDialog() = _uiState.update { it.copy(isLoginDialogOpen = true, loginError = null) }
    fun closeLoginDialog() = _uiState.update { it.copy(isLoginDialogOpen = false, loginError = null) }

    fun submitTechnicianLogin(kuerzelRaw: String, pinRaw: String) {
        val kuerzel = kuerzelRaw.trim().uppercase(Locale.ROOT)
        val pin = pinRaw.trim()
        val users = _uiState.value.users
        val account = users[kuerzel]
        if (account == null) {
            _uiState.update { it.copy(loginError = "Unbekanntes Kürzel: $kuerzel") }
            return
        }
        if (account.status != "active") {
            _uiState.update { it.copy(loginError = "Techniker ist inaktiv.") }
            return
        }
        val hash = InnowatechRepository.hashPin(pin)
        if (hash != account.pinHash.lowercase(Locale.ROOT)) {
            _uiState.update { it.copy(loginError = "Falsche PIN für $kuerzel.") }
            return
        }
        viewModelScope.launch { preferences.setActiveUser(kuerzel) }
        _uiState.update { it.copy(isLoginDialogOpen = false, loginError = null) }
        emitToast("Angemeldet als ${account.name} ($kuerzel)")
    }

    fun logoutTechnician() {
        viewModelScope.launch { preferences.setActiveUser(null) }
        emitToast("Präsentations-Modus (Alle 17 Module freigeschaltet)")
    }

    fun openAdminAuthDialog() = _uiState.update { it.copy(isAdminAuthDialogOpen = true, adminAuthError = null) }
    fun closeAdminAuthDialog() = _uiState.update { it.copy(isAdminAuthDialogOpen = false, adminAuthError = null) }

    fun submitAdminPin(pinRaw: String) {
        val users = _uiState.value.users
        val admin = users["ADMIN"] ?: users.values.firstOrNull { it.isAdmin }
        if (admin == null) {
            _uiState.update { it.copy(adminAuthError = "Kein Admin-Konto konfiguriert.") }
            return
        }
        val hash = InnowatechRepository.hashPin(pinRaw.trim())
        if (hash != admin.pinHash.lowercase(Locale.ROOT)) {
            _uiState.update { it.copy(adminAuthError = "Ungültige Admin-PIN.") }
            return
        }
        _uiState.update {
            it.copy(
                isAdminAuthDialogOpen = false,
                adminAuthError = null,
                isAdminConsoleOpen = true
            )
        }
    }

    fun closeAdminConsole() = _uiState.update { it.copy(isAdminConsoleOpen = false) }

    fun saveAdminUser(
        kuerzelRaw: String,
        name: String,
        email: String,
        newPin: String,
        autoUpdate: Boolean,
        modules: List<String>
    ) {
        val k = kuerzelRaw.trim().uppercase(Locale.ROOT)
        if (k.isEmpty()) {
            emitToast("Bitte ein Kürzel eingeben.")
            return
        }
        val currentUsers = _uiState.value.users.toMutableMap()
        val existing = currentUsers[k]
        val finalHash = if (newPin.isNotBlank()) {
            InnowatechRepository.hashPin(newPin.trim())
        } else {
            existing?.pinHash ?: InnowatechRepository.hashPin("0000")
        }
        currentUsers[k] = UserAccount(
            kuerzel = k,
            name = name.trim().ifEmpty { k },
            email = email.trim(),
            pinHash = finalHash,
            status = "active",
            isAdmin = existing?.isAdmin ?: (k == "ADMIN"),
            autoUpdate = autoUpdate,
            modules = modules
        )
        val json = InnowatechRepository.serializeUsersJson(currentUsers)
        viewModelScope.launch { preferences.setCustomUsersJson(json) }
        emitToast("Techniker $k gespeichert.")
    }

    fun deleteAdminUser(kuerzel: String) {
        if (kuerzel == "ADMIN") {
            emitToast("ADMIN kann nicht gelöscht werden.")
            return
        }
        val currentUsers = _uiState.value.users.toMutableMap()
        currentUsers.remove(kuerzel)
        val json = InnowatechRepository.serializeUsersJson(currentUsers)
        viewModelScope.launch { preferences.setCustomUsersJson(json) }
        emitToast("Techniker $kuerzel entfernt.")
    }

    fun resetUsersToDefaults() {
        viewModelScope.launch { preferences.setCustomUsersJson(null) }
        emitToast("Benutzer auf Standard zurückgesetzt.")
    }

    fun saveSondeEntry(entry: SondeInfo) {
        val db = _uiState.value.databases
        val next = db.sonden.toMutableList()
        val idx = next.indexOfFirst { it.id == entry.id }
        if (idx >= 0) next[idx] = entry else next += entry
        val nextDb = db.copy(sonden = next)
        viewModelScope.launch { preferences.setCustomDatabasesJson(InnowatechRepository.serializeDatabasesJson(nextDb)) }
        emitToast("Sonde ${entry.label} gespeichert.")
    }

    fun saveEnthaerterEntry(entry: EnthaerterInfo) {
        val db = _uiState.value.databases
        val next = db.enthaerter.toMutableList()
        val idx = next.indexOfFirst { it.id == entry.id }
        if (idx >= 0) next[idx] = entry else next += entry
        val nextDb = db.copy(enthaerter = next)
        viewModelScope.launch { preferences.setCustomDatabasesJson(InnowatechRepository.serializeDatabasesJson(nextDb)) }
        emitToast("Enthärter ${entry.label} gespeichert.")
    }

    fun saveAnlageEntry(entry: AnlageInfo) {
        val db = _uiState.value.databases
        val next = db.anlagen.toMutableList()
        val idx = next.indexOfFirst { it.id == entry.id }
        if (idx >= 0) next[idx] = entry else next += entry
        val nextDb = db.copy(anlagen = next)
        viewModelScope.launch { preferences.setCustomDatabasesJson(InnowatechRepository.serializeDatabasesJson(nextDb)) }
        emitToast("Anlage ${entry.label} gespeichert.")
    }

    fun resetDatabasesToDefaults() {
        viewModelScope.launch { preferences.setCustomDatabasesJson(null) }
        emitToast("Datenbanken auf Standard zurückgesetzt.")
    }

    // Calculator Inputs & Reset
    fun updateCalcInputs(transform: (CalculatorInputs) -> CalculatorInputs) {
        _uiState.update { it.copy(calcInputs = transform(it.calcInputs)) }
    }

    fun updateWasserwerkLsek(value: String) {
        val ls = value.replace(',', '.').toDoubleOrNull() ?: 0.0
        _uiState.update { state ->
            val h = state.calcInputs.wwStunden.replace(',', '.').toDoubleOrNull() ?: 0.0
            val m3h = ls * 3.6
            val newM3h = if (m3h > 0.0) String.format(Locale.US, "%.2f", m3h) else ""
            val newM3Tag = if (h > 0.0 && ls > 0.0) String.format(Locale.US, "%.2f", m3h * h) else state.calcInputs.wwM3Tag
            state.copy(
                calcInputs = state.calcInputs.copy(
                    wwLsek = value,
                    wwM3h = newM3h,
                    wwM3Tag = newM3Tag
                )
            )
        }
    }

    fun updateWasserwerkStunden(value: String) {
        val h = value.replace(',', '.').toDoubleOrNull() ?: 0.0
        _uiState.update { state ->
            val ls = state.calcInputs.wwLsek.replace(',', '.').toDoubleOrNull() ?: 0.0
            val t = state.calcInputs.wwM3Tag.replace(',', '.').toDoubleOrNull() ?: 0.0
            val next = if (ls > 0.0 && h > 0.0) {
                val m3h = ls * 3.6
                state.calcInputs.copy(
                    wwStunden = value,
                    wwM3h = String.format(Locale.US, "%.2f", m3h),
                    wwM3Tag = String.format(Locale.US, "%.2f", m3h * h)
                )
            } else if (t > 0.0 && h > 0.0) {
                val m3h = t / h
                state.calcInputs.copy(
                    wwStunden = value,
                    wwM3h = String.format(Locale.US, "%.2f", m3h),
                    wwLsek = String.format(Locale.US, "%.2f", m3h / 3.6)
                )
            } else {
                state.calcInputs.copy(wwStunden = value)
            }
            state.copy(calcInputs = next)
        }
    }

    fun updateWasserwerkM3Tag(value: String) {
        val t = value.replace(',', '.').toDoubleOrNull() ?: 0.0
        _uiState.update { state ->
            val h = state.calcInputs.wwStunden.replace(',', '.').toDoubleOrNull() ?: 0.0
            val next = if (h > 0.0 && t > 0.0) {
                val m3h = t / h
                state.calcInputs.copy(
                    wwM3Tag = value,
                    wwM3h = String.format(Locale.US, "%.2f", m3h),
                    wwLsek = String.format(Locale.US, "%.2f", m3h / 3.6)
                )
            } else {
                state.calcInputs.copy(wwM3Tag = value)
            }
            state.copy(calcInputs = next)
        }
    }

    fun updatePoolDimensions(
        laenge: String = _uiState.value.calcInputs.poolLaenge,
        breite: String = _uiState.value.calcInputs.poolBreite,
        tiefe: String = _uiState.value.calcInputs.poolTiefe
    ) {
        val l = laenge.replace(',', '.').toDoubleOrNull() ?: 0.0
        val b = breite.replace(',', '.').toDoubleOrNull() ?: 0.0
        val t = tiefe.replace(',', '.').toDoubleOrNull() ?: 0.0
        val v = l * b * t * 1000.0
        val volStr = if (l > 0.0 && b > 0.0 && t > 0.0 && v.isFinite()) {
            String.format(Locale.US, "%.0f", v)
        } else {
            _uiState.value.calcInputs.poolVolumen
        }
        _uiState.update {
            it.copy(
                calcInputs = it.calcInputs.copy(
                    poolLaenge = laenge,
                    poolBreite = breite,
                    poolTiefe = tiefe,
                    poolVolumen = volStr
                )
            )
        }
    }

    fun selectRadarTank(volumeLiters: String) {
        val spec = InnowatechRepository.radarTankSpecs.find { it.volumeLiters == volumeLiters }
        _uiState.update {
            it.copy(
                calcInputs = it.calcInputs.copy(
                    selectedRadarVol = volumeLiters,
                    radarMinMm = "0",
                    radarMaxMm = spec?.maxMm?.toString() ?: ""
                )
            )
        }
    }

    fun resetActiveModule() {
        when (_uiState.value.activeModule) {
            ModuleId.STANDARD -> updateCalcInputs { it.copy(stdZiel = "", stdVol = "", stdAnol = "", stdZehrung = "") }
            ModuleId.WASSERWERK -> updateCalcInputs {
                it.copy(wwM3Tag = "", wwStunden = "", wwLsek = "", wwM3h = "", wwAnolKonz = "", wwZiel = "", wwZehrung = "", wwImpuls = "", wwMaxFlow = "")
            }
            ModuleId.SCHWIMMBAD -> updateCalcInputs {
                it.copy(poolLaenge = "", poolBreite = "", poolTiefe = "", poolVolumen = "", poolChlorsorte = "anolyte", poolChlorIst = "", poolChlorSoll = "", poolChlorKonz = "", poolPhIst = "", poolPhSoll = "", poolAlk = "", poolSaeureKonz = "")
            }
            ModuleId.PID -> updateCalcInputs {
                it.copy(pidBetriebsart = "kalt_kombiniert", pidTestL = "", pidWirkstoff = "", pidChlorDiff = "", pidTu = "", pidTg = "", pidGrundwert = "", pidSollwert = "")
            }
            ModuleId.VERDUENNUNG -> updateCalcInputs {
                it.copy(verdAnlagentyp = "500", verdCustomKonz = "1650", verdCustomZiel = "400", verdMesswert = "")
            }
            ModuleId.DOSIERAUSWERTUNG -> updateCalcInputs {
                it.copy(daWzAlt = "", daWzNeu = "", daDateAlt = "", daDateNeu = "", daDosAlt = "", daDosNeu = "", daKonz = "", daSoll = "")
            }
            ModuleId.SONDEN -> updateCalcInputs { it.copy(selectedSondeId = "") }
            ModuleId.ANLAGEN -> updateCalcInputs { it.copy(selectedAnlageId = "") }
            ModuleId.RADAR -> updateCalcInputs { it.copy(selectedRadarVol = "", radarMinMm = "0", radarMaxMm = "") }
            ModuleId.ENTHAERTER -> updateCalcInputs { it.copy(selectedEnthaerterId = "", enthaerterHaerte = "") }
            ModuleId.SCHLAUCHVOLUMEN -> updateCalcInputs { it.copy(schlauchD = "", schlauchL = "", schlauchPumpEnabled = false, schlauchPumpLh = "") }
            ModuleId.AUSLITERN -> {
                stopAusliterTimer()
                updateCalcInputs { it.copy(ausliterMedium = "katholyte", ausliterSec = 60, ausliterKathMl = "", ausliterAnolMl = "") }
                _uiState.update { it.copy(ausliterTimeLeftSec = 60) }
            }
            ModuleId.NOTIZEN -> clearServiceNotes()
            ModuleId.KUNDENNOTIZEN -> newKnEntry()
            ModuleId.ENTHAERTERTAUSCH -> resetEnthaerterChecklist()
            else -> return
        }
        if (_uiState.value.activeModule != ModuleId.NOTIZEN &&
            _uiState.value.activeModule != ModuleId.KUNDENNOTIZEN &&
            _uiState.value.activeModule != ModuleId.ENTHAERTERTAUSCH
        ) {
            emitToast("Eingaben zurückgesetzt.")
        }
    }

    // Ausliter timer
    fun setAusliterMedium(medium: String) = updateCalcInputs { it.copy(ausliterMedium = medium) }

    fun setAusliterDuration(sec: Int) {
        stopAusliterTimer()
        updateCalcInputs { it.copy(ausliterSec = sec) }
        _uiState.update { it.copy(ausliterTimeLeftSec = sec) }
    }

    fun startAusliterTimer() {
        val sec = _uiState.value.calcInputs.ausliterSec
        ausliterTimerJob?.cancel()
        _uiState.update { it.copy(ausliterTimeLeftSec = sec, ausliterRunning = true) }
        ausliterTimerJob = viewModelScope.launch {
            val endMs = System.currentTimeMillis() + sec * 1000L
            while (true) {
                val left = ((endMs - System.currentTimeMillis() + 999L) / 1000L).toInt().coerceAtLeast(0)
                _uiState.update { it.copy(ausliterTimeLeftSec = left) }
                if (left <= 0) {
                    _uiState.update { it.copy(ausliterRunning = false) }
                    SoundAndVibrateHelper.playTimerAlert(appContext, _uiState.value.prefs.timerBeepEnabled)
                    emitToast("${sec} Sekunden abgelaufen! Aufgefangene ml eingeben.")
                    break
                }
                delay(250)
            }
        }
    }

    fun stopAusliterTimer() {
        ausliterTimerJob?.cancel()
        ausliterTimerJob = null
        _uiState.update { it.copy(ausliterRunning = false) }
    }

    // Flashlight
    fun toggleFlashlight() {
        val cur = _uiState.value
        if (!cur.ledTorchOn && !cur.screenTorchOn) {
            when (TorchController.toggleLedTorch(appContext, false)) {
                TorchController.TorchOutcome.LedOn -> _uiState.update { it.copy(ledTorchOn = true) }
                TorchController.TorchOutcome.FallbackScreenTorch -> _uiState.update { it.copy(screenTorchOn = true) }
                TorchController.TorchOutcome.LedOff -> _uiState.update { it.copy(ledTorchOn = false) }
            }
        } else if (cur.ledTorchOn) {
            TorchController.toggleLedTorch(appContext, true)
            _uiState.update { it.copy(ledTorchOn = false) }
        } else {
            _uiState.update { it.copy(screenTorchOn = false) }
        }
    }

    fun turnOffScreenTorch() {
        _uiState.update { it.copy(screenTorchOn = false) }
    }

    // Service Notizen
    fun updateServiceNotes(text: String) {
        viewModelScope.launch { preferences.setServiceNotes(text) }
    }

    fun appendServiceNoteLine(line: String) {
        val cur = _uiState.value.prefs.serviceNotes
        val next = buildString {
            append(cur)
            if (cur.isNotEmpty() && !cur.endsWith('\n')) append('\n')
            append(line)
            if (!line.endsWith(' ') && !line.endsWith('\n')) append(' ')
        }
        updateServiceNotes(next)
    }

    fun clearServiceNotes() {
        updateServiceNotes("")
        emitToast("Service-Notizen geleert.")
    }

    fun setFaultClearAfter(enabled: Boolean) {
        viewModelScope.launch { preferences.setFaultClearAfter(enabled) }
    }

    fun copyDosierauswertungToNotes(result: InnowatechCalculations.DosierauswertungResult) {
        if (result.wasserText == "-" || result.anolytText == "-") {
            emitToast("Bitte zuerst gültige Dosierdaten eingeben.")
            return
        }
        val block = buildString {
            appendLine("📊 DOSIERAUSWERTUNG")
            appendLine("Wasserverbrauch: ${result.wasserText} | Dosiertes Anolyte: ${result.anolytText}")
            appendLine("Dosierquote: ${result.quoteText} | Real eindosiertes Aktivchlor: ${result.chlorText}")
            appendLine("Verhältnis zum Sollwert: ${result.faktorText}")
            if (result.tageText != null) {
                appendLine("Zeitraum (${result.tageText}): ${result.wasserTagText ?: "-"} | ${result.dosierTagText ?: "-"}")
            }
        }
        val cur = _uiState.value.prefs.serviceNotes
        val next = buildString {
            append(cur)
            if (cur.isNotEmpty() && !cur.endsWith('\n')) append("\n\n")
            append(block)
        }
        updateServiceNotes(next)
        emitToast("Dosierauswertung in Service-Notizen übernommen!")
    }

    // Servicekoffer & Lightbox
    fun selectKofferBox(boxId: String, forPicker: Boolean = false) {
        _uiState.update {
            if (forPicker) {
                it.copy(kofferPickerBoxId = boxId, kofferPickerActivePinNr = "")
            } else {
                it.copy(kofferSelectedBoxId = boxId, kofferActivePinNr = "")
            }
        }
    }

    fun updateKofferSearch(query: String, forPicker: Boolean = false) {
        _uiState.update {
            if (forPicker) it.copy(kofferPickerSearchQuery = query) else it.copy(kofferSearchQuery = query)
        }
    }

    fun selectKofferPin(boxId: String, pinNr: String, forPicker: Boolean = false) {
        val norm = InnowatechRepository.normalizePinNr(boxId, pinNr)
        _uiState.update {
            if (forPicker) {
                it.copy(kofferPickerBoxId = boxId, kofferPickerActivePinNr = norm)
            } else {
                it.copy(kofferSelectedBoxId = boxId, kofferActivePinNr = norm)
            }
        }
    }

    fun clearKofferPin(forPicker: Boolean = false) {
        _uiState.update {
            if (forPicker) it.copy(kofferPickerActivePinNr = "") else it.copy(kofferActivePinNr = "")
        }
    }

    fun openKofferPicker() {
        val allowed = _uiState.value.modules.find { it.module == ModuleId.KOFFER }?.userAllowed ?: true
        if (!allowed) {
            emitToast("Servicekoffer ist für dieses Kürzel nicht freigeschaltet.")
            return
        }
        _uiState.update { it.copy(kofferPickerOpen = true) }
    }

    fun closeKofferPicker() = _uiState.update { it.copy(kofferPickerOpen = false) }

    fun insertKofferVerbautToNotes(item: KofferItem, closePicker: Boolean = false) {
        val cur = _uiState.value.prefs.serviceNotes
        val tag = "[IN-${item.inNr}] ${item.beschreibung}"
        val next = if (Regex(":[ \\t]*$").containsMatchIn(cur)) {
            cur + (if (cur.endsWith(' ')) "" else " ") + tag
        } else {
            buildString {
                append(cur)
                if (cur.isNotEmpty() && !cur.endsWith('\n')) append('\n')
                append("🛠️ Ersatzteil / Verbaut: $tag\n")
            }
        }
        updateServiceNotes(next)
        if (closePicker) closeKofferPicker()
        emitToast("[IN-${item.inNr}] in Service-Notizen übernommen.")
    }

    fun requestKofferNachorderQty(boxId: String, item: KofferItem, closePickerOnConfirm: Boolean) {
        _uiState.update {
            it.copy(pendingKofferQty = KofferQtyPending(boxId, item, closePickerOnConfirm))
        }
    }

    fun dismissKofferQtyDialog() = _uiState.update { it.copy(pendingKofferQty = null) }

    fun confirmKofferNachorderQty(qty: Int) {
        val pending = _uiState.value.pendingKofferQty ?: return
        val q = qty.coerceIn(1, 999)
        val cur = _uiState.value.prefs.serviceNotes
        val line = "🧰 Nachorder Servicekoffer: ${q}x [IN-${pending.item.inNr}] ${pending.item.beschreibung}\n"
        val next = buildString {
            append(cur)
            if (cur.isNotEmpty() && !cur.endsWith('\n')) append('\n')
            append(line)
        }
        updateServiceNotes(next)
        _uiState.update {
            it.copy(
                pendingKofferQty = null,
                kofferPickerOpen = if (pending.closePickerOnConfirm) false else it.kofferPickerOpen
            )
        }
        emitToast("Nachorder ${q}x [IN-${pending.item.inNr}] in Notiz eingetragen.")
    }

    fun openVegaLightbox(stepIndex: Int) {
        val step = InnowatechRepository.vegaSteps.getOrNull(stepIndex) ?: return
        val img = step.imageRes ?: return
        val caption = (step.summaryBullets + listOfNotNull(step.note)).joinToString(" • ")
        _uiState.update {
            it.copy(
                lightboxContent = LightboxContent.VegaGuide(
                    title = "${step.num}. ${step.title}",
                    imageRes = img,
                    caption = caption
                )
            )
        }
    }

    fun openTurbineLightbox(nr: String, imageRes: Int) {
        _uiState.update {
            it.copy(lightboxContent = LightboxContent.TurbinePhoto(nr = nr, imageRes = imageRes))
        }
    }

    fun openKofferLightbox(boxId: String, highlightPin: String, fromPicker: Boolean) {
        val box = _uiState.value.databases.koffer.find { it.id == boxId } ?: return
        val hl = if (highlightPin.isNotBlank()) InnowatechRepository.normalizePinNr(boxId, highlightPin) else ""
        val summary = buildKofferPinSummary(boxId, hl)
        _uiState.update {
            it.copy(
                lightboxContent = LightboxContent.KofferPhoto(
                    boxId = box.id,
                    boxName = box.name,
                    imageRes = box.imageRes,
                    activePinNr = hl,
                    pins = InnowatechRepository.kofferPins[box.id].orEmpty(),
                    itemSummary = summary,
                    fromPicker = fromPicker
                )
            )
        }
    }

    fun selectPinInLightbox(pinNr: String) {
        val lb = _uiState.value.lightboxContent as? LightboxContent.KofferPhoto ?: return
        val hl = InnowatechRepository.normalizePinNr(lb.boxId, pinNr)
        val summary = buildKofferPinSummary(lb.boxId, hl)
        _uiState.update {
            it.copy(
                lightboxContent = lb.copy(activePinNr = hl, itemSummary = summary),
                kofferSelectedBoxId = if (!lb.fromPicker) lb.boxId else it.kofferSelectedBoxId,
                kofferActivePinNr = if (!lb.fromPicker) hl else it.kofferActivePinNr,
                kofferPickerBoxId = if (lb.fromPicker) lb.boxId else it.kofferPickerBoxId,
                kofferPickerActivePinNr = if (lb.fromPicker) hl else it.kofferPickerActivePinNr
            )
        }
    }

    fun closeLightbox() = _uiState.update { it.copy(lightboxContent = null) }

    private fun buildKofferPinSummary(boxId: String, pinNr: String): String {
        if (pinNr.isBlank()) return "Nummer im Foto antippen für Artikel-Info · Mit 2 Fingern zoomen"
        val box = _uiState.value.databases.koffer.find { it.id == boxId } ?: return "Fach $pinNr"
        val matches = box.items.filter { InnowatechRepository.normalizePinNr(boxId, it.nr) == pinNr }
        if (matches.isEmpty()) return "Fach $pinNr"
        val head = matches.take(2).joinToString(" · ") { "${it.beschreibung} (IN ${it.inNr})" }
        val extra = if (matches.size > 2) " (+${matches.size - 2})" else ""
        return "Fach $pinNr: $head$extra"
    }

    // Kunden- & Eigennotizen
    fun updateKnDraft(transform: (KundenNotizDraft) -> KundenNotizDraft) {
        val next = transform(_uiState.value.prefs.kundenNotizDraft)
        viewModelScope.launch { preferences.setKundenNotizDraft(next) }
    }

    fun saveKnEntry() {
        val draft = _uiState.value.prefs.kundenNotizDraft
        if (draft.kunde.isBlank() && draft.betreff.isBlank() && draft.text.isBlank()) {
            emitToast("Bitte mindestens Kunde, Betreff oder Notiztext ausfüllen.")
            return
        }
        val list = _uiState.value.prefs.kundenNotizenArchive.toMutableList()
        val now = InnowatechRepository.nowIsoTimestamp()
        val author = _uiState.value.prefs.activeUserKuerzel ?: "-"
        val existingIdx = draft.id?.let { id -> list.indexOfFirst { it.id == id } } ?: -1
        val savedId: String
        if (existingIdx >= 0) {
            val prev = list[existingIdx]
            savedId = prev.id
            list[existingIdx] = prev.copy(
                typ = draft.typ,
                sn = draft.sn.trim(),
                kunde = draft.kunde.trim(),
                betreff = draft.betreff.trim(),
                text = draft.text.trim(),
                author = author.ifBlank { prev.author },
                updatedAtIso = now
            )
        } else {
            savedId = "kn_${System.currentTimeMillis()}"
            list.add(
                0,
                KundenNotizEntry(
                    id = savedId,
                    typ = draft.typ,
                    sn = draft.sn.trim(),
                    kunde = draft.kunde.trim(),
                    betreff = draft.betreff.trim(),
                    text = draft.text.trim(),
                    author = author,
                    createdAtIso = now,
                    updatedAtIso = now
                )
            )
        }
        if (list.size > 80) {
            list.subList(80, list.size).clear()
        }
        viewModelScope.launch {
            preferences.setKundenNotizenArchive(list)
            preferences.setKundenNotizDraft(draft.copy(id = savedId))
        }
        emitToast("Notiz im Archiv gespeichert.")
    }

    fun newKnEntry() {
        viewModelScope.launch { preferences.setKundenNotizDraft(KundenNotizDraft()) }
        emitToast("Neue Notiz bereit.")
    }

    fun loadKnEntry(entry: KundenNotizEntry) {
        viewModelScope.launch {
            preferences.setKundenNotizDraft(
                KundenNotizDraft(
                    id = entry.id,
                    typ = entry.typ,
                    sn = entry.sn,
                    kunde = entry.kunde,
                    betreff = entry.betreff,
                    text = entry.text
                )
            )
        }
        emitToast("Notiz „${entry.betreff.ifBlank { entry.kunde.ifBlank { entry.typ } }}“ geladen.")
    }

    fun deleteKnEntry(id: String) {
        val list = _uiState.value.prefs.kundenNotizenArchive.filter { it.id != id }
        val draft = _uiState.value.prefs.kundenNotizDraft
        viewModelScope.launch {
            preferences.setKundenNotizenArchive(list)
            if (draft.id == id) preferences.setKundenNotizDraft(draft.copy(id = null))
        }
        emitToast("Notiz gelöscht.")
    }

    // Checkliste Enthärtertausch
    fun updateEtDraft(transform: (EnthaerterChecklistState) -> EnthaerterChecklistState) {
        val next = transform(_uiState.value.prefs.enthaerterDraft)
        viewModelScope.launch { preferences.setEnthaerterDraft(next) }
    }

    fun saveEtRecord() {
        val state = _uiState.value.prefs.enthaerterDraft
        val kunde = state.kunde.trim()
        val ort = state.ort.trim()
        val fab = state.fabnr.trim()
        if (kunde.isEmpty() && ort.isEmpty() && fab.isEmpty() && state.typ.isBlank()) {
            emitToast("Bitte mindestens Kunde, Ort, Fab.-Nr. oder Enthärtertyp eintragen.")
            return
        }
        val list = _uiState.value.prefs.enthaerterRecords.toMutableList()
        val title = "Aufnahme Enthärtertausch · ${kunde.ifEmpty { ort.ifEmpty { "Ohne Name" } }}"
        val now = InnowatechRepository.nowIsoTimestamp()
        val idx = state.recordId?.let { id -> list.indexOfFirst { it.id == id } } ?: -1
        val finalId: String
        if (idx >= 0) {
            finalId = list[idx].id
            list[idx] = SavedEnthaerterRecord(
                id = finalId,
                title = title,
                updatedAtIso = now,
                state = state.copy(recordId = finalId)
            )
        } else {
            finalId = "et_${System.currentTimeMillis()}"
            list.add(
                0,
                SavedEnthaerterRecord(
                    id = finalId,
                    title = title,
                    updatedAtIso = now,
                    state = state.copy(recordId = finalId)
                )
            )
        }
        if (list.size > 40) list.subList(40, list.size).clear()
        viewModelScope.launch {
            preferences.setEnthaerterRecords(list)
            preferences.setEnthaerterDraft(state.copy(recordId = finalId))
        }
        emitToast("Gespeichert: $title")
    }

    fun loadEtRecord(record: SavedEnthaerterRecord) {
        viewModelScope.launch {
            preferences.setEnthaerterDraft(record.state.copy(recordId = record.id))
        }
        emitToast("Aufnahme „${record.state.kunde.ifBlank { record.title }}“ geladen.")
    }

    fun deleteEtRecord(id: String) {
        val list = _uiState.value.prefs.enthaerterRecords.filter { it.id != id }
        val draft = _uiState.value.prefs.enthaerterDraft
        viewModelScope.launch {
            preferences.setEnthaerterRecords(list)
            if (draft.recordId == id) preferences.setEnthaerterDraft(draft.copy(recordId = null))
        }
        emitToast("Aufnahme gelöscht.")
    }

    fun resetEnthaerterChecklist() {
        val activeKuerzel = _uiState.value.prefs.activeUserKuerzel.orEmpty()
        viewModelScope.launch {
            preferences.setEnthaerterDraft(EnthaerterChecklistState(kuerzel = activeKuerzel))
        }
        emitToast("Neue / leere Checkliste bereit.")
    }

    override fun onCleared() {
        super.onCleared()
        TorchController.turnOffLedIfAvailable(appContext)
    }
}
