package com.hermex.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import com.hermex.android.ui.theme.EvaDesignTokens
import com.hermex.android.ui.theme.EvaMotionPrefs
import kotlin.math.sin

/**
 * EvaAmbientParticles — Аппаратный холст с парящими лепестками сакуры (Сакура)
 * или тлеющими рубиновыми искрами и алыми лепестками (Яндере).
 */
@Composable
fun EvaAmbientParticles(
    modifier: Modifier = Modifier,
    isSakura: Boolean = true,
    enabled: Boolean = true
) {
    if (!enabled || EvaMotionPrefs.reducedMotion()) return

    val transition = rememberInfiniteTransition(label = "evaParticlesTransition")

    val progress1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSakura) 8500 else 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle1"
    )

    val progress2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSakura) 11000 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle2"
    )

    val progress3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSakura) 9500 else 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle3"
    )

    val progress4 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSakura) 12500 else 8500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle4"
    )

    val particleBrush = remember(isSakura) {
        if (isSakura) {
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFCE7F3),
                    EvaDesignTokens.Colors.SakuraAccent
                )
            )
        } else {
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF4D6D),
                    Color(0xFFE11D48),
                    Color(0xFF880820)
                )
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Частица 1 (Лепесток / тлеющий уголек)
        val y1 = if (isSakura) progress1 * (h + 60f) - 30f else (1f - progress1) * (h + 60f) - 30f
        val x1 = (w * 0.25f) + (sin(progress1 * 6.28f) * 35f)
        val rot1 = progress1 * 360f
        val alpha1 = when {
            progress1 < 0.1f -> progress1 * 10f
            progress1 > 0.85f -> (1f - progress1) * 6.6f
            else -> 0.75f
        }

        rotate(rot1, pivot = Offset(x1, y1)) {
            val path1 = Path().apply {
                moveTo(x1, y1 - 12f)
                cubicTo(x1 + 10f, y1 - 8f, x1 + 8f, y1 + 10f, x1, y1 + 14f)
                cubicTo(x1 - 8f, y1 + 10f, x1 - 10f, y1 - 8f, x1, y1 - 12f)
                close()
            }
            drawPath(path1, particleBrush, alpha = alpha1)
        }

        // Частица 2 (Лепесток / тлеющий уголек)
        val y2 = if (isSakura) progress2 * (h + 60f) - 30f else (1f - progress2) * (h + 60f) - 30f
        val x2 = (w * 0.72f) + (sin(progress2 * 5.0f) * 45f)
        val rot2 = progress2 * -280f
        val alpha2 = when {
            progress2 < 0.1f -> progress2 * 10f
            progress2 > 0.85f -> (1f - progress2) * 6.6f
            else -> 0.65f
        }

        rotate(rot2, pivot = Offset(x2, y2)) {
            val path2 = Path().apply {
                moveTo(x2, y2 - 10f)
                cubicTo(x2 + 8f, y2 - 6f, x2 + 6f, y2 + 8f, x2, y2 + 11f)
                cubicTo(x2 - 6f, y2 + 8f, x2 - 8f, y2 - 6f, x2, y2 - 10f)
                close()
            }
            drawPath(path2, particleBrush, alpha = alpha2)
        }

        // Частица 3: В режиме Сакуры — 3-й лепесток по центру; в Яндере — горящая искра
        if (isSakura) {
            val y3 = progress3 * (h + 60f) - 30f
            val x3 = (w * 0.48f) + (sin(progress3 * 7.0f) * 30f)
            val rot3 = progress3 * 220f
            val alpha3 = when {
                progress3 < 0.1f -> progress3 * 10f
                progress3 > 0.85f -> (1f - progress3) * 6.6f
                else -> 0.70f
            }
            rotate(rot3, pivot = Offset(x3, y3)) {
                val path3 = Path().apply {
                    moveTo(x3, y3 - 11f)
                    cubicTo(x3 + 9f, y3 - 7f, x3 + 7f, y3 + 9f, x3, y3 + 12f)
                    cubicTo(x3 - 7f, y3 + 9f, x3 - 9f, y3 - 7f, x3, y3 - 11f)
                    close()
                }
                drawPath(path3, particleBrush, alpha = alpha3)
            }

            // Частица 4: 4-й нежный лепесток правее центра
            val y4 = progress4 * (h + 60f) - 30f
            val x4 = (w * 0.88f) + (sin(progress4 * 4.5f) * 25f)
            val rot4 = progress4 * -180f
            val alpha4 = when {
                progress4 < 0.1f -> progress4 * 10f
                progress4 > 0.85f -> (1f - progress4) * 6.6f
                else -> 0.60f
            }
            rotate(rot4, pivot = Offset(x4, y4)) {
                val path4 = Path().apply {
                    moveTo(x4, y4 - 8f)
                    cubicTo(x4 + 7f, y4 - 5f, x4 + 5f, y4 + 7f, x4, y4 + 9f)
                    cubicTo(x4 - 5f, y4 + 7f, x4 - 7f, y4 - 5f, x4, y4 - 8f)
                    close()
                }
                drawPath(path4, particleBrush, alpha = alpha4)
            }
        } else {
            val y3 = (1f - progress3) * (h + 40f)
            val x3 = (w * 0.48f) + (sin(progress3 * 8.0f) * 25f)
            val alpha3 = (1f - progress3) * 0.8f
            drawCircle(
                color = Color(0xFFFF2A4D),
                radius = 3.5f,
                center = Offset(x3, y3),
                alpha = alpha3
            )
        }
    }
}

/**
 * Обратная совместимость для существующих вызовов EvaPetalCanvas
 */
@Composable
fun EvaPetalCanvas(
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    EvaAmbientParticles(modifier = modifier, isSakura = true, enabled = enabled)
}
