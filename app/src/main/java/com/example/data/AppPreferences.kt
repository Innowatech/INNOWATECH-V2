package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.DashboardDensity
import com.example.model.EnthaerterChecklistState
import com.example.model.KundenNotizDraft
import com.example.model.KundenNotizEntry
import com.example.model.ModuleCategory
import com.example.model.SavedEnthaerterRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "innowatech_tool_prefs_v56")

data class PersistedAppState(
    val darkMode: Boolean = false,
    val dashboardDensity: DashboardDensity = DashboardDensity.COMPACT,
    val showDashboardIcons: Boolean = true,
    val keepScreenAwake: Boolean = false,
    val timerBeepEnabled: Boolean = true,
    val persistCalcInputs: Boolean = false,
    val activeUserKuerzel: String? = null,
    val pinnedTabIds: List<String> = emptyList(),
    val categoryOrderKeys: List<String> = InnowatechRepository.defaultCategoryOrder.map { it.key },
    val hiddenModuleIds: Set<String> = emptySet(),
    val moduleOrderIds: List<String> = InnowatechRepository.defaultModules.map { it.module.id },
    val serviceNotes: String = "",
    val faultClearAfter: Boolean = true,
    val enthaerterRecords: List<SavedEnthaerterRecord> = emptyList(),
    val enthaerterDraft: EnthaerterChecklistState = EnthaerterChecklistState(),
    val kundenNotizenArchive: List<KundenNotizEntry> = emptyList(),
    val kundenNotizDraft: KundenNotizDraft = KundenNotizDraft(),
    val customDatabasesJson: String? = null,
    val customUsersJson: String? = null
)

