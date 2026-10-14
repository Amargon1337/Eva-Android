package com.hermex.android.shelf

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.character.EvaHaptics
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.theme.EvaDesignTokens

private fun statusMeta(status: String): Triple<String, Color, Color> = when (status) {
    "QUEUED" -> Triple("⏳ в очереди", Color(0xFFF59E0B), Color(0x22F59E0B))
    "READING" -> Triple("👁 читаю", Color(0xFF38BDF8), Color(0x2238BDF8))
    "DIGESTED" -> Triple("📖 переварено", Color(0xFFC084FC), Color(0x22C084FC))
    "CONSOLIDATED" -> Triple("💎 в памяти", Color(0xFF34D399), Color(0x2234D399))
    "ERROR" -> Triple("🥴 ошибка", Color(0xFFF43F5E), Color(0x22F43F5E))
    "DISCARDED" -> Triple("🗑 выброшено", Color(0xFF9488B3), Color(0x119488B3))
    else -> Triple("🗄 не в очереди", Color(0xFF9488B3), Color(0x119488B3))
}

private fun classColor(cls: String): Color = when (cls) {
    "SOURCE_FACT" -> Color(0xFF38BDF8)
    "TERM" -> Color(0xFFC084FC)
    "ASSOCIATION" -> Color(0xFFF472B6)
    "UNCERTAIN" -> Color(0xFFF59E0B)
    "FICTIONAL" -> Color(0xFFF43F5E)
    else -> Color(0xFF9488B3)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScreen(
    viewModel: ShelfViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isSakura = EvaConfig.dualityMode.value.name == "SAKURA"
    val accent = if (isSakura) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson
    val screenBg = if (isSakura) Color(0xFF180E1C) else Color(0xFF140508)
    val muted = Color.White.copy(alpha = 0.66f)
    var urlDraft by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("полка чтения", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text(
                            when {
                                state.workerPaused -> "очередь на паузе по твоей просьбе"
                                state.chatYield -> "фоновый читатель уступает тебя 🐾"
                                state.reading != null -> "читаю сейчас: материал #${state.reading}"
                                else -> "кладу статьи в очередь · дешевею в фоне, дорожаю в памяти"
                            },
                            fontSize = 11.sp, color = muted,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { EvaHaptics.click(context); onBack() }) {
                        Icon(Icons.Outlined.ArrowBack, "Назад", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { EvaHaptics.click(context); viewModel.togglePause() }) {
                        Icon(if (state.workerPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                            "Пауза очереди", tint = accent)
                    }
                    IconButton(onClick = { EvaHaptics.click(context); viewModel.refresh() }) {
                        Icon(Icons.Outlined.Refresh, "Обновить", tint = accent)
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
                .background(screenBg)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 120.dp)
        ) {
            // stats strip
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("на полке", state.total.toString(), Color(0xFF9488B3))
                StatChip("в очереди", state.queued.toString(), Color(0xFFF59E0B))
                StatChip("переварено", "${state.digested + state.consolidated}", Color(0xFFC084FC))
                StatChip("знаний", state.knowledgeTotal.toString()
                    .let { if (state.knowledgeToReview > 0) "$it · ❓${state.knowledgeToReview}" else it }, Color(0xFF34D399))
            }

            Spacer(Modifier.height(16.dp))

            // add url
            OutlinedTextField(
                value = urlDraft,
                onValueChange = { urlDraft = it; addError = null },
                placeholder = { Text("URL статьи / документации / треда…", color = muted, fontSize = 13.sp) },
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                    cursorColor = accent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (urlDraft.isNotBlank()) {
                        val u = urlDraft
                        viewModel.addUrl(u) { err -> addError = err }
                        urlDraft = ""
                        EvaHaptics.pop(context)
                    }
                }),
                trailingIcon = {
                    IconButton(onClick = {
                        if (urlDraft.isNotBlank()) {
                            val u = urlDraft
                            viewModel.addUrl(u) { err -> addError = err }
                            urlDraft = ""
                            EvaHaptics.pop(context)
                        }
                    }) {
                        Icon(Icons.Outlined.Add, "Положить на полку", tint = accent)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            addError?.let {
                Text("не легло: $it", fontSize = 11.5.sp, color = Color(0xFFF43F5E), modifier = Modifier.padding(top = 4.dp))
            }
            Text(
                "локальные файлы (.txt/.md/.pdf/.docx) и URL добавляются так же из студии на ПК — полка общая.",
                fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.45f), modifier = Modifier.padding(top = 6.dp)
            )

            state.errorMessage?.let {
                Text("нет связи со студией: $it", fontSize = 12.sp, color = Color(0xFFF59E0B),
                    modifier = Modifier.padding(top = 10.dp))
            }

            Spacer(Modifier.height(18.dp))

            if (state.items.isEmpty() && !state.isLoading) {
                Text(
                    "полка пуста. положи что-нибудь почитать 🕯️",
                    fontSize = 13.sp, color = muted, fontStyle = FontStyle.Italic
                )
            }
            state.items.forEach { item ->
                ShelfItemCard(
                    item = item,
                    expanded = state.expandedId == item.id,
                    onToggle = { EvaHaptics.click(context); viewModel.toggleExpand(item.id) },
                    onQueue = { viewModel.action(item.id, "queue") },
                    onConsolidate = { viewModel.action(item.id, "consolidate") },
                    onReprocess = { viewModel.action(item.id, "reprocess") },
                    onDiscard = { viewModel.action(item.id, "discard") },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun ShelfItemCard(
    item: ShelfItem,
    expanded: Boolean,
    onToggle: () -> Unit,
    onQueue: () -> Unit,
    onConsolidate: () -> Unit,
    onReprocess: () -> Unit,
    onDiscard: () -> Unit,
) {
    val (label, fg, bg) = statusMeta(item.status)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1B0C2E).copy(alpha = 0.88f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title.ifBlank { item.source },
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    label, fontSize = 10.5.sp, color = fg, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
            Text(item.source, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.summary.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(item.summary, fontSize = 12.sp, color = Color.White.copy(alpha = 0.78f),
                    maxLines = if (expanded) 20 else 3, lineHeight = 17.sp, overflow = TextOverflow.Ellipsis)
            }
            item.lastError?.let {
                Spacer(Modifier.height(6.dp))
                Text("🥴 $it", fontSize = 11.sp, color = Color(0xFFF43F5E))
            }
            if (item.counts.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("SOURCE_FACT" to "факты", "TERM" to "термины", "ASSOCIATION" to "ассоциации",
                        "UNCERTAIN" to "спорно", "FICTIONAL" to "фикшн").forEach { (k, l) ->
                        val n = item.counts[k] ?: 0
                        if (n > 0) Text("$l $n", fontSize = 10.sp, color = classColor(k),
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(classColor(k).copy(alpha = 0.10f)).padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (expanded) "скрыть знания" else "что я из этого вынесла",
                    fontSize = 12.sp, color = EvaDesignTokens.Colors.SakuraLavender, fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggle() }
                        .padding(6.dp)
                )
                Spacer(Modifier.weight(1f))
                if (item.status == "DIGESTED") TextAction("💎 в память", onConsolidate, Color(0xFF34D399))
                if (item.status == "UNREAD") TextAction("⏳ читать", onQueue, Color(0xFFF59E0B))
                if (item.status == "CONSOLIDATED" || item.status == "ERROR") TextAction("🔄", onReprocess, Color(0xFF38BDF8))
                if (item.status != "DISCARDED" && item.status != "CONSOLIDATED") TextAction("🗑", onDiscard, Color.White.copy(alpha = 0.4f))
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 6.dp)) {
                    if (item.knowledge.isEmpty()) {
                        Text(if (item.status == "DIGESTED" || item.status == "CONSOLIDATED")
                            "пусто — странно, нажми 🔄" else "знаний пока нет — я до него ещё не дошла",
                            fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.5f), fontStyle = FontStyle.Italic)
                    }
                    item.knowledge.forEach { k ->
                        val col = classColor(k.classification)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(Modifier.size(7.dp, 18.dp).clip(RoundedCornerShape(3.dp)).background(col))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(k.text, fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.88f), lineHeight = 17.sp)
                                Text(
                                    when (k.consolidate) {
                                        "added" -> "💎 добавлено в память #${k.memoryId ?: "?"}"
                                        "already_known" -> "уже знала раньше"
                                        "review" -> "❓ к просмотру — сама не решилась"
                                        "uncertain" -> "осталось неуверенным"
                                        else -> k.classification.lowercase()
                                    },
                                    fontSize = 10.5.sp,
                                    color = when (k.consolidate) {
                                        "added" -> Color(0xFF34D399)
                                        "review" -> Color(0xFFF43F5E)
                                        else -> Color.White.copy(alpha = 0.45f)
                                    },
                                    textDecoration = if (k.consolidate == "skip") TextDecoration.LineThrough else null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextAction(text: String, onClick: () -> Unit, color: Color) {
    Text(
        text, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(horizontal = 9.dp, vertical = 7.dp)
    )
}
