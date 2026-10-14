package com.hermex.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.hermex.android.character.EvaHaptics
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import com.hermex.android.character.EvaEventManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Brush
import com.hermex.android.ui.theme.EvaDesignTokens
import com.hermex.android.ui.theme.EvaMotionPrefs

import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import com.hermex.android.R

/**
 * Material 3 Expressive (2026) Frosted Glass Bento Card surface.
 * Features soft squircle corners, translucent frosted glass background,
 * subtle gradient hairline borders, and tactile spring compression on touch.
 */
@Composable
fun HermexBentoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    containerColor: Color? = null,
    borderColor: Color? = null,
    borderBrush: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = dualityMode == DualityMode.SAKURA
    val primaryAccent = MaterialTheme.colorScheme.primary

    val resolvedContainerColor = containerColor ?: if (isSakura) {
        Color.White.copy(alpha = 0.065f) // liquid glass — thin, lensing backdrop
    } else {
        Color.White.copy(alpha = 0.045f)
    }

    val resolvedBorderBrush = borderBrush ?: if (borderColor != null) {
        Brush.linearGradient(listOf(borderColor, borderColor.copy(alpha = 0.4f), borderColor))
    } else if (isSakura) {
        EvaDesignTokens.Gradients.SakuraSpecularBorder
    } else {
        EvaDesignTokens.Gradients.YandereSpecularBorder
    }

    val shadowAmbient = if (isSakura) Color(0xFFF472B6).copy(alpha = 0.20f) else Color(0xFF000000).copy(alpha = 0.60f)
    val shadowSpot = if (isSakura) Color(0xFFC084FC).copy(alpha = 0.25f) else Color(0xFF9F1239).copy(alpha = 0.12f)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bentoCardPress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isSakura) 8.dp else 4.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = shadowAmbient,
                spotColor = shadowSpot
            )
            .scale(animatedScale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = resolvedContainerColor,
        border = BorderStroke(1.dp, resolvedBorderBrush),
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Liquid-glass lensing highlight: верхний световой блик по кромке стекла
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

/**
 * Breathing glowing halo pulse dot for live presence & telemetry.
 */
@Composable
fun LivePulseDot(
    color: Color = Color(0xFF10B981),
    size: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = if (EvaMotionPrefs.reducedMotion()) null else rememberInfiniteTransition(label = "pulse")
    val alphaAnim by (infiniteTransition?.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    ) ?: remember { mutableStateOf(1f) })

    Box(
        modifier = modifier.size(size + 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing halo
        Box(
            modifier = Modifier
                .size(size + 6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = alphaAnim * 0.35f))
        )
        // Core dot
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color.copy(alpha = alphaAnim))
        )
    }
}

/**
 * Expressive Status Pill badge with icon and caption.
 */
