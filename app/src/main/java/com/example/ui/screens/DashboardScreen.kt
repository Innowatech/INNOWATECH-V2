package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.DashboardDensity
import com.example.model.ModuleCategory
import com.example.model.ModuleConfig
import com.example.model.ModuleId
import com.example.ui.ServiceToolUiState
import com.example.ui.components.HintSeverity
import com.example.ui.components.IndustrialHintBanner
import com.example.ui.components.SectionTitle
import com.example.ui.components.VegaTileCard
import com.example.ui.components.categoryAccentColor
import java.util.Locale

@Composable
fun DashboardScreen(
    state: ServiceToolUiState,
    onSearchChange: (String) -> Unit,
    onOpenModule: (ModuleId) -> Unit,
    modifier: Modifier = Modifier
) {
    val q = state.dashboardSearchQuery.trim().lowercase(Locale.GERMAN)
    val allowedVisible = remember(state.modules, q) {
        state.modules.filter { cfg ->
            cfg.visible && cfg.userAllowed && (
                q.isEmpty() ||
                    cfg.module.title.lowercase(Locale.GERMAN).contains(q) ||
                    cfg.module.shortSubtitle.lowercase(Locale.GERMAN).contains(q) ||
                    cfg.module.defaultCategory.title.lowercase(Locale.GERMAN).contains(q)
                )
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 840.dp -> if (state.prefs.dashboardDensity == DashboardDensity.COMPACT) 4 else 3
            maxWidth >= 600.dp -> 3
            else -> state.prefs.dashboardDensity.columns
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.dashboardSearchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Modul oder Rechner suchen (z. B. Radar, Koffer, Chlor)…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.dashboardSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Suche leeren")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_search_input")
                )
            }

            if (allowedVisible.isEmpty()) {
                item {
                    IndustrialHintBanner(
                        text = "Keine passenden Module gefunden.",
                        severity = HintSeverity.INFO
                    )
                }
            } else {
                state.orderedCategories.forEach { category ->
                    val catModules = allowedVisible.filter { it.module.defaultCategory == category }
                    if (catModules.isNotEmpty()) {
                        item(key = "cat_${category.key}") {
                            CategoryGridBlock(
                                category = category,
                                modules = catModules,
                                columns = columns,
                                density = state.prefs.dashboardDensity,
                                showIcons = state.prefs.showDashboardIcons,
                                activeModule = state.activeModule,
                                onOpenModule = onOpenModule
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun CategoryModuleListScreen(
    categories: List<ModuleCategory>,
    state: ServiceToolUiState,
    onOpenModule: (ModuleId) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleInCategories = remember(state.modules, categories) {
        state.modules.filter { it.visible && it.userAllowed && categories.contains(it.module.defaultCategory) }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 840.dp -> 3
            maxWidth >= 600.dp -> 2
            else -> state.prefs.dashboardDensity.columns
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (visibleInCategories.isEmpty()) {
                item {
                    IndustrialHintBanner(
                        text = "In dieser Kategorie sind aktuell keine Module sichtbar oder für Ihr Kürzel freigeschaltet.",
                        severity = HintSeverity.WARNING
                    )
                }
            } else {
                state.orderedCategories.filter { categories.contains(it) }.forEach { category ->
                    val catModules = visibleInCategories.filter { it.module.defaultCategory == category }
                    if (catModules.isNotEmpty()) {
                        item(key = "tab_cat_${category.key}") {
                            CategoryGridBlock(
                                category = category,
                                modules = catModules,
                                columns = columns,
                                density = DashboardDensity.LARGE,
                                showIcons = state.prefs.showDashboardIcons,
                                activeModule = state.activeModule,
                                onOpenModule = onOpenModule
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun CategoryGridBlock(
    category: ModuleCategory,
    modules: List<ModuleConfig>,
    columns: Int,
    density: DashboardDensity,
    showIcons: Boolean,
    activeModule: ModuleId?,
    onOpenModule: (ModuleId) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(
            title = category.title,
            accentColor = categoryAccentColor(category)
        )
        val rows = modules.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { cfg ->
                        VegaTileCard(
                            module = cfg.module,
                            density = density,
                            showIcons = showIcons,
                            isActive = activeModule == cfg.module,
                            onClick = { onOpenModule(cfg.module) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    val missing = columns - rowItems.size
                    repeat(missing) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
