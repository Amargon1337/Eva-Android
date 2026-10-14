package com.hermex.android.settings

import com.hermex.android.ui.components.EvaHeaderTitle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import coil3.compose.AsyncImage
import com.hermex.android.BuildConfig
import com.hermex.android.PRIVACY_POLICY_URL
import com.hermex.android.R
import com.hermex.android.core.storage.AppIconVariant
import com.hermex.android.core.storage.HeaderLogoColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.components.ExpressivePill
import com.hermex.android.ui.components.LivePulseDot
import com.hermex.android.ui.theme.EvaDesignTokens
import com.hermex.android.ui.theme.HermexReadableContent
import com.hermex.android.ui.theme.HermexRadii
import com.hermex.android.navigation.LocalHermexDrawerOpener
import com.hermex.android.ui.theme.toComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenDefaultModel: () -> Unit,
    onOpenCustomHeaders: () -> Unit,
    onOpenServers: () -> Unit,
    modifier: Modifier = Modifier,
    // True only in the wide-layout right pane, where the persistent left pane already shows
    // "Settings" is the open section -- the top bar's own literal title adds nothing there, so
    // it's dropped rather than shown redundantly. The back button stays exactly as it is either way.
    isPaneMode: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // There's no in-app back arrow here (the top bar's nav icon opens the drawer instead, see
    // below) -- system/gesture back is the only way out, so it must go through [onBack] itself
    // rather than falling through to NavHost's default pop, or the refresh signals onBack sets
    // (e.g. refreshHeaderLogoColor, refreshShowSubagentSessions) never fire.
    BackHandler(onBack = onBack)
    val openDrawer = LocalHermexDrawerOpener.current
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showAppIconPicker by remember { mutableStateOf(false) }
    var showInitialsDialog by remember { mutableStateOf(false) }
    var showNotificationEducation by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentDualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var latencyResult by remember { mutableStateOf<String?>(null) }
    var isPinging by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                viewModel.setNotificationsEnabled(true)
            } else {
                viewModel.setNotificationsEnabled(false)
                showNotificationEducation = true
            }
        },
    )

    if (showNotificationEducation) {
        AlertDialog(
            onDismissRequest = { showNotificationEducation = false },
            shape = RoundedCornerShape(HermexRadii.Dialog),
            title = { Text("уведомления") },
            text = {
                Text("Notifications are disabled. Enable them in Settings → Apps → Hermex → Notifications.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationEducation = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // If the settings intent fails, just dismiss
                    }
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationEducation = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showColorPicker) {
        HeaderLogoColorDialog(
            selected = uiState.headerLogoColor,
            onSelect = {
                viewModel.setHeaderLogoColor(it)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
        )
    }

    if (showInitialsDialog) {
        var draftInitials by remember { mutableStateOf(uiState.userInitials) }
        AlertDialog(
            onDismissRequest = { showInitialsDialog = false },
            shape = RoundedCornerShape(HermexRadii.Dialog),
            title = { Text("Set initials") },
            text = {
                TextField(
                    value = draftInitials,
                    onValueChange = { if (it.length <= 4) draftInitials = it.uppercase() },
                    singleLine = true,
                    label = { Text("Initials (max 4 chars)") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showInitialsDialog = false
                    viewModel.setUserInitials(draftInitials)
                }) { Text("сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showInitialsDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showAppIconPicker) {
        AppIconVariantDialog(
            selected = uiState.appIconVariant,
            onSelect = {
                viewModel.setAppIconVariant(it)
                showAppIconPicker = false
            },
            onDismiss = { showAppIconPicker = false },
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            shape = RoundedCornerShape(HermexRadii.Dialog),
            title = { Text("Sign out?") },
            text = { Text("Signs out of the active server and returns to onboarding.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutConfirm = false
                    viewModel.signOut()
                }) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    if (!isPaneMode) {
                        EvaHeaderTitle("настройки", "сервер · вид · уведомления")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { openDrawer() }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Меню", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
            )
        },
    ) { innerPadding ->
        HermexReadableContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                // SCREEN 10: REFERENCE BOARD SETTINGS LIST
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (currentDualityMode == DualityMode.SAKURA) Color(0xFF221128).copy(alpha = 0.88f) else Color(0xFF1E070D).copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, if (currentDualityMode == DualityMode.SAKURA) Color(0x35F472B6) else Color(0x35E11D48)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Server Row: Server | [Server selector v]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Dns,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Server",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Surface(
                                onClick = onOpenServers,
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uiState.activeServerName ?: "Server selector",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        )

                        // 1. Connection
                        ReferenceSettingsRow(
                            icon = Icons.Outlined.Link,
                            label = "Connection",
                            onClick = onOpenCustomHeaders
                        )

                        // 2. Appearance -- opens the color picker dialog (dedup 14.09: the old
                        // standalone "Appearance" card merged into this row).
                        ReferenceSettingsRow(
                            icon = Icons.Outlined.Palette,
                            label = "Appearance",
                            onClick = { showColorPicker = true }
                        )
                        ReferenceSettingsRow(
                            icon = Icons.Outlined.Face,
                            label = "иконка приложения",
                            onClick = { showAppIconPicker = true }
                        )
                        ReferenceSettingsRow(
                            icon = Icons.Outlined.Person,
                            label = "User Name",
                            onClick = { showInitialsDialog = true }
                        )

                        // 3. Notifications -- the single toggle (dedup 14.09): replaces the old
                        // separate "Notifications" card below, same permission-aware logic.
                        ReferenceSettingsRow(
                            icon = Icons.Outlined.Notifications,
                            label = "уведомления",
                            onClick = {
                                val enable = !uiState.notificationsEnabled
                                if (enable) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val permission = Manifest.permission.POST_NOTIFICATIONS
                                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission)
                                            == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        ) {
                                            viewModel.setNotificationsEnabled(true)
                                        } else {
                                            notificationPermissionLauncher.launch(permission)
                                        }
                                    } else {
                                        viewModel.setNotificationsEnabled(true)
                                    }
                                } else {
                                    viewModel.setNotificationsEnabled(false)
                                }
                            }
                        )

                        // 4. Voice
                        ReferenceSettingsRow(
                            icon = Icons.Outlined.Mic,
                            label = "Voice",
                            onClick = {
                                EvaConfig.autoVoicePlay = !EvaConfig.autoVoicePlay
                                Toast.makeText(
                                    context,
                                    "Voice playback: ${if (EvaConfig.autoVoicePlay) "Enabled" else "Disabled"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        // 5. Models
                        ReferenceSettingsRow(
                            icon = Icons.Outlined.SmartToy,
                            label = "Models",
                            onClick = onOpenDefaultModel
                        )

                        // 6. Memory
                        ReferenceSettingsRow(
                            icon = Icons.Outlined.Folder,
                            label = "Memory",
                            onClick = {
                                Toast.makeText(context, "Memory management active", Toast.LENGTH_SHORT).show()
                            }
                        )

                        // 7. Privacy
                        ReferenceSettingsRow(
                            icon = Icons.Outlined.Lock,
                            label = "Privacy",
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                                }
                            }
                        )

                        // 8. Reduced Motion (with toggle switch)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.RemoveRedEye,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Reduced Motion",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            Switch(
                                checked = !EvaConfig.streamPulseAnimation,
                                onCheckedChange = { reduced ->
                                    EvaConfig.streamPulseAnimation = !reduced
                                }
                            )
                        }

                        // 9. About -- now also hosts Copy Diagnostics and Subagent Sessions
                        // (dedup 14.09: the old standalone "App"/"Session List" cards merged here).
                        ReferenceSettingsRow(
                            icon = Icons.Outlined.Info,
                            label = "About",
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Hermex v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                        ReferenceSettingsRow(
                            icon = Icons.Outlined.BugReport,
                            label = "Copy Diagnostics",
                            onClick = { viewModel.copyDiagnostics(context) }
                        )
                        ReferenceSettingsRow(
                            icon = Icons.Outlined.ViewStream,
                            label = "Subagent Sessions",
                            onClick = { viewModel.setShowSubagentSessions(!uiState.showSubagentSessions) }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                SectionLabel("Eva Companion & Персонализация")
                Card {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = "Режим дуальности личности Евы",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Определяет тон общения, стиль ответов и цветовую гамму интерфейса",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DualityMode.entries.forEach { mode ->
                                val isSelected = currentDualityMode == mode
                                val accentColor = if (mode == DualityMode.SAKURA) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) accentColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainer,
                                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            EvaConfig.setMode(mode)
                                            if (EvaConfig.hapticsEnabled) {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (mode == DualityMode.SAKURA) "🌸" else "🦇", style = MaterialTheme.typography.titleMedium)
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = mode.badge,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = mode.desc,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = "Тактильный отклик (Haptics)",
                        description = "Вибрация при отправке сообщений, нажатиях и раскрытии мыслей Евы",
                        checked = EvaConfig.hapticsEnabled,
                        onCheckedChange = { EvaConfig.hapticsEnabled = it }
                    )
                    SettingsSwitchRow(
                        label = "Пульсирующие анимации присутствия",
                        description = "Живой дыхательный индикатор работы модели и статуса ПК",
                        checked = EvaConfig.streamPulseAnimation,
                        onCheckedChange = { EvaConfig.streamPulseAnimation = it },
                        showDivider = false
                    )
                }

                Spacer(Modifier.height(24.dp))
                // Dedup 14.09: Server / Default Model / Connection Headers / Version rows removed
                // -- they duplicated the reference-board list above (Server/Models/Connection/About).
                // Only the unique bits stay: status, the Tailscale endpoint toggle, and ping.
                SectionLabel("Tailscale & Диагностика")
                Card {
                    SettingsRow("Status", if (uiState.serverUrl != null) "Connected" else "Not signed in")
                    SettingsRow(
                        label = "Tailscale Endpoint (Нажмите для переключения)",
                        value = EvaConfig.baseUrl,
                        onClick = {
                            EvaConfig.toggleNetworkHost()
                            if (EvaConfig.hapticsEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Диагностика задержки (Ping)", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = latencyResult ?: "Проверить реальную задержку соединения с сервером",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (latencyResult?.startsWith("🟢") == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isPinging) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Button(
                                    onClick = {
                                        isPinging = true
                                        coroutineScope.launch {
                                            val (ok, ms) = EvaConfig.measureLatencyMs()
                                            isPinging = false
                                            latencyResult = if (ok) "🟢 ${ms}ms · Tailscale TLS в норме" else "🔴 Ошибка отклика (${ms}ms)"
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Ping", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }


                Spacer(Modifier.height(24.dp))
                SectionLabel("Chat")
                Card {
                    SettingsSwitchRow(
                        label = "Expand Thinking by Default",
                        description = "Thinking blocks start expanded instead of collapsed. Tapping a block still toggles it.",
                        checked = uiState.expandThinkingByDefault,
                        onCheckedChange = viewModel::setExpandThinkingByDefault,
                    )
                    SettingsSwitchRow(
                        label = "Expand Tool Calls by Default",
                        description = "Tool call blocks start expanded instead of collapsed. Tapping a block still toggles it.",
                        checked = uiState.expandToolCallsByDefault,
                        onCheckedChange = viewModel::setExpandToolCallsByDefault,
                        showDivider = false,
                    )
                }

                Spacer(Modifier.height(24.dp))
                SectionLabel("Account")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(HermexRadii.SettingsCard))
                        .padding(16.dp),
                ) {
                    Text(
                        "Signs out of the active server and returns to onboarding.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showSignOutConfirm = true },
                        enabled = !uiState.isSigningOut,
                        shape = RoundedCornerShape(HermexRadii.Cell),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (uiState.isSigningOut) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Sign Out of This Server")
                        }
                    }
                }

                uiState.errorMessage?.let { message ->
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(HermexRadii.Accessory),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            content()
        }
    }
}


@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
private fun SettingsRow(label: String, value: String, showDivider: Boolean = true, onClick: (() -> Unit)? = null) {
    Column {
        Column(
            modifier = Modifier
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 14.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
        if (showDivider) {
            SettingsDivider()
        }
    }
}

@Composable
private fun HeaderLogoColorDialog(
    selected: HeaderLogoColor,
    onSelect: (HeaderLogoColor) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(HermexRadii.Dialog),
        title = { Text("Header Logo Color") },
        text = {
            Column {
                HeaderLogoColor.entries.forEach { color ->
                    ListItem(
                        modifier = Modifier.clickable { onSelect(color) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(color.toComposeColor(), CircleShape),
                            )
                        },
                        headlineContent = { Text(color.displayName) },
                        trailingContent = {
                            if (color == selected) {
                                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("закрыть") }
        },
    )
}

@Composable
private fun AppIconVariantDialog(
    selected: AppIconVariant,
    onSelect: (AppIconVariant) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(HermexRadii.Dialog),
        title = { Text("App Icon") },
        text = {
            Column {
                AppIconVariant.entries.forEach { variant ->
                    ListItem(
                        modifier = Modifier.clickable { onSelect(variant) },
                        leadingContent = {
                            // painterResource() only supports VectorDrawable and rasterized
                            // (PNG/JPG/WEBP) resources -- it throws IllegalArgumentException on
                            // an <adaptive-icon> mipmap (our ic_launcher_* resources, once
                            // mipmap-anydpi-v26 was added). AsyncImage's Coil-backed pipeline
                            // loads AdaptiveIconDrawable correctly, same as the OS launcher does.
                            AsyncImage(
                                model = variant.previewMipmapRes(isDarkTheme),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                            )
                        },
                        headlineContent = { Text(variant.displayName) },
                        supportingContent = { Text(variant.description) },
                        trailingContent = {
                            if (variant == selected) {
                                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

/** SYSTEM has no launcher icon of its own (see [AppIconVariant]) -- shown here as whichever of
 * Light/Dark it currently resolves to, matching [com.hermex.android.core.appicon.AppIconResolver]. */
private fun AppIconVariant.previewMipmapRes(isDarkTheme: Boolean): Int = when (this) {
    AppIconVariant.SYSTEM -> if (isDarkTheme) R.mipmap.ic_launcher_dark else R.mipmap.ic_launcher_light
    AppIconVariant.LIGHT -> R.mipmap.ic_launcher_light
    AppIconVariant.DARK -> R.mipmap.ic_launcher_dark
    AppIconVariant.DISCO -> R.mipmap.ic_launcher_disco
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        if (showDivider) {
            SettingsDivider()
        }
    }
}

@Composable
private fun ReferenceSettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.size(18.dp)
        )
    }
}

