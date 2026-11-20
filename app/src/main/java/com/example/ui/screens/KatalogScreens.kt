package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.InnowatechRepository
import com.example.model.KofferBox
import com.example.model.KofferItem
import com.example.model.ModuleId
import com.example.ui.ServiceToolUiState
import com.example.ui.ServiceToolViewModel
import com.example.ui.components.IndustrialCard
import com.example.ui.components.IndustrialDropdownField
import com.example.ui.components.InteractiveKofferPhotoBox
import com.example.ui.components.ModuleBackBar
import com.example.ui.components.SectionTitle
import com.example.ui.components.categoryAccentColor
import com.example.ui.theme.InnotechTheme
import com.example.util.PlatformActions
import java.util.Locale

@Composable
fun KatalogModuleHost(
    module: ModuleId,
    state: ServiceToolUiState,
    viewModel: ServiceToolViewModel,
    onBack: () -> Unit
) {
    val accent = categoryAccentColor(module.defaultCategory)
    val resetLabel = if (module == ModuleId.ENTHAERTERTAUSCH) "Neu" else null
    Column(modifier = Modifier.fillMaxSize()) {
        ModuleBackBar(
            module = module,
            resetLabel = resetLabel,
            onBack = onBack,
            onReset = if (resetLabel != null) viewModel::resetActiveModule else null
        )
        when (module) {
            ModuleId.KOFFER -> ServicekofferBody(
                state = state,
                viewModel = viewModel,
                forPicker = false,
                accentColor = accent
            )
            ModuleId.ENTHAERTERTAUSCH -> EnthaertertauschBody(
                state = state,
                viewModel = viewModel,
                accentColor = accent
            )
            else -> Unit
        }
    }
}

private data class KofferMatch(
    val box: KofferBox,
    val item: KofferItem
)

