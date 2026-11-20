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
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.example.ui.components.KeyValueRow
import com.example.ui.components.ModuleBackBar
import com.example.ui.components.SectionTitle
import com.example.ui.components.categoryAccentColor
import com.example.ui.theme.InnotechTheme
import com.example.util.PlatformActions

@Composable
fun ParameterModuleHost(
    module: ModuleId,
    state: ServiceToolUiState,
    viewModel: ServiceToolViewModel,
    onBack: () -> Unit
) {
    val accent = categoryAccentColor(module.defaultCategory)
    Column(modifier = Modifier.fillMaxSize()) {
        ModuleBackBar(
            module = module,
            resetLabel = "Reset",
            onBack = onBack,
            onReset = viewModel::resetActiveModule
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SectionTitle(title = module.title, accentColor = accent) }
            item {
                when (module) {
                    ModuleId.SONDEN -> SondendatenContent(state, viewModel)
                    ModuleId.ANLAGEN -> AquadronAnlagenContent(state, viewModel)
                    ModuleId.RADAR -> VegaRadarContent(state, viewModel)
                    ModuleId.ENTHAERTER -> EnthaerterContent(state, viewModel)
                    else -> Unit
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SondendatenContent(state: ServiceToolUiState, viewModel: ServiceToolViewModel) {
    val sonden = state.databases.sonden
    val selected = sonden.find { it.id == state.calcInputs.selectedSondeId }
    val options = listOf("" to "Bitte Sonde wählen…") + sonden.map { it.id to it.label }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustrialCard {
            IndustrialDropdownField(
                label = "Sonde (Datenbank · ${sonden.size} Einträge)",
                selectedLabel = selected?.label ?: "Bitte Sonde wählen…",
                options = options,
                onSelect = { id -> viewModel.updateCalcInputs { it.copy(selectedSondeId = id) } },
                testTag = "sonden_dropdown"
            )
            if (selected != null) {
                IndustrialResultBox(
                    label = "RV1 Messbereich",
                    value = selected.messRv1,
                    highlight = true
                )
                IndustrialResultBox(
                    label = "RV1 Steilheit",
                    value = selected.steilRv1
                )
            } else {
                IndustrialHintBanner(
                    text = "Bitte eine Sonde aus der Datenbank auswählen, um RV1 Messbereich und Steilheit anzuzeigen."
                )
            }
        }
    }
}

@Composable
private fun AquadronAnlagenContent(state: ServiceToolUiState, viewModel: ServiceToolViewModel) {
    val anlagen = state.databases.anlagen
    var filterQuery by remember { mutableStateOf("") }
    val filtered = remember(anlagen, filterQuery) {
        val q = filterQuery.trim().lowercase()
        if (q.isEmpty()) anlagen else anlagen.filter { it.label.lowercase().contains(q) }
    }
    val selected = anlagen.find { it.id == state.calcInputs.selectedAnlageId }
    val options = listOf("" to "Bitte Aquadron-Anlage wählen…") + filtered.map { it.id to it.label }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustrialCard {
            com.example.ui.components.IndustrialTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                label = "Anlagen-Filter (${filtered.size} / ${anlagen.size})",
                placeholder = "z. B. FX, FXM, 1000, ECO…"
            )
            IndustrialDropdownField(
                label = "Aquadron-Anlage auswählen",
                selectedLabel = selected?.label ?: "Bitte Anlage wählen…",
                options = options,
                onSelect = { id -> viewModel.updateCalcInputs { it.copy(selectedAnlageId = id) } },
                testTag = "anlagen_dropdown"
            )
            if (selected != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyValueRow("Durchfluss (l/h)", selected.durchfluss)
                    KeyValueRow("Elektrolyse-Strom (A)", selected.strom)
                    KeyValueRow("Zellspannung (V)", selected.spannung)
                    KeyValueRow("Vorlauf-/Nachlaufzeit", selected.zeit)
                    KeyValueRow("Produktionszyklus", selected.prod)
                    KeyValueRow("Pausenzeit", selected.pause)
                }
            } else {
                IndustrialHintBanner(
                    text = "Bitte eine der ${anlagen.size} Aquadron-Anlagen auswählen, um alle Produktionsparameter anzuzeigen."
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VegaRadarContent(state: ServiceToolUiState, viewModel: ServiceToolViewModel) {
    val colors = InnotechTheme.colors
    val specs = InnowatechRepository.radarTankSpecs
    val selectedVol = state.calcInputs.selectedRadarVol
    val selectedSpec = specs.find { it.volumeLiters == selectedVol }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustrialCard {
            Text("Tankvolumen-Vorgabe (VEGAPULS 11)", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                specs.forEach { spec ->
                    FilterChip(
                        selected = spec.volumeLiters == selectedVol,
                        onClick = { viewModel.selectRadarTank(spec.volumeLiters) },
                        label = { Text(spec.label) },
                        modifier = Modifier.testTag("radar_tank_${spec.volumeLiters}")
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndustrialNumberField(
                    value = state.calcInputs.radarMinMm,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(radarMinMm = v) } },
                    label = "Min.-Abgleich (100 %)",
                    unit = "mm",
                    modifier = Modifier.weight(1f)
                )
                IndustrialNumberField(
                    value = state.calcInputs.radarMaxMm,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(radarMaxMm = v) } },
                    label = "Max.-Abgleich (0 %)",
                    unit = "mm",
                    modifier = Modifier.weight(1f)
                )
            }
            IndustrialResultBox(
                label = "Tankdurchmesser (Ø)",
                value = selectedSpec?.let { "${it.diameterMm} mm" } ?: "-",
                highlight = selectedSpec != null
            )
        }

        SectionTitle("Inbetriebnahme-Schritte (VEGA Tools App)", accentColor = colors.namurCheck)

        InnowatechRepository.vegaSteps.forEachIndexed { idx, step ->
            val isStep4 = step.num == "4"
            IndustrialCard(
                borderColor = if (isStep4 && selectedSpec != null) colors.namurCheck else colors.cardBorder
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${step.num}. ${step.title}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                step.summaryBullets.forEach { bullet ->
                    val matchesTank = isStep4 && selectedSpec != null && bullet.startsWith("${selectedSpec.volumeLiters} l ")
                    if (matchesTank) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.namurCheckContainer,
                            border = BorderStroke(1.5.dp, colors.namurCheck),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "• $bullet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.namurCheck,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "• $bullet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                step.note?.let { note ->
                    IndustrialHintBanner(text = note, severity = HintSeverity.INFO)
                }

                step.imageRes?.let { resId ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.openVegaLightbox(idx) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                                .background(colors.subtleSurface)
                        ) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = "${step.num}. ${step.title}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.headerBackground.copy(alpha = 0.88f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ZoomIn,
                                        contentDescription = "Vergrößern",
                                        tint = colors.headerAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Vergrößern",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.headerContent
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

@Composable
private fun EnthaerterContent(state: ServiceToolUiState, viewModel: ServiceToolViewModel) {
    val context = LocalContext.current
    val list = state.databases.enthaerter
    val selected = list.find { it.id == state.calcInputs.selectedEnthaerterId }
    val options = listOf("" to "Bitte Enthärter-Modell wählen…") + list.map { it.id to it.label }
    val haerte = state.calcInputs.enthaerterHaerte.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
    val calc = InnowatechCalculations.calcEnthaerter80(selected, haerte)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustrialCard {
            IndustrialDropdownField(
                label = "Enthärtungsanlage (${list.size} Modelle)",
                selectedLabel = selected?.label ?: "Bitte Enthärter-Modell wählen…",
                options = options,
                onSelect = { id -> viewModel.updateCalcInputs { it.copy(selectedEnthaerterId = id) } },
                testTag = "enthaerter_dropdown"
            )
            if (selected != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyValueRow("Harzmenge", selected.harz)
                    KeyValueRow("Kapazität (100 %)", selected.kapazitaet)
                    KeyValueRow("Sole / Salz", "${selected.sole} · ${selected.salz}")
                    KeyValueRow("1. Rückfüllen", selected.rueckfuelle)
                    KeyValueRow("2. Rückspülen", selected.rueckspuelen)
                    KeyValueRow("3. Besalzen", selected.besalzen)
                    KeyValueRow("4. Schnellspülen", selected.schnellspuelen)
                    KeyValueRow("5. Sole nachfüllen", selected.soleNachf)
                }
            }
        }

        IndustrialCard {
            Text("Quellharz-Sensor (80-%-Kapazität)", style = MaterialTheme.typography.titleSmall)
            IndustrialNumberField(
                value = state.calcInputs.enthaerterHaerte,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(enthaerterHaerte = v) } },
                label = "Rohwasserhärte",
                unit = "°dH",
                testTag = "enthaerter_haerte_input"
            )
            IndustrialResultBox(label = "80-%-Nutzkapazität", value = calc.kap80Text)
            IndustrialResultBox(
                label = "Enthärtete Wassermenge bis Regeneration",
                value = calc.literText,
                highlight = calc.literText != "-"
            )
            IndustrialHintBanner(text = calc.hint, severity = HintSeverity.INFO)

            if (selected != null) {
                OutlinedButton(
                    onClick = {
                        PlatformActions.printTextReport(
                            context = context,
                            documentTitle = "Enthärter ${selected.label}",
                            heading = "Enthärtungsanlage: ${selected.label}",
                            subtitle = "INNOWATΞCH Service Tool",
                            rows = listOf(
                                "Harzmenge" to selected.harz,
                                "Kapazität" to selected.kapazitaet,
                                "Sole / Salz" to "${selected.sole} / ${selected.salz}",
                                "Rückfüllen / Rückspülen" to "${selected.rueckfuelle} / ${selected.rueckspuelen}",
                                "Besalzen / Schnellspülen" to "${selected.besalzen} / ${selected.schnellspuelen}",
                                "Sole nachfüllen" to selected.soleNachf,
                                "Rohwasserhärte" to if (haerte > 0.0) "$haerte °dH" else "-",
                                "80-%-Nutzkapazität" to calc.kap80Text,
                                "Weichwasser bis Reg." to calc.literText
                            ),
                            bodyText = calc.hint
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Datenblatt als PDF drucken")
                }
            }
        }
    }
}
