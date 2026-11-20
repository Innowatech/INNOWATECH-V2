package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.InnowatechCalculations
import com.example.data.InnowatechRepository
import com.example.model.CalculatorInputs
import com.example.model.ModuleId
import com.example.ui.ServiceToolViewModel
import com.example.ui.components.HintSeverity
import com.example.ui.components.IndustrialCard
import com.example.ui.components.IndustrialDropdownField
import com.example.ui.components.IndustrialHintBanner
import com.example.ui.components.IndustrialNumberField
import com.example.ui.components.IndustrialResultBox
import com.example.ui.components.ModuleBackBar
import com.example.ui.components.SectionTitle
import com.example.ui.components.SegmentedOptionRow
import com.example.ui.components.categoryAccentColor
import com.example.ui.theme.InnotechTheme
import com.example.util.PlatformActions
import java.util.Locale

private fun String.toDoubleComma(): Double =
    this.replace(',', '.').trim().toDoubleOrNull() ?: 0.0

private fun String.toNullableDoubleComma(): Double? {
    val t = this.replace(',', '.').trim()
    return if (t.isEmpty()) null else t.toDoubleOrNull()
}

@Composable
fun AnwendungModuleHost(
    module: ModuleId,
    inputs: CalculatorInputs,
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
            item {
                SectionTitle(title = module.title, accentColor = accent)
            }
            item {
                when (module) {
                    ModuleId.STANDARD -> TrinkwasserContent(inputs, viewModel)
                    ModuleId.WASSERWERK -> WasserwerkContent(inputs, viewModel)
                    ModuleId.SCHWIMMBAD -> SchwimmbadContent(inputs, viewModel)
                    ModuleId.PID -> PidReglerContent(inputs, viewModel)
                    ModuleId.VERDUENNUNG -> VerduennungContent(inputs, viewModel)
                    ModuleId.DOSIERAUSWERTUNG -> DosierauswertungContent(inputs, viewModel)
                    else -> Unit
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TrinkwasserContent(inputs: CalculatorInputs, viewModel: ServiceToolViewModel) {
    val res = InnowatechCalculations.calcStandard(
        ziel = inputs.stdZiel.toDoubleComma(),
        volL = inputs.stdVol.toDoubleComma(),
        anolMgL = inputs.stdAnol.toDoubleComma(),
        zehrung = inputs.stdZehrung.toDoubleComma()
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustrialCard {
            IndustrialNumberField(
                value = inputs.stdZiel,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(stdZiel = v) } },
                label = "Zielkonzentration",
                unit = "mg/l",
                placeholder = "z. B. 0,4",
                testTag = "std_ziel_input"
            )
            IndustrialNumberField(
                value = inputs.stdVol,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(stdVol = v) } },
                label = "Tankvolumen",
                unit = "l",
                placeholder = "z. B. 1000",
                testTag = "std_vol_input"
            )
            IndustrialNumberField(
                value = inputs.stdAnol,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(stdAnol = v) } },
                label = "Anolytkonzentration",
                unit = "mg/l",
                placeholder = "z. B. 500",
                testTag = "std_anol_input"
            )
            IndustrialNumberField(
                value = inputs.stdZehrung,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(stdZehrung = v) } },
                label = "Zehrung",
                unit = "mg/l",
                placeholder = "z. B. 0,1",
                testTag = "std_zehrung_input"
            )
        }

        IndustrialCard {
            IndustrialResultBox(
                label = "Effektive Sollkonzentration (Ziel + Zehrung)",
                value = String.format(Locale.US, "%.2f mg/l", res.sollMgL)
            )
            IndustrialResultBox(
                label = "Erforderliche Dosiermenge Anolyte",
                value = String.format(Locale.US, "%.0f ml (%.2f l)", res.mengeMl, res.mengeMl / 1000.0),
                highlight = true
            )
        }
    }
}

