package com.hermex.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.hermex.android.ui.theme.EvaMotionPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

/**
 * EvaGlitchEffect — Процедурный живой глитч для режима Яндере (2026 Madness Edition).
 * При активном режиме Яндере запускает постоянный периодический эффект безумия:
 * внезапные резкие сдвиги (jitter), микро-повороты, хроматические алые вспышки и горизонтальные сканлайны.
 */
@Composable
fun Modifier.evaGlitch(
    triggerKey: Any? = null,
    enabled: Boolean = true,
    maxOffsetPx: Float = 6f,
    onBurst: (() -> Unit)? = null
): Modifier {
    if (!enabled || EvaMotionPrefs.reducedMotion()) return this

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scaleAnimX = remember { Animatable(1f) }
    val rotationAnim = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }
    var glitchTintAlpha by remember { mutableStateOf(0f) }
    var showScanlines by remember { mutableStateOf(false) }

    // Ручной разовый триггер
    LaunchedEffect(triggerKey) {
        if (triggerKey != null && triggerKey != 0 && triggerKey != Unit) {
            val randX = (Random.nextFloat() * 2f - 1f) * maxOffsetPx
            val randY = (Random.nextFloat() * 2f - 1f) * (maxOffsetPx * 0.5f)

            glitchTintAlpha = 0.15f
            showScanlines = true
            offsetX.snapTo(randX)
            offsetY.snapTo(randY)
            scaleAnimX.snapTo(1.02f)
            rotationAnim.snapTo(Random.nextFloat() * 0.6f - 0.3f)
            alphaAnim.snapTo(0.92f)
            delay(28)

            offsetX.snapTo(-randX * 0.6f)
            offsetY.snapTo(-randY * 0.4f)
            scaleAnimX.snapTo(0.99f)
            rotationAnim.snapTo(-rotationAnim.value * 0.5f)
            delay(24)

            glitchTintAlpha = 0f
            showScanlines = false
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
            scaleAnimX.snapTo(1f)
            rotationAnim.snapTo(0f)
            alphaAnim.snapTo(1f)
        }
    }

    // Постоянный происходящий временный глитч безумия (каждые 2.4 - 4.8 сек)
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        while (isActive) {
            val nextInterval = Random.nextLong(2400, 4800)
            delay(nextInterval)

            val intensity = Random.nextFloat() * 0.5f + 0.7f // 0.7 .. 1.2
            val burstX = (Random.nextFloat() * 2f - 1f) * (maxOffsetPx * 2.2f * intensity)
            val burstY = (Random.nextFloat() * 2f - 1f) * (maxOffsetPx * 0.8f * intensity)

            // Реакция персонажа на всплеск безумия (смена выражения лица и т.п.)
            onBurst?.invoke()

            // Импульс 1: Внезапный скачок в безумие
            glitchTintAlpha = 0.22f * intensity
            showScanlines = true
            offsetX.snapTo(burstX)
            offsetY.snapTo(burstY)
            scaleAnimX.snapTo(1f + 0.025f * intensity)
            rotationAnim.snapTo((Random.nextFloat() * 0.8f - 0.4f) * intensity)
            alphaAnim.snapTo(0.90f)
            delay(32)

            // Импульс 2: Обратный судорожный отскок
            glitchTintAlpha = 0.12f * intensity
            offsetX.snapTo(-burstX * 0.75f)
            offsetY.snapTo(-burstY * 0.5f)
            scaleAnimX.snapTo(1f - 0.015f * intensity)
            rotationAnim.snapTo(-rotationAnim.value * 0.7f)
            delay(28)

            // Импульс 3: Микро-тремор перед затуханием
            if (Random.nextBoolean()) {
                offsetX.snapTo(burstX * 0.3f)
                scaleAnimX.snapTo(1.01f)
                delay(22)
            }

            // Возврат в зловещую тишину
            glitchTintAlpha = 0f
            showScanlines = false
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
            scaleAnimX.snapTo(1f)
            rotationAnim.snapTo(0f)
            alphaAnim.snapTo(1f)
        }
    }

    return this
        .graphicsLayer {
            translationX = offsetX.value
            translationY = offsetY.value
            scaleX = scaleAnimX.value
            rotationZ = rotationAnim.value
            alpha = alphaAnim.value
        }
        .drawWithContent {
            drawContent()
            if (glitchTintAlpha > 0f) {
                drawRect(
                    color = Color(0xFFEF4444).copy(alpha = glitchTintAlpha),
                    size = size
                )
            }
            if (showScanlines) {
                val step = 8f
                var y = 0f
                while (y < size.height) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.25f),
                        topLeft = Offset(0f, y),
                        size = Size(size.width, 2f)
                    )
                    y += step
                }
            }
        }
}

/**
 * Полноэкранный атмосферный оверлей безумия Яндере.
 */
@Composable
fun YandereMadnessOverlay(enabled: Boolean, modifier: Modifier = Modifier) {
    if (!enabled || EvaMotionPrefs.reducedMotion()) return
    Box(modifier = modifier.fillMaxSize().evaGlitch(enabled = true, maxOffsetPx = 8f))
}
