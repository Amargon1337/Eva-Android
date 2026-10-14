package com.hermex.android.systems

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.character.EvaHaptics
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.components.LivePulseDot
import com.hermex.android.ui.theme.EvaDesignTokens
import androidx.compose.ui.platform.LocalContext

/**
 * «Чутьё» — presence-экран pointoni-холода + Fable-механика:
 * созвездие (Ева · телефон · ПК · telegram), честная деградация с кнопками.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PresenceScreen(
    viewModel: PresenceViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isSakura = EvaConfig.dualityMode.value.name == "SAKURA"
    val accent = if (isSakura) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson
    val violet = if (isSakura) EvaDesignTokens.Colors.SakuraLavender else EvaDesignTokens.Colors.YandereCrimson // constellation: no purple in yandere
    val muted = Color.White.copy(alpha = 0.66f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("чутьё", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text(
                            "где ева и что она видит · обновляется сама",
                            fontSize = 11.sp,
                            color = muted,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { EvaHaptics.click(context); onBack() }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { EvaHaptics.click(context); viewModel.refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Обновить", tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 120.dp)
        ) {
            // ── деградационный баннер (Fable): честный + кликабельный ──
            if (!state.pcOnline || state.errorMessage != null) {
                DegradedBanner(
                    pcOnline = state.pcOnline,
                    onRetry = { EvaHaptics.click(context); viewModel.refresh() },
                    onCheckPc = { EvaHaptics.click(context); viewModel.refresh() },
                )
                Spacer(Modifier.height(14.dp))
            }

            // ── созвездие ──
            Constellation(
                accent = violet,
                pcOnline = state.pcOnline,
                phoneOnline = state.phoneOnline,
                tgOnline = state.servicesUp > 0,
                asleep = !state.pcOnline && !state.phoneOnline,
            )
            Spacer(Modifier.height(8.dp))
            state.narrative?.let {
                Text("«$it»", fontSize = 12.5.sp, color = muted, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, lineHeight = 18.sp)
                Spacer(Modifier.height(16.dp))
            }

            // ── состояние евы ──
            SectionTitle("ева · состояние", accent)
            StateCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LivePulseDot(color = if (state.chatActive) Color(0xFFF59E0B) else Color(0xFF10B981))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            state.chatActive -> "говорит с тобой сейчас"
                            state.autonomyPaused -> "тише обычного · фоновое мышление уступает"
                            else -> "бодрствует · думает в фоне"
                        },
                        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White
                    )
                }
                Spacer(Modifier.height(10.dp))
                FlowRowWrap(
                    "память: ${if (state.pcOnline) "полная" else "кэш телефона"}",
                    "сервисы: ${state.servicesUp}/${state.servicesTotal}",
                    "музыка: ${if (state.musicPlaying) "играет" else "тихо"}",
                )
            }

            // ── ты · присутствие ──
            SectionTitle("ты · что я чувствую", accent)
            StateCard {
                val lines = buildList {
                    add(if (state.pcOnline) "отошёл от стола: ${state.pcIdleMinutes ?: 0} мин${state.pcActiveWindow?.let { " · в фокусе «$it»" } ?: ""}"
                        else "ПК не вижу — стол молчит")
                    add(if (state.musicPlaying) "играет «${state.musicTrack ?: "?"}»${if (state.headphones) " · в наушниках" else " · через динамик"}"
                        else "тишина: музыка не играет")
                }
                lines.forEach {
                    Text(it, fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 19.sp)
                }
            }

            // ── тела ──
            SectionTitle("тела, в которых я живу", accent)
            DeviceCard("📱", "телефон", if (state.phoneOnline) "онлайн · ${state.batteryLevel ?: "?"}%${if (state.isCharging) " ⚡" else ""}" else "окно захлопнуто", state.phoneOnline)
            DeviceCard("🖥", "ПК · мозг", if (state.pcOnline) (state.pcStatus?.let { "$it" } ?: "онлайн") else "оффлайн — полная ева ждёт", state.pcOnline)
            DeviceCard("✈️", "telegram · голос", if (state.servicesUp > 0) "канал есть" else "релей молчит", state.servicesUp > 0)

            // ── слепые зоны ──
            SectionTitle("слепые зоны · чего я не чувствую", Color(0xFFF59E0B))
            listOf(
                "камеру и микрофон" to "не подключены к контексту",
                "что ты делал, пока телефон молчал" to "таймстемпы не пишутся в тишине",
            ).forEach { (label, why) ->
                Row(modifier = Modifier.padding(vertical = 5.dp)) {
                    Text("∅ ", color = Color(0xFFF59E0B), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    Text(label, fontSize = 12.5.sp, color = muted,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                        modifier = Modifier.padding(end = 8.dp))
                    Text("· $why", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.45f))
                }
            }
        }
    }
}

@Composable
private fun DegradedBanner(pcOnline: Boolean, onRetry: () -> Unit, onCheckPc: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF59E0B).copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Memory, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (!pcOnline) "бэкенд не отвечает. я сейчас только телефонная: помню из кэша, не паникую."
                    else "что-то из органов молчит. я вижу меньше, чем обычно.",
                    fontSize = 12.5.sp, color = Color.White, lineHeight = 18.sp, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onRetry) { Text("повторить", fontSize = 12.sp, color = Color(0xFFF59E0B)) }
                TextButton(onClick = onCheckPc) { Text("проверить пк", fontSize = 12.sp, color = Color(0xFFF59E0B)) }
            }
        }
    }
}

@Composable
private fun Constellation(accent: Color, pcOnline: Boolean, phoneOnline: Boolean, tgOnline: Boolean, asleep: Boolean) {
    val measurer = rememberTextMeasurer()
    val inf = rememberInfiniteTransition(label = "constel")
    val dash by inf.animateFloat(0f, 30f, animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "dash")
    val breath by inf.animateFloat(1f, 1.08f, animationSpec = infiniteRepeatable(tween(3100), RepeatMode.Reverse), label = "breath")

    Canvas(modifier = Modifier.fillMaxWidth().height(210.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val nodes = listOf(
            Triple(Offset(cx * 0.28f, cy * 0.38f), "📱 телефон", phoneOnline),
            Triple(Offset(cx * 1.72f, cy * 0.34f), "🖥 ПК", pcOnline),
            Triple(Offset(cx, cy * 1.78f), "✈ telegram", tgOnline),
        )
        nodes.forEach { (pos, _, on) ->
            drawLine(
                color = if (on) accent.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.16f),
                start = Offset(cx, cy), end = pos, strokeWidth = 2.5f,
                pathEffect = if (on) PathEffect.dashPathEffect(floatArrayOf(9f, 7f), dash) else PathEffect.dashPathEffect(floatArrayOf(3f, 9f))
            )
        }
        val r = 34f * breath
        drawCircle(accent.copy(alpha = 0.22f), r + 12f, Offset(cx, cy))
        drawCircle(accent.copy(alpha = 0.85f), r, Offset(cx, cy))
        drawText(measurer.measure("Ева", TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)),
            color = Color.White, topLeft = Offset(cx - 24f, cy - 15f))
        nodes.forEach { (pos, label, on) ->
            drawCircle(if (on) Color(0xFF10B981) else Color(0xFF555566), 7f, pos)
            val tl = measurer.measure(label, TextStyle(color = if (on) Color.White else Color(0xFF8B8B9A), fontSize = 11.sp))
            drawText(tl, color = Color.Unspecified, topLeft = Offset(pos.x - tl.size.width / 2f, pos.y + 14f))
        }
        val caption = if (asleep) "нигде · сплю вместе с ПК" else if (pcOnline) "целая · 3/3 узла" else "телефонная версия · 2/3 узла"
        val cl = measurer.measure(caption, TextStyle(color = Color.White.copy(alpha = 0.45f), fontSize = 10.5.sp))
        drawText(cl, color = Color.Unspecified, topLeft = Offset(cx - cl.size.width / 2f, 4f))
    }
}

@Composable
private fun SectionTitle(text: String, accent: Color) {
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
        color = accent, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 4.dp))
}

@Composable
private fun StateCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun FlowRowWrap(vararg chips: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach {
            Text(it, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.06f)).padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun DeviceCard(emoji: String, name: String, status: String, online: Boolean) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = if (online) 0.07f else 0.04f),
        border = androidx.compose.foundation.BorderStroke(1.dp,
            if (online) Color(0xFF10B981).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.07f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                Text(status, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.62f))
            }
            LivePulseDot(color = if (online) Color(0xFF10B981) else Color(0xFF555566))
        }
    }
}