@Composable
private fun WasserwerkContent(inputs: CalculatorInputs, viewModel: ServiceToolViewModel) {
    val res = InnowatechCalculations.calcWasserwerk(
        m3Tag = inputs.wwM3Tag.toDoubleComma(),
        anolKonz = inputs.wwAnolKonz.toDoubleComma(),
        ziel = inputs.wwZiel.toDoubleComma(),
        zehrung = inputs.wwZehrung.toDoubleComma(),
        impulsL = inputs.wwImpuls.toDoubleComma(),
        maxFlowM3h = inputs.wwMaxFlow.toDoubleComma()
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustrialCard {
            Text("Förderleistung & Durchfluss", style = MaterialTheme.typography.titleSmall)
            IndustrialNumberField(
                value = inputs.wwM3Tag,
                onValueChange = viewModel::updateWasserwerkM3Tag,
                label = "Tagesförderung",
                unit = "m³/Tag",
                testTag = "ww_m3tag_input"
            )
            IndustrialNumberField(
                value = inputs.wwStunden,
                onValueChange = viewModel::updateWasserwerkStunden,
                label = "Laufzeit pro Tag",
                unit = "h",
                testTag = "ww_stunden_input"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndustrialNumberField(
                    value = inputs.wwLsek,
                    onValueChange = viewModel::updateWasserwerkLsek,
                    label = "Durchfluss l/s",
                    unit = "l/s",
                    modifier = Modifier.weight(1f),
                    testTag = "ww_lsek_input"
                )
                IndustrialNumberField(
                    value = inputs.wwM3h,
                    onValueChange = {},
                    label = "Durchfluss m³/h",
                    unit = "m³/h",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        IndustrialCard {
            Text("Dosierparameter & Signal", style = MaterialTheme.typography.titleSmall)
            IndustrialNumberField(
                value = inputs.wwAnolKonz,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(wwAnolKonz = v) } },
                label = "Anolytkonzentration",
                unit = "mg/l"
            )
            IndustrialNumberField(
                value = inputs.wwZiel,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(wwZiel = v) } },
                label = "Zielkonzentration",
                unit = "mg/l"
            )
            IndustrialNumberField(
                value = inputs.wwZehrung,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(wwZehrung = v) } },
                label = "Zehrung",
                unit = "mg/l"
            )
            IndustrialNumberField(
                value = inputs.wwImpuls,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(wwImpuls = v) } },
                label = "Wasserzähler-Impulsabstand",
                unit = "l/Imp"
            )
            IndustrialNumberField(
                value = inputs.wwMaxFlow,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(wwMaxFlow = v) } },
                label = "Max. Förderleistung (für 20 mA)",
                unit = "m³/h"
            )
        }

        IndustrialCard {
            IndustrialResultBox(
                label = "Effektive Sollkonzentration",
                value = String.format(Locale.US, "%.2f mg/l", res.konzMgL)
            )
            IndustrialResultBox(
                label = "Tagesbedarf Anolyte",
                value = String.format(Locale.US, "%.1f l/Tag", res.bedarfLTag),
                highlight = true
            )
            IndustrialResultBox(
                label = "Erforderlicher Pumpenhub pro Impuls",
                value = String.format(Locale.US, "%.2f ml/Imp", res.hubMl)
            )
            IndustrialResultBox(
                label = "Spezifische Dosiermenge & 20-mA-Skalierung",
                value = String.format(Locale.US, "%.2f l/m³ · %.1f l/h bei 20 mA", res.lProM3, res.pumpe20MaLh)
            )
        }
    }
}

