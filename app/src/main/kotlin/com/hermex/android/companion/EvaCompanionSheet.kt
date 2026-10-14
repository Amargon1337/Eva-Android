package com.hermex.android.companion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.character.EvaHaptics
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig

/**
 * EVA COMPANION Sheet — Pixel-perfect match to Screen 7 on Reference Board.
 * Displays expandable options for Diary, Telemetry, Tokens & models, and Insights.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaCompanionSheet(
    onDismiss: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenTelemetry: () -> Unit,
    onOpenTokens: () -> Unit,
    onOpenInsights: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = dualityMode == DualityMode.SAKURA
    val context = LocalContext.current

    val sheetBg = if (isSakura) Color(0xFF1B0E1D) else Color(0xFF16060A)
    val accentColor = if (isSakura) Color(0xFFF472B6) else Color(0xFFE11D48)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetBg,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            // Header: EVA COMPANION + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ева рядом",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = Color.White
                )

                IconButton(
                    onClick = {
                        EvaHaptics.click(context)
                        onDismiss()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // 1. Diary
            CompanionMenuTile(
                icon = Icons.Outlined.FavoriteBorder,
                title = "дневник",
                subtitle = "её внутренний поток · витрина и под замком",
                accentColor = accentColor,
                isSakura = isSakura,
                onClick = {
                    EvaHaptics.click(context)
                    onDismiss()
                    onOpenDiary()
                }
            )

            Spacer(Modifier.height(10.dp))

            // 2. Telemetry
            CompanionMenuTile(
                icon = Icons.Outlined.DataUsage,
                title = "сервисы и пк",
                subtitle = "органы цифрового тела · watchdog'и",
                accentColor = accentColor,
                isSakura = isSakura,
                onClick = {
                    EvaHaptics.click(context)
                    onDismiss()
                    onOpenTelemetry()
                }
            )

            Spacer(Modifier.height(10.dp))

            // 3. Tokens & models
            CompanionMenuTile(
                icon = Icons.Outlined.AutoAwesome,
                title = "токены",
                    subtitle = "технический учёт · без магии",
                accentColor = accentColor,
                isSakura = isSakura,
                onClick = {
                    EvaHaptics.click(context)
                    onDismiss()
                    onOpenTokens()
                }
            )

            Spacer(Modifier.height(10.dp))

            // 4. Insights
            CompanionMenuTile(
                icon = Icons.Outlined.Lightbulb,
                title = "инсайты",
                subtitle = "что я заметила за тобой",
                accentColor = accentColor,
                isSakura = isSakura,
                onClick = {
                    EvaHaptics.click(context)
                    onDismiss()
                    onOpenInsights()
                }
            )

            Spacer(Modifier.height(10.dp))

            // Footer: честная телеметрия подключения (референс GPT SOL, локализовано)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ева · v" + com.hermex.android.BuildConfig.VERSION_NAME,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.40f),
                )
                Text(
                    text = if (EvaConfig.baseUrl.contains("ts.net")) "tailscale · дом пк" else EvaConfig.baseUrl.removePrefix("http://").take(22),
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = accentColor.copy(alpha = 0.55f),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CompanionMenuTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    isSakura: Boolean,
    onClick: () -> Unit
) {
    val tileBg = if (isSakura) Color(0xFF261229).copy(alpha = 0.85f) else Color(0xFF22080F).copy(alpha = 0.85f)
    val tileBorder = if (isSakura) Color(0x35F472B6) else Color(0x35E11D48)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = tileBg,
        border = BorderStroke(1.dp, tileBorder),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
