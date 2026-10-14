package com.hermex.android.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermex.android.core.storage.HeaderLogoColor

object HermexColors {
    val HermesGold = Color(0xFFFFD700)
    val AccentBlueDark = Color(0xFF0A84FF)
    val AccentBlueLight = Color(0xFF007AFF)
    val SuccessDark = Color(0xFF30D158)
    val SuccessLight = Color(0xFF34C759)
    val WarningDark = Color(0xFFFF9F0A)
    val WarningLight = Color(0xFFFF9500)
    val ErrorDark = Color(0xFFFF453A)
    val ErrorLight = Color(0xFFFF3B30)
    val DarkBackground = Color(0xFF07040D)
    val DarkSecondaryBackground = Color(0xFF12081E)
    val DarkSurfaceLow = Color(0xFF0D0616)
    val DarkSurfaceHigh = Color(0xFF1A0D2A)
    val DarkSurfaceHighest = Color(0xFF24123B)
    val DarkLabel = Color(0xFFFFFFFF)
    val DarkSecondaryLabel = Color(0xFF98989F)
    val DarkTertiaryLabel = Color(0xFF636366)
    val LightTertiaryLabel = Color(0xFFC4C4C6)
    val DarkSeparator = Color(0xFF321A4E)
    val UserBubbleDark = Color(0xFF2A1442)
    val CodeBackgroundDark = Color(0xFF0A0512)

    // Eva Cyber-Anime Palette
    val EvaPurple = Color(0xFFC084FC)
    val EvaPurpleDark = Color(0xFFA855F7)
    val EvaObsidian = Color(0xFF07040D)
    val SakuraPink = Color(0xFFF472B6)
    val SakuraBlush = Color(0xFFFB7185)
    val YandereCrimson = Color(0xFFEF4444)
    val YandereBlood = Color(0xFF991B1B)
    val NeonCyan = Color(0xFF38BDF8)
    val NeonEmerald = Color(0xFF10B981)
}

/** Mirrors the design system's `tokens/typography.css` scale (itself mirrored from the iOS app's
 * Dynamic Type-inspired `Type.kt`) -- sizes/weights/line-heights only, no custom font family, so
 * this still resolves to the platform system font (Roboto) exactly like the Material3 default it
 * replaces. Only the 11 slots the token file actually defines are overridden; `displayLarge/
 * Medium/Small` (unused by this dense, mobile-first UI) keep Material3's stock defaults. */
private val HermexTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 37.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp),
)

object HermexRadii {
    /** @deprecated Use [EvaDesignTokens.Shapes.Bubble] (22dp) -- unified radius system. */
    val Bubble = EvaDesignTokens.Shapes.Bubble
    val Accessory = 10.dp
    val Tool = 9.dp
    val Cell = EvaDesignTokens.Shapes.Cell
    val SettingsCard = 18.dp
    val Dialog = EvaDesignTokens.Shapes.Dialog
    val Composer = 22.dp
    val Code = 24.dp
}

/** Common spacing scale for new/restyled layouts. Existing screens keep their inline dp values
 * until they're touched by a restyle pass; this isn't meant to force a retrofit. */
@Deprecated("Use EvaDesignTokens.Spacing instead -- legacy bridge kept until call sites migrate.")
object HermexSpacing {
    val XS = EvaDesignTokens.Spacing.xs
    val SM = EvaDesignTokens.Spacing.sm
    val MD = EvaDesignTokens.Spacing.md
    val LG = EvaDesignTokens.Spacing.lg
    val XL = EvaDesignTokens.Spacing.xl
    val XXL = EvaDesignTokens.Spacing.xxl
}

// Kept for the legacy Settings header-color option. It is not the primary action/accent color.
internal val HermexRed = Color(0xFFE0473C)

private val HeaderLogoBlue = Color(0xFF4A90D9)
private val HeaderLogoPurple = Color(0xFF9B59B6)
private val HeaderLogoGreen = Color(0xFF4CAF50)
private val HeaderLogoOrange = Color(0xFFFF9800)
private val HeaderLogoPink = Color(0xFFE91E8C)
private val HeaderLogoMono = Color(0xFF9E9E9E)

/** Resolves a [HeaderLogoColor] to an actual paintable [Color]. [HeaderLogoColor.DEFAULT] must
 * stay a `@Composable` read of the current title color (not a fixed constant) so it exactly
 * preserves today's appearance in both light and dark mode, rather than hardcoding one of them. */