@Composable
private fun SchwimmbadContent(inputs: CalculatorInputs, viewModel: ServiceToolViewModel) {
    val res = InnowatechCalculations.calcSchwimmbad(
        volumenL = inputs.poolVolumen.toDoubleComma(),
        chlorsorte = inputs.poolChlorsorte,
        chlorIst = inputs.poolChlorIst.toDoubleComma(),
        chlorSoll = inputs.poolChlorSoll.toDoubleComma(),
        chlorKonz = inputs.poolChlorKonz.toDoubleComma(),
        phIst = inputs.poolPhIst.toDoubleComma(),
        phSoll = inputs.poolPhSoll.toDoubleComma(),
        alk = inputs.poolAlk.toNullableDoubleComma(),
        saeureKonz = inputs.poolSaeureKonz.toNullableDoubleComma()
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustrialCard {
            Text("Beckengröße & Volumen", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IndustrialNumberField(
                    value = inputs.poolLaenge,
                    onValueChange = { viewModel.updatePoolDimensions(laenge = it) },
                    label = "Länge",
                    unit = "m",
                    modifier = Modifier.weight(1f)
                )
                IndustrialNumberField(
                    value = inputs.poolBreite,
                    onValueChange = { viewModel.updatePoolDimensions(breite = it) },
                    label = "Breite",
                    unit = "m",
                    modifier = Modifier.weight(1f)
                )
                IndustrialNumberField(
                    value = inputs.poolTiefe,
                    onValueChange = { viewModel.updatePoolDimensions(tiefe = it) },
                    label = "Tiefe",
                    unit = "m",
                    modifier = Modifier.weight(1f)
                )
            }
            IndustrialNumberField(
                value = inputs.poolVolumen,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(poolVolumen = v) } },
                label = "Beckenvolumen (oder direkt eingeben)",
                unit = "l",
                testTag = "pool_vol_input"
            )
        }

        IndustrialCard {
            Text("Chlor-Korrektur", style = MaterialTheme.typography.titleSmall)
            SegmentedOptionRow(
                options = listOf("anolyte" to "Anolyte (l)", "fest" to "Chlorgranulat (kg)"),
                selected = inputs.poolChlorsorte,
                onSelect = { s -> viewModel.updateCalcInputs { it.copy(poolChlorsorte = s) } }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndustrialNumberField(
                    value = inputs.poolChlorIst,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(poolChlorIst = v) } },
                    label = "Chlor Ist",
                    unit = "mg/l",
                    modifier = Modifier.weight(1f)
                )
                IndustrialNumberField(
                    value = inputs.poolChlorSoll,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(poolChlorSoll = v) } },
                    label = "Chlor Soll",
                    unit = "mg/l",
                    modifier = Modifier.weight(1f)
                )
            }
            IndustrialNumberField(
                value = inputs.poolChlorKonz,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(poolChlorKonz = v) } },
                label = if (inputs.poolChlorsorte == "anolyte") "Anolyte Wirkstoffgehalt (mg/l)" else "Granulat Wirkstoff (% / g/kg)",
                unit = if (inputs.poolChlorsorte == "anolyte") "mg/l" else "g/kg"
            )
        }

        IndustrialCard {
            Text("pH-Korrektur", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndustrialNumberField(
                    value = inputs.poolPhIst,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(poolPhIst = v) } },
                    label = "pH Ist",
                    modifier = Modifier.weight(1f)
                )
                IndustrialNumberField(
                    value = inputs.poolPhSoll,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(poolPhSoll = v) } },
                    label = "pH Soll",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndustrialNumberField(
                    value = inputs.poolAlk,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(poolAlk = v) } },
                    label = "Alkalinität (Std. 100)",
                    unit = "mg/l",
                    modifier = Modifier.weight(1f)
                )
                IndustrialNumberField(
                    value = inputs.poolSaeureKonz,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(poolSaeureKonz = v) } },
                    label = "Säurekonz. (Std. 20)",
                    unit = "%",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        IndustrialCard {
            IndustrialResultBox(label = "Chlorbedarf", value = res.chlorText, highlight = !res.chlorError)
            IndustrialHintBanner(
                text = res.chlorHint,
                severity = if (res.chlorError) HintSeverity.ERROR else HintSeverity.INFO
            )
            IndustrialResultBox(label = "pH-Stellmittelbedarf", value = res.phText)
            IndustrialHintBanner(text = res.phHint, severity = HintSeverity.INFO)
        }
    }
}

