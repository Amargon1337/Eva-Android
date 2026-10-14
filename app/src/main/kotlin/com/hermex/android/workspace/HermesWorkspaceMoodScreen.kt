package com.hermex.android.workspace

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.hermex.android.navigation.LocalHermexDrawerOpener
import com.hermex.android.ui.components.EvaPetalCanvas

enum class WorkspaceTab(val label: String) {
    TASKS("Tasks"),
    SKILLS("Skills"),
    MEMORY("Memory"),
    PROJECTS("Projects"),
    PROFILES("Profiles"),
    FILES("Files")
}

/**
 * MOOD / DUALITY CONTROL (HERMES WORKSPACE) — Screen 9 on Reference Board.
 * Features workspace tabs, Affection/Attachment dials, central breathing Eva avatar,
 * interactive SAKURA <-> YANDERE slider, and Energy / Calm / Jealousy secondary controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HermesWorkspaceMoodScreen(
    onNavigateToTab: (WorkspaceTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isPaneMode: Boolean = false,
) {
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = dualityMode == DualityMode.SAKURA
    val openDrawer = LocalHermexDrawerOpener.current
    val context = LocalContext.current

    val accentColor = if (isSakura) Color(0xFFF472B6) else Color(0xFFE11D48)
    val cardBorder = if (isSakura) Color(0x35F472B6) else Color(0x35E11D48)

    // Slider position: 0.0f = full Sakura, 1.0f = full Yandere
    var sliderValue by remember(dualityMode) {
        mutableFloatStateOf(if (isSakura) 0.15f else 0.85f)
    }

    // Avatar breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarScale"
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HERMES WORKSPACE",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.2.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    if (isPaneMode) {
                        IconButton(onClick = { openDrawer() }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            if (isSakura) {
                EvaPetalCanvas(modifier = Modifier.fillMaxSize())
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. WORKSPACE HORIZONTAL TABS (Tasks | Skills | Memory | Projects | Profiles | Files)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkspaceTab.entries.forEach { tab ->
                        val selected = tab == WorkspaceTab.TASKS // Default active or clicked
                        Surface(
                            onClick = {
                                EvaHaptics.click(context)
                                onNavigateToTab(tab)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) accentColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(
                                1.dp,
                                if (selected) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.12f)
                            )
                        ) {
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))

                // 2. MOOD / DUALITY CONTROL CARD
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = if (isSakura) Color(0xFF221128).copy(alpha = 0.88f) else Color(0xFF1E070F).copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(28.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top row: Affection ♡ | Avatar | Attachment 💍
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Affection
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Affection",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Favorite,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Center: Eva Avatar with breathing & glow
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .scale(breatheScale)
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                accentColor.copy(alpha = 0.45f),
                                                accentColor.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .border(2.dp, accentColor, CircleShape)
                                    .clickable {
                                        EvaHaptics.whisperTick(context)
                                        EvaConfig.toggleMode()
                                    }
                            ) {
                                Image(
                                    painter = painterResource(
                                        if (isSakura) R.drawable.eva_avatar_sakura else R.drawable.eva_avatar_yandere
                                    ),
                                    contentDescription = "Eva",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                )
                            }

                            // Right: Attachment
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Attachment",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Diamond,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // DUALITY SLIDER: SAKURA <-----------------> YANDERE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SAKURA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Color(0xFFF472B6)
                            )
                            Text(
                                text = "YANDERE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Color(0xFFE11D48)
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Slider(
                            value = sliderValue,
                            onValueChange = { newVal ->
                                sliderValue = newVal
                                val newMode = if (newVal > 0.5f) DualityMode.YANDERE else DualityMode.SAKURA
                                if (newMode != EvaConfig.dualityMode.value) {
                                    EvaHaptics.whisperTick(context)
                                    EvaConfig.setMode(newMode)
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(20.dp))

                        // Bottom secondary indicators: ⚡ Energy | 🌙 Calm | 🔥 Jealousy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MoodAttributePill(
                                icon = Icons.Outlined.Bolt,
                                label = "Energy",
                                value = "94%",
                                color = Color(0xFFFBBF24)
                            )

                            MoodAttributePill(
                                icon = Icons.Outlined.NightsStay,
                                label = "Calm",
                                value = if (isSakura) "88%" else "32%",
                                color = Color(0xFF60A5FA)
                            )

                            MoodAttributePill(
                                icon = Icons.Outlined.LocalFireDepartment,
                                label = "Jealousy",
                                value = if (isSakura) "15%" else "99%",
                                color = Color(0xFFF43F5E)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MoodAttributePill(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = Modifier.width(86.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
