package com.hermex.android.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.theme.EvaDesignTokens

/** Liquid-glass material — iOS 26 / Android 16 style.
 *  One background for the whole app (EvaAmbientBackdrop) + every card/dock is a
 *  semi-transparent blurred surface with a specular rim. This makes the bottom dock
 *  visually merge with the main gradient instead of looking like a separate slab.
 */
@Composable
fun evaGlassColors(): Pair<Color, Color> {
    val duality by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = duality == DualityMode.SAKURA
    // glass fill — 8-12% white + tint of mode, so it reads on top of EvaAmbientBackdrop
    val fill = if (isSakura) Color(0xFFF472B6).copy(alpha = 0.09f) else Color(0xFFE11D48).copy(alpha = 0.08f)
    val base = Color.White.copy(alpha = if (isSakura) 0.06f else 0.04f)
    return base.copy(alpha = base.alpha + fill.alpha * 0.5f) to fill
}

@Composable
fun EvaGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = EvaDesignTokens.Shapes.Card,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val duality by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = duality == DualityMode.SAKURA
    val accent = if (isSakura) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson
    val shape = RoundedCornerShape(cornerRadius)
    // liquid glass: translucent + specular top highlight + soft outer glow
    val glassFill = if (isSakura) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.035f)
    val tint = accent.copy(alpha = if (isSakura) 0.06f else 0.045f)
    val border = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.18f),
            Color.White.copy(alpha = 0.04f),
            accent.copy(alpha = 0.22f)
        )
    )
    Surface(
        modifier = modifier
            .clip(shape),
        shape = shape,
        color = glassFill.copy(alpha = glassFill.alpha + tint.alpha),
        border = BorderStroke(1.dp, border),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.Transparent, Color.Transparent)
                    ),
                    shape
                )
        ) { content() }
    }
}

@Composable
fun EvaGlassDock(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val duality by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = duality == DualityMode.SAKURA
    val accent = if (isSakura) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson
    val shape = RoundedCornerShape(28.dp)
    // dock rides on the same backdrop — blurred, not a solid slab
    val fill = if (isSakura) Color(0xFF1B0F1F).copy(alpha = 0.52f) else Color(0xFF12080D).copy(alpha = 0.58f)
    Surface(
        modifier = modifier
            .clip(shape),
        shape = shape,
        color = fill,
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), accent.copy(alpha = 0.18f)))),
        shadowElevation = 16.dp,
    ) { content() }
}