@Composable
private fun PidReglerContent(inputs: CalculatorInputs, viewModel: ServiceToolViewModel) {
    val betriebsarten = listOf(
        "kalt_kombiniert" to "Kaltwasser – Kombiniert (Durchfluss + Regelung)",
        "kalt_rein" to "Kaltwasser – Reine Chlor-Regelung",
        "warm_kombiniert" to "Warmwasser / Zirkulation – Kombiniert",
        "warm_rein" to "Warmwasser / Zirkulation – Reine Chlor-Regelung"
    )
    val selectedLabel = betriebsarten.find { it.first == inputs.pidBetriebsart }?.second ?: betriebsarten.first().second
    val isWarm = inputs.pidBetriebsart.contains("warm")
    val isWarmKomb = inputs.pidBetriebsart == "warm_kombiniert"

    val res = InnowatechCalculations.calcPid(
        betriebsart = inputs.pidBetriebsart,
        testL = inputs.pidTestL.toNullableDoubleComma(),
        wirkstoff = inputs.pidWirkstoff.toNullableDoubleComma(),
        chlorDiff = inputs.pidChlorDiff.toNullableDoubleComma(),
        tu = inputs.pidTu.toNullableDoubleComma(),
        tg = inputs.pidTg.toNullableDoubleComma(),
        grundwert = inputs.pidGrundwert.toNullableDoubleComma(),
        sollwert = inputs.pidSollwert.toNullableDoubleComma()
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustrialCard {
            IndustrialDropdownField(
                label = "Betriebsart",
                selectedLabel = selectedLabel,
                options = betriebsarten,
                onSelect = { art -> viewModel.updateCalcInputs { it.copy(pidBetriebsart = art) } }
            )
            IndustrialNumberField(
                value = inputs.pidTestL,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(pidTestL = v) } },
                label = "Test-Dosiermenge (Sprung)",
                unit = "l/h"
            )
            IndustrialNumberField(
                value = inputs.pidWirkstoff,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(pidWirkstoff = v) } },
                label = "Tank-Wirkstoffgehalt",
                unit = "mg/l"
            )
            IndustrialNumberField(
                value = inputs.pidChlorDiff,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(pidChlorDiff = v) } },
                label = "Gemessener Chloranstieg ΔY",
                unit = "mg/l"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndustrialNumberField(
                    value = inputs.pidTu,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(pidTu = v) } },
                    label = "Totzeit Tu",
                    unit = "s",
                    modifier = Modifier.weight(1f)
                )
                IndustrialNumberField(
                    value = inputs.pidTg,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(pidTg = v) } },
                    label = "Anstiegszeit Tg",
                    unit = "s",
                    modifier = Modifier.weight(1f)
                )
            }
            if (isWarm) {
                IndustrialNumberField(
                    value = inputs.pidGrundwert,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(pidGrundwert = v) } },
                    label = "Erwarteter Restchlorgehalt (optional)",
                    unit = "mg/l"
                )
            }
            if (isWarmKomb) {
                IndustrialNumberField(
                    value = inputs.pidSollwert,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(pidSollwert = v) } },
                    label = "Chlor-Sollwert (für Grunddosierung)",
                    unit = "mg/l"
                )
            }
        }

        IndustrialCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndustrialResultBox(label = "P-Anteil (Xp)", value = res.pText, highlight = res.valid, modifier = Modifier.weight(1f))
                IndustrialResultBox(label = "I-Anteil (Tn)", value = res.iText, modifier = Modifier.weight(1f))
            }
            IndustrialResultBox(label = "Totband", value = res.totbandText)
            res.grunddosierungText?.let { gd ->
                IndustrialResultBox(label = "Empfohlene Grunddosierung", value = gd, highlight = true)
            }
            res.durchflussText?.let { df ->
                IndustrialResultBox(label = res.durchflussLabel, value = df)
            }
            IndustrialHintBanner(
                text = res.hinweis,
                severity = if (res.valid) HintSeverity.SUCCESS else HintSeverity.INFO
            )
        }
    }
}

