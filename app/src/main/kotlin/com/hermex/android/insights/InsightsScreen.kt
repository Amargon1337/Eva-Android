package com.hermex.android.insights

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import android.os.Build
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.character.EvaHaptics
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.theme.EvaDesignTokens
import com.hermex.android.ui.components.EvaGlassCard
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    onBack: () -> Unit,
    onOpenCategory: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val duality by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = duality == DualityMode.SAKURA
    val accent = if (isSakura) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("инсайты", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White, fontFamily = FontFamily.Serif)
                        Text("что я поняла за последние 30 дней", fontSize = 11.sp, color = Color.White.copy(alpha = 0.62f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { EvaHaptics.click(context); onBack() }) {
                        Icon(Icons.Outlined.ArrowBack, "Назад", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { padding ->
        Box(Modifier.fillMaxSize().background(Color.Transparent)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = padding.calculateTopPadding()),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { Spacer(Modifier.height(2.dp)) }
                if (uiState.isLoading) {
                    item { Text("думаю о нас…", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, fontStyle = FontStyle.Italic) }
                }
                uiState.errorMessage?.let {
                    item { Text("студия молчит: $it", color = Color(0xFFF59E0B), fontSize = 12.sp) }
                }

                // hero — modern: huge number + caption + glass
                item {
                    // hero — glass
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.045f),
                        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), accent.copy(alpha = 0.22f)))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.Bottom) {
                            Text("${uiState.totalMemories}", fontSize = 46.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace, lineHeight = 46.sp)
                            androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
                            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                                Text("фактов", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 14.sp)
                                Text("с датой и источником · 30 дней", fontSize = 11.sp, color = Color.White.copy(alpha = 0.62f), lineHeight = 14.sp)
                            }
                            Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
                        }
                    }
                }

                if (uiState.byCategory.isNotEmpty()) {
                    item { SectionLabel("карта памяти", accent) }
                    item { Card { uiState.byCategory.take(8).forEach { CategoryBar(it.category, it.count, uiState.totalMemories.coerceAtLeast(1), accent, onClick = { onOpenCategory(it.category) }) } } }
                }

                if (uiState.recent.isNotEmpty()) {
                    item { SectionLabel("что запомнила недавно", accent) }
                    items(uiState.recent.take(10), key = { it.id }) { r ->
                        Card {
                            Row(verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text("«${r.content}»", fontSize = 12.5.sp, color = Color.White, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(r.category, fontSize = 10.sp, color = accent)
                                        Text(r.importance.lowercase(), fontSize = 10.sp, color = Color.White.copy(alpha = 0.45f))
                                        Text(r.createdAt, fontSize = 10.sp, color = Color.White.copy(alpha = 0.35f), fontFamily = FontFamily.Monospace)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { viewModel.deleteMemory(r.id) }, modifier = Modifier.size(28.dp)) {
                                    Text("×", color = Color.White.copy(alpha = 0.35f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (uiState.doubts.isNotEmpty()) {
                    item { SectionLabel("мои сомнения · confidence < 85%", Color(0xFFF59E0B)) }
                    items(uiState.doubts, key = { it.id }) { d ->
                        Card {
                            Row(verticalAlignment = Alignment.Top) {
                                Box(Modifier.width(3.dp).height(40.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(d.content, fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.9f), lineHeight = 18.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                    Text("верю на ${(d.confidence * 100).toInt()}% · ${d.status.lowercase()}", fontSize = 10.5.sp, color = Color(0xFFF59E0B))
                                }
                                IconButton(onClick = { viewModel.deleteMemory(d.id) }, modifier = Modifier.size(28.dp)) {
                                    Text("×", color = Color.White.copy(alpha = 0.35f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (uiState.paradoxes.isNotEmpty()) {
                    item { SectionLabel("парадоксы человека", Color(0xFFFB7185)) }
                    items(uiState.paradoxes, key = { it.id }) { p ->
                        Card {
                            Row(verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text("«${p.hypothesis}»", fontSize = 12.5.sp, color = Color.White, fontStyle = FontStyle.Italic, lineHeight = 18.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(4.dp))
                                    Text("причина: ${p.reason.ifEmpty { "не назвал(а)" }} · ${(p.confidence * 100).toInt()}%", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.5f))
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { viewModel.deleteMemory(p.id) }, modifier = Modifier.size(28.dp)) {
                                    Text("×", color = Color.White.copy(alpha = 0.35f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (uiState.thoughtsByDay.isNotEmpty()) {
                    item { SectionLabel("ритм моей головы (30 дней)", accent) }
                    item {
                        Card {
                            val maxC = (uiState.thoughtsByDay.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
                            Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                uiState.thoughtsByDay.takeLast(21).forEachIndexed { i, d ->
                                    val h = (d.count.toFloat() / maxC)
                                    Box(
                                        modifier = Modifier.weight(1f)
                                            .fillMaxHeight(if (h < 0.08f) 0.08f else h)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (i == uiState.thoughtsByDay.takeLast(21).lastIndex) accent else accent.copy(alpha = 0.28f))
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("${uiState.thoughtsByDay.sumOf { it.count }} мыслей · вайбы: ${uiState.moodMix.joinToString(" · ") { "${it.category} ${it.count}" }}",
                                fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }

                if (uiState.weight.isNotEmpty()) {
                    item { SectionLabel("вес (последние замеры)", Color(0xFF38BDF8)) }
                    item {
                        Card {
                            Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val kgs = uiState.weight.takeLast(14).map { it.kg }
                                val lo = (kgs.minOrNull() ?: 0.0) - 0.5
                                val hi = (kgs.maxOrNull() ?: 1.0) + 0.5
                                kgs.forEachIndexed { i, kg ->
                                    val h = ((kg - lo) / (hi - lo).coerceAtLeast(0.1)).toFloat()
                                    Box(Modifier.weight(1f).fillMaxHeight(h.coerceIn(0.12f, 1f)).clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF38BDF8).copy(alpha = if (i == kgs.lastIndex) 1f else 0.35f)))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("${uiState.weight.last().kg} кг · сегодня", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }

                uiState.looksmax?.let { lm ->
                    item { SectionLabel("looksmax · средний могг", Color(0xFFF472B6)) }
                    item {
                        Card {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${String.format("%.1f", lm.avg)}%", fontSize = 30.sp, fontWeight = FontWeight.Bold,
                                        color = Color.White, fontFamily = FontFamily.Monospace)
                                    Text("среднее по ${lm.count} фото. разброс ${lm.worst}–${lm.best}%.",
                                        fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.62f), lineHeight = 16.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("сейчас", fontSize = 9.5.sp, color = Color.White.copy(alpha = 0.45f))
                                    Text("${lm.last}%", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF472B6), fontFamily = FontFamily.Monospace)
                                    lm.trend?.let {
                                        Text((if (it >= 0) "▲ +" else "▼ ") + String.format("%.1f", it), fontSize = 11.sp,
                                            color = if (it >= 0) Color(0xFF34D399) else Color(0xFFF59E0B), fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("ревью — это мой взгляд по фото, не приговор. отёк утром — это просто утро.",
                                fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.42f), fontStyle = FontStyle.Italic)
                        }
                    }
                }

                if (uiState.memes.isNotEmpty()) {
                    item { SectionLabel("наш лор", Color(0xFF34D399)) }
                    items(uiState.memes) { m ->
                        Card {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(m.title, fontSize = 12.5.sp, color = Color.White, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("×${m.timesUsed}", fontSize = 12.sp, color = Color(0xFF34D399), fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(text.uppercase(), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = color,
        modifier = Modifier.padding(top = 6.dp, start = 4.dp))
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.055f),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.05f)))),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // liquid-glass lensing highlight
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.02f), Color.Transparent)
                        )
                    )
            )
            Column(Modifier.padding(14.dp), content = content)
        }
    }
}

@Composable
private fun CategoryBar(category: String, count: Int, total: Int, accent: Color, onClick: () -> Unit = {}) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "categoryBarPress"
    )
    Surface(
        shape = RoundedCornerShape(9999.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.88f), modifier = Modifier.width(112.dp))
            BoxWithConstraints(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f))) {
                Box(Modifier.fillMaxWidth(count.toFloat() / total.coerceAtLeast(1)).height(6.dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.45f), accent))))
            }
            Spacer(Modifier.width(10.dp))
            Text("$count", fontSize = 12.sp, color = Color.White.copy(alpha = 0.65f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text("›", fontSize = 14.sp, color = accent.copy(alpha = 0.7f))
        }
    }
}
