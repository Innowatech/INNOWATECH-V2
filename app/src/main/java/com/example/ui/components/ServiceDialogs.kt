package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.KofferPin
import com.example.model.LightboxContent
import com.example.model.UserAccount
import com.example.ui.KofferQtyPending
import com.example.ui.theme.InnotechTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TechnicianLoginDialog(
    users: Map<String, UserAccount>,
    activeUserKuerzel: String?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onLogin: (kuerzel: String, pin: String) -> Unit,
    onLogout: () -> Unit
) {
    var kuerzel by remember(activeUserKuerzel) { mutableStateOf(activeUserKuerzel.orEmpty()) }
    var pin by remember { mutableStateOf("") }
    val colors = InnotechTheme.colors
    val activeUsers = remember(users) {
        users.values.filter { it.status == "active" }.sortedBy { it.kuerzel }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.signalYellow),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = colors.onSignalYellow)
            }
        },
        title = { Text("Techniker-Anmeldung") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!activeUserKuerzel.isNullOrBlank()) {
                    val acc = users[activeUserKuerzel]
                    IndustrialHintBanner(
                        text = "Aktiv angemeldet: ${acc?.name ?: activeUserKuerzel} ($activeUserKuerzel)",
                        severity = HintSeverity.SUCCESS
                    )
                } else {
                    Text(
                        text = "Alle 17 Module sind im Präsentations-Modus aktiv. Optional per Techniker-Kürzel & PIN anmelden:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    activeUsers.forEach { user ->
                        FilterChip(
                            selected = kuerzel.equals(user.kuerzel, ignoreCase = true),
                            onClick = { kuerzel = user.kuerzel },
                            label = { Text(user.kuerzel) }
                        )
                    }
                }

                OutlinedTextField(
                    value = kuerzel,
                    onValueChange = { kuerzel = it.uppercase() },
                    label = { Text("Kürzel") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_kuerzel_field")
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = errorMessage != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_pin_field")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLogin(kuerzel, pin) },
                enabled = kuerzel.isNotBlank() && pin.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.signalYellow,
                    contentColor = colors.onSignalYellow
                ),
                modifier = Modifier.testTag("login_submit_button")
            ) {
                Text("Anmelden")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!activeUserKuerzel.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            onLogout()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("login_logout_button")
                    ) {
                        Text("Abmelden")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Schließen")
                }
            }
        }
    )
}

@Composable
fun AdminPinDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val colors = InnotechTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.signalYellow),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = colors.onSignalYellow)
            }
        },
        title = { Text("Admin-Konsole öffnen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Bitte Administrator-PIN eingeben, um Techniker-Profile und Datenbanken zu verwalten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("Admin-PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = errorMessage != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input")
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pin) },
                enabled = pin.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.signalYellow,
                    contentColor = colors.onSignalYellow
                ),
                modifier = Modifier.testTag("admin_pin_confirm")
            ) {
                Text("Öffnen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
fun ChangelogDialog(
    changelogText: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.History, contentDescription = null, tint = InnotechTheme.colors.signalYellow)
        },
        title = { Text("Changelog (v4.6 – v5.6)") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = changelogText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KofferQtyModal(
    pending: KofferQtyPending,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var qtyText by remember(pending.item.inNr) { mutableStateOf("1") }
    val currentQty = qtyText.toIntOrNull()?.coerceIn(1, 999) ?: 1
    val colors = InnotechTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nachorder-Menge festlegen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "[IN-${pending.item.inNr}] ${pending.item.beschreibung}",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { qtyText = (currentQty - 1).coerceAtLeast(1).toString() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Weniger")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }.take(3)
                            qtyText = clean
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(90.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { qtyText = (currentQty + 1).coerceAtMost(999).toString() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Mehr")
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 2, 3, 4, 5, 10).forEach { preset ->
                        FilterChip(
                            selected = currentQty == preset,
                            onClick = { qtyText = preset.toString() },
                            label = { Text("${preset}x") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentQty) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.signalYellow,
                    contentColor = colors.onSignalYellow
                ),
                modifier = Modifier.testTag("koffer_qty_confirm")
            ) {
                Text("Übernehmen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
fun LightboxModal(
    content: LightboxContent,
    onSelectKofferPin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember(content) { mutableFloatStateOf(1f) }
        var offsetX by remember(content) { mutableFloatStateOf(0f) }
        var offsetY by remember(content) { mutableFloatStateOf(0f) }
        val colors = InnotechTheme.colors

        val title = when (content) {
            is LightboxContent.VegaGuide -> content.title
            is LightboxContent.KofferPhoto -> content.boxName
            is LightboxContent.TurbinePhoto -> "Durchflussmesser / Turbine ${content.nr}"
        }
        val subtitle = when (content) {
            is LightboxContent.VegaGuide -> content.caption
            is LightboxContent.KofferPhoto -> content.itemSummary
            is LightboxContent.TurbinePhoto -> "Mit 2 Fingern zoomen · Tippen oder Schließen zum Beenden"
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xF2111418)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    if (scale > 1.05f) {
                        TextButton(onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }) {
                            Text("1:1", color = colors.signalYellow)
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("lightbox_close_button")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Schließen", tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(content) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 4f)
                                scale = nextScale
                                if (nextScale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val imgRes = when (content) {
                        is LightboxContent.VegaGuide -> content.imageRes
                        is LightboxContent.KofferPhoto -> content.imageRes
                        is LightboxContent.TurbinePhoto -> content.imageRes
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (content is LightboxContent.KofferPhoto) {
                            InteractiveKofferPhotoBox(
                                imageRes = imgRes,
                                contentDescription = title,
                                pins = content.pins,
                                activePinNr = content.activePinNr,
                                onSelectPin = onSelectKofferPin
                            )
                        } else {
                            Image(
                                painter = painterResource(id = imgRes),
                                contentDescription = title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1F242D),
                    border = BorderStroke(1.dp, colors.signalYellow.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveKofferPhotoBox(
    imageRes: Int,
    contentDescription: String,
    pins: List<KofferPin>,
    activePinNr: String,
    onSelectPin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = InnotechTheme.colors
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1E24))
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        pins.forEach { pin ->
            val isActive = pin.nr == activePinNr
            val pinDp = if (isActive) 28.dp else 22.dp
            Box(
                modifier = Modifier
                    .offset {
                        val halfPx = (pinDp.toPx() / 2f)
                        IntOffset(
                            x = (pin.xFraction * widthPx - halfPx).roundToInt(),
                            y = (pin.yFraction * heightPx - halfPx).roundToInt()
                        )
                    }
                    .size(pinDp)
                    .clip(CircleShape)
                    .background(if (isActive) colors.signalYellow else Color(0xD9181C22))
                    .border(
                        width = if (isActive) 2.dp else 1.2.dp,
                        color = if (isActive) Color.White else colors.signalYellow,
                        shape = CircleShape
                    )
                    .clickable { onSelectPin(pin.nr) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pin.nr,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) colors.onSignalYellow else Color.White,
                    maxLines = 1
                )
            }
        }
    }
}
