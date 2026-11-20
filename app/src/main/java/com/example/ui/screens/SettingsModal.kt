package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.DashboardDensity
import com.example.ui.ServiceToolUiState
import com.example.ui.ServiceToolViewModel
import com.example.ui.components.IndustrialCard
import com.example.ui.components.SegmentedOptionRow
import com.example.ui.components.categoryAccentColor
import com.example.ui.theme.InnotechTheme

@Composable
fun SettingsModalDialog(
    state: ServiceToolUiState,
    viewModel: ServiceToolViewModel
) {
    val colors = InnotechTheme.colors
    Dialog(
        onDismissRequest = viewModel::closeSettings,
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
                        text = "Einstellungen & Struktur",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.headerContent
                    )
                    IconButton(onClick = viewModel::closeSettings) {
                        Icon(Icons.Filled.Close, contentDescription = "Schließen", tint = colors.headerContent)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        IndustrialCard {
                            Text("Erscheinungsbild & Kacheldichte", style = MaterialTheme.typography.titleSmall)
                            SettingSwitchRow(
                                title = "Dunkelmodus (VEGA Anthrazit)",
                                checked = state.prefs.darkMode,
                                onCheckedChange = viewModel::setDarkMode
                            )
                            SettingSwitchRow(
                                title = "Symbole auf Dashboard-Kacheln",
                                checked = state.prefs.showDashboardIcons,
                                onCheckedChange = viewModel::setShowDashboardIcons
                            )
                            Text("Kacheldichte Dashboard", style = MaterialTheme.typography.bodySmall)
                            SegmentedOptionRow(
                                options = listOf(
                                    DashboardDensity.COMPACT to "Kompakt (3er-Raster)",
                                    DashboardDensity.LARGE to "Groß (2er-Raster)"
                                ),
                                selected = state.prefs.dashboardDensity,
                                onSelect = viewModel::setDashboardDensity
                            )
                        }
                    }

                    item {
                        IndustrialCard {
                            Text("Komfort & Timer", style = MaterialTheme.typography.titleSmall)
                            SettingSwitchRow(
                                title = "Display dauerhaft wach halten",
                                checked = state.prefs.keepScreenAwake,
                                onCheckedChange = viewModel::setKeepScreenAwake
                            )
                            SettingSwitchRow(
                                title = "Signalton beim Ausliter-Timer",
                                checked = state.prefs.timerBeepEnabled,
                                onCheckedChange = viewModel::setTimerBeep
                            )
                            SettingSwitchRow(
                                title = "Rechner-Eingaben nach Neustart merken",
                                checked = state.prefs.persistCalcInputs,
                                onCheckedChange = viewModel::setPersistCalcInputs
                            )
                        }
                    }

                    item {
                        IndustrialCard {
                            Text("Reihenfolge der Kategorien", style = MaterialTheme.typography.titleSmall)
                            state.orderedCategories.forEachIndexed { idx, cat ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.subtleSurface,
                                    border = BorderStroke(1.dp, categoryAccentColor(cat).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cat.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.moveCategory(cat, -1) },
                                            enabled = idx > 0
                                        ) {
                                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Nach oben")
                                        }
                                        IconButton(
                                            onClick = { viewModel.moveCategory(cat, 1) },
                                            enabled = idx < state.orderedCategories.lastIndex
                                        ) {
                                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Nach unten")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        IndustrialCard {
                            Text("Module ein-/ausblenden & sortieren", style = MaterialTheme.typography.titleSmall)
                            state.orderedCategories.forEach { cat ->
                                val modsInCat = state.modules.filter { it.module.defaultCategory == cat && it.userAllowed }
                                if (modsInCat.isNotEmpty()) {
                                    Text(
                                        text = cat.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = categoryAccentColor(cat),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    modsInCat.forEachIndexed { idx, cfg ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = cfg.visible,
                                                onCheckedChange = { viewModel.toggleModuleVisibility(cfg.module) }
                                            )
                                            Text(
                                                text = cfg.module.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { viewModel.moveModuleInCategory(cfg.module, -1) },
                                                enabled = idx > 0
                                            ) {
                                                Icon(Icons.Filled.ArrowUpward, contentDescription = "Hoch")
                                            }
                                            IconButton(
                                                onClick = { viewModel.moveModuleInCategory(cfg.module, 1) },
                                                enabled = idx < modsInCat.lastIndex
                                            ) {
                                                Icon(Icons.Filled.ArrowDownward, contentDescription = "Runter")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        IndustrialCard {
                            Text("Konto, Changelog & Administration", style = MaterialTheme.typography.titleSmall)
                            OutlinedButton(
                                onClick = {
                                    viewModel.closeSettings()
                                    viewModel.openLoginDialog()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    state.prefs.activeUserKuerzel?.let { "Techniker wechseln ($it)" }
                                        ?: "Techniker-Kürzel anmelden"
                                )
                            }
                            OutlinedButton(
                                onClick = viewModel::openChangelog,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.History, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Changelog (v4.6 – v5.6) anzeigen")
                            }
                            Button(
                                onClick = {
                                    viewModel.closeSettings()
                                    viewModel.openAdminAuthDialog()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.signalYellow,
                                    contentColor = colors.onSignalYellow
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_admin_console_button")
                            ) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Admin-Konsole öffnen")
                            }
                        }
                    }

                    item {
                        TextButton(
                            onClick = viewModel::closeSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Einstellungen schließen")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