class AppPreferences(private val context: Context) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DENSITY = stringPreferencesKey("dashboard_density")
        val SHOW_ICONS = booleanPreferencesKey("show_dashboard_icons")
        val KEEP_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val TIMER_BEEP = booleanPreferencesKey("timer_beep")
        val PERSIST_CALC = booleanPreferencesKey("persist_calc_inputs")
        val ACTIVE_USER = stringPreferencesKey("active_user_kuerzel")
        val PINNED_TABS = stringPreferencesKey("pinned_tabs_json")
        val CATEGORY_ORDER = stringPreferencesKey("category_order_json")
        val HIDDEN_MODULES = stringPreferencesKey("hidden_modules_json")
        val MODULE_ORDER = stringPreferencesKey("module_order_json")
        val SERVICE_NOTES = stringPreferencesKey("service_notes")
        val FAULT_CLEAR_AFTER = booleanPreferencesKey("fault_clear_after")
        val ET_RECORDS = stringPreferencesKey("et_records_json")
        val ET_DRAFT = stringPreferencesKey("et_draft_json")
        val KN_ARCHIVE = stringPreferencesKey("kn_archive_json")
        val KN_DRAFT = stringPreferencesKey("kn_draft_json")
        val CUSTOM_DATABASES = stringPreferencesKey("custom_databases_json")
        val CUSTOM_USERS = stringPreferencesKey("custom_users_json")
    }

    val state: Flow<PersistedAppState> = context.dataStore.data.map { prefs ->
        PersistedAppState(
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            dashboardDensity = DashboardDensity.fromKey(prefs[Keys.DENSITY]),
            showDashboardIcons = prefs[Keys.SHOW_ICONS] ?: true,
            keepScreenAwake = prefs[Keys.KEEP_AWAKE] ?: false,
            timerBeepEnabled = prefs[Keys.TIMER_BEEP] ?: true,
            persistCalcInputs = prefs[Keys.PERSIST_CALC] ?: false,
            activeUserKuerzel = prefs[Keys.ACTIVE_USER]?.takeIf { it.isNotBlank() },
            pinnedTabIds = parseStringList(prefs[Keys.PINNED_TABS]),
            categoryOrderKeys = parseStringList(prefs[Keys.CATEGORY_ORDER]).ifEmpty {
                ModuleCategory.entries.map { it.key }
            },
            hiddenModuleIds = parseStringList(prefs[Keys.HIDDEN_MODULES]).toSet(),
            moduleOrderIds = parseStringList(prefs[Keys.MODULE_ORDER]),
            serviceNotes = prefs[Keys.SERVICE_NOTES] ?: "",
            faultClearAfter = prefs[Keys.FAULT_CLEAR_AFTER] ?: true,
            enthaerterRecords = InnowatechRepository.parseEtRecords(prefs[Keys.ET_RECORDS] ?: ""),
            enthaerterDraft = parseEtDraft(prefs[Keys.ET_DRAFT]),
            kundenNotizenArchive = InnowatechRepository.parseKnArchive(prefs[Keys.KN_ARCHIVE] ?: ""),
            kundenNotizDraft = parseKnDraft(prefs[Keys.KN_DRAFT]),
            customDatabasesJson = prefs[Keys.CUSTOM_DATABASES],
            customUsersJson = prefs[Keys.CUSTOM_USERS]
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setDashboardDensity(density: DashboardDensity) {
        context.dataStore.edit { it[Keys.DENSITY] = density.key }
    }

    suspend fun setShowDashboardIcons(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_ICONS] = show }
    }

    suspend fun setKeepScreenAwake(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_AWAKE] = enabled }
    }

    suspend fun setTimerBeep(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TIMER_BEEP] = enabled }
    }

    suspend fun setPersistCalcInputs(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PERSIST_CALC] = enabled }
    }

    suspend fun setActiveUser(kuerzel: String?) {
        context.dataStore.edit {
            if (kuerzel.isNullOrBlank()) it.remove(Keys.ACTIVE_USER) else it[Keys.ACTIVE_USER] = kuerzel
        }
    }

    suspend fun setPinnedTabs(ids: List<String>) {
        context.dataStore.edit { it[Keys.PINNED_TABS] = JSONArray(ids.take(5)).toString() }
    }

    suspend fun setCategoryOrder(keys: List<String>) {
        context.dataStore.edit { it[Keys.CATEGORY_ORDER] = JSONArray(keys).toString() }
    }

    suspend fun setHiddenModules(ids: Set<String>) {
        context.dataStore.edit { it[Keys.HIDDEN_MODULES] = JSONArray(ids.toList()).toString() }
    }

    suspend fun setModuleOrder(ids: List<String>) {
        context.dataStore.edit { it[Keys.MODULE_ORDER] = JSONArray(ids).toString() }
    }

    suspend fun setServiceNotes(text: String) {
        context.dataStore.edit { it[Keys.SERVICE_NOTES] = text }
    }

    suspend fun setFaultClearAfter(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FAULT_CLEAR_AFTER] = enabled }
    }

    suspend fun setEnthaerterRecords(records: List<SavedEnthaerterRecord>) {
        context.dataStore.edit { it[Keys.ET_RECORDS] = InnowatechRepository.serializeEtRecords(records) }
    }

    suspend fun setEnthaerterDraft(draft: EnthaerterChecklistState) {
        val obj = JSONObject()
            .put("recordId", draft.recordId ?: "")
            .put("kuerzel", draft.kuerzel)
            .put("fabnr", draft.fabnr)
            .put("kunde", draft.kunde)
            .put("ort", draft.ort)
            .put("anlage", draft.anlage)
            .put("typ", draft.typ)
            .put("kabinettNeu", draft.kabinettNeu)
            .put("kabinettArt", draft.kabinettArt)
            .put("schlauchHv", draft.schlauchHv)
            .put("schlauchHvCustom", draft.schlauchHvCustom)
            .put("schlauchHvNeu", draft.schlauchHvNeu)
            .put("schlauchAbfluss", draft.schlauchAbfluss)
            .put("schlauchAbflussCustom", draft.schlauchAbflussCustom)
            .put("panzerGroesse", draft.panzerGroesse)
            .put("panzerGroesseCustom", draft.panzerGroesseCustom)
            .put("panzerLaenge", draft.panzerLaenge)
            .put("panzerLaengeCustom", draft.panzerLaengeCustom)
            .put("panzerAnschluss", draft.panzerAnschluss)
            .put("panzerAnschlussCustom", draft.panzerAnschlussCustom)
            .put("schwimmerlanze", draft.schwimmerlanze)
            .put("turbine", draft.turbine)
            .put("fotos", draft.fotos)
            .put("bemerkung", draft.bemerkung)
        context.dataStore.edit { it[Keys.ET_DRAFT] = obj.toString() }
    }

    suspend fun setKundenNotizenArchive(entries: List<KundenNotizEntry>) {
        context.dataStore.edit { it[Keys.KN_ARCHIVE] = InnowatechRepository.serializeKnArchive(entries) }
    }

    suspend fun setKundenNotizDraft(draft: KundenNotizDraft) {
        val obj = JSONObject()
            .put("id", draft.id ?: "")
            .put("typ", draft.typ)
            .put("sn", draft.sn)
            .put("kunde", draft.kunde)
            .put("betreff", draft.betreff)
            .put("text", draft.text)
        context.dataStore.edit { it[Keys.KN_DRAFT] = obj.toString() }
    }

    suspend fun setCustomDatabasesJson(json: String?) {
        context.dataStore.edit {
            if (json == null) it.remove(Keys.CUSTOM_DATABASES) else it[Keys.CUSTOM_DATABASES] = json
        }
    }

    suspend fun setCustomUsersJson(json: String?) {
        context.dataStore.edit {
            if (json == null) it.remove(Keys.CUSTOM_USERS) else it[Keys.CUSTOM_USERS] = json
        }
    }

    private fun parseStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    add(arr.optString(i))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseEtDraft(raw: String?): EnthaerterChecklistState {
        if (raw.isNullOrBlank()) return EnthaerterChecklistState()
        return try {
            val obj = JSONObject(raw)
            EnthaerterChecklistState(
                recordId = obj.optString("recordId").takeIf { it.isNotBlank() },
                kuerzel = obj.optString("kuerzel"),
                fabnr = obj.optString("fabnr"),
                kunde = obj.optString("kunde"),
                ort = obj.optString("ort"),
                anlage = obj.optString("anlage"),
                typ = obj.optString("typ"),
                kabinettNeu = obj.optString("kabinettNeu"),
                kabinettArt = obj.optString("kabinettArt"),
                schlauchHv = obj.optString("schlauchHv"),
                schlauchHvCustom = obj.optString("schlauchHvCustom"),
                schlauchHvNeu = obj.optString("schlauchHvNeu"),
                schlauchAbfluss = obj.optString("schlauchAbfluss"),
                schlauchAbflussCustom = obj.optString("schlauchAbflussCustom"),
                panzerGroesse = obj.optString("panzerGroesse"),
                panzerGroesseCustom = obj.optString("panzerGroesseCustom"),
                panzerLaenge = obj.optString("panzerLaenge"),
                panzerLaengeCustom = obj.optString("panzerLaengeCustom"),
                panzerAnschluss = obj.optString("panzerAnschluss"),
                panzerAnschlussCustom = obj.optString("panzerAnschlussCustom"),
                schwimmerlanze = obj.optString("schwimmerlanze"),
                turbine = obj.optString("turbine"),
                fotos = obj.optString("fotos"),
                bemerkung = obj.optString("bemerkung")
            )
        } catch (_: Exception) {
            EnthaerterChecklistState()
        }
    }

    private fun parseKnDraft(raw: String?): KundenNotizDraft {
        if (raw.isNullOrBlank()) return KundenNotizDraft()
        return try {
            val obj = JSONObject(raw)
            KundenNotizDraft(
                id = obj.optString("id").takeIf { it.isNotBlank() },
                typ = obj.optString("typ", "Kundennotiz"),
                sn = obj.optString("sn"),
                kunde = obj.optString("kunde"),
                betreff = obj.optString("betreff"),
                text = obj.optString("text")
            )
        } catch (_: Exception) {
            KundenNotizDraft()
        }
    }
}
