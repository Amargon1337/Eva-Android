package com.hermex.android.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermex.android.core.storage.HeaderLogoColor
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.components.ExpressivePill
import com.hermex.android.ui.components.LivePulseDot
import com.hermex.android.ui.theme.HermexColors
import com.hermex.android.ui.theme.toComposeColor

@Composable
fun HermexDrawerContent(
    uiState: SessionListUiState,
    selectedNavItem: SessionListNavItem?,
    onNavItemSelected: (SessionListNavItem) -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenSessions: () -> Unit,
    onCreateSession: () -> Unit,
    onOpenSettings: () -> Unit,
    initialText: String?,
    headerLogoColor: HeaderLogoColor = HeaderLogoColor.DEFAULT,
    modifier: Modifier = Modifier,
) {
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val primaryAccent = MaterialTheme.colorScheme.primary

    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = if (dualityMode == DualityMode.SAKURA) Color(0xF213071F) else Color(0xF209040F),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Header Hero Box
                Surface(
                    color = primaryAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 16.dp, top = 32.dp, bottom = 18.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LivePulseDot(color = primaryAccent, size = 8.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "EVA // HERMEX",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryAccent,
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Sovereign Companion · 2026",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        ExpressivePill(
                            text = dualityMode.badge,
                            accentColor = primaryAccent,
                            onClick = { EvaConfig.toggleMode() }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Section 1: Eva Companion Features
                DrawerSectionHeader(title = "ева рядом")

                DrawerNavItem(
                    icon = Icons.Filled.Face,
                    label = "🪞 Лукмакс & Биохакинг",
                    isSelected = selectedNavItem == SessionListNavItem.LOOKSMAX,
                    accentColor = primaryAccent,
                    onClick = { onNavItemSelected(SessionListNavItem.LOOKSMAX) },
                )
                DrawerNavItem(
                    icon = Icons.Filled.BarChart,
                    label = "📊 Токены и модели",
                    isSelected = selectedNavItem == SessionListNavItem.TOKENS,
                    accentColor = primaryAccent,
                    onClick = { onNavItemSelected(SessionListNavItem.TOKENS) },
                )
                DrawerNavItem(
                    icon = Icons.Filled.Info,
                    label = "📖 Дневник Евы",
                    isSelected = selectedNavItem == SessionListNavItem.DIARY,
                    accentColor = primaryAccent,
                    onClick = { onNavItemSelected(SessionListNavItem.DIARY) },
                )
                DrawerNavItem(
                    icon = Icons.Filled.Build,
                    label = "🖥️ ПК & Службы",
                    isSelected = selectedNavItem == SessionListNavItem.TELEMETRY,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = { onNavItemSelected(SessionListNavItem.TELEMETRY) },
                )

                Spacer(Modifier.height(12.dp))

                // Section 2: Hermes Workspace
                DrawerSectionHeader(title = "гермес · работа")

                DrawerNavItem(
                    icon = Icons.Filled.Forum,
                    label = "чат",
                    isSelected = false,
                    accentColor = primaryAccent,
                    onClick = onOpenSessions,
                )
                DrawerNavItem(
                    icon = Icons.Filled.DateRange,
                    label = "задачи",
                    isSelected = selectedNavItem == SessionListNavItem.TASKS,
                    accentColor = primaryAccent,
                    onClick = { onNavItemSelected(SessionListNavItem.TASKS) },
                )
                DrawerNavItem(
                    icon = Icons.Filled.Build,
                    label = "скиллы",
                    isSelected = selectedNavItem == SessionListNavItem.SKILLS,
                    accentColor = primaryAccent,
                    onClick = { onNavItemSelected(SessionListNavItem.SKILLS) },
                )
                DrawerNavItem(
                    icon = Icons.Filled.Face,
                    label = "память",
                    isSelected = selectedNavItem == SessionListNavItem.MEMORY,
                    accentColor = primaryAccent,
                    onClick = { onNavItemSelected(SessionListNavItem.MEMORY) },
                )
                DrawerNavItem(
                    icon = Icons.Filled.Info,
                    label = "инсайты",
                    isSelected = selectedNavItem == SessionListNavItem.INSIGHTS,
                    accentColor = primaryAccent,
                    onClick = { onNavItemSelected(SessionListNavItem.INSIGHTS) },
                )
                DrawerNavItem(
                    icon = Icons.Filled.Person,
                    label = "профили",
                    isSelected = selectedNavItem == SessionListNavItem.PROFILES,
                    accentColor = primaryAccent,
                    onClick = { onNavItemSelected(SessionListNavItem.PROFILES) },
                )
                DrawerNavItem(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = "проекты",
                    isSelected = selectedNavItem == SessionListNavItem.PROJECTS,
                    accentColor = primaryAccent,
                    onClick = { onNavItemSelected(SessionListNavItem.PROJECTS) },
                )

                Spacer(Modifier.height(12.dp))

                // Section 3: Preferences
                DrawerSectionHeader(title = "настройки")
                DrawerNavItem(
                    icon = Icons.Filled.Settings,
                    label = "настройки",
                    isSelected = false,
                    accentColor = primaryAccent,
                    onClick = onOpenSettings,
                )

                val recentSessions = uiState.filteredSessions.take(5)
                if (recentSessions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    DrawerSectionHeader(title = "недавнее")
                    recentSessions.forEach { session ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .clickable { session.sessionId?.let(onOpenSession) }
                        ) {
                            Text(
                                text = session.title?.ifBlank { null } ?: "без названия",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // Bottom Profile & New Chat Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenSettings)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .border(1.dp, primaryAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (initialText != null) {
                                Text(
                                    text = initialText,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = primaryAccent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Button(
                        onClick = onCreateSession,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier.height(42.dp),
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "новый чат",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 22.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val containerColor = if (isSelected) accentColor.copy(alpha = 0.16f) else Color.Transparent
    val borderStroke = if (isSelected) BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)) else null
    val contentColor = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = borderStroke,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
            )
        }
    }
}

