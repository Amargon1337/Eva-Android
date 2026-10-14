package com.hermex.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.character.EvaEventManager
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig

/**
 * EvaAmbientBackdrop — Канонический фоновый слой Hermex 2026 с эффектом глубокого
 * неземного свечения (Ambient Mesh Glow) и подложкой для матового стекла (Glassmorphism).
 * Создает живую световую среду, сквозь которую полупрозрачные карточки сияют неоном и глубиной.
 */
@Composable
fun EvaAmbientBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val glitchKey by EvaEventManager.glitchKey.collectAsStateWithLifecycle()
    val isSakura = dualityMode == DualityMode.SAKURA

    val baseBackground = if (isSakura) Color(0xFF180E1C) else Color(0xFF140508)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackground)
            .then(if (!isSakura) Modifier.evaGlitch(triggerKey = glitchKey, enabled = true, maxOffsetPx = 4f) else Modifier)
    ) {
        // Аппаратный Canvas с мягкими рассеянными радиальными сферами света (без обрезки)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            if (isSakura) {
                // Сфера 1 (Верхний левый край: акцентный розовый неон Сакуры)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF472B6).copy(alpha = 0.32f),
                            Color(0xFFC084FC).copy(alpha = 0.18f),
                            Color(0xFF818CF8).copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.25f, h * 0.15f),
                        radius = w * 1.1f
                    )
                )

                // Сфера 2 (Правый центр: нежный лавандово-румяный свет)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFB7185).copy(alpha = 0.22f),
                            Color(0xFFE879F9).copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.85f, h * 0.45f),
                        radius = w * 0.95f
                    )
                )

                // Сфера 3 (Нижний левый угол: глубокий аметистовый отсвет)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9333EA).copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.15f, h * 0.85f),
                        radius = w * 0.90f
                    )
                )
            } else {
                // Яндере: Матовая бездна с винным карминовым ядром и тёмным аметистом
                // Сфера 1 (Верхний центр: глубокий винный кармин)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE11D48).copy(alpha = 0.32f),
                            Color(0xFF9F1239).copy(alpha = 0.20f),
                            Color(0xFF4C0519).copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.50f, h * 0.16f),
                        radius = w * 1.1f
                    )
                )

                // Сфера 2 (Нижний правый угол: холодный кроваво-винный импульс)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9F1239).copy(alpha = 0.20f),
                            Color(0xFF2E0812).copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.80f, h * 0.70f),
                        radius = w * 0.90f
                    )
                )
            }

            // Верхняя мягкая неоновая аура по периметру
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        (if (isSakura) Color(0xFFE879F9) else Color(0xFFE11D48)).copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.25f
                )
            )
        }

        // Парящие лепестки в режиме Сакура или тлеющие искры в режиме Яндере
        EvaAmbientParticles(modifier = Modifier.fillMaxSize(), isSakura = isSakura)

        // Контент экрана поверх световой подложки
        content()
    }
}
