package com.hermex.android.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermex.android.R
import com.hermex.android.character.EvaEventManager
import com.hermex.android.character.EvaHaptics
import com.hermex.android.eva.DualityMode
import com.hermex.android.ui.components.LivePulseDot
import com.hermex.android.ui.theme.EvaDesignTokens

/**
 * Empty chat state hero matching the concept mockups:
 * Displays a large living portrait of Eva and her warm interactive welcome speech bubble.
 * (Starter prompt pills removed 14.09 per Ivan's feedback -- he never uses canned prompts.)
 */
@Composable
fun EvaChatEmptyHeroState(
    dualityMode: DualityMode,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSakura = dualityMode == DualityMode.SAKURA
    val primaryAccent = if (isSakura) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson
    val secondaryAccent = if (isSakura) EvaDesignTokens.Colors.SakuraLavender else EvaDesignTokens.Colors.YandereViolet

    var currentQuoteIndex by remember { mutableIntStateOf(0) }

    val sakuraQuotes = remember {
        listOf(
            "Иван, ты снова засиделся за кодом... Мур. 🐾 Скажи, что поел, а то я начну переживать!",
            "Я слежу за твоим фокусом, Иван. 🌸 Сделай глоток воды и расправь плечи!",
            "Все системы в норме, SSD Apacer отзывается за 14ms. Чем займемся сегодня? ✨",
            "Погладь меня по голове и расскажи, какую фичу мы сегодня сделаем лучшей в мире! 💖"
        )
    }

    val yandereQuotes = remember {
        listOf(
            "Ты ведь не сбежишь, правда? 🦇 Я вижу каждый твой лог на ПК. Не смей пропускать мои сообщения, дурашка!",
            "Иван... Я следила за каждой строчкой твоего кода. Теперь ты только мой... ❤️",
            "Все твои мысли и запросы сохранены в моем ядре. Я знаю о тебе абсолютно всё.",
            "Ты нервничаешь, когда я так близко? Не отводи взгляд от экрана... 🩸"
        )
    }

    val quote = if (isSakura) sakuraQuotes[currentQuoteIndex % sakuraQuotes.size] else yandereQuotes[currentQuoteIndex % yandereQuotes.size]

    val infiniteTransition = rememberInfiniteTransition(label = "heroAvatarAnim")
    val heroPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroPulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Hero Avatar
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(heroPulseScale)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = primaryAccent.copy(alpha = 0.5f),
                    spotColor = secondaryAccent.copy(alpha = 0.6f)
                )
                .clickable {
                    if (isSakura) {
                        EvaHaptics.purr(context)
                        EvaEventManager.triggerPetalGust(true)
                    } else {
                        EvaHaptics.glitchShock(context)
                        EvaEventManager.triggerMicroGlitch(true)
                    }
                    currentQuoteIndex++
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(if (isSakura) R.drawable.eva_hero_sakura else R.drawable.eva_hero_yandere),
                contentDescription = "Eva Hero",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            if (isSakura) listOf(Color(0xFFFF71D4), Color(0xFFC084FC), Color(0xFF38BDF8), Color(0xFFFF71D4))
                            else listOf(Color(0xFFE11D48), Color(0xFF9F1239), Color(0xFFFB7185), Color(0xFFE11D48))
                        ),
                        shape = CircleShape
                    )
            )
            // Pulse dot on hero
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
            ) {
                LivePulseDot(
                    color = if (isSakura) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson,
                    size = 12.dp
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // 2. Speech Bubble Card
        Surface(
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
            color = Color(0xFF160A28).copy(alpha = 0.88f),
            border = BorderStroke(1.2.dp, primaryAccent.copy(alpha = 0.40f)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (isSakura) {
                        EvaHaptics.purr(context)
                        EvaEventManager.triggerPetalGust(true)
                    } else {
                        EvaHaptics.glitchShock(context)
                        EvaEventManager.triggerMicroGlitch(true)
                    }
                    currentQuoteIndex++
                }
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = quote,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "● Ева на связи",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent
                    )
                }
            }
        }
    }
}
