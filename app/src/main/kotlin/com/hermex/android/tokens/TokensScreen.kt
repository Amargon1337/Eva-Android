package com.hermex.android.tokens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.components.HermexBentoCard
import com.hermex.android.ui.theme.EvaDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokensScreen(
    viewModel: TokensViewModel,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val primaryAccent = if (dualityMode == DualityMode.SAKURA) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson
    val secondaryAccent = MaterialTheme.colorScheme.secondary

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📊 Токены и модели", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("расход за 7 дней · ${EvaConfig.baseUrl}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = primaryAccent)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = primaryAccent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "ТОКЕНЫ И МОДЕЛИ",
                    color = primaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            if (uiState.error != null && uiState.summary == null) {
                item {
                    HermexBentoCard(
                        cornerRadius = 22.dp,
                        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(uiState.error ?: "", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                val summary = uiState.summary
                if (uiState.daily.isNotEmpty()) {
                    item {
                        HermexBentoCard(cornerRadius = 22.dp, borderColor = primaryAccent.copy(alpha = 0.2f)) {
                            Text("по дням · 14 дней", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(10.dp))
                            val maxT = uiState.daily.maxOf { it.tokens }.coerceAtLeast(1L)
                            Text("тапни день — покажу, какие чаты и модели его съели", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp))
                            // bars — entire column is clickable with indication + expanded hit area
                            Row(
                                modifier = Modifier.fillMaxWidth().height(88.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                uiState.daily.forEachIndexed { i, d ->
                                    val h = d.tokens.toFloat() / maxT
                                    val sel = uiState.selectedDate == d.date
                                    val interaction = remember { MutableInteractionSource() }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(
                                                interactionSource = interaction,
                                                indication = null,
                                                onClick = { viewModel.selectDate(d.date) }
                                            )
                                            .background(
                                                if (sel) primaryAccent.copy(alpha = 0.14f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                width = if (sel) 1.2.dp else 0.dp,
                                                color = if (sel) primaryAccent.copy(alpha = 0.55f) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            // expanded hit area: internal padding + centered bar
                                            .padding(horizontal = 2.dp, vertical = 3.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                    ) {
                                        // bar — Box does NOT intercept click (no clickable), fills width 85% so column edges remain tappable
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.88f)
                                                .fillMaxHeight(if (h < 0.08f) 0.08f else h)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    when {
                                                        sel -> primaryAccent
                                                        i == uiState.daily.lastIndex -> primaryAccent.copy(alpha = 0.85f)
                                                        else -> primaryAccent.copy(alpha = 0.30f)
                                                    }
                                                )
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = d.date.takeLast(2),
                                            color = if (sel) primaryAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 8.sp,
                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("${uiState.daily.first().date.substring(5)} · ${uiState.daily.last().date.substring(5)} · пик ${formatK(maxT)} токенов",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                }
                val selDate = uiState.selectedDate
                if (selDate != null) {
                    item {
                        HermexBentoCard(cornerRadius = 22.dp, borderColor = primaryAccent.copy(alpha = 0.35f)) {
                            Text("${selDate.substring(8)}.${selDate.substring(5,7)} · разбор дня", color = primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))
                            when {
                                uiState.dayDetailLoading -> Text("считаю...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                uiState.dayDetailError != null -> Text("не получилось: ${uiState.dayDetailError}", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                uiState.dayDetail != null -> {
                                    val dd = uiState.dayDetail!!
                                    if (dd.models.isEmpty() && dd.sessions.isEmpty()) {
                                        Text("в этот день тишина — ни токенов, ни чатов", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                    if (dd.sessions.isNotEmpty()) {
                                        Text("какие чаты", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                        Spacer(Modifier.height(4.dp))
                                        dd.sessions.forEach { s ->
                                            val canOpen = s.sessionId.isNotBlank()
                                            val rowInteraction = remember { MutableInteractionSource() }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .then(
                                                        if (canOpen) Modifier.clickable(
                                                            interactionSource = rowInteraction,
                                                            indication = null,
                                                            onClick = { onOpenSession(s.sessionId) }
                                                        ) else Modifier
                                                    )
                                                    .padding(vertical = 6.dp, horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = s.title.ifBlank { s.sessionId.take(12).ifBlank { "чат" } },
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (canOpen) FontWeight.Medium else FontWeight.Normal,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (s.input > 0 || s.output > 0 || s.cache > 0) {
                                                        Text(
                                                            text = "in ${formatK(s.input)} · out ${formatK(s.output)}" + if (s.cache > 0) " · cache ${formatK(s.cache)}" else "",
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontSize = 10.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (!canOpen) {
                                                        Text(
                                                            text = "сессия без id",
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Text(formatK(s.tokens), color = primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                if (canOpen) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                        contentDescription = "открыть чат",
                                                        tint = primaryAccent.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(10.dp))
                                    }
                                    if (dd.models.isNotEmpty()) {
                                        Text("какие модели · in/out/кэш/размышления", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                        Spacer(Modifier.height(4.dp))
                                        dd.models.forEach { m ->
                                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(m.model, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(formatK(m.input + m.output + m.cache + m.reasoning), color = primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Text("in ${formatK(m.input)} · out ${formatK(m.output)} · кэш ${formatK(m.cache)} · думала ${formatK(m.reasoning)} · ${m.calls} выз. · ${m.provider}",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (summary == null) {
                    item {
                        Text("Считаю токены...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                } else {
                    item {
                        HermexBentoCard(
                            cornerRadius = 22.dp,
                            borderColor = primaryAccent.copy(alpha = 0.25f)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MetricTile("Всего токенов", formatK(summary.totalInput + summary.totalOutput), primaryAccent, Modifier.weight(1f))
                                MetricTile("API вызовов", "${summary.totalCalls}", secondaryAccent, Modifier.weight(1f))
                                MetricTile("Провайдеров", "${summary.uniqueProviders}", EvaDesignTokens.Colors.StatusOnline, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("По провайдерам", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            val maxProviderTokens = summary.byProvider.maxOfOrNull { it.inputTokens + it.outputTokens }?.coerceAtLeast(1L) ?: 1L
                            summary.byProvider.take(8).forEach { p ->
                                val t = p.inputTokens + p.outputTokens
                                ProviderUsageRow(p.provider, t, p.apiCalls, t.toFloat() / maxProviderTokens, primaryAccent)
                            }
                        }
                    }
                    item {
                        HermexBentoCard(
                            cornerRadius = 22.dp,
                            borderColor = primaryAccent.copy(alpha = 0.25f)
                        ) {
                            Text("Топ моделей", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            summary.byModel.sortedByDescending { it.inputTokens + it.outputTokens }.take(12).forEach { m ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(m.model, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("in ${formatK(m.inputTokens)} · out ${formatK(m.outputTokens)} · cache ${formatK(m.cacheReadTokens)} · ${m.provider}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("${m.apiCalls} выз.", color = primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)) {
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ProviderUsageRow(name: String, tokens: Long, calls: Long, fraction: Float, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(92.dp), maxLines = 1)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.03f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(formatK(tokens), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(56.dp))
    }
}

private fun formatK(n: Long): String = when {
    n >= 1_000_000_000 -> String.format("%.1fB", n / 1_000_000_000.0)
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fk", n / 1_000.0)
    else -> n.toString()
}
