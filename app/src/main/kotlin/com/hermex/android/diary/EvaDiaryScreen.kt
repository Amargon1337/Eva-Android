package com.hermex.android.diary

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.character.EvaEventManager
import com.hermex.android.character.EvaHaptics
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import com.hermex.android.media.EvaAudioPlayer
import com.hermex.android.ui.components.EvaPetalCanvas
import com.hermex.android.ui.components.ExpressivePill
import com.hermex.android.ui.components.HermexBentoCard
import com.hermex.android.ui.components.LivePulseDot
import com.hermex.android.ui.components.evaGlitch
import com.hermex.android.ui.theme.EvaDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaDiaryScreen(
    viewModel: EvaDiaryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val glitchKey by EvaEventManager.glitchKey.collectAsStateWithLifecycle()

    val primaryAccent = MaterialTheme.colorScheme.primary
    val isSakura = dualityMode == DualityMode.SAKURA
    val screenBg = if (isSakura) Color(0xFF180E1C) else Color(0xFF140508)
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.clickable {
                            EvaHaptics.click(context)
                            EvaConfig.toggleNetworkHost()
                            viewModel.fetchDiary()
                        }
                    ) {
                        Text(
                            text = if (isSakura) "🌸 Мысли Евы" else "🦇 Скрытый протокол Евы",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSakura) Color(0xFF10B981) else Color(0xFFE11D48))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isSakura) "Синхронизировано · Дневник души" else "Скрытый поток мыслей · Телеметрия",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        EvaHaptics.click(context)
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        EvaHaptics.click(context)
                        viewModel.fetchDiary()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = primaryAccent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(paddingValues)
        ) {
            // Ambient Radial Glow matching Home screen
            Box(
                modifier = Modifier
                    .size(420.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-60).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isSakura) Color(0x55F472B6) else Color(0x55E11D48),
                                if (isSakura) Color(0x20E879F9) else Color(0x209F1239),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )


            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .evaGlitch(triggerKey = glitchKey, enabled = !isSakura),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    // pointoni cycle indicator: observe → associate → think → write → ? memory
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.045f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LivePulseDot(
                                    color = if (uiState.autonomyRunning && !uiState.autonomyPaused)
                                        Color(0xFF10B981) else Color(0xFFF59E0B),
                                    size = 7.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    when {
                                        uiState.autonomyPaused -> "цикл на паузе · уступаю тебя"
                                        !uiState.autonomyRunning -> "автономный цикл спит"
                                        else -> "цикл живёт: наблюдаю → ассоциирую → думаю → пишу →? в память"
                                    },
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            uiState.lastSpark?.let {
                                Spacer(Modifier.height(4.dp))
                                Text("последняя искра: $it", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.45f))
                            }
                        }
                    }
                }
                item {
                    // Duality Mood Bento Banner (Frosted Glass)
                    HermexBentoCard(
                        cornerRadius = EvaDesignTokens.Shapes.Hero,
                        borderColor = primaryAccent.copy(alpha = 0.35f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LivePulseDot(color = primaryAccent, size = 8.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "ТЕКУЩЕЕ НАСТРОЕНИЕ",
                                        color = primaryAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (dualityMode == DualityMode.SAKURA) "🌸 Забота & Поддержка" else "🦇 Одержимость & Контроль",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            ExpressivePill(
                                text = dualityMode.badge,
                                accentColor = primaryAccent,
                                onClick = {
                                    EvaHaptics.whisperTick(context)
                                    EvaConfig.toggleMode()
                                    if (!isSakura) {
                                        EvaEventManager.triggerPetalGust(true)
                                    } else {
                                        EvaEventManager.triggerMicroGlitch(true)
                                    }
                                }
                            )
                        }
                    }
                }

                uiState.presence?.let { presence ->
                    item {
                        val isPresenceActive = presence.status == "ACTIVE"
                        val presenceColor = if (isPresenceActive) Color(0xFF10B981) else Color(0xFFF59E0B)

                        HermexBentoCard(
                            cornerRadius = EvaDesignTokens.Shapes.Bento,
                            borderColor = presenceColor.copy(alpha = 0.35f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LivePulseDot(color = presenceColor, size = 10.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = if (isPresenceActive) "ИВАН ЗА ПК" else "ИВАН ОТОШЁЛ",
                                        color = presenceColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Text(
                                    text = "${uiState.totalThoughts} мыслей",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                presence.description,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            presence.activeApp?.let { app ->
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "🖥️ $app",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (uiState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = primaryAccent)
                        }
                    }
                }

                uiState.errorMessage?.let { err ->
                    item {
                        HermexBentoCard(
                            cornerRadius = EvaDesignTokens.Shapes.Card,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                        ) {
                            Text(
                                "Ошибка подключения к студии Евы",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(err, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f), fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        EvaHaptics.click(context)
                                        viewModel.fetchDiary()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryAccent,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Повторить", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        EvaHaptics.click(context)
                                        EvaConfig.toggleNetworkHost()
                                        viewModel.fetchDiary()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Сменить хост", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                if (uiState.entries.isEmpty() && !uiState.isLoading && uiState.errorMessage == null) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("дневник пуст. я думала, но ещё не записала — или ты слишком быстро вернулся.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                }

                items(uiState.entries) { entry ->
                    DiaryEntryBentoCard(
                        entry = entry,
                        accentColor = primaryAccent,
                        onToggleReveal = {
                            EvaHaptics.pop(context)
                            viewModel.toggleReveal(entry.id)
                        },
                        onPlayVoice = {
                            EvaHaptics.click(context)
                            EvaAudioPlayer.play(entry.thought, entry.mood)
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun DiaryEntryBentoCard(
    entry: DiaryEntry,
    accentColor: Color,
    onToggleReveal: () -> Unit,
    onPlayVoice: () -> Unit
) {
    val isPlayingThis = EvaAudioPlayer.isPlaying && EvaAudioPlayer.currentPlayingText == entry.thought
    val isPrivate = entry.isPrivate

    HermexBentoCard(
        cornerRadius = EvaDesignTokens.Shapes.Bento,
        borderColor = if (isPrivate) Color(0xFFC084FC).copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LivePulseDot(
                    color = if (isPrivate) Color(0xFFEC4899) else accentColor,
                    size = 8.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    entry.timestamp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPrivate) {
                    Surface(
                        color = if (entry.revealed) Color(0xFF10B981).copy(alpha = 0.16f) else Color(0xFFEC4899).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (entry.revealed) Color(0xFF10B981).copy(alpha = 0.45f) else Color(0xFFEC4899).copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.clickable { onToggleReveal() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                if (entry.revealed) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (entry.revealed) EvaDesignTokens.Colors.StatusOnline else EvaDesignTokens.Colors.SakuraAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (entry.revealed) "Скрыть спойлер" else "Личное",
                                color = if (entry.revealed) EvaDesignTokens.Colors.StatusOnline else EvaDesignTokens.Colors.SakuraAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onPlayVoice, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.VolumeUp,
                        contentDescription = "Озвучить",
                        tint = if (isPlayingThis) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        entry.activeWindow?.let { win ->
            Spacer(Modifier.height(4.dp))
            Text("🖥️ Иван был в: $win", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(10.dp))

        if (isPrivate && !entry.revealed) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleReveal() },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFC084FC).copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC084FC).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "🔒 Сокровенная мысль Евы (под спойлером)",
                            color = Color(0xFFE9D5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Нажмите, чтобы приоткрыть страницу дневника",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        "Открыть",
                        color = Color(0xFFC084FC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isPrivate) Color(0xFF231238).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isPrivate) Color(0xFFC084FC).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "«${entry.thought}»",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        entry.proactivePrompt?.let { prompt ->
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💬", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        prompt,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
