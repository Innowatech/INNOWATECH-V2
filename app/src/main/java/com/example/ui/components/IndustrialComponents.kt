package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.InnowatechRepository
import com.example.model.DashboardDensity
import com.example.model.ModuleCategory
import com.example.model.ModuleId
import com.example.ui.theme.IndustrialTextStyles
import com.example.ui.theme.InnotechTheme

fun categoryAccentColor(category: ModuleCategory): Color = when (category) {
    ModuleCategory.ANWENDUNG -> Color(0xFF2563EB)
    ModuleCategory.PARAMETER -> Color(0xFF16A34A)
    ModuleCategory.HILFSMITTEL -> Color(0xFFD97706)
    ModuleCategory.KATALOG_CHECKLISTEN -> Color(0xFFDC2626)
}

fun moduleIcon(moduleId: ModuleId): ImageVector = when (moduleId) {
    ModuleId.STANDARD -> Icons.Filled.WaterDrop
    ModuleId.WASSERWERK -> Icons.Filled.Factory
    ModuleId.SCHWIMMBAD -> Icons.Filled.Pool
    ModuleId.PID -> Icons.Filled.Tune
    ModuleId.VERDUENNUNG -> Icons.Filled.Science
    ModuleId.DOSIERAUSWERTUNG -> Icons.Filled.Analytics
    ModuleId.SONDEN -> Icons.Filled.Sensors
    ModuleId.ANLAGEN -> Icons.Filled.PrecisionManufacturing
    ModuleId.RADAR -> Icons.Filled.Radar
    ModuleId.ENTHAERTER -> Icons.Filled.FilterAlt
    ModuleId.TASCHENLAMPE -> Icons.Filled.FlashlightOn
    ModuleId.SCHLAUCHVOLUMEN -> Icons.Filled.Timeline
    ModuleId.AUSLITERN -> Icons.Filled.Timer
    ModuleId.NOTIZEN -> Icons.Filled.EditNote
    ModuleId.KUNDENNOTIZEN -> Icons.Filled.FolderShared
    ModuleId.KOFFER -> Icons.Filled.BusinessCenter
    ModuleId.ENTHAERTERTAUSCH -> Icons.Filled.Checklist
}

@Composable
fun InnotechTopBar(
    title: String,
    activeUserKuerzel: String?,
    activeModule: ModuleId?,
    isPinned: Boolean,
    darkTheme: Boolean,
    showBack: Boolean,
    onBackClick: () -> Unit,
    onUserClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = InnotechTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.headerBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("topbar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = colors.headerContent
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp, end = 10.dp)
                        .size(width = 6.dp, height = 30.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.signalYellow)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.headerContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = if (!activeUserKuerzel.isNullOrBlank()) {
                    "Service Tool ${InnowatechRepository.APP_VERSION} · $activeUserKuerzel"
                } else {
                    "Service Tool ${InnowatechRepository.APP_VERSION}"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.signalYellow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (!activeUserKuerzel.isNullOrBlank()) {
                    colors.signalYellow
                } else {
                    Color.White.copy(alpha = 0.12f)
                },
                modifier = Modifier
                    .padding(end = 2.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onUserClick)
                    .testTag("topbar_user_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = stringResource(R.string.cd_user_profile),
                        tint = if (!activeUserKuerzel.isNullOrBlank()) colors.onSignalYellow else colors.headerContent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = activeUserKuerzel ?: "Login",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (!activeUserKuerzel.isNullOrBlank()) colors.onSignalYellow else colors.headerContent
                    )
                }
            }

            if (activeModule != null) {
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("topbar_pin_button")
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = stringResource(R.string.cd_pin_tab),
                        tint = if (isPinned) colors.signalYellow else colors.headerContent
                    )
                }
            }

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = stringResource(R.string.cd_toggle_theme),
                    tint = colors.headerContent
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = colors.headerContent
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(colors.signalYellow)
        )
    }
}

