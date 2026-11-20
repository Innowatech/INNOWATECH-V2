package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.InnowatechRepository
import com.example.model.AnlageInfo
import com.example.model.ModuleId
import com.example.model.SondeInfo
import com.example.ui.ServiceToolUiState
import com.example.ui.ServiceToolViewModel
import com.example.ui.components.IndustrialCard
import com.example.ui.components.IndustrialTextField
import com.example.ui.components.SegmentedOptionRow
import com.example.ui.theme.InnotechTheme
import com.example.util.PlatformActions

@Composable
fun AdminConsoleModal(
    state: ServiceToolUiState,
    viewModel: ServiceToolViewModel
) {
    val colors = InnotechTheme.colors
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = viewModel::closeAdminConsole,
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
                        .background(colors.headerBackground)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Admin-Konsole",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.headerContent
                    )
                    IconButton(onClick = viewModel::closeAdminConsole) {
                        Icon(Icons.Filled.Close, contentDescription = "Schließen", tint = colors.headerContent)
                    }
                }
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Techniker (${state.users.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Datenbanken") }
                    )
                }

                if (selectedTab == 0) {
                    AdminUsersTab(state, viewModel)
                } else {
                    AdminDatabasesTab(state, viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminUsersTab(state: ServiceToolUiState, viewModel: ServiceToolViewModel) {
    val context = LocalContext.current
    val colors = InnotechTheme.colors
    var kuerzel by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var autoUpdate by remember { mutableStateOf(true) }
    var allowedModules by remember { mutableStateOf(ModuleId.entries.map { it.id }.toSet()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            IndustrialCard {
                Text("Techniker bearbeiten / anlegen", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IndustrialTextField(
                        value = kuerzel,
                        onValueChange = { kuerzel = it.uppercase() },
                        label = "Kürzel",
                        modifier = Modifier.weight(0.4f)
                    )
                    IndustrialTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = "Neue PIN (optional)",
                        modifier = Modifier.weight(0.6f)
                    )
                }
                IndustrialTextField(value = name, onValueChange = { name = it }, label = "Name")
                IndustrialTextField(value = email, onValueChange = { email = it }, label = "E-Mail")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Update", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = autoUpdate, onCheckedChange = { autoUpdate = it })
                }
                Text("Freigeschaltete Module (${allowedModules.size}/${ModuleId.entries.size})", style = MaterialTheme.typography.bodySmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModuleId.entries.forEach { mod ->
                        val active = allowedModules.contains(mod.id)
                        FilterChip(
                            selected = active,
                            onClick = {
                                allowedModules = if (active) allowedModules - mod.id else allowedModules + mod.id
                            },
                            label = { Text(mod.title) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveAdminUser(
                                kuerzelRaw = kuerzel,
                                name = name,
                                email = email,
                                newPin = newPin,
                                autoUpdate = autoUpdate,
                                modules = allowedModules.toList()
                            )
                            newPin = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.signalYellow,
                            contentColor = colors.onSignalYellow
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Speichern")
                    }
                    OutlinedButton(
                        onClick = {
                            PlatformActions.sharePlainText(
                                context,
                                "users.json Export",
                                InnowatechRepository.serializeUsersJson(state.users)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("users.json")
                    }
                }
                OutlinedButton(
                    onClick = viewModel::resetUsersToDefaults,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Benutzer auf Standard zurücksetzen")
                }
            }
        }

        item {
            IndustrialCard {
                Text("Vorhandene Techniker (antippen zum Laden)", style = MaterialTheme.typography.titleSmall)
                state.users.values.sortedBy { it.kuerzel }.forEach { user ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.subtleSurface,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                kuerzel = user.kuerzel
                                name = user.name
                                email = user.email
                                autoUpdate = user.autoUpdate
                                allowedModules = user.modules.toSet()
                                newPin = ""
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${user.kuerzel} · ${user.name}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${user.email.ifBlank { "Keine E-Mail" }} · ${user.modules.size} Module",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (user.kuerzel != "ADMIN") {
                                IconButton(onClick = { viewModel.deleteAdminUser(user.kuerzel) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Löschen", tint = colors.namurFailure)
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
private fun AdminDatabasesTab(state: ServiceToolUiState, viewModel: ServiceToolViewModel) {
    val context = LocalContext.current
    val colors = InnotechTheme.colors
    var subTab by remember { mutableStateOf("sonden") }
    var sondeId by remember { mutableStateOf("") }
    var sondeLabel by remember { mutableStateOf("") }
    var sondeMess by remember { mutableStateOf("") }
    var sondeSteil by remember { mutableStateOf("") }

    var anlId by remember { mutableStateOf("") }
    var anlLabel by remember { mutableStateOf("") }
    var anlFlow by remember { mutableStateOf("") }
    var anlStrom by remember { mutableStateOf("") }
    var anlSpannung by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SegmentedOptionRow(
                options = listOf("sonden" to "Sonden (${state.databases.sonden.size})", "anlagen" to "Anlagen (${state.databases.anlagen.size})"),
                selected = subTab,
                onSelect = { subTab = it }
            )
        }

        if (subTab == "sonden") {
            item {
                IndustrialCard {
                    Text("Sonde bearbeiten / hinzufügen", style = MaterialTheme.typography.titleSmall)
                    IndustrialTextField(value = sondeId, onValueChange = { sondeId = it }, label = "ID")
                    IndustrialTextField(value = sondeLabel, onValueChange = { sondeLabel = it }, label = "Bezeichnung")
                    IndustrialTextField(value = sondeMess, onValueChange = { sondeMess = it }, label = "Messbereich RV1")
                    IndustrialTextField(value = sondeSteil, onValueChange = { sondeSteil = it }, label = "Steilheit RV1")
                    Button(
                        onClick = {
                            if (sondeId.isNotBlank() && sondeLabel.isNotBlank()) {
                                viewModel.saveSondeEntry(SondeInfo(sondeId.trim(), sondeLabel.trim(), sondeMess.trim(), sondeSteil.trim()))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.signalYellow, contentColor = colors.onSignalYellow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sonde speichern")
                    }
                    state.databases.sonden.forEach { s ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.subtleSurface,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sondeId = s.id
                                    sondeLabel = s.label
                                    sondeMess = s.messRv1
                                    sondeSteil = s.steilRv1
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(s.label, style = MaterialTheme.typography.titleSmall)
                                Text("${s.messRv1} · ${s.steilRv1}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                IndustrialCard {
                    Text("Aquadron-Anlage bearbeiten", style = MaterialTheme.typography.titleSmall)
                    IndustrialTextField(value = anlId, onValueChange = { anlId = it }, label = "ID")
                    IndustrialTextField(value = anlLabel, onValueChange = { anlLabel = it }, label = "Anlagentyp")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IndustrialTextField(value = anlFlow, onValueChange = { anlFlow = it }, label = "Durchfluss", modifier = Modifier.weight(1f))
                        IndustrialTextField(value = anlStrom, onValueChange = { anlStrom = it }, label = "Strom", modifier = Modifier.weight(1f))
                    }
                    IndustrialTextField(value = anlSpannung, onValueChange = { anlSpannung = it }, label = "Spannung")
                    Button(
                        onClick = {
                            val existing = state.databases.anlagen.find { it.id == anlId.trim() }
                            if (anlId.isNotBlank() && anlLabel.isNotBlank()) {
                                viewModel.saveAnlageEntry(
                                    AnlageInfo(
                                        id = anlId.trim(),
                                        label = anlLabel.trim(),
                                        durchfluss = anlFlow.trim(),
                                        strom = anlStrom.trim(),
                                        spannung = anlSpannung.trim(),
                                        zeit = existing?.zeit ?: "-",
                                        prod = existing?.prod ?: "-",
                                        pause = existing?.pause ?: "-"
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.signalYellow, contentColor = colors.onSignalYellow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Anlage speichern")
                    }
                    state.databases.anlagen.forEach { a ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.subtleSurface,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    anlId = a.id
                                    anlLabel = a.label
                                    anlFlow = a.durchfluss
                                    anlStrom = a.strom
                                    anlSpannung = a.spannung
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(a.label, style = MaterialTheme.typography.titleSmall)
                                Text("${a.durchfluss} · ${a.strom} · ${a.spannung}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        item {
            IndustrialCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            PlatformActions.sharePlainText(
                                context,
                                "databases.json Export",
                                InnowatechRepository.serializeDatabasesJson(state.databases)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("databases.json")
                    }
                    OutlinedButton(
                        onClick = viewModel::resetDatabasesToDefaults,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Standard-DB")
                    }
                }
            }
        }
    }
}