@Composable
fun HeaderLogoColor.toComposeColor(): Color = when (this) {
    HeaderLogoColor.DEFAULT -> MaterialTheme.colorScheme.onSurface
    HeaderLogoColor.BLUE -> HeaderLogoBlue
    HeaderLogoColor.PURPLE -> HeaderLogoPurple
    HeaderLogoColor.GREEN -> HeaderLogoGreen
    HeaderLogoColor.ORANGE -> HeaderLogoOrange
    HeaderLogoColor.RED -> HermexRed
    HeaderLogoColor.PINK -> HeaderLogoPink
    HeaderLogoColor.MONO -> HeaderLogoMono
}

/** Sakura mode: full dark scheme built from EvaDesignTokens (audit P0-3: tokens wired in). */
private val SakuraColors = darkColorScheme(
    primary = EvaDesignTokens.Colors.SakuraAccent,
    onPrimary = EvaDesignTokens.Colors.SakuraOnAccent,
    primaryContainer = EvaDesignTokens.Colors.SakuraContainer,
    onPrimaryContainer = EvaDesignTokens.Colors.SakuraText,
    secondary = EvaDesignTokens.Colors.SakuraSecondary,
    onSecondary = EvaDesignTokens.Colors.SakuraOnSecondary,
    secondaryContainer = EvaDesignTokens.Colors.SakuraSecondaryContainer,
    onSecondaryContainer = EvaDesignTokens.Colors.SakuraOnSecondaryContainer,
    tertiary = EvaDesignTokens.Colors.StatusOnline,
    onTertiary = EvaDesignTokens.Colors.SakuraOnTertiary,
    background = EvaDesignTokens.Colors.SakuraBackground,
    onBackground = Color.White,
    surface = EvaDesignTokens.Colors.SurfaceDark,
    onSurface = EvaDesignTokens.Colors.SakuraText,
    surfaceVariant = EvaDesignTokens.Colors.SurfaceCardHighest,
    onSurfaceVariant = EvaDesignTokens.Colors.SakuraTextMuted,
    surfaceContainerLowest = EvaDesignTokens.Colors.VoidBlack,
    surfaceContainerLow = EvaDesignTokens.Colors.SakuraSurfaceLow,
    surfaceContainer = EvaDesignTokens.Colors.SurfaceCard,
    surfaceContainerHigh = EvaDesignTokens.Colors.SurfaceCardHigh,
    surfaceContainerHighest = EvaDesignTokens.Colors.SurfaceCardHighest,
    outline = EvaDesignTokens.Colors.SakuraOutline,
    outlineVariant = EvaDesignTokens.Colors.SakuraOutlineVariant,
    error = EvaDesignTokens.Colors.StatusError,
    errorContainer = Color(0xFF3A1210),
    onErrorContainer = Color(0xFFFFB3AD),
)

/** Yandere mode: full dark scheme, shifted redder (audit: environment psychology). */
private val YandereColors = darkColorScheme(
    primary = EvaDesignTokens.Colors.YandereCrimson,
    onPrimary = EvaDesignTokens.Colors.YandereOnAccent,
    primaryContainer = EvaDesignTokens.Colors.YandereDarkBlood,
    onPrimaryContainer = EvaDesignTokens.Colors.YandereText,
    secondary = Color(0xFFFDA4AF),
    onSecondary = EvaDesignTokens.Colors.YandereOnSecondary,
    secondaryContainer = EvaDesignTokens.Colors.YandereSecondaryContainer,
    onSecondaryContainer = EvaDesignTokens.Colors.YandereOnSecondaryContainer,
    tertiary = EvaDesignTokens.Colors.YandereTertiary,
    onTertiary = EvaDesignTokens.Colors.YandereOnTertiary,
    background = EvaDesignTokens.Colors.YandereBackground,
    onBackground = Color.White,
    surface = EvaDesignTokens.Colors.YandereSurfaceDark,
    onSurface = EvaDesignTokens.Colors.YandereText,
    surfaceVariant = EvaDesignTokens.Colors.YandereSurfaceHighest,
    onSurfaceVariant = EvaDesignTokens.Colors.YandereTextMuted,
    surfaceContainerLowest = EvaDesignTokens.Colors.YandereSurfaceLowest,
    surfaceContainerLow = EvaDesignTokens.Colors.YandereSurfaceLow,
    surfaceContainer = EvaDesignTokens.Colors.YandereSurface,
    surfaceContainerHigh = EvaDesignTokens.Colors.YandereSurfaceHigh,
    surfaceContainerHighest = EvaDesignTokens.Colors.YandereSurfaceHighest,
    outline = EvaDesignTokens.Colors.YandereOutline,
    outlineVariant = EvaDesignTokens.Colors.YandereOutlineVariant,
    error = EvaDesignTokens.Colors.StatusError,
    errorContainer = Color(0xFF3A1210),
    onErrorContainer = Color(0xFFFFB3AD),
)

