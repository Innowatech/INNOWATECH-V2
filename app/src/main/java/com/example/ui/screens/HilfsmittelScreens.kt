package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.InnowatechCalculations
import com.example.data.InnowatechRepository
import com.example.model.ModuleId
import com.example.ui.ServiceToolUiState
import com.example.ui.ServiceToolViewModel
import com.example.ui.components.HintSeverity
import com.example.ui.components.IndustrialCard
import com.example.ui.components.IndustrialDropdownField
import com.example.ui.components.IndustrialHintBanner
import com.example.ui.components.IndustrialNumberField
import com.example.ui.components.IndustrialResultBox
import com.example.ui.components.IndustrialTextField
import com.example.ui.components.ModuleBackBar
import com.example.ui.components.SectionTitle
import com.example.ui.components.SegmentedOptionRow
import com.example.ui.components.categoryAccentColor
import com.example.ui.theme.IndustrialTextStyles
import com.example.ui.theme.InnotechTheme
import com.example.util.PlatformActions
import java.util.Locale

private fun String.toCommaDouble(): Double =
    this.replace(',', '.').trim().toDoubleOrNull() ?: 0.0

@Composable
fun HilfsmittelModuleHost(
    module: ModuleId,
    state: ServiceToolUiState,
    viewModel: ServiceToolViewModel,
    onStartVoiceDictation: (target: String) -> Unit,
    onBack: () -> Unit
) {
    val accent = categoryAccentColor(module.defaultCategory)
    val resetLabel = when (module) {
        ModuleId.TASCHENLAMPE -> null
        ModuleId.NOTIZEN -> "Leeren"
        ModuleId.KUNDENNOTIZEN -> "Neu"
        else -> "Reset"
    }
    Column(modifier = Modifier.fillMaxSize()) {
        ModuleBackBar(
            module = module,
            resetLabel = resetLabel,
            onBack = onBack,
            onReset = if (resetLabel != null) viewModel::resetActiveModule else null
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SectionTitle(title = module.title, accentColor = accent) }
            item {
                when (module) {
                    ModuleId.TASCHENLAMPE -> TaschenlampeContent(state, viewModel)
                    ModuleId.SCHLAUCHVOLUMEN -> SchlauchvolumenContent(state, viewModel)
                    ModuleId.AUSLITERN -> AusliternContent(state, viewModel)
                    ModuleId.NOTIZEN -> ServiceNotizenContent(state, viewModel, onStartVoiceDictation)
                    ModuleId.KUNDENNOTIZEN -> KundenNotizenContent(state, viewModel, onStartVoiceDictation)
                    else -> Unit
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TaschenlampeContent(state: ServiceToolUiState, viewModel: ServiceToolViewModel) {
    val colors = InnotechTheme.colors
    val active = state.ledTorchOn || state.screenTorchOn
    IndustrialCard {
        Text(
            text = "Kamerablitz-LED (ohne Kamera-Berechtigung über System-Torch) oder weißer Display-Notlicht-Modus.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = viewModel::toggleFlashlight,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active) colors.namurFailure else colors.signalYellow,
                contentColor = if (active) Color.White else colors.onSignalYellow
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("torch_toggle_button")
        ) {
            Icon(
                imageVector = if (active) Icons.Filled.FlashlightOff else Icons.Filled.FlashlightOn,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    state.ledTorchOn -> "LED-Taschenlampe AUS"
                    state.screenTorchOn -> "Display-Notlicht AUS"
                    else -> "Taschenlampe EIN"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
        IndustrialHintBanner(
            text = if (state.ledTorchOn) {
                "LED-Blitzlicht aktiv."
            } else {
                "Falls kein Kamerablitz verfügbar ist, schaltet der Button automatisch das helle Display-Notlicht ein."
            },
            severity = if (active) HintSeverity.SUCCESS else HintSeverity.INFO
        )
    }
}

@Composable
private fun SchlauchvolumenContent(state: ServiceToolUiState, viewModel: ServiceToolViewModel) {
    val inputs = state.calcInputs
    val res = InnowatechCalculations.calcSchlauch(
        dMm = inputs.schlauchD.toCommaDouble(),
        lM = inputs.schlauchL.toCommaDouble(),
        pumpEnabled = inputs.schlauchPumpEnabled,
        pumpLh = inputs.schlauchPumpLh.toCommaDouble()
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustrialCard {
            IndustrialNumberField(
                value = inputs.schlauchD,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(schlauchD = v) } },
                label = "Schlauch-Innendurchmesser",
                unit = "mm",
                placeholder = "z. B. 4",
                testTag = "schlauch_d_input"
            )
            IndustrialNumberField(
                value = inputs.schlauchL,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(schlauchL = v) } },
                label = "Schlauchlänge",
                unit = "m",
                placeholder = "z. B. 12",
                testTag = "schlauch_l_input"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Förderleistung für Vorlaufzeit berechnen", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = inputs.schlauchPumpEnabled,
                    onCheckedChange = { e -> viewModel.updateCalcInputs { it.copy(schlauchPumpEnabled = e) } }
                )
            }
            if (inputs.schlauchPumpEnabled) {
                IndustrialNumberField(
                    value = inputs.schlauchPumpLh,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(schlauchPumpLh = v) } },
                    label = "Pumpen-Förderleistung",
                    unit = "l/h"
                )
            }
        }
        IndustrialCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndustrialResultBox(label = "Volumen pro Meter", value = res.proMeterText, modifier = Modifier.weight(1f))
                IndustrialResultBox(label = "Gesamtvolumen", value = res.gesamtText, highlight = true, modifier = Modifier.weight(1f))
            }
            if (inputs.schlauchPumpEnabled) {
                IndustrialResultBox(label = "Vorpump- / Ankunftszeit", value = res.zeitText, highlight = res.zeitText != "--")
                if (res.hint.isNotEmpty()) IndustrialHintBanner(text = res.hint)
            }
        }
    }
}