@Composable
fun PinChipBar(
    pinnedModules: List<ModuleId>,
    activeModule: ModuleId?,
    onSelectModule: (ModuleId) -> Unit,
    onUnpinModule: (ModuleId) -> Unit
) {
    if (pinnedModules.isEmpty()) return
    val colors = InnotechTheme.colors
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pinnedModules.forEach { mod ->
                    val selected = activeModule == mod
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) colors.signalYellow else colors.subtleSurface,
                        border = BorderStroke(
                            1.dp,
                            if (selected) colors.signalYellow else colors.cardBorder
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectModule(mod) }
                            .testTag("pin_chip_${mod.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = moduleIcon(mod),
                                contentDescription = null,
                                tint = if (selected) colors.onSignalYellow else categoryAccentColor(mod.defaultCategory),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = mod.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) colors.onSignalYellow else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .clickable { onUnpinModule(mod) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "${mod.title} lösen",
                                    tint = if (selected) colors.onSignalYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = colors.cardBorder)
        }
    }
}

@Composable
fun ModuleBackBar(
    module: ModuleId,
    resetLabel: String? = null,
    onBack: () -> Unit,
    onReset: (() -> Unit)? = null
) {
    val colors = InnotechTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("module_back_to_overview")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Zurück zur Übersicht")
        }

        if (resetLabel != null && onReset != null) {
            OutlinedButton(
                onClick = onReset,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, colors.cardBorder),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("module_top_reset_${module.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = resetLabel,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(resetLabel, maxLines = 1)
            }
        }
    }
}

@Composable
fun VegaTileCard(
    module: ModuleId,
    density: DashboardDensity,
    showIcons: Boolean,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = InnotechTheme.colors
    val accent = categoryAccentColor(module.defaultCategory)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("tile_${module.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) colors.signalYellow else colors.cardBorder
        ),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accent)
            )
            if (density == DashboardDensity.COMPACT) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (showIcons) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent.copy(alpha = 0.13f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = moduleIcon(module),
                                contentDescription = module.title,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = module.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 112.dp)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    if (showIcons) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.signalYellow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = moduleIcon(module),
                                contentDescription = module.title,
                                tint = colors.onSignalYellow,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Text(
                        text = module.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = module.shortSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    accentColor: Color = InnotechTheme.colors.signalYellow,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun IndustrialCard(
    modifier: Modifier = Modifier,
    borderColor: Color = InnotechTheme.colors.cardBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun IndustrialNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String? = null,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder) }) else null,
        suffix = unit?.let { u -> { Text(u, style = MaterialTheme.typography.labelMedium) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    )
}

@Composable
fun IndustrialTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder) }) else null,
        singleLine = singleLine,
        minLines = minLines,
        readOnly = readOnly,
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    )
}

@Composable
fun <T> IndustrialDropdownField(
    label: String,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = InnotechTheme.colors
    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = colors.subtleSurface,
            border = BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clip(MaterialTheme.shapes.medium)
                .clickable { expanded = true }
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "$label auswählen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp)
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    }
                )
            }
        }
    }
}

@Composable
fun IndustrialResultBox(
    label: String,
    value: String,
    highlight: Boolean = false,
    accentColor: Color = InnotechTheme.colors.signalYellow,
    modifier: Modifier = Modifier
) {
    val colors = InnotechTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (highlight) colors.signalYellowSoft else colors.subtleSurface,
        border = BorderStroke(
            width = if (highlight) 1.5.dp else 1.dp,
            color = if (highlight) accentColor else colors.cardBorder
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = IndustrialTextStyles.ReadoutMedium,
                color = if (highlight) colors.onSignalYellow else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

enum class HintSeverity { INFO, SUCCESS, WARNING, ERROR }

@Composable
fun IndustrialHintBanner(
    text: String,
    severity: HintSeverity = HintSeverity.INFO,
    modifier: Modifier = Modifier
) {
    val colors = InnotechTheme.colors
    val (bg, fg) = when (severity) {
        HintSeverity.INFO -> colors.namurCheckContainer to colors.namurCheck
        HintSeverity.SUCCESS -> colors.namurOkContainer to colors.namurOk
        HintSeverity.WARNING -> colors.namurMaintenanceContainer to colors.namurMaintenance
        HintSeverity.ERROR -> colors.namurFailureContainer to colors.namurFailure
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun <T> SegmentedOptionRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = InnotechTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) colors.signalYellow else colors.subtleSurface,
                border = BorderStroke(1.dp, if (isSelected) colors.signalYellow else colors.cardBorder),
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(value) }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) colors.onSignalYellow else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, InnotechTheme.colors.cardBorder, RoundedCornerShape(8.dp))
            .background(InnotechTheme.colors.subtleSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = IndustrialTextStyles.ReadoutSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
