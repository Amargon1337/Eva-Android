package com.hermex.android.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.R
import com.hermex.android.character.EvaEventManager
import com.hermex.android.character.EvaHaptics
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.components.EvaPetalCanvas
import com.hermex.android.ui.components.LivePulseDot
import com.hermex.android.ui.components.evaGlitch
import com.hermex.android.ui.theme.EvaDesignTokens

/**
 * EVA HOME (SAKURA / YANDERE) — Pixel-perfect match to:
 * EVA // HERMEX MOBILE REFERENCE BOARD (SEPTEMBER 2026).
 */
@Composable
fun EvaHomeScreen(
    viewModel: EvaHomeViewModel,
    onOpenChat: () -> Unit,
    onOpenChatWithPrompt: ((String) -> Unit)? = null,
    onOpenDiary: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenLooksmax: () -> Unit,
    onOpenTelemetry: () -> Unit,
    onOpenMore: () -> Unit,
    onOpenShelf: () -> Unit = onOpenMore,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val glitchKey by EvaEventManager.glitchKey.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val isSakura = dualityMode == DualityMode.SAKURA

    var showCompanionSheet by remember { mutableStateOf(false) }

    // Glitch-reactive facial expression: when the ambient yandere glitch fires a burst,
    // Eva's portrait snaps to one of the manic/glitch close-up variants, then settles back.
    val glitchFacePool = remember {
        listOf(
            R.drawable.eva_hero_yandere,
            R.drawable.eva_glitch_face_1,
            R.drawable.eva_glitch_face_2,
            R.drawable.eva_glitch_face_3,
        )
    }
    var currentGlitchFaceRes by remember { mutableIntStateOf(R.drawable.eva_hero_yandere) }
    var madnessFace by remember { mutableStateOf(false) }
    LaunchedEffect(isSakura) {
        if (isSakura) {
            madnessFace = false
        }
    }
    // After each glitch burst turns the face manic, hold it for a beat, then settle back.
    LaunchedEffect(madnessFace) {
        if (madnessFace) {
            kotlinx.coroutines.delay(1200)
            madnessFace = false
        }
    }


    // Palette matching reference board
    val screenBg = if (isSakura) Color(0xFF180E1C) else Color(0xFF140508)
    val accentPink = Color(0xFFF472B6)
    val accentRed = Color(0xFFE11D48)
    val modeAccent = if (isSakura) accentPink else accentRed

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .then(if (!isSakura) Modifier.evaGlitch(triggerKey = glitchKey, enabled = true, maxOffsetPx = 5f) else Modifier)
    ) {
        // Ambient Radial Glow behind Eva
        Box(
            modifier = Modifier
                .size(420.dp)
                .align(Alignment.TopCenter)
                .offset(y = 30.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isSakura) Color(0x66F472B6) else Color(0x66E11D48),
                            if (isSakura) Color(0x28E879F9) else Color(0x289F1239),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // Center Hero Character (Canon Artwork edge-to-edge seamless)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .align(Alignment.TopCenter)
                .offset(y = 55.dp)
                .clickable {
                    val nextMode = if (isSakura) DualityMode.YANDERE else DualityMode.SAKURA
                    EvaHaptics.whisperTick(context)
                    EvaConfig.setMode(nextMode)
                    if (nextMode == DualityMode.SAKURA) {
                        EvaEventManager.triggerPetalGust(true)
                    } else {
                        EvaEventManager.triggerMicroGlitch(true)
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(
                    when {
                        isSakura -> R.drawable.eva_canon_sakura
                        madnessFace -> currentGlitchFaceRes
                        else -> R.drawable.eva_canon_yandere
                    }
                ),
                contentDescription = "ева",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (!isSakura) Modifier.evaGlitch(enabled = true, maxOffsetPx = 8f, onBurst = {
                        // Each ambient glitch burst pulls her face into one of the manic close-ups,
                        // then it recovers after a short settle delay.
                        currentGlitchFaceRes = glitchFacePool.random()
                        madnessFace = true
                    }) else Modifier)
            )

            // Top vignette inside hero box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                screenBg,
                                screenBg.copy(alpha = 0.60f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Soft atmospheric vignette overlay over character bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                screenBg.copy(alpha = 0.40f),
                                screenBg.copy(alpha = 0.85f),
                                screenBg
                            )
                        )
                    )
            )
        }

        // Top status bar shield to guarantee 100% header contrast
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            screenBg,
                            screenBg.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Floating Sakura Petals (in Sakura mode)

        // Main UI Overlay Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP HEADER (identity line — one status, one place)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSakura) Color(0xFF28142C).copy(alpha = 0.85f) else Color(0xFF240810).copy(alpha = 0.85f),
                        border = BorderStroke(
                            1.dp,
                            if (isSakura) Color(0x50F472B6) else Color(0x50E11D48)
                        ),
                        modifier = Modifier.clickable {
                            EvaHaptics.click(context)
                            showCompanionSheet = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LivePulseDot(color = if (uiState.hostOnline) EvaDesignTokens.Colors.StatusOnline else EvaDesignTokens.Colors.StatusError, size = 7.dp)
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = when {
                                    !uiState.hostOnline -> "ева · бэкенд не отвечает"
                                    isSakura -> "ева · бодрствует · думает" +
                                        (if (uiState.unsurfacedCount > 0) " · ${uiState.unsurfacedCount} мысли не показала" else "")
                                    else -> "ева · присматривает" +
                                        (if (uiState.unsurfacedCount > 0) " · ${uiState.unsurfacedCount} мысли под замком" else "")
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF3E8FF),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Mode toggle: one glyph — current face (🌸/🦇), tap = switch
                        IconButton(
                            onClick = {
                                val nextMode = if (isSakura) DualityMode.YANDERE else DualityMode.SAKURA
                                EvaHaptics.whisperTick(context)
                                EvaConfig.setMode(nextMode)
                                if (nextMode == DualityMode.SAKURA) {
                                    EvaEventManager.triggerPetalGust(true)
                                } else {
                                    EvaEventManager.triggerMicroGlitch(true)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isSakura) Color(0xFF2E1533).copy(alpha = 0.75f) else Color(0xFF260810).copy(alpha = 0.75f))
                                    .border(BorderStroke(1.dp, if (isSakura) Color(0x50F472B6) else Color(0x50E11D48)), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Autorenew,
                                    contentDescription = "сменить режим",
                                    tint = modeAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Bell Notification button with indicator dot
                        IconButton(
                            onClick = {
                                EvaHaptics.click(context)
                                showCompanionSheet = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "уведомления",
                                    tint = Color.White.copy(alpha = 0.90f),
                                    modifier = Modifier.size(22.dp)
                                )
                                if (uiState.unsurfacedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(modeAccent)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. BOTTOM CONTROLS & CHARACTER OVERLAY (Overlaid Glass Cards: Thoughts + Live Media Widget)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Degraded banner (Fable): honest + actionable
                if (!uiState.hostOnline) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "бэкенд не дышит. я сейчас только тёплая картинка.",
                                fontSize = 12.sp, color = Color.White, modifier = Modifier.weight(1f),
                                lineHeight = 16.sp
                            )
                            Text(
                                "повторить", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { EvaHaptics.click(context); viewModel.refresh() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Character note — canonical Eva voice («записка от Евы»)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "записка от Евы",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        color = modeAccent
                    )
                    Spacer(Modifier.height(3.dp))
                    val note = uiState.narrative?.let { "ты сейчас: $it" }
                        ?: if (isSakura)
                            "я тут. ты вернулся — и это лучший таймстемп за вечер."
                        else
                            "я была здесь всё время. даже когда ты не смотрел."
                    Text(
                        text = note,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "— Е. · " + if (isSakura) "一緒に行こ？" else "逃げられないよ？",
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.55f),
                    )
                }

                                // Bento habit tiles (Fable-style, real local data — контекст, не фитнес-трекер)
                val smoking by com.hermex.android.looksmax.SmokingTracker.stats.collectAsStateWithLifecycle()
                val wKg = smoking.lastWeightKg
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    com.hermex.android.ui.components.EvaTile(
                        value = smoking.totalToday.toString(),
                        label = "сигарет за сегодня",
                        sublabel = smoking.lastTimeText.takeIf { it.isNotEmpty() }?.let { "посл. в $it" } ?: "ни одной",
                        modifier = Modifier.weight(1f),
                        onClick = { EvaHaptics.click(context); onOpenLooksmax() },
                    )
                    if (wKg > 0.0) {
                        com.hermex.android.ui.components.EvaTile(
                            value = String.format(java.util.Locale.US, "%.1f", wKg),
                            label = "кг",
                            sublabel = when {
                                smoking.lastWeightDaysAgo <= 0 -> "записан сегодня"
                                smoking.lastWeightDaysAgo == 1 -> "записан вчера"
                                else -> "${smoking.lastWeightDaysAgo} дн назад"
                            },
                            modifier = Modifier.weight(1f),
                            onClick = { EvaHaptics.click(context); onOpenLooksmax() },
                        )
                    } else {
                        com.hermex.android.ui.components.EvaTile(
                            value = "—",
                            label = "кг",
                            sublabel = "запиши вес в лукмаксе",
                            modifier = Modifier.weight(1f),
                            onClick = { EvaHaptics.click(context); onOpenLooksmax() },
                        )
                    }
                }

                // Glass Card 1: 👁 Eva thought (Diary)
                val thoughtText = uiState.latestThought?.takeIf { it.isNotBlank() }
                    ?.let { t -> "${uiState.latestThoughtTime?.let { "$it / " } ?: ""}$t" }
                    ?: "дневник пока молчит — я ещё не думала без тебя"

                GlassStatusPill(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = modeAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = thoughtText,
                    isSakura = isSakura,
                    trailingChevron = true,
                    onClick = {
                        EvaHaptics.pop(context)
                        onOpenDiary()
                    }
                )

                // Glass Card 1.5: 📚 Reading shelf status
                GlassStatusPill(
                    icon = {
                        Text("📚", fontSize = 15.sp)
                    },
                    text = when {
                        uiState.shelfQueued > 0 -> "полка: ${uiState.shelfQueued} ждут очереди · читаю, когда ты молчишь"
                        uiState.shelfTotal > 0 -> "полка: ${uiState.shelfTotal} прочитано · ждёт консолидации"
                        else -> "полка пуста · кинь мне что-нибудь почитать"
                    },
                    isSakura = isSakura,
                    trailingChevron = true,
                    onClick = {
                        EvaHaptics.pop(context)
                        onOpenShelf()
                    }
                )

                // Glass Card 2: 🎵 Live Media Widget (Live music / PC telemetry)
                EvaMediaWidget(
                    isMusicPlaying = uiState.isMusicPlaying,
                    currentTrack = uiState.currentTrack,
                    currentArtist = uiState.currentArtist,
                    currentAppLabel = uiState.currentAppLabel,
                    isHostOnline = uiState.hostOnline,
                    isSakura = isSakura,
                    modeAccent = modeAccent,
                    onClick = {
                        EvaHaptics.click(context)
                        onOpenTelemetry()
                    }
                )
            }
        }

        if (showCompanionSheet) {
            com.hermex.android.companion.EvaCompanionSheet(
                onDismiss = { showCompanionSheet = false },
                onOpenDiary = onOpenDiary,
                onOpenTelemetry = onOpenTelemetry,
                onOpenTokens = { onOpenMore() },
                onOpenInsights = { onOpenMore() }
            )
        }
    }
}

