package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class IndustrialColors(
    val headerBackground: Color,
    val headerContent: Color,
    val signalYellow: Color,
    val onSignalYellow: Color,
    val signalYellowSoft: Color,
    val cardBorder: Color,
    val subtleSurface: Color,
    val namurOk: Color,
    val namurOkContainer: Color,
    val namurMaintenance: Color,
    val namurMaintenanceContainer: Color,
    val namurOutOfSpec: Color,
    val namurOutOfSpecContainer: Color,
    val namurFailure: Color,
    val namurFailureContainer: Color,
    val namurCheck: Color,
    val namurCheckContainer: Color,
)

val LocalIndustrialColors = staticCompositionLocalOf {
    IndustrialColors(
        headerBackground = Anthracite800,
        headerContent = SurfaceWhite,
        signalYellow = SignalYellow,
        onSignalYellow = Anthracite900,
        signalYellowSoft = SignalYellowSoft,
        cardBorder = BorderLight,
        subtleSurface = SurfaceSubtleLight,
        namurOk = NamurGreen,
        namurOkContainer = NamurGreenBg,
        namurMaintenance = NamurYellow,
        namurMaintenanceContainer = NamurYellowBg,
        namurOutOfSpec = NamurOrange,
        namurOutOfSpecContainer = NamurOrangeBg,
        namurFailure = NamurRed,
        namurFailureContainer = NamurRedBg,
        namurCheck = NamurBlue,
        namurCheckContainer = NamurBlueBg,
    )
}

private val IndustrialLightColorScheme = lightColorScheme(
    primary = SignalYellow,
    onPrimary = Anthracite900,
    primaryContainer = SignalYellowSoft,
    onPrimaryContainer = Anthracite900,
    secondary = Anthracite700,
    onSecondary = SurfaceWhite,
    secondaryContainer = Color(0xFFE5E7EB),
    onSecondaryContainer = Anthracite900,
    tertiary = NamurBlue,
    onTertiary = SurfaceWhite,
    background = CanvasLight,
    onBackground = Anthracite900,
    surface = SurfaceWhite,
    onSurface = Anthracite900,
    surfaceVariant = SurfaceSubtleLight,
    onSurfaceVariant = Slate600,
    outline = BorderLight,
    outlineVariant = Color(0xFFD1D5DB),
    error = NamurRed,
    onError = SurfaceWhite
)

private val IndustrialDarkColorScheme = darkColorScheme(
    primary = SignalYellow,
    onPrimary = Anthracite900,
    primaryContainer = SignalYellowDarkContainer,
    onPrimaryContainer = SignalYellow,
    secondary = Slate400,
    onSecondary = Anthracite900,
    secondaryContainer = Anthracite700,
    onSecondaryContainer = SurfaceWhite,
    tertiary = NamurBlue,
    onTertiary = SurfaceWhite,
    background = CanvasDark,
    onBackground = Color(0xFFF3F4F6),
    surface = SurfaceDark,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = Slate400,
    outline = BorderDark,
    outlineVariant = Color(0xFF3E4656),
    error = Color(0xFFEF4444),
    onError = SurfaceWhite
)

private val IndustrialShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp)
)

@Composable
fun InnotechServiceTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) IndustrialDarkColorScheme else IndustrialLightColorScheme
    val industrialColors = if (darkTheme) {
        IndustrialColors(
            headerBackground = Anthracite900,
            headerContent = Color(0xFFF3F4F6),
            signalYellow = SignalYellow,
            onSignalYellow = Anthracite900,
            signalYellowSoft = SignalYellowDarkContainer,
            cardBorder = BorderDark,
            subtleSurface = SurfaceElevatedDark,
            namurOk = Color(0xFF22C55E),
            namurOkContainer = Color(0xFF0F2E1A),
            namurMaintenance = Color(0xFFFACC15),
            namurMaintenanceContainer = Color(0xFF362B05),
            namurOutOfSpec = Color(0xFFFB923C),
            namurOutOfSpecContainer = Color(0xFF3B1A06),
            namurFailure = Color(0xFFF87171),
            namurFailureContainer = Color(0xFF3B0D0D),
            namurCheck = Color(0xFF60A5FA),
            namurCheckContainer = Color(0xFF0E2244),
        )
    } else {
        IndustrialColors(
            headerBackground = Anthracite800,
            headerContent = SurfaceWhite,
            signalYellow = SignalYellow,
            onSignalYellow = Anthracite900,
            signalYellowSoft = SignalYellowSoft,
            cardBorder = BorderLight,
            subtleSurface = SurfaceSubtleLight,
            namurOk = NamurGreen,
            namurOkContainer = NamurGreenBg,
            namurMaintenance = NamurYellow,
            namurMaintenanceContainer = NamurYellowBg,
            namurOutOfSpec = NamurOrange,
            namurOutOfSpecContainer = NamurOrangeBg,
            namurFailure = NamurRed,
            namurFailureContainer = NamurRedBg,
            namurCheck = NamurBlue,
            namurCheckContainer = NamurBlueBg,
        )
    }

    CompositionLocalProvider(LocalIndustrialColors provides industrialColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = IndustrialShapes,
            content = content
        )
    }
}

object InnotechTheme {
    val colors: IndustrialColors
        @Composable get() = LocalIndustrialColors.current
}
