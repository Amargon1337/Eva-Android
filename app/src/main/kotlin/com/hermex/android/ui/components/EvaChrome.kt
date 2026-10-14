package com.hermex.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.theme.EvaDesignTokens

/**
 * EvaChrome — единый слой оболочки Hermex (wave 3).
 * Один заголовок, одна карточка, одна плитка, одна палитра на все вкладки.
 * Никакого корпоративного канцелярита: только lowercase-канон Евы.
 */

/** Акцент текущего режима (сакура-розовый / яндере-кровавый). */
@Composable
fun evaAccent(): Color {
    val duality by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    return if (duality == DualityMode.SAKURA) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson
}

/** Фоновые частицы, честные к режиму: лепестки в сакуре, тлеющие искры в яндере. */
@Composable
fun EvaModeParticles(modifier: Modifier = Modifier, enabled: Boolean = true) {
    val duality by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    EvaAmbientParticles(modifier = modifier, isSakura = duality == DualityMode.SAKURA, enabled = enabled)
}

/**
 * EvaHeaderTitle — каноническая шапка вкладки: serif lowercase + 11sp muted-подпись.
 * Используется внутри TopAppBar.title во всех экранах.
 */
@Composable
fun EvaHeaderTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            fontFamily = FontFamily.Serif,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Color.White.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** EvaSectionLabel — единый uppercase-маркер секции с цветной точкой. */
@Composable
fun EvaSectionLabel(text: String, color: Color = evaAccent(), modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(5.dp).clip(RoundedCornerShape(2.dp)).background(color.copy(alpha = 0.9f)))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Bold,
            color = color.copy(alpha = 0.85f),
        )
    }
}

/**
 * EvaTile — bento-плитка с крупной цифрой: «цифра → подпись → мелкий подтекст».
 * Хирургическая сетка в стиле одобренных референсов, но в токенах Евы.
 */
@Composable
fun EvaTile(
    value: String,
    label: String,
    sublabel: String? = null,
    accent: Color = evaAccent(),
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(EvaDesignTokens.Shapes.Card),
        color = if (onClick != null) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
        border = BorderStroke(
            EvaDesignTokens.Hairline,
            if (onClick != null) accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = value,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = if (onClick != null) accent else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = Color.White.copy(alpha = 0.62f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** EvaCard — стандартная карточка контента (обёртка над bento-стеклом). */
@Composable
fun EvaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    HermexBentoCard(
        modifier = modifier,
        cornerRadius = EvaDesignTokens.Shapes.Card,
        onClick = onClick,
        content = content,
    )
}

/** Компактный формат чисел: 1_234_567 -> «1.2M». Едино для токенов/метрик. */
fun evaFormatK(n: Long): String = when {
    n >= 1_000_000_000L -> String.format("%.1fB", n / 1_000_000_000.0)
    n >= 1_000_000L -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000L -> String.format("%.1fk", n / 1_000.0)
    else -> n.toString()
}

/** Относительное время для списков: «сейчас · 38 мин · 4 ч · вчера». */
fun evaRelativeTime(minutesAgo: Long): String = when {
    minutesAgo < 1 -> "сейчас"
    minutesAgo < 60 -> "$minutesAgo мин"
    minutesAgo < 24 * 60 -> "${minutesAgo / 60} ч"
    minutesAgo < 48 * 60 -> "вчера"
    else -> "${minutesAgo / (24 * 60)} дн"
}

/** Инициал-аватар: буква заголовка на градиентном круге (акцент режима). */
@Composable
fun EvaInitialAvatar(text: String, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 40.dp, accent: Color = evaAccent()) {
    val initial = text.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = modifier.size(size).clip(CircleShape)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.65f), accent.copy(alpha = 0.25f)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value / 2.4f).sp)
    }
}
