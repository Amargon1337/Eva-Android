package com.hermex.android.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * EvaDesignTokens — Единая каноническая система токенов Hermex 2.0 (Eva OS)
 * для 120 Гц дисплеев на базе Material 3 Expressive.
 */
object EvaDesignTokens {

    object Colors {
        // Shared Obsidian Void Base (не чистый #000000, а глубокий чернильный)
        val VoidBlack = Color(0xFF07030C)
        val DeepObsidian = Color(0xFF0C0714)
        val SurfaceDark = Color(0xFF130920)
        val SurfaceCard = Color(0xFF1B0C2E)
        val SurfaceCardHigh = Color(0xFF261240)
        val SurfaceCardHighest = Color(0xFF321854)

        // Sakura Palette (Интимный ночной сад, акцентный розовый)
        val SakuraAccent = Color(0xFFF472B6)
        val SakuraBlush = Color(0xFFFB7185)
        val SakuraLavender = Color(0xFFC084FC)
        val SakuraCreamSurface = Color(0xFF1B0C2E)
        val SakuraBorder = Color(0x40F472B6)
        val SakuraBorderGlow = Color(0x66F472B6)
        val SakuraText = Color(0xFFFCE7F3)
        val SakuraTextMuted = Color(0xFFD8B4E2)

        // Sakura scheme extras (theme wiring, audit P0-3)
        val SakuraOnAccent = Color(0xFF1B0311)
        val SakuraContainer = Color(0xFF4A1032)
        val SakuraSecondary = Color(0xFF38BDF8)
        val SakuraOnSecondary = Color(0xFF001F33)
        val SakuraSecondaryContainer = Color(0xFF0C3554)
        val SakuraOnSecondaryContainer = Color(0xFFBAE6FD)
        val SakuraOnTertiary = Color(0xFF002214)
        val SakuraBackground = DeepObsidian
        val SakuraSurfaceLow = Color(0xFF11071C)
        val SakuraOutline = Color(0xFF4A1C78)
        val SakuraOutlineVariant = Color(0xFF2E124D)

        // Yandere Palette (Лаконичный матовый обсидиан 2026: глубокий агат, деликатные винные акценты)
        val YandereCrimson = Color(0xFFE11D48)
        val YandereWine = Color(0xFF9F1239)
        val YandereDarkBlood = Color(0xFF2E0812)
        val YandereViolet = Color(0xFF9F1239) // no purple in yandere — deep wine
        val YandereNeonMagenta = Color(0xFFFB7185) // rose, not magenta-violet
        val YandereSurface = Color(0xFF09040C)
        val YandereSurfaceHigh = Color(0xFF100716)
        val YandereSurfaceHighest = Color(0xFF180A20)
        val YandereBorder = Color(0x30FB7185)
        val YandereBorderGlow = Color(0x209F1239)
        val YandereText = Color(0xFFFFF1F2)
        val YandereTextMuted = Color(0xFFFDA4AF)

        // Yandere scheme extras (theme wiring, audit P0-3)
        val YandereOnAccent = Color(0xFF1C0207)
        val YandereSecondary = Color(0xFFFB7185) // was SakuraLavender (purple) -> rose for yandere, no purple text
        val YandereOnSecondary = Color(0xFF1F0433)
        val YandereSecondaryContainer = Color(0xFF3A0A18)
        val YandereOnSecondaryContainer = Color(0xFFFFE4E6) // warm rose, not lavender
        val YandereTertiary = Color(0xFFF97316)
        val YandereOnTertiary = Color(0xFF2A0A00)
        val YandereBackground = Color(0xFF060309)
        val YandereSurfaceDark = Color(0xFF0A040E)
        val YandereSurfaceLowest = Color(0xFF040206)
        val YandereSurfaceLow = Color(0xFF08030B)
        val YandereOutline = Color(0xFF380C18)
        val YandereOutlineVariant = Color(0xFF1E060D)

        // Status & Telemetry
        val StatusOnline = Color(0xFF10B981)
        val StatusAway = Color(0xFFF59E0B)
        val StatusBusy = Color(0xFFF59E0B) // amber, not violet
        val StatusError = Color(0xFFEF4444)
        val LanBadge = Color(0xFF06B6D4)
        val CodeBackground = Color(0xFF0A0512)
    }

    object Gradients {
        val SakuraSpecularBorder = androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(
                Color(0x75F472B6),
                Color(0x30C084FC),
                Color(0x15F472B6)
            )
        )
        val YandereSpecularBorder = androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(
                Color(0x50FB7185),
                Color(0x209F1239),
                Color(0x102E0812)
            )
        )
        val SakuraNeonHalo = androidx.compose.ui.graphics.Brush.sweepGradient(
            listOf(
                Color(0xFFFF71D4),
                Color(0xFFC084FC),
                Color(0xFF38BDF8),
                Color(0xFFFF71D4)
            )
        )
        val YandereObsidianHalo = androidx.compose.ui.graphics.Brush.sweepGradient(
            listOf(
                Color(0xFFFB7185),
                Color(0xFF9F1239),
                Color(0xFFE11D48),
                Color(0xFFFB7185)
            )
        )
    }

    val Hairline: Dp = Shapes.Hairline

    object Sakura {
        val Pink: Color = Colors.SakuraAccent
        val Border: Color = Colors.SakuraBorder
        val Background: Color = Colors.SakuraBackground
    }

    object Yandere {
        val Crimson: Color = Colors.YandereCrimson
        val Wine: Color = Colors.YandereWine
        val Surface: Color = Colors.YandereSurface
        val Background: Color = Colors.YandereBackground
    }

    object Shapes {
        val Hairline: Dp = 0.75.dp
        val Tag = 8.dp
        val Cell = 12.dp
        val Card = 16.dp
        val Bento = 20.dp
        val Bubble = 22.dp
        val Composer = 24.dp
        val Hero = 28.dp
        val Dialog = 28.dp
        val Pill = 999.dp
    }

    object Spacing {
        val xxs: Dp = 2.dp
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 24.dp
        val xxl: Dp = 32.dp
    }

    object Motion {
        val SnappySpring = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )

        val GentleSpring = spring<Float>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )

        val StrictSpring = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )

        const val TokenRevealDurationMs = 75
        const val ModeSwitchDurationMs = 280
        const val GlitchFrameDurationMs = 24
    }

    object Elevation {
        val Level0 = 0.dp
        val Level1 = 2.dp
        val Level2 = 6.dp
        val Level3 = 10.dp
        val Level4 = 16.dp
    }
}