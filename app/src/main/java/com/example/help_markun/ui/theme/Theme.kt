package com.example.help_markun.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = HelpRed,
    onPrimary = Color.White,
    primaryContainer = Red90,
    onPrimaryContainer = Red10,
    secondary = HelpRedDeep,
    onSecondary = Color.White,
    secondaryContainer = Red95,
    onSecondaryContainer = HelpRedDeep,
    tertiary = Plum,
    onTertiary = Color.White,
    tertiaryContainer = Plum90,
    onTertiaryContainer = Plum,
    error = HelpRed,
    errorContainer = Red90,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Surface2,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Surface1,
    surfaceContainer = Surface2,
    surfaceContainerHigh = Surface3,
    surfaceContainerHighest = Color(0xFFEDDCDD),
    inverseSurface = Color(0xFF392E2F),
    inverseOnSurface = Color(0xFFFFEDEE),
    outline = Color(0xFFA08C8E),
    outlineVariant = Hairline,
)

// ダーク：赤みを帯びた暖かい黒。赤い面（HelpRed）と白文字の組み合わせはそのまま使える
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6B80),
    onPrimary = Color(0xFF5C0016),
    primaryContainer = Color(0xFF5E0C1E),
    onPrimaryContainer = Color(0xFFFFD9DD),
    secondary = Color(0xFFFFB2BA),
    onSecondary = Color(0xFF5C0016),
    secondaryContainer = Color(0xFF3B2124),
    onSecondaryContainer = Color(0xFFFFD9DD),
    tertiary = Color(0xFFFFB0CB),
    onTertiary = Color(0xFF4C1230),
    tertiaryContainer = Color(0xFF5E2640),
    onTertiaryContainer = Color(0xFFFFD8E6),
    error = Color(0xFFFF6B80),
    errorContainer = Color(0xFF5E0C1E),
    background = Color(0xFF191112),
    onBackground = Color(0xFFF1DEDF),
    surface = Color(0xFF191112),
    onSurface = Color(0xFFF1DEDF),
    surfaceVariant = Color(0xFF2B2021),
    onSurfaceVariant = Color(0xFFE4CFD1),
    surfaceContainerLowest = Color(0xFF130C0D),
    surfaceContainerLow = Color(0xFF211819),
    surfaceContainer = Color(0xFF261C1D),
    surfaceContainerHigh = Color(0xFF312627),
    surfaceContainerHighest = Color(0xFF3C3132),
    inverseSurface = Color(0xFFF1DEDF),
    inverseOnSurface = Color(0xFF392E2F),
    outline = Color(0xFFA08C8E),
    outlineVariant = Color(0xFF524345),
)

// Expressive らしく角丸を大きめに
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(44.dp),
)

/** テーマ切り替え時に色がパッと変わらず、なめらかに移り変わるようにする */
@Composable
private fun animated(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(450)
    @Composable
    fun Color.a(label: String) = animateColorAsState(this, spec, label = label).value
    return target.copy(
        primary = target.primary.a("primary"),
        onPrimary = target.onPrimary.a("onPrimary"),
        primaryContainer = target.primaryContainer.a("primaryContainer"),
        onPrimaryContainer = target.onPrimaryContainer.a("onPrimaryContainer"),
        secondaryContainer = target.secondaryContainer.a("secondaryContainer"),
        onSecondaryContainer = target.onSecondaryContainer.a("onSecondaryContainer"),
        tertiary = target.tertiary.a("tertiary"),
        onTertiary = target.onTertiary.a("onTertiary"),
        tertiaryContainer = target.tertiaryContainer.a("tertiaryContainer"),
        onTertiaryContainer = target.onTertiaryContainer.a("onTertiaryContainer"),
        background = target.background.a("background"),
        onBackground = target.onBackground.a("onBackground"),
        surface = target.surface.a("surface"),
        onSurface = target.onSurface.a("onSurface"),
        onSurfaceVariant = target.onSurfaceVariant.a("onSurfaceVariant"),
        surfaceContainerLowest = target.surfaceContainerLowest.a("scLowest"),
        surfaceContainerLow = target.surfaceContainerLow.a("scLow"),
        surfaceContainer = target.surfaceContainer.a("sc"),
        surfaceContainerHigh = target.surfaceContainerHigh.a("scHigh"),
        surfaceContainerHighest = target.surfaceContainerHighest.a("scHighest"),
        inverseSurface = target.inverseSurface.a("inverseSurface"),
        inverseOnSurface = target.inverseOnSurface.a("inverseOnSurface"),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Help_markunTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = animated(if (darkTheme) DarkColors else LightColors),
        motionScheme = MotionScheme.expressive(),
        shapes = AppShapes,
        typography = Typography,
        content = content
    )
}
