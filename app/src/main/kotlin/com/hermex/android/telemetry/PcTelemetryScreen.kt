package com.hermex.android.telemetry

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
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.components.ExpressivePill
import com.hermex.android.ui.components.HermexBentoCard
import com.hermex.android.ui.components.LivePulseDot
import com.hermex.android.ui.components.EvaPetalCanvas
import com.hermex.android.ui.components.evaGlitch
import com.hermex.android.ui.theme.EvaDesignTokens
import com.hermex.android.ui.theme.evaBreathe
import com.hermex.android.ui.theme.evaSpringPress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcTelemetryScreen(
    viewModel: PcTelemetryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val primaryAccent = if (dualityMode == DualityMode.SAKURA) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.clickable {
                            EvaConfig.toggleNetworkHost()
                            viewModel.refreshAll()
                        }
                    ) {
                        Text("сервисы пк", fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, color = Color.White)
                        Text(EvaConfig.baseUrl, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = primaryAccent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (dualityMode == DualityMode.YANDERE) Modifier.evaGlitch(triggerKey = Unit) else Modifier)
        ) {
            LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                // Active Desktop Activity Bento Card (Frosted Glass)
                val desk = uiState.desktopInfo
                HermexBentoCard(
                    cornerRadius = 24.dp,
                    borderColor = primaryAccent.copy(alpha = 0.35f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LivePulseDot(color = Color(0xFF38BDF8), size = 8.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "АКТИВНОСТЬ ИВАНА НА ПК",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                        desk?.activeWindow?.let {
                            ExpressivePill(text = "Online", accentColor = Color(0xFF10B981))
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = desk?.activeWindow ?: "Окно не определено или ПК спит",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoGaugeTile(
                            title = "CPU",
                            value = "${desk?.cpuPercent?.toInt() ?: 0}%",
                            accent = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1f)
                        )
                        BentoGaugeTile(
                            title = "RAM",
                            value = "${desk?.ramPercent?.toInt() ?: 0}%",
                            accent = Color(0xFFF472B6),
                            modifier = Modifier.weight(1f)
                        )
                        desk?.gpuPercent?.let { gpu ->
                            BentoGaugeTile(
                                title = "GPU",
                                value = "${gpu.toInt()}%",
                                accent = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryAccent)
                    }
                }
            }

            item {
                Text(
                    text = "ФОНОВЫЕ СЛУЖБЫ И ВОЧДОГИ",
                    color = primaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            items(uiState.services) { service ->
                val isRunning = service.status.lowercase() == "running" || service.status.lowercase() == "active"
                val serviceColor = if (isRunning) EvaDesignTokens.Colors.StatusOnline else EvaDesignTokens.Colors.StatusError

                HermexBentoCard(
                    cornerRadius = 18.dp,
                    borderColor = if (isRunning) MaterialTheme.colorScheme.outlineVariant else EvaDesignTokens.Colors.YandereCrimson.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            LivePulseDot(color = serviceColor, size = 10.dp)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    service.title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                val portText = service.port?.let { ":$it" } ?: ""
                                val pidText = service.pid?.let { " · PID $it" } ?: ""
                                Text(
                                    "${service.status.uppercase()}$portText$pidText",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        FilledTonalIconButton(
                            onClick = { viewModel.restartService(service.name) },
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = primaryAccent
                            )
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Перезапустить", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(28.dp))
            }
        }
        }
    }
}

@Composable
private fun BentoGaugeTile(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(title, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

