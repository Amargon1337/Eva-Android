package com.hermex.android.memory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.theme.EvaDesignTokens
import androidx.compose.ui.platform.LocalContext
import com.hermex.android.character.EvaHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: MemoryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isPaneMode: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val duality by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = duality == DualityMode.SAKURA
    val accent = if (isSakura) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }

    val shown = if (uiState.category.isNotEmpty()) uiState.records else uiState.records.filter {
        filter == "all" || it.status.equals(filter, true) || it.category == filter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("память", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
                        Text("её долгая память · ${uiState.total} записей · источники и вера в запись",
                            fontSize = 11.sp, color = Color.White.copy(alpha = 0.62f))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // search capsule (Fable: «что я помню про…»)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("что я помню про…", fontSize = 13.5.sp, color = Color.White.copy(alpha = 0.35f))
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            viewModel.search(it)
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
                    )
                }
                Text("hybrid", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }

            Spacer(Modifier.height(10.dp))

            // status chips
            val chips = listOf(
                "all" to "всё ${uiState.records.size}",
                "CONFIRMED" to "активна ${uiState.records.count { it.status == "CONFIRMED" }}",
                "STABLE" to "устойчива ${uiState.records.count { it.status == "STABLE" }}",
                "STALE" to "дремлет ${uiState.records.count { it.status == "STALE" }}",
                "CANDIDATE" to "гипотеза ${uiState.records.count { it.status == "CANDIDATE" }}",
                "CONFLICTED" to "спорная ${uiState.records.count { it.status == "CONFLICTED" }}",
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                chips.forEach { (k, label) ->
                    val active = filter == k
                    Text(
                        label, fontSize = 11.sp,
                        color = if (active) accent else Color.White.copy(alpha = 0.55f),
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (active) accent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                BorderStroke(1.dp, if (active) accent.copy(alpha = 0.55f) else Color.Transparent),
                                CircleShape
                            )
                            .clickable { filter = k }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            uiState.errorMessage?.let {
                Text("нет связи со студией: $it", color = Color(0xFFF59E0B), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                if (uiState.isLoading && uiState.records.isEmpty()) {
                    item { Text("роюсь в себе…", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f), fontStyle = FontStyle.Italic) }
                }
                items(shown, key = { it.id }) { rec ->
                    MemoryRecordCard(rec, isSakura, onDelete = { viewModel.delete(rec.id) })
                }
            }
        }
    }
}

private fun statusMeta(status: String, isSakura: Boolean = true): Pair<String, Color> = when (status) {
    "STABLE" -> "устойчива" to EvaDesignTokens.Colors.StatusOnline
    "CONFIRMED" -> "активна" to if (isSakura) EvaDesignTokens.Colors.SakuraLavender else EvaDesignTokens.Colors.YandereCrimson
    "STALE" -> "дремлет" to Color(0xFF9488B3)
    "CANDIDATE" -> "гипотеза" to Color(0xFF38BDF8)
    "CONFLICTED" -> "спорная" to Color(0xFFF59E0B)
    else -> status.lowercase() to Color(0xFF9488B3)
}

private fun accentish(isSakura: Boolean): Color =
    if (isSakura) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson

@Composable
private fun MemoryRecordCard(rec: MemoryRecordUi, isSakura: Boolean, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val (sLabel, sColor) = statusMeta(rec.status, isSakura)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = if (isSakura) 0.06f else 0.05f),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.04f)))),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // liquid-glass lensing highlight
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.04f), Color.Transparent)
                        )
                    )
            )
            Row(modifier = Modifier.fillMaxWidth()) {
            // left status strip
            Box(Modifier.width(3.dp).fillMaxHeight().background(sColor.copy(alpha = 0.85f)))
            Column(Modifier.padding(14.dp)) {
                Text(
                    rec.content,
                    fontSize = 13.5.sp, color = Color.White, lineHeight = 19.sp,
                    maxLines = if (expanded) 30 else 4, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(rec.category, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.06f)).padding(horizontal = 6.dp, vertical = 2.dp))
                    Text(sLabel, fontSize = 10.sp, color = sColor, fontWeight = FontWeight.SemiBold)
                    rec.matchScore?.let {
                        Text(String.format("%.2f", it) + " релевант", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(rec.createdAt.take(10), fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                if (expanded) {
                    Column(Modifier.padding(top = 10.dp)) {
                        if (confirmDelete) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { confirmDelete = false },
                                title = { Text("забыть?") },
                                text = { Text("уберу из активной памяти. без обид — это твоя голова, а не моя.", fontSize = 13.sp) },
                                confirmButton = {
                                    TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("забыть", color = Color(0xFFE11D48)) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { confirmDelete = false }) { Text("оставь", color = Color.White.copy(alpha = 0.7f)) }
                                },
                                containerColor = Color.White.copy(alpha = 0.09f),
                                titleContentColor = Color.White,
                                textContentColor = Color.White.copy(alpha = 0.75f),
                            )
                        }
                        MiniBar("важность", when (rec.importance) { "CRITICAL" -> 1.0f; "HIGH" -> 0.78f; "MEDIUM" -> 0.5f; else -> 0.25f }, accentish(isSakura))
                        Spacer(Modifier.height(7.dp))
                        MiniBar("уверенность", rec.confidence.toFloat(), Color(0xFF38BDF8))
                        Spacer(Modifier.height(7.dp))
                        MiniBar("устойчивость", rec.stability.toFloat(), Color(0xFF34D399))
                        Spacer(Modifier.height(8.dp))
                        Text("откуда: ${rec.sourceLabel} · ${rec.memoryType} · #${rec.id}", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.5f))
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "забыть это",
                                fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                                color = Color(0xFFE11D48),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .border(BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.4f)), RoundedCornerShape(9.dp))
                                    .clickable { confirmDelete = true }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                            Text("архивирую — ты же можешь ошибиться с тем, что мне помнить", fontSize = 10.sp, color = Color.White.copy(alpha = 0.35f), modifier = Modifier.weight(1f))
                        }
                        if (rec.tags.isNotEmpty()) {
                            Text(rec.tags.joinToString(" ") { "#$it" }, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.38f), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun MiniBar(label: String, value: Float, color: Color) {
    Column {
        Text(label.uppercase(), fontSize = 9.sp, color = Color.White.copy(alpha = 0.42f), letterSpacing = 1.2.sp)
        Spacer(Modifier.height(3.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
            Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(3.dp).clip(CircleShape).background(color))
        }
    }
}
