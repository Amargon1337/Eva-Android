package com.hermex.android.home

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermex.android.R
import com.hermex.android.character.EvaHaptics
import com.hermex.android.eva.DualityMode
import kotlinx.coroutines.delay

/**
 * SAKURA <-> YANDERE TRANSITION — Screen 3 on Reference Board.
 * Displays side-by-side emotional shift with "EMOTIONAL SHIFT 280ms" pill badge.
 */
@Composable
fun EvaTransitionOverlay(
    visible: Boolean,
    targetMode: DualityMode,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LaunchedEffect(visible) {
        if (visible) {
            delay(1800)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.95f, animationSpec = tween(180)),
        exit = fadeOut(animationSpec = tween(220)) + scaleOut(targetScale = 0.95f, animationSpec = tween(220)),
        modifier = modifier
    ) {
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Main card matching Screen 3
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(32.dp)),
                color = Color(0xFF160A14),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, Color(0x35F472B6))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SAKURA ↔ YANDERE TRANSITION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color.White.copy(alpha = 0.60f)
                    )

                    Spacer(Modifier.height(18.dp))

                    // Side-by-side comparison with arrow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Sakura Portrait
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF241026))
                                    .border(
                                        BorderStroke(
                                            2.dp,
                                            if (targetMode == DualityMode.SAKURA) Color(0xFFF472B6) else Color(0x30F472B6)
                                        ),
                                        RoundedCornerShape(20.dp)
                                    )
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.eva_avatar_sakura),
                                    contentDescription = "Sakura",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "SAKURA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF472B6)
                            )
                        }

                        // Transition Arrow
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Transition",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )

                        // Right: Yandere Portrait
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF260810))
                                    .border(
                                        BorderStroke(
                                            2.dp,
                                            if (targetMode == DualityMode.YANDERE) Color(0xFFE11D48) else Color(0x30E11D48)
                                        ),
                                        RoundedCornerShape(20.dp)
                                    )
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.eva_avatar_yandere),
                                    contentDescription = "Yandere",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "YANDERE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE11D48)
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // Prominent Pill Badge: EMOTIONAL SHIFT 280ms
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF221128).copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, Color(0x40F472B6)),
                        modifier = Modifier
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                            .clickable {
                                EvaHaptics.whisperTick(context)
                                onDismiss()
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "EMOTIONAL SHIFT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = Color.White.copy(alpha = 0.70f)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "280ms",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (targetMode == DualityMode.SAKURA) Color(0xFFF472B6) else Color(0xFFE11D48)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Dots indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f))
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.35f))
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.35f))
                        )
                    }
                }
            }
        }
    }
}
