package com.hermex.android.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * EvaMotion — Аппаратные 120 Гц модификаторы движения (RenderThread / graphicsLayer).
 * Исключает повторные фазы measure/layout и гарантирует соблюдение 8.33 мс бюджета кадра.
 */

/**
 * Пружинный тактильный отклик при нажатии на карточку/кнопку
 */
@Composable
fun Modifier.evaSpringPress(
    pressed: Boolean,
    targetScale: Float = 0.96f
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) targetScale else 1.0f,
        animationSpec = EvaDesignTokens.Motion.SnappySpring,
        label = "evaSpringPress"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Органичное живое дыхание (для Hero Card, аватара и фокуса присутствия)
 */
@Composable
fun Modifier.evaBreathe(
    enabled: Boolean = true,
    periodMs: Int = 4200,
    minScale: Float = 0.985f,
    maxScale: Float = 1.015f
): Modifier {
    // Audit P1-4: honor the system reduced-motion setting -- render as a static frame.
    if (!enabled || EvaMotionPrefs.reducedMotion()) return this
    val infiniteTransition = rememberInfiniteTransition(label = "evaBreatheTransition")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "evaBreatheScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Плавный перелив прозрачности и подъема без перерасчета геометрии
 */
@Composable
fun Modifier.evaFadeSlide(
    visible: Boolean,
    offsetY: Dp = 16.dp
): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1.0f else 0.0f,
        animationSpec = EvaDesignTokens.Motion.GentleSpring,
        label = "evaFadeSlideAlpha"
    )
    val translation by animateFloatAsState(
        targetValue = if (visible) 0.0f else 1.0f,
        animationSpec = EvaDesignTokens.Motion.GentleSpring,
        label = "evaFadeSlideTrans"
    )
    return this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translation * offsetY.toPx()
    }
}

object EvaMotion {
    // Keep object reference for any existing callers
}