@Composable
private fun AusliternContent(state: ServiceToolUiState, viewModel: ServiceToolViewModel) {
    val colors = InnotechTheme.colors
    val inputs = state.calcInputs
    val factor = 3600.0 / inputs.ausliterSec.toDouble()
    val kathMl = inputs.ausliterKathMl.toCommaDouble()
    val anolMl = inputs.ausliterAnolMl.toCommaDouble()
    val kathLh = (kathMl * factor) / 1000.0
    val anolLh = (anolMl * factor) / 1000.0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustrialCard {
            Text("Medium & Messdauer", style = MaterialTheme.typography.titleSmall)
            SegmentedOptionRow(
                options = listOf(
                    "katholyte" to "Katholyte",
                    "anolyte" to "Anolyte",
                    "beide" to "Beide"
                ),
                selected = inputs.ausliterMedium,
                onSelect = viewModel::setAusliterMedium
            )
            SegmentedOptionRow(
                options = listOf(15 to "15 s", 30 to "30 s", 60 to "60 s"),
                selected = inputs.ausliterSec,
                onSelect = viewModel::setAusliterDuration
            )
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = colors.subtleSurface,
                border = BorderStroke(1.dp, if (state.ausliterRunning) colors.signalYellow else colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format(Locale.US, "%02d s", state.ausliterTimeLeftSec),
                        style = IndustrialTextStyles.ReadoutLarge,
                        color = if (state.ausliterRunning) colors.signalYellow else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (state.ausliterRunning) viewModel.stopAusliterTimer() else viewModel.startAusliterTimer()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.ausliterRunning) colors.namurFailure else colors.signalYellow,
                            contentColor = if (state.ausliterRunning) Color.White else colors.onSignalYellow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ausliter_timer_button")
                    ) {
                        Icon(
                            if (state.ausliterRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (state.ausliterRunning) "Timer stoppen" else "Timer starten (${inputs.ausliterSec} s)")
                    }
                }
            }
        }

        IndustrialCard {
            if (inputs.ausliterMedium == "katholyte" || inputs.ausliterMedium == "beide") {
                IndustrialNumberField(
                    value = inputs.ausliterKathMl,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(ausliterKathMl = v) } },
                    label = "Aufgefangene Menge Katholyte",
                    unit = "ml",
                    testTag = "ausliter_kath_input"
                )
                IndustrialResultBox(
                    label = "Katholyte-Leistung (hochgerechnet)",
                    value = String.format(Locale.US, "%.1f l/h", kathLh),
                    highlight = kathMl > 0.0
                )
            }
            if (inputs.ausliterMedium == "anolyte" || inputs.ausliterMedium == "beide") {
                IndustrialNumberField(
                    value = inputs.ausliterAnolMl,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(ausliterAnolMl = v) } },
                    label = "Aufgefangene Menge Anolyte",
                    unit = "ml",
                    testTag = "ausliter_anol_input"
                )
                IndustrialResultBox(
                    label = "Anolyte-Leistung (hochgerechnet)",
                    value = String.format(Locale.US, "%.1f l/h", anolLh),
                    highlight = anolMl > 0.0
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceNotizenContent(
    state: ServiceToolUiState,
    viewModel: ServiceToolViewModel,
    onStartVoiceDictation: (String) -> Unit
) {
    val context = LocalContext.current
    val colors = InnotechTheme.colors
    val notes = state.prefs.serviceNotes
    val userTag = state.prefs.activeUserKuerzel?.let { " ($it)" }.orEmpty()

    var urgency by remember { mutableStateOf("normal") }
    var hotlineAnlage by remember { mutableStateOf("") }
    var hotlineSn by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustrialCard {
            Text("Schnellbausteine & Werkzeuge", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val chips = listOf(
                    "⏱️ Zeitstempel" to { viewModel.appendServiceNoteLine(InnowatechRepository.nowTimestampChip()) },
                    "📋 Einsatzgrund" to { viewModel.appendServiceNoteLine("📋 Einsatzgrund / Auftrag:") },
                    "👷 Mitarbeiter$userTag" to { viewModel.appendServiceNoteLine("👷 Mitarbeiter$userTag:") },
                    "🏢 Kunde" to { viewModel.appendServiceNoteLine("🏢 Kunde:") },
                    "🔢 Anlagen-SN" to { viewModel.appendServiceNoteLine("🔢 Anlagen-SN:") },
                    "🛠️ Ersatzteil" to { viewModel.appendServiceNoteLine("🛠️ Ersatzteil benötigt:") },
                    "📄 Angebot" to { viewModel.appendServiceNoteLine("📄 Angebot erstellen für:") },
                    "🧪 Verbrauch" to { viewModel.appendServiceNoteLine("🧪 Verbrauchsmaterial:") },
                    "📦 Nachorder" to { viewModel.appendServiceNoteLine("📦 Nachorder Servicekoffer:") }
                )
                chips.forEach { (label, action) ->
                    AssistChip(onClick = action, label = { Text(label) })
                }
                AssistChip(
                    onClick = { onStartVoiceDictation("service") },
                    leadingIcon = { Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Diktieren") }
                )
                AssistChip(
                    onClick = viewModel::openKofferPicker,
                    leadingIcon = { Icon(Icons.Filled.BusinessCenter, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Koffer-Teil wählen") }
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = viewModel::updateServiceNotes,
                label = { Text("Service-Notizen") },
                placeholder = { Text("Einsatz, offene Punkte, Ersatzteile oder Diktat…") },
                minLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_notes_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (PlatformActions.copyToClipboard(context, "Service-Notizen", notes)) {
                            viewModel.emitToast("Service-Notizen kopiert.")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kopieren")
                }
                OutlinedButton(
                    onClick = { PlatformActions.sharePlainText(context, "Service-Notizen", notes) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Teilen")
                }
                OutlinedButton(
                    onClick = {
                        PlatformActions.printTextReport(
                            context = context,
                            documentTitle = "Service-Notizen INNOWATΞCH",
                            heading = "Service-Notizen",
                            subtitle = state.prefs.activeUserKuerzel?.let { "Techniker: $it" } ?: "INNOWATΞCH Service Tool",
                            rows = emptyList(),
                            bodyText = notes.ifBlank { "-" }
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF")
                }
            }
        }

        IndustrialCard(borderColor = colors.namurFailure.copy(alpha = 0.5f)) {
            Text(
                text = "Störmeldung an Service-Hotline",
                style = MaterialTheme.typography.titleSmall,
                color = colors.namurFailure
            )
            SegmentedOptionRow(
                options = listOf("normal" to "Normal", "dringend" to "Dringend", "ausfall" to "Anlagen-Ausfall"),
                selected = urgency,
                onSelect = { urgency = it }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IndustrialTextField(
                    value = hotlineAnlage,
                    onValueChange = { hotlineAnlage = it },
                    label = "Kunde / Anlage",
                    modifier = Modifier.weight(1f)
                )
                IndustrialTextField(
                    value = hotlineSn,
                    onValueChange = { hotlineSn = it },
                    label = "SN",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.prefs.faultClearAfter,
                    onCheckedChange = viewModel::setFaultClearAfter
                )
                Text("Service-Notizen nach E-Mail automatisch leeren", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    val subject = "[${urgency.uppercase()}] Service-Meldung ${hotlineAnlage.ifBlank { "Anlage" }} ${hotlineSn.ifBlank { "" }}".trim()
                    val body = buildString {
                        appendLine("Techniker: ${state.prefs.activeUserKuerzel ?: "-"}")
                        appendLine("Dringlichkeit: $urgency")
                        if (hotlineAnlage.isNotBlank()) appendLine("Kunde / Anlage: $hotlineAnlage")
                        if (hotlineSn.isNotBlank()) appendLine("SN: $hotlineSn")
                        appendLine()
                        append(notes)
                    }
                    PlatformActions.openEmailIntent(
                        context = context,
                        toList = listOf(InnowatechRepository.HOTLINE_EMAIL),
                        subject = subject,
                        body = body
                    )
                    if (state.prefs.faultClearAfter) viewModel.updateServiceNotes("")
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.namurFailure, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hotline_email_button")
            ) {
                Icon(Icons.Filled.Email, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("An ${InnowatechRepository.HOTLINE_EMAIL} senden")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KundenNotizenContent(
    state: ServiceToolUiState,
    viewModel: ServiceToolViewModel,
    onStartVoiceDictation: (String) -> Unit
) {
    val context = LocalContext.current
    val colors = InnotechTheme.colors
    val draft = state.prefs.kundenNotizDraft
    val directory = remember(state.users, state.prefs.activeUserKuerzel) {
        InnowatechRepository.getKnDirectory(state.users, state.prefs.activeUserKuerzel)
    }

    var selectedTo by remember { mutableStateOf(setOf<String>()) }
    var selectedCc by remember { mutableStateOf(setOf<String>()) }
    var archiveSearch by remember { mutableStateOf("") }
    var archiveTypFilter by remember { mutableStateOf("Alle") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustrialCard {
            IndustrialDropdownField(
                label = "Notiz-Typ",
                selectedLabel = draft.typ,
                options = InnowatechRepository.kundenNotizTypes.map { it to it },
                onSelect = { t -> viewModel.updateKnDraft { it.copy(typ = t) } }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IndustrialTextField(
                    value = draft.kunde,
                    onValueChange = { v -> viewModel.updateKnDraft { it.copy(kunde = v) } },
                    label = "Kunde / Objekt",
                    modifier = Modifier.weight(1f)
                )
                IndustrialTextField(
                    value = draft.sn,
                    onValueChange = { v -> viewModel.updateKnDraft { it.copy(sn = v) } },
                    label = "Anlagen-SN",
                    modifier = Modifier.weight(1f)
                )
            }
            IndustrialTextField(
                value = draft.betreff,
                onValueChange = { v -> viewModel.updateKnDraft { it.copy(betreff = v) } },
                label = "Betreff"
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {
                        viewModel.updateKnDraft {
                            val sep = if (it.text.isNotEmpty() && !it.text.endsWith('\n')) "\n" else ""
                            it.copy(text = it.text + sep + InnowatechRepository.nowTimestampChip() + " ")
                        }
                    },
                    label = { Text("⏱️ Zeitstempel") }
                )
                AssistChip(
                    onClick = { onStartVoiceDictation("kunden") },
                    leadingIcon = { Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Diktieren") }
                )
            }
            OutlinedTextField(
                value = draft.text,
                onValueChange = { v -> viewModel.updateKnDraft { it.copy(text = v) } },
                label = { Text("Notiztext") },
                minLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kn_text_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = viewModel::saveKnEntry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.signalYellow,
                        contentColor = colors.onSignalYellow
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kn_save_button")
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Speichern")
                }
                OutlinedButton(
                    onClick = {
                        PlatformActions.printTextReport(
                            context = context,
                            documentTitle = "${draft.typ} - ${draft.kunde.ifBlank { "Notiz" }}",
                            heading = draft.typ,
                            subtitle = "Kunde: ${draft.kunde.ifBlank { "-" }} · SN: ${draft.sn.ifBlank { "-" }}",
                            rows = listOf("Betreff" to draft.betreff.ifBlank { "-" }),
                            bodyText = draft.text.ifBlank { "-" }
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF")
                }
            }
        }

        IndustrialCard {
            Text("Kollegen-Mail (An / CC aus users.json)", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                directory.forEach { rec ->
                    val inTo = selectedTo.contains(rec.email)
                    val inCc = selectedCc.contains(rec.email)
                    FilterChip(
                        selected = inTo || inCc,
                        onClick = {
                            when {
                                !inTo && !inCc -> selectedTo = selectedTo + rec.email
                                inTo -> {
                                    selectedTo = selectedTo - rec.email
                                    selectedCc = selectedCc + rec.email
                                }
                                else -> selectedCc = selectedCc - rec.email
                            }
                        },
                        label = {
                            val role = if (inTo) " [An]" else if (inCc) " [CC]" else ""
                            Text("${rec.name}$role")
                        }
                    )
                }
            }
            Button(
                onClick = {
                    if (selectedTo.isEmpty() && selectedCc.isEmpty()) {
                        viewModel.emitToast("Bitte mindestens einen Empfänger antippen.")
                    } else {
                        val toFinal = if (selectedTo.isNotEmpty()) selectedTo.toList() else selectedCc.toList()
                        val ccFinal = if (selectedTo.isNotEmpty()) selectedCc.toList() else emptyList()
                        val subj = "[${draft.typ}] ${draft.betreff.ifBlank { draft.kunde.ifBlank { "Notiz" } }}"
                        val body = "Kunde: ${draft.kunde}\nSN: ${draft.sn}\n\n${draft.text}"
                        PlatformActions.openEmailIntent(context, toFinal, ccFinal, subj, body)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Email, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Per Mail senden (An/CC)")
            }
        }

        IndustrialCard {
            Text("Lokales Archiv (${state.prefs.kundenNotizenArchive.size})", style = MaterialTheme.typography.titleSmall)
            IndustrialTextField(
                value = archiveSearch,
                onValueChange = { archiveSearch = it },
                label = "Archiv durchsuchen",
                placeholder = "Kunde, Betreff, Text…"
            )
            IndustrialDropdownField(
                label = "Typ-Filter",
                selectedLabel = archiveTypFilter,
                options = (listOf("Alle") + InnowatechRepository.kundenNotizTypes).map { it to it },
                onSelect = { archiveTypFilter = it }
            )
            val filteredArchive = remember(state.prefs.kundenNotizenArchive, archiveSearch, archiveTypFilter) {
                val q = archiveSearch.trim().lowercase(Locale.GERMAN)
                state.prefs.kundenNotizenArchive.filter { e ->
                    (archiveTypFilter == "Alle" || e.typ == archiveTypFilter) && (
                        q.isEmpty() ||
                            e.kunde.lowercase(Locale.GERMAN).contains(q) ||
                            e.betreff.lowercase(Locale.GERMAN).contains(q) ||
                            e.text.lowercase(Locale.GERMAN).contains(q) ||
                            e.sn.lowercase(Locale.GERMAN).contains(q)
                        )
                }
            }
            if (filteredArchive.isEmpty()) {
                Text("Keine gespeicherten Notizen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                filteredArchive.forEach { entry ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.subtleSurface,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.loadKnEntry(entry) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${entry.typ} · ${entry.kunde.ifBlank { entry.betreff.ifBlank { "-" } }}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${InnowatechRepository.formatGermanDateTime(entry.updatedAtIso)} (${entry.author}) · ${entry.text.take(60)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.deleteKnEntry(entry.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Löschen", tint = colors.namurFailure)
                            }
                        }
                    }
                }
            }
        }
    }
}
