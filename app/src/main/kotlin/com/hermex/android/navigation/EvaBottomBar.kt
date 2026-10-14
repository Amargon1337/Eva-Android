package com.hermex.android.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import android.os.Build
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.R
import com.hermex.android.character.EvaHaptics
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.theme.EvaDesignTokens

/**
 * EVA Bottom Dock — «комната · дневник · [Ева] · чутьё · системы».
 * pointoni chassis: центральный кружок — это НЕ вкладка чата, это сама Ева (звать её).
 * Чат/сессии — по тапу на неё; drawer остаётся на экране списков сессий.
 */
@Composable
fun EvaBottomBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenCompanion: () -> Unit = onOpenDrawer,
    onCenterAction: () -> Unit = { onNavigateToRoute(Routes.SESSION_LIST) },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = dualityMode == DualityMode.SAKURA

    val pillBackground = if (isSakura) {
        Color(0xFF221128).copy(alpha = 0.92f)
    } else {
        Color(0xFF1C060B).copy(alpha = 0.94f)
    }

    val activeAccent = if (isSakura) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson

    val borderStroke = BorderStroke(
        1.2.dp,
        if (isSakura) Color(0x4DF472B6) else Color(0x4DE11D48)
    )

    val selectedIndex = when (currentRoute) {
        Routes.HOME -> 0
        Routes.LOOKSMAX -> 1
        Routes.SESSION_LIST, Routes.NEW_CHAT -> 2
        Routes.PRESENCE -> 3
        Routes.SYSTEMS, Routes.SHELF, Routes.TELEMETRY, Routes.SETTINGS, Routes.TOKENS,
        Routes.INSIGHTS, Routes.LOOKSMAX, Routes.SERVERS, Routes.CUSTOM_HEADERS,
        Routes.DEFAULT_MODEL, Routes.SKILLS, Routes.TASKS, Routes.PROFILES, Routes.PROJECTS,
        Routes.WORKSPACE_MOOD -> 4
        else -> when {
            currentRoute?.startsWith("chat/") == true -> 2
            currentRoute?.startsWith("memory") == true -> 4
            currentRoute?.startsWith("share/") == true -> 2
            else -> 0
        }
    }

    // liquid glass dock — rides on the same EvaAmbientBackdrop, blurred, not a separate slab
    val glassFillBase = if (isSakura) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.05f)
    val glassTint = if (isSakura) Color(0xFFF472B6).copy(alpha = 0.06f) else Color(0xFFE11D48).copy(alpha = 0.05f)
    val glassFill = glassFillBase.copy(alpha = glassFillBase.alpha + glassTint.alpha*0.6f)
    val glassBorderBrush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f), if (isSakura) Color(0xFFF472B6).copy(alpha = 0.16f) else Color(0xFFE11D48).copy(alpha = 0.14f)))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            color = glassFill,
            border = BorderStroke(1.dp, glassBorderBrush),
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 12.dp
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val tabWidth = maxWidth / 5
                val targetOffset = tabWidth * selectedIndex
                val animatedOffset by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                    label = "bottomBarSlidingPill"
                )

                // Sliding neon pill (пропускаем центр — там аватар)
                if (selectedIndex != 2) {
                    Box(
                        modifier = Modifier
                            .offset(x = animatedOffset)
                            .width(tabWidth)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 44.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            activeAccent.copy(alpha = 0.35f),
                                            activeAccent.copy(alpha = 0.12f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 42.dp, height = 40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(activeAccent.copy(alpha = 0.10f))
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconSlot(
                        icon = Icons.Outlined.Home, label = "комната",
                        isSelected = selectedIndex == 0, activeAccent = activeAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { EvaHaptics.click(context); onNavigateToRoute(Routes.HOME) }
                    )
                    IconSlot(
                        icon = Icons.Outlined.AutoAwesome, label = "looksmax",
                        isSelected = selectedIndex == 1, activeAccent = activeAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { EvaHaptics.click(context); onNavigateToRoute(Routes.LOOKSMAX) }
                    )
                    // Центр — Ева: тап зовёт её в чат
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { EvaHaptics.whisperTick(context); onCenterAction() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarScale by animateFloatAsState(
                            targetValue = if (selectedIndex == 2) 1.10f else 0.96f,
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                            label = "evaAvatarScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .scale(avatarScale)
                                .shadow(10.dp, CircleShape, ambientColor = activeAccent, spotColor = activeAccent)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(activeAccent.copy(alpha = 0.45f), Color(0xFF150D24))
                                    )
                                )
                                .border(1.6.dp, activeAccent.copy(alpha = 0.85f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(
                                    if (isSakura) R.drawable.eva_avatar_sakura else R.drawable.eva_avatar_yandere
                                ),
                                contentDescription = "Ева — позвать",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(41.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                    IconSlot(
                        icon = Icons.Outlined.Visibility, label = "чутьё",
                        isSelected = selectedIndex == 3, activeAccent = activeAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { EvaHaptics.click(context); onNavigateToRoute(Routes.PRESENCE) }
                    )
                    IconSlot(
                        icon = Icons.Outlined.Apps, label = "системы",
                        isSelected = selectedIndex == 4, activeAccent = activeAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { EvaHaptics.click(context); onNavigateToRoute(Routes.SYSTEMS) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IconSlot(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    activeAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "navIconScale"
    )
    val tint by animateColorAsState(
        targetValue = if (isSelected) activeAccent else Color.White.copy(alpha = 0.62f),
        animationSpec = tween(200),
        label = "navIconTint"
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(23.dp).scale(scale),
            tint = tint
        )
    }
}


