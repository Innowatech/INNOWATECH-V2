package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.InnowatechRepository
import com.example.ui.ServiceToolUiState
import com.example.ui.ServiceToolViewModel
import com.example.ui.components.IndustrialCard
import com.example.ui.components.IndustrialDropdownField
import com.example.ui.components.IndustrialResultBox
import com.example.ui.components.IndustrialTextField
import com.example.ui.components.SectionTitle
import com.example.ui.theme.InnotechTheme
import com.example.util.PlatformActions

private data class TurbineChoice(
    val nr: String,
    val label: String,
    val imageRes: Int?
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnthaertertauschBody(
    state: ServiceToolUiState,
    viewModel: ServiceToolViewModel,
    accentColor: Color
) {
    val context = LocalContext.current
    val colors = InnotechTheme.colors
    val draft = state.prefs.enthaerterDraft
    val zeitaufwand = InnowatechRepository.getEtTypTime(draft.typ) ?: "Bitte Enthärtertyp auswählen…"

    var selectedRecipients by remember {
        mutableStateOf(InnowatechRepository.etMailRecipients.map { it.email }.toSet())
    }

    val turbines = remember {
        listOf(
            TurbineChoice("100041", "Turbine 100041", R.drawable.turbine_100041),
            TurbineChoice("109012", "Turbine 109012", R.drawable.turbine_109012),
            TurbineChoice("103547", "Turbine 103547", R.drawable.turbine_103547),
            TurbineChoice("Ohne", "Ohne / Keine verbaut", null)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitle(title = "Checkliste Enthärtertausch", accentColor = accentColor)
        }

        item {
            IndustrialCard {
                Text("Stammdaten & Enthärtertyp", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IndustrialTextField(
                        value = draft.kuerzel,
                        onValueChange = { v -> viewModel.updateEtDraft { it.copy(kuerzel = v.uppercase()) } },
                        label = "Kürzel",
                        modifier = Modifier.weight(0.35f)
                    )
                    IndustrialTextField(
                        value = draft.fabnr,
                        onValueChange = { v -> viewModel.updateEtDraft { it.copy(fabnr = v) } },
                        label = "Fab.-Nr.",
                        modifier = Modifier.weight(0.65f)
                    )
                }
                IndustrialTextField(
                    value = draft.kunde,
                    onValueChange = { v -> viewModel.updateEtDraft { it.copy(kunde = v) } },
                    label = "Kundenname",
                    testTag = "et_kunde_input"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IndustrialTextField(
                        value = draft.ort,
                        onValueChange = { v -> viewModel.updateEtDraft { it.copy(ort = v) } },
                        label = "PLZ & Ort",
                        modifier = Modifier.weight(1f)
                    )
                    IndustrialTextField(
                        value = draft.anlage,
                        onValueChange = { v -> viewModel.updateEtDraft { it.copy(anlage = v) } },
                        label = "Anlagentyp",
                        modifier = Modifier.weight(1f)
                    )
                }
                IndustrialDropdownField(
                    label = "Enthärtertyp (14 Typen)",
                    selectedLabel = InnowatechRepository.enthaerterTypSpecs.find { it.value == draft.typ }?.displayLabel
                        ?: "Bitte Enthärtertyp wählen…",
                    options = listOf("" to "Bitte Enthärtertyp wählen…") +
                        InnowatechRepository.enthaerterTypSpecs.map { it.value to it.displayLabel },
                    onSelect = { typ -> viewModel.updateEtDraft { it.copy(typ = typ) } },
                    testTag = "et_typ_dropdown"
                )
                IndustrialResultBox(
                    label = "Zeitaufwand (inkl. Regeneration)",
                    value = zeitaufwand,
                    highlight = draft.typ.isNotBlank()
                )
            }
        }

        item {
            IndustrialCard {
                Text("Kabinett & Verschlauchung", style = MaterialTheme.typography.titleSmall)
                ChoiceChipGroup(
                    label = "Kabinett auch erneuern",
                    options = listOf("Ja", "Nein"),
                    selected = draft.kabinettNeu,
                    onSelect = { v -> viewModel.updateEtDraft { it.copy(kabinettNeu = v) } }
                )
                ChoiceChipGroup(
                    label = "Kabinett-Art",
                    options = listOf("Standard (klein)", "Doppelkabinett", "Salzbehälter separat", "Kein Kabinett"),
                    selected = draft.kabinettArt,
                    onSelect = { v -> viewModel.updateEtDraft { it.copy(kabinettArt = v) } }
                )
                ChoiceChipGroupWithCustom(
                    label = "Schlauch Enthärter → Hauptventil (AD)",
                    options = listOf("6 mm", "8 mm", "10 mm", "12 mm", "Sonstiges"),
                    selected = draft.schlauchHv,
                    customValue = draft.schlauchHvCustom,
                    onSelect = { v -> viewModel.updateEtDraft { it.copy(schlauchHv = v) } },
                    onCustomChange = { c -> viewModel.updateEtDraft { it.copy(schlauchHvCustom = c) } }
                )
                ChoiceChipGroup(
                    label = "Schlauch zum Hauptventil erneuern",
                    options = listOf("Ja", "Nein"),
                    selected = draft.schlauchHvNeu,
                    onSelect = { v -> viewModel.updateEtDraft { it.copy(schlauchHvNeu = v) } }
                )
                ChoiceChipGroupWithCustom(
                    label = "Schlauch Enthärter → Abfluss (ID)",
                    options = listOf("12 mm", "13 mm", "16 mm", "19 mm", "Sonstiges"),
                    selected = draft.schlauchAbfluss,
                    customValue = draft.schlauchAbflussCustom,
                    onSelect = { v -> viewModel.updateEtDraft { it.copy(schlauchAbfluss = v) } },
                    onCustomChange = { c -> viewModel.updateEtDraft { it.copy(schlauchAbflussCustom = c) } }
                )
            }
        }

        item {
            IndustrialCard {
                Text("Panzerschlauch & Schwimmerlanze", style = MaterialTheme.typography.titleSmall)
                ChoiceChipGroupWithCustom(
                    label = "Panzerschlauch Größe",
                    options = listOf("3/4\"", "1\"", "1 1/4\"", "Sonstiges"),
                    selected = draft.panzerGroesse,
                    customValue = draft.panzerGroesseCustom,
                    onSelect = { v -> viewModel.updateEtDraft { it.copy(panzerGroesse = v) } },
                    onCustomChange = { c -> viewModel.updateEtDraft { it.copy(panzerGroesseCustom = c) } }
                )
                ChoiceChipGroupWithCustom(
                    label = "Panzerschlauch Länge",
                    options = listOf("500 mm", "750 mm", "1000 mm", "1500 mm", "Sonstiges"),
                    selected = draft.panzerLaenge,
                    customValue = draft.panzerLaengeCustom,
                    onSelect = { v -> viewModel.updateEtDraft { it.copy(panzerLaenge = v) } },
                    onCustomChange = { c -> viewModel.updateEtDraft { it.copy(panzerLaengeCustom = c) } }
                )
                ChoiceChipGroupWithCustom(
                    label = "Panzerschlauch Anschlussart",
                    options = listOf("ÜWM / ÜWM", "AG / ÜWM", "90° Bogen", "Sonstiges"),
                    selected = draft.panzerAnschluss,
                    customValue = draft.panzerAnschlussCustom,
                    onSelect = { v -> viewModel.updateEtDraft { it.copy(panzerAnschluss = v) } },
                    onCustomChange = { c -> viewModel.updateEtDraft { it.copy(panzerAnschlussCustom = c) } }
                )
                ChoiceChipGroup(
                    label = "Extra Schwimmerlanze (Solesättigung)",
                    options = listOf("Ja", "Nein"),
                    selected = draft.schwimmerlanze,
                    onSelect = { v -> viewModel.updateEtDraft { it.copy(schwimmerlanze = v) } }
                )
            }
        }

        item {
            IndustrialCard {
                Text("Durchflussmesser / Turbine", style = MaterialTheme.typography.titleSmall)
                val rows = turbines.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { choice ->
                                val isSelected = draft.turbine == choice.nr
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) colors.namurCheck else colors.cardBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { viewModel.updateEtDraft { it.copy(turbine = choice.nr) } }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        if (choice.imageRes != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(4f / 3f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.subtleSurface)
                                            ) {
                                                Image(
                                                    painter = painterResource(id = choice.imageRes),
                                                    contentDescription = choice.label,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                IconButton(
                                                    onClick = { viewModel.openTurbineLightbox(choice.nr, choice.imageRes) },
                                                    modifier = Modifier.align(Alignment.BottomEnd)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.ZoomIn,
                                                        contentDescription = "Zoom",
                                                        tint = Color.White
                                                    )
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(4f / 3f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.subtleSurface),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "Keine Turbine",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = "Ausgewählt",
                                                    tint = colors.namurCheck,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = choice.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) colors.namurCheck else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            IndustrialCard {
                ChoiceChipGroup(
                    label = "Fotos gemacht (alle Seiten / Bauteile / Rotgussfittings)",
                    options = listOf("Ja", "Nein"),
                    selected = draft.fotos,
                    onSelect = { v -> viewModel.updateEtDraft { it.copy(fotos = v) } }
                )
                IndustrialTextField(
                    value = draft.bemerkung,
                    onValueChange = { v -> viewModel.updateEtDraft { it.copy(bemerkung = v) } },
                    label = "Bemerkungen",
                    singleLine = false,
                    minLines = 3
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = viewModel::saveEtRecord,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.signalYellow,
                            contentColor = colors.onSignalYellow
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("et_save_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lokal speichern")
                    }
                    OutlinedButton(
                        onClick = {
                            val title = "Aufnahme Enthärtertausch - ${draft.kunde.ifBlank { "Kunde" }}"
                            val text = InnowatechRepository.buildEnthaertertauschText(draft)
                            PlatformActions.printTextReport(
                                context = context,
                                documentTitle = title,
                                heading = "Aufnahme Enthärtertausch",
                                subtitle = "${draft.kunde.ifBlank { "-" }} · ${draft.ort.ifBlank { "-" }}",
                                rows = emptyList(),
                                bodyText = text
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF drucken")
                    }
                }
            }
        }

        item {
            IndustrialCard {
                Text("E-Mail-Verteiler Enthärtertausch", style = MaterialTheme.typography.titleSmall)
                InnowatechRepository.etMailRecipients.forEach { rec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedRecipients = if (selectedRecipients.contains(rec.email)) {
                                    selectedRecipients - rec.email
                                } else {
                                    selectedRecipients + rec.email
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedRecipients.contains(rec.email),
                            onCheckedChange = { checked ->
                                selectedRecipients = if (checked) {
                                    selectedRecipients + rec.email
                                } else {
                                    selectedRecipients - rec.email
                                }
                            }
                        )
                        Column {
                            Text(rec.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(rec.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (selectedRecipients.isEmpty()) {
                                viewModel.emitToast("Bitte mindestens eine Empfängerin / einen Empfänger wählen.")
                            } else {
                                val text = InnowatechRepository.buildEnthaertertauschText(draft)
                                val subj = "Aufnahme Enthärtertausch - ${draft.kunde.ifBlank { draft.ort.ifBlank { "Kunde" } }}"
                                PlatformActions.openEmailIntent(
                                    context = context,
                                    toList = selectedRecipients.toList(),
                                    subject = subj,
                                    body = text
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("et_email_button")
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mail öffnen")
                    }
                    OutlinedButton(
                        onClick = {
                            val text = InnowatechRepository.buildEnthaertertauschText(draft)
                            if (PlatformActions.copyToClipboard(context, "Checkliste Enthärtertausch", text)) {
                                viewModel.emitToast("Checkliste als Text kopiert.")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Text kopieren")
                    }
                }
            }
        }

        item {
            IndustrialCard {
                Text("Gespeicherte Aufnahmen (${state.prefs.enthaerterRecords.size})", style = MaterialTheme.typography.titleSmall)
                if (state.prefs.enthaerterRecords.isEmpty()) {
                    Text(
                        "Noch keine Aufnahmen lokal gespeichert.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.prefs.enthaerterRecords.forEach { rec ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.subtleSurface,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.loadEtRecord(rec) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rec.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${InnowatechRepository.formatGermanDateTime(rec.updatedAtIso)} · ${rec.state.typ.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteEtRecord(rec.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Löschen", tint = colors.namurFailure)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceChipGroup(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { opt ->
                val active = selected == opt
                FilterChip(
                    selected = active,
                    onClick = { onSelect(if (active) "" else opt) },
                    leadingIcon = if (active) ({
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }) else null,
                    label = { Text(opt) }
                )
            }
        }
    }
}

@Composable
private fun ChoiceChipGroupWithCustom(
    label: String,
    options: List<String>,
    selected: String,
    customValue: String,
    onSelect: (String) -> Unit,
    onCustomChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ChoiceChipGroup(label = label, options = options, selected = selected, onSelect = onSelect)
        if (selected == "Sonstiges") {
            IndustrialTextField(
                value = customValue,
                onValueChange = onCustomChange,
                label = "$label – Sonstiges angeben"
            )
        }
    }
}