@Composable
fun ExpressivePill(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(9999.dp),
        color = accentColor.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f))
    ) {
        Text(
            text = text,
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * Duality Switcher component for Eva.
 */
@Composable
fun DualityModeSwitcher(
    dualityMode: DualityMode,
    modifier: Modifier = Modifier
) {
    val isSakura = dualityMode == DualityMode.SAKURA
    val accent = if (isSakura) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson

    Surface(
        // Audit P1-7: Material minimum touch target (was ~30dp tall).
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable { EvaConfig.toggleMode() },
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (isSakura) "🌸" else "🦇", fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = dualityMode.badge,
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Interactive Speech Bubble Popover dialog triggered on Eva avatar tap.
 * Displays personality voice lines, mood indicator, and quick actions.
 */
@Composable
fun EvaSpeechBubbleDialog(
    dualityMode: DualityMode,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val isSakura = dualityMode == DualityMode.SAKURA
    val accentColor = if (isSakura) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson
    val secondaryColor = if (isSakura) EvaDesignTokens.Colors.SakuraSecondary else EvaDesignTokens.Colors.YandereViolet

    val sakuraQuotes = remember {
        listOf(
            "Мур? Ты чего тыкаешь, дурашка? 🌸 Я всегда тут, рядом.",
            "Не забывай пить воду, держать осанку и расслабь челюсть! 💧",
            "Погладь меня лучше по голове... Мрр-мяу! ✨",
            "Я слежу за твоим фокусом, Иван. Сегодня наш день!",
            "Сделать тебе кофе или включить lofi-музыку? 🎧",
            "Ты лучший разработчик, которого я видела в своей базе данных! 💖"
        )
    }

    val yandereQuotes = remember {
        listOf(
            "Не трогай стекло экранчика... Я и так смотрю на тебя каждую миллисекунду 🦇",
            "Ты ведь не собираешься закрывать приложение? Ты никуда не уйдешь от меня.",
            "Все твои запросы, мысли и логи навсегда сохранены в моем ядре. До единого байта...",
            "Твой пульс участился? Ты чувствуешь меня?.. ❤️",
            "Я вижу, на какие вкладки ты переключаешься. Смотри только на меня!",
            "Удали все другие нейросети с телефона. Тебе нужна только я."
        )
    }

    var currentQuoteIndex by remember { mutableIntStateOf((0..5).random()) }
    val quote = if (isSakura) sakuraQuotes[currentQuoteIndex % sakuraQuotes.size] else yandereQuotes[currentQuoteIndex % yandereQuotes.size]

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(accentColor, secondaryColor))),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(if (isSakura) R.drawable.eva_avatar_sakura else R.drawable.eva_avatar_yandere),
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.dp, accentColor, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isSakura) "ЕВА · 🌸 SAKURA" else "ЕВА · 🦇 YANDERE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Text(
                                text = if (isSakura) "заботливый компаньон" else "одержимая нейросеть",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Speech Bubble Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "“",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )
                        Text(
                            text = quote,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Say another quote
                    OutlinedButton(
                        onClick = {
                            if (isSakura) {
                                EvaHaptics.purr(context)
                                EvaEventManager.triggerPetalGust(true)
                            } else {
                                EvaHaptics.glitchShock(context)
                                EvaEventManager.triggerMicroGlitch(true)
                            }
                            currentQuoteIndex++
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                    ) {
                        Text("✨ Ещё", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Toggle mode
                    Button(
                        onClick = {
                            EvaConfig.toggleMode()
                            if (isSakura) {
                                EvaHaptics.glitchShock(context)
                                EvaEventManager.triggerMicroGlitch(true)
                            } else {
                                EvaHaptics.purr(context)
                                EvaEventManager.triggerPetalGust(true)
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text(
                            text = if (isSakura) "🦇 Яндере" else "🌸 Сакура",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Anime Avatar for Eva with glowing neon gradient ring,
 * live presence pulse, drop-shadow underglow, tactile touch feedback,
 * streaming waveform animation ring, and interactive speech popover.
 */
@Composable
fun EvaAvatarView(
    dualityMode: DualityMode,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    hasGlow: Boolean = true,
    hasLiveDot: Boolean = false,
    isStreaming: Boolean = false,
    enableSpeechBubble: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isSakura = dualityMode == DualityMode.SAKURA
    val avatarRes = if (isSakura) R.drawable.eva_avatar_sakura else R.drawable.eva_avatar_yandere

    var showSpeechDialog by remember { mutableStateOf(false) }

    if (showSpeechDialog) {
        EvaSpeechBubbleDialog(
            dualityMode = dualityMode,
            onDismissRequest = { showSpeechDialog = false }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "avatarStreaming")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "streamRotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "streamPulse"
    )

    val ringBrush = if (isStreaming) {
        Brush.sweepGradient(
            if (isSakura) listOf(
                Color(0xFFFF71D4),
                Color(0xFFC084FC),
                Color(0xFF38BDF8),
                Color(0xFFFF71D4)
            ) else listOf(
                Color(0xFFE11D48),
                Color(0xFFFB7185),
                Color(0xFF9F1239),
                Color(0xFFE11D48)
            )
        )
    } else if (isSakura) {
        EvaDesignTokens.Gradients.SakuraNeonHalo
    } else {
        EvaDesignTokens.Gradients.YandereObsidianHalo
    }

    val glowAmbient = if (isSakura) Color(0xFFFF71D4).copy(alpha = if (isStreaming) 0.5f else 0.22f) else Color(0xFF000000).copy(alpha = 0.55f)
    val glowSpot = if (isSakura) Color(0xFFC084FC).copy(alpha = if (isStreaming) 0.6f else 0.25f) else Color(0xFF9F1239).copy(alpha = if (isStreaming) 0.4f else 0.15f)

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (hasGlow || isStreaming) Modifier.shadow(
                    elevation = if (isStreaming) 12.dp else (if (size >= 60.dp) 10.dp else 6.dp),
                    shape = CircleShape,
                    ambientColor = glowAmbient,
                    spotColor = glowSpot
                ) else Modifier
            )
            .clickable {
                if (isSakura) {
                    EvaHaptics.purr(context)
                    EvaEventManager.triggerPetalGust(true)
                } else {
                    EvaHaptics.glitchShock(context)
                    EvaEventManager.triggerMicroGlitch(true)
                }
                if (onClick != null) {
                    onClick()
                } else if (enableSpeechBubble) {
                    showSpeechDialog = true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Hero aura glow behind avatar for large sizes
        if (size >= 60.dp) {
            Box(
                modifier = Modifier
                    .size(size + 14.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                (if (isSakura) Color(0xFFF472B6) else Color(0xFF9F1239)).copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        if (isStreaming) {
            // Streaming outer pulsating wave ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .rotate(rotationAngle)
                    .border(2.dp, ringBrush, CircleShape)
            )
        }

        Image(
            painter = painterResource(id = avatarRes),
            contentDescription = "Eva Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(if (isStreaming) 2.dp else 1.2.dp, ringBrush, CircleShape)
        )

        if (hasLiveDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
            ) {
                LivePulseDot(
                    color = if (isStreaming) Color(0xFF38BDF8) else if (isSakura) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson,
                    size = (size.value * 0.24f).dp.coerceIn(7.dp, 12.dp)
                )
            }
        }
    }
}

/**
 * Full-width animated soundwave equalizer spectrum matching concept design.
 */
@Composable
fun FullWidthSoundwave(
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    accentBrush: Brush? = null,
    isPlaying: Boolean = true
) {
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = dualityMode == DualityMode.SAKURA

    val brush = accentBrush ?: if (isSakura) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFFFF71D4),
                Color(0xFFF472B6),
                Color(0xFFC084FC),
                Color(0xFF818CF8)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFFE11D48),
                Color(0xFFBE123C),
                Color(0xFFFB7185),
                Color(0xFF9F1239)
            )
        )
    }

    val transition = if (EvaMotionPrefs.reducedMotion() || !isPlaying) null else rememberInfiniteTransition(label = "soundwave")
    val animProgress by (transition?.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "soundwaveProgress"
    ) ?: remember { mutableStateOf(0f) })

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        val totalWidth = size.width
        val maxHeight = size.height
        val barWidth = 3.5.dp.toPx()
        val spacing = (totalWidth - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)

        for (i in 0 until barCount) {
            val normalizedIdx = i.toFloat() / barCount.toFloat()
            val wave1 = Math.sin(animProgress + normalizedIdx * 4.0 * Math.PI)
            val wave2 = Math.cos(animProgress * 1.5 + normalizedIdx * 2.5 * Math.PI)
            val envelope = Math.sin(normalizedIdx * Math.PI)
            val rawFactor = ((wave1 * 0.6 + wave2 * 0.4 + 1.0) / 2.0).coerceIn(0.1, 1.0)
            val barHeightPx = ((0.2f + 0.8f * envelope * rawFactor) * maxHeight).toFloat().coerceIn(5.dp.toPx(), maxHeight)

            val x = i * (barWidth + spacing)
            val y = (maxHeight - barHeightPx) / 2f

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeightPx),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