@Composable
fun HermexTheme(
    content: @Composable () -> Unit,
) {
    val dualityMode by com.hermex.android.eva.EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = dualityMode == com.hermex.android.eva.DualityMode.SAKURA

    // Audit P0-2: the app is dark-first by design -- duality selects between two full
    // dark environments (sakura garden / crimson terminal). The old fake-light branch
    // (LightColors with a single primary swap) is gone, along with the mixed
    // light/dark Frankenstein it produced on screens like Looksmax.
    val targetScheme = if (isSakura) SakuraColors else YandereColors

    // Audit P1-5: animate the mode switch (EvaDesignTokens.Motion.ModeSwitchDurationMs)
    // instead of a hard cut. Each slot crossfades over the same 280ms budget.
    val animated = ColorScheme(
        primary = animateColorAsState(targetScheme.primary, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityPrimary").value,
        onPrimary = animateColorAsState(targetScheme.onPrimary, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityOnPrimary").value,
        primaryContainer = animateColorAsState(targetScheme.primaryContainer, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityPrimaryContainer").value,
        onPrimaryContainer = animateColorAsState(targetScheme.onPrimaryContainer, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityOnPrimaryContainer").value,
        inversePrimary = animateColorAsState(targetScheme.inversePrimary, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityInversePrimary").value,
        secondary = animateColorAsState(targetScheme.secondary, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualitySecondary").value,
        onSecondary = animateColorAsState(targetScheme.onSecondary, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityOnSecondary").value,
        secondaryContainer = animateColorAsState(targetScheme.secondaryContainer, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualitySecondaryContainer").value,
        onSecondaryContainer = animateColorAsState(targetScheme.onSecondaryContainer, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityOnSecondaryContainer").value,
        tertiary = animateColorAsState(targetScheme.tertiary, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityTertiary").value,
        onTertiary = animateColorAsState(targetScheme.onTertiary, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityOnTertiary").value,
        tertiaryContainer = animateColorAsState(targetScheme.tertiaryContainer, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityTertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(targetScheme.onTertiaryContainer, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityOnTertiaryContainer").value,
        background = animateColorAsState(targetScheme.background, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityBackground").value,
        onBackground = animateColorAsState(targetScheme.onBackground, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityOnBackground").value,
        surface = animateColorAsState(targetScheme.surface, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualitySurface").value,
        onSurface = animateColorAsState(targetScheme.onSurface, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityOnSurface").value,
        surfaceVariant = animateColorAsState(targetScheme.surfaceVariant, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualitySurfaceVariant").value,
        onSurfaceVariant = animateColorAsState(targetScheme.onSurfaceVariant, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityOnSurfaceVariant").value,
        surfaceTint = animateColorAsState(targetScheme.surfaceTint, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualitySurfaceTint").value,
        inverseSurface = animateColorAsState(targetScheme.inverseSurface, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityInverseSurface").value,
        inverseOnSurface = animateColorAsState(targetScheme.inverseOnSurface, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityInverseOnSurface").value,
        error = animateColorAsState(targetScheme.error, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityError").value,
        onError = animateColorAsState(targetScheme.onError, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityOnError").value,
        errorContainer = animateColorAsState(targetScheme.errorContainer, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityErrorContainer").value,
        onErrorContainer = animateColorAsState(targetScheme.onErrorContainer, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityOnErrorContainer").value,
        outline = animateColorAsState(targetScheme.outline, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityOutline").value,
        outlineVariant = animateColorAsState(targetScheme.outlineVariant, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityOutlineVariant").value,
        scrim = animateColorAsState(targetScheme.scrim, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualityScrim").value,
        surfaceBright = animateColorAsState(targetScheme.surfaceBright, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualitySurfaceBright").value,
        surfaceDim = animateColorAsState(targetScheme.surfaceDim, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualitySurfaceDim").value,
        surfaceContainer = animateColorAsState(targetScheme.surfaceContainer, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualitySurfaceContainer").value,
        surfaceContainerHigh = animateColorAsState(targetScheme.surfaceContainerHigh, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualitySurfaceContainerHigh").value,
        surfaceContainerHighest = animateColorAsState(targetScheme.surfaceContainerHighest, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualitySurfaceContainerHighest").value,
        surfaceContainerLow = animateColorAsState(targetScheme.surfaceContainerLow, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualitySurfaceContainerLow").value,
        surfaceContainerLowest = animateColorAsState(targetScheme.surfaceContainerLowest, tween(EvaDesignTokens.Motion.ModeSwitchDurationMs), label = "dualitySurfaceContainerLowest").value,
    )

    MaterialTheme(
        colorScheme = animated,
        typography = HermexTypography,
        content = content,
    )
}