@Composable
fun ServicekofferBody(
    state: ServiceToolUiState,
    viewModel: ServiceToolViewModel,
    forPicker: Boolean,
    accentColor: androidx.compose.ui.graphics.Color
) {
    val context = LocalContext.current
    val colors = InnotechTheme.colors
    val boxes = state.databases.koffer
    val selectedBoxId = if (forPicker) state.kofferPickerBoxId else state.kofferSelectedBoxId
    val searchQuery = if (forPicker) state.kofferPickerSearchQuery else state.kofferSearchQuery
    val activePin = if (forPicker) state.kofferPickerActivePinNr else state.kofferActivePinNr

    val selectedBox = boxes.find { it.id == selectedBoxId }
    val boxOptions = listOf("alle" to "Alle 6 L-Boxen (${boxes.sumOf { it.items.size }} Artikel)") +
        boxes.map { it.id to "${it.name} (${it.items.size} Pos.)" }

    val matches = remember(boxes, selectedBoxId, searchQuery, activePin) {
        val q = searchQuery.trim().lowercase(Locale.GERMAN)
        val sourceBoxes = if (selectedBoxId == "alle") boxes else boxes.filter { it.id == selectedBoxId }
        buildList {
            sourceBoxes.forEach { box ->
                box.items.forEach { item ->
                    val normPin = InnowatechRepository.normalizePinNr(box.id, item.nr)
                    val pinOk = activePin.isBlank() || normPin == activePin
                    val qOk = q.isEmpty() ||
                        item.beschreibung.lowercase(Locale.GERMAN).contains(q) ||
                        item.inNr.lowercase(Locale.GERMAN).contains(q) ||
                        item.hstNr.lowercase(Locale.GERMAN).contains(q) ||
                        item.hersteller.lowercase(Locale.GERMAN).contains(q) ||
                        item.nr.lowercase(Locale.GERMAN).contains(q)
                    if (pinOk && qOk) add(KofferMatch(box, item))
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!forPicker) {
            item {
                SectionTitle(title = "Servicekoffer (6 L-Boxen)", accentColor = accentColor)
            }
        }

        item {
            IndustrialCard {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateKofferSearch(it, forPicker) },
                    placeholder = { Text("IN-Nr., Artikel, Hersteller oder Fach-Nr.…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateKofferSearch("", forPicker) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Leeren")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(if (forPicker) "koffer_picker_search" else "koffer_search_input")
                )

                IndustrialDropdownField(
                    label = "L-Box auswählen",
                    selectedLabel = selectedBox?.name ?: "Alle 6 L-Boxen",
                    options = boxOptions,
                    onSelect = { id -> viewModel.selectKofferBox(id, forPicker) },
                    testTag = if (forPicker) "koffer_picker_dropdown" else "koffer_box_dropdown"
                )

                if (selectedBox == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        boxes.forEach { box ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.subtleSurface,
                                border = BorderStroke(1.dp, colors.cardBorder),
                                modifier = Modifier
                                    .width(126.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.selectKofferBox(box.id, forPicker) }
                            ) {
                                Column {
                                    Image(
                                        painter = painterResource(id = box.imageRes),
                                        contentDescription = box.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(4f / 3f)
                                    )
                                    Text(
                                        text = box.shortName,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val pins = InnowatechRepository.kofferPins[selectedBox.id].orEmpty()
                    InteractiveKofferPhotoBox(
                        imageRes = selectedBox.imageRes,
                        contentDescription = selectedBox.name,
                        pins = pins,
                        activePinNr = activePin,
                        onSelectPin = { pin -> viewModel.selectKofferPin(selectedBox.id, pin, forPicker) }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activePin.isNotBlank()) {
                            AssistChip(
                                onClick = { viewModel.clearKofferPin(forPicker) },
                                label = { Text("Fach $activePin (Filter löschen ✕)") }
                            )
                        } else {
                            Text(
                                text = "Nummer im Foto antippen für Fachfilter",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.openKofferLightbox(selectedBox.id, activePin, forPicker) }
                        ) {
                            Icon(Icons.Filled.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Zoom")
                        }
                    }
                }
            }
        }

        items(matches, key = { "${it.box.id}_${it.item.nr}_${it.item.inNr}_${it.item.beschreibung}" }) { entry ->
            val normPin = InnowatechRepository.normalizePinNr(entry.box.id, entry.item.nr)
            val isPinHighlighted = activePin.isNotBlank() && normPin == activePin
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (isPinHighlighted) colors.signalYellowSoft else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isPinHighlighted) 2.dp else 1.dp,
                    color = if (isPinHighlighted) colors.signalYellow else colors.cardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {
                                viewModel.selectKofferPin(entry.box.id, entry.item.nr, forPicker)
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            label = { Text("${entry.box.shortName} · Fach ${entry.item.nr}") }
                        )
                        Text(
                            text = "Soll: ${entry.item.soll}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = entry.item.beschreibung,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "IN-${entry.item.inNr} · ${entry.item.hersteller} (${entry.item.hstNr})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (PlatformActions.copyToClipboard(context, "IN-Nr", entry.item.inNr)) {
                                    viewModel.emitToast("IN-${entry.item.inNr} kopiert.")
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("IN", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { viewModel.insertKofferVerbautToNotes(entry.item, closePicker = forPicker) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Text("Verbaut / Notiz", maxLines = 1)
                        }
                        Button(
                            onClick = { viewModel.requestKofferNachorderQty(entry.box.id, entry.item, forPicker) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.signalYellow,
                                contentColor = colors.onSignalYellow
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Text("Nachorder", maxLines = 1)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun KofferPickerModal(
    state: ServiceToolUiState,
    viewModel: ServiceToolViewModel
) {
    Dialog(
        onDismissRequest = viewModel::closeKofferPicker,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(InnotechTheme.colors.headerBackground)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Koffer-Teil für Service-Notiz wählen",
                        style = MaterialTheme.typography.titleMedium,
                        color = InnotechTheme.colors.headerContent,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = viewModel::closeKofferPicker) {
                        Text("Schließen", color = InnotechTheme.colors.signalYellow)
                    }
                }
                ServicekofferBody(
                    state = state,
                    viewModel = viewModel,
                    forPicker = true,
                    accentColor = categoryAccentColor(ModuleId.KOFFER.defaultCategory)
                )
            }
        }
    }
}