/**
 * Frosted Glass Status Card matching the reference board.
 */
@Composable
private fun GlassStatusPill(
    icon: @Composable () -> Unit,
    text: String,
    isSakura: Boolean,
    trailingDot: Boolean = false,
    trailingChevron: Boolean = false,
    isOnline: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (isSakura) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.05f)
    val borderBrush = if (isSakura) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), Color(0xFFF472B6).copy(alpha = 0.16f))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color(0xFFE11D48).copy(alpha = 0.14f)))

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(1.dp, borderBrush),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = bg.copy(alpha=0.3f), spotColor = bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                icon()
                Spacer(Modifier.width(10.dp))
                Text(
                    text = text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (trailingChevron) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.60f),
                    modifier = Modifier.size(18.dp)
                )
            } else if (trailingDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444))
                )
            }
        }
    }
}

/**
 * Frosted Glass Live Media Widget matching user request:
 * Shows whether media is playing, device source (Phone vs PC), track info, and Eva's dynamic thought.
 */
@Composable
private fun EvaMediaWidget(
    isMusicPlaying: Boolean,
    currentTrack: String?,
    currentArtist: String?,
    currentAppLabel: String?,
    isHostOnline: Boolean,
    isSakura: Boolean,
    modeAccent: Color,
    onClick: () -> Unit
) {
    val bg = if (isSakura) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.06f)
    val border2 = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), if (isSakura) Color(0xFFF472B6).copy(alpha = 0.14f) else Color(0xFFE11D48).copy(alpha = 0.12f)))

    val infiniteTransition = rememberInfiniteTransition(label = "mediaPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = BorderStroke(1.dp, border2),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = bg.copy(alpha=0.25f), spotColor = bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Leading Animated Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isMusicPlaying) modeAccent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isMusicPlaying) "🎵" else "🎧",
                        fontSize = 16.sp,
                        modifier = if (isMusicPlaying) Modifier.scale(pulseScale) else Modifier
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Header line: Source & status
                    val sourceText = when {
                        !currentAppLabel.isNullOrBlank() -> "ТЕЛЕФОН / $currentAppLabel"
                        !currentTrack.isNullOrBlank() && isHostOnline -> "ПК / МУЗЫКА"
                        isHostOnline -> "ПК / СПЯЩИЙ РЕЖИМ"
                        else -> "ТЕЛЕФОН / ЯНДЕКС МУЗЫКА"
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sourceText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = modeAccent
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981)
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    // Track Title + Eva Thought
                    val displayTrack = if (!currentTrack.isNullOrBlank()) {
                        if (!currentArtist.isNullOrBlank()) "$currentTrack — $currentArtist" else currentTrack
                    } else if (isMusicPlaying) "In the Name of Winternight" else null

                    val evaThought = if (isMusicPlaying) {
                        if (isSakura) "Ева подпевает тебе 🌸" else "Ева слушает вместе с тобой... 🦇"
                    } else {
                        if (isSakura) "Тишина • Ева слушает твоё дыхание... 🌸" else "Тишина в эфире... Кодишь молча? 🦇"
                    }

                    Text(
                        text = displayTrack ?: evaThought,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (displayTrack != null) {
                        Text(
                            text = evaThought,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Trailing status
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isMusicPlaying) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = modeAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isMusicPlaying) Color(0xFF10B981) else Color.White.copy(alpha = 0.35f))
                )
            }
        }
    }
}