@Composable
private fun VerduennungContent(inputs: CalculatorInputs, viewModel: ServiceToolViewModel) {
    val typen = listOf(
        "250" to "Aquadron 250 (Direktmessung)",
        "500" to "Aquadron 500 (1:1 · Faktor 2)",
        "600" to "Aquadron 600 (1:1 · Faktor 2)",
        "1000" to "Aquadron ≥ 1000 (1:3 · Faktor 4)",
        "custom" to "Benutzerdefiniert (Auto-Empfehlung)"
    )
    val selectedLabel = typen.find { it.first == inputs.verdAnlagentyp }?.second ?: typen.first().second
    val res = InnowatechCalculations.calcVerduennung(
        anlagentyp = inputs.verdAnlagentyp,
        customKonz = inputs.verdCustomKonz.toNullableDoubleComma(),
        customZiel = inputs.verdCustomZiel.toNullableDoubleComma(),
        messwert = inputs.verdMesswert.toNullableDoubleComma()
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustrialCard {
            IndustrialDropdownField(
                label = "Anlagentyp / Modus",
                selectedLabel = selectedLabel,
                options = typen,
                onSelect = { t -> viewModel.updateCalcInputs { it.copy(verdAnlagentyp = t) } }
            )
            if (inputs.verdAnlagentyp == "custom") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IndustrialNumberField(
                        value = inputs.verdCustomKonz,
                        onValueChange = { v -> viewModel.updateCalcInputs { it.copy(verdCustomKonz = v) } },
                        label = "Erwartete Max-Konz.",
                        unit = "mg/l",
                        modifier = Modifier.weight(1f)
                    )
                    IndustrialNumberField(
                        value = inputs.verdCustomZiel,
                        onValueChange = { v -> viewModel.updateCalcInputs { it.copy(verdCustomZiel = v) } },
                        label = "Hanna-Zielwert (<500)",
                        unit = "mg/l",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            IndustrialNumberField(
                value = inputs.verdMesswert,
                onValueChange = { v -> viewModel.updateCalcInputs { it.copy(verdMesswert = v) } },
                label = "Hanna Photometer Messwert (10 ml Küvette)",
                unit = "mg/l",
                testTag = "verd_messwert_input"
            )
        }

        IndustrialCard {
            IndustrialResultBox(label = "Verdünnungsstufe & Faktor", value = "${res.stufeText} · ${res.faktorText}")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndustrialResultBox(label = "Anolyte in Küvette", value = res.analytText, modifier = Modifier.weight(1f))
                IndustrialResultBox(label = "Dest. Wasser", value = res.wasserText, modifier = Modifier.weight(1f))
            }
            IndustrialResultBox(label = "Tatsächliche Anolytkonzentration", value = res.finalText, highlight = !res.isError)
            IndustrialHintBanner(
                text = res.hinweis,
                severity = when {
                    res.isError -> HintSeverity.ERROR
                    res.isWarning -> HintSeverity.WARNING
                    else -> HintSeverity.INFO
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DosierauswertungContent(inputs: CalculatorInputs, viewModel: ServiceToolViewModel) {
    val context = LocalContext.current
    val colors = InnotechTheme.colors
    var datePickerTarget by remember { mutableStateOf<String?>(null) }

    val res = InnowatechCalculations.calcDosierauswertung(
        wzAlt = inputs.daWzAlt.toNullableDoubleComma(),
        wzNeu = inputs.daWzNeu.toNullableDoubleComma(),
        dosAlt = inputs.daDosAlt.toNullableDoubleComma(),
        dosNeu = inputs.daDosNeu.toNullableDoubleComma(),
        konz = inputs.daKonz.toNullableDoubleComma(),
        soll = inputs.daSoll.toNullableDoubleComma(),
        dateAltIso = inputs.daDateAlt,
        dateNeuIso = inputs.daDateNeu
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndustrialCard {
            Text("Zählerstände & Dosiermengen", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndustrialNumberField(
                    value = inputs.daWzAlt,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(daWzAlt = v) } },
                    label = "Wasserzähler alt",
                    unit = "m³",
                    modifier = Modifier.weight(1f),
                    testTag = "da_wz_alt"
                )
                IndustrialNumberField(
                    value = inputs.daWzNeu,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(daWzNeu = v) } },
                    label = "Wasserzähler neu",
                    unit = "m³",
                    modifier = Modifier.weight(1f),
                    testTag = "da_wz_neu"
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndustrialNumberField(
                    value = inputs.daDosAlt,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(daDosAlt = v) } },
                    label = "Dosiermenge alt",
                    unit = "l",
                    modifier = Modifier.weight(1f),
                    testTag = "da_dos_alt"
                )
                IndustrialNumberField(
                    value = inputs.daDosNeu,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(daDosNeu = v) } },
                    label = "Dosiermenge neu",
                    unit = "l",
                    modifier = Modifier.weight(1f),
                    testTag = "da_dos_neu"
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { datePickerTarget = "alt" },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (inputs.daDateAlt.isBlank()) "Datum alt" else InnowatechRepository.formatIsoToGermanDate(inputs.daDateAlt)
                    )
                }
                OutlinedButton(
                    onClick = { datePickerTarget = "neu" },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (inputs.daDateNeu.isBlank()) "Datum neu" else InnowatechRepository.formatIsoToGermanDate(inputs.daDateNeu)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndustrialNumberField(
                    value = inputs.daKonz,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(daKonz = v) } },
                    label = "Anolyt-Chlorgehalt",
                    unit = "mg/l",
                    modifier = Modifier.weight(1f),
                    testTag = "da_konz"
                )
                IndustrialNumberField(
                    value = inputs.daSoll,
                    onValueChange = { v -> viewModel.updateCalcInputs { it.copy(daSoll = v) } },
                    label = "Chlor-Sollwert",
                    unit = "mg/l",
                    modifier = Modifier.weight(1f),
                    testTag = "da_soll"
                )
            }
        }

        IndustrialCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndustrialResultBox(label = "Wasserverbrauch", value = res.wasserText, modifier = Modifier.weight(1f))
                IndustrialResultBox(label = "Dosiertes Anolyte", value = res.anolytText, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IndustrialResultBox(label = "Reale Dosierquote", value = res.quoteText, modifier = Modifier.weight(1f))
                IndustrialResultBox(
                    label = "Real eindos. Aktivchlor",
                    value = res.chlorText,
                    highlight = res.status == InnowatechCalculations.DosierStatus.OK,
                    modifier = Modifier.weight(1f)
                )
            }
            IndustrialResultBox(label = "Verhältnis zum Sollwert", value = res.faktorText)
            if (res.tageText != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IndustrialResultBox(label = "Zeitraum", value = res.tageText, modifier = Modifier.weight(1f))
                    IndustrialResultBox(label = "Wasser / Tag", value = res.wasserTagText ?: "-", modifier = Modifier.weight(1f))
                }
                IndustrialResultBox(label = "Dosiermenge / Tag", value = res.dosierTagText ?: "-")
            }
            IndustrialHintBanner(
                text = res.hinweis,
                severity = when (res.status) {
                    InnowatechCalculations.DosierStatus.OK -> HintSeverity.SUCCESS
                    InnowatechCalculations.DosierStatus.WARN -> HintSeverity.WARNING
                    InnowatechCalculations.DosierStatus.ERROR -> HintSeverity.ERROR
                    InnowatechCalculations.DosierStatus.NEUTRAL -> HintSeverity.INFO
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.copyDosierauswertungToNotes(res) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.signalYellow,
                        contentColor = colors.onSignalYellow
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("da_to_notes_button")
                ) {
                    Icon(Icons.Filled.EditNote, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("In Notizen")
                }
                OutlinedButton(
                    onClick = {
                        PlatformActions.printTextReport(
                            context = context,
                            documentTitle = "Dosierauswertung INNOWATΞCH",
                            heading = "Dosierauswertung",
                            subtitle = "INNOWATΞCH Service Tool",
                            rows = listOfNotNull(
                                "Wasserzähler (alt / neu)" to "${inputs.daWzAlt.ifBlank { "-" }} / ${inputs.daWzNeu.ifBlank { "-" }} m³",
                                "Dosiermenge (alt / neu)" to "${inputs.daDosAlt.ifBlank { "-" }} / ${inputs.daDosNeu.ifBlank { "-" }} l",
                                "Wasserverbrauch" to res.wasserText,
                                "Dosiertes Anolyte" to res.anolytText,
                                "Reale Dosierquote" to res.quoteText,
                                "Aktivchlor real" to res.chlorText,
                                "Verhältnis zum Sollwert" to res.faktorText,
                                res.tageText?.let { "Zeitraum" to "$it (${res.wasserTagText}, ${res.dosierTagText})" }
                            ),
                            bodyText = res.hinweis
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF drucken")
                }
            }
        }
    }

    datePickerTarget?.let { target ->
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        val iso = InnowatechRepository.formatMillisToIsoDate(millis)
                        viewModel.updateCalcInputs {
                            if (target == "alt") it.copy(daDateAlt = iso) else it.copy(daDateNeu = iso)
                        }
                    }
                    datePickerTarget = null
                }) { Text("Übernehmen") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.updateCalcInputs {
                            if (target == "alt") it.copy(daDateAlt = "") else it.copy(daDateNeu = "")
                        }
                        datePickerTarget = null
                    }) { Text("Löschen") }
                    TextButton(onClick = { datePickerTarget = null }) { Text("Abbrechen") }
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
