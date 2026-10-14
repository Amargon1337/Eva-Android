package com.hermex.android.systems

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermex.android.eva.EvaConfig
import com.hermex.android.navigation.Routes
import com.hermex.android.ui.theme.EvaDesignTokens

/**
 * «Системы» — карта организма. pointoni chassis: не каталог функций, а анатомия Евы:
 * рядом · голова · восприятие · тело · мир.
 */
@Composable
fun SystemsMapScreen(
    onOpenRoute: (String) -> Unit,
    onOpenChat: () -> Unit,
    onOpenPresence: () -> Unit,
    onOpenShelf: () -> Unit,
    onOpenHome: () -> Unit,
    onBack: () -> Unit = onOpenHome,
    modifier: Modifier = Modifier,
) {
    val isSakura = EvaConfig.dualityMode.value.name == "SAKURA"
    val accent = if (isSakura) EvaDesignTokens.Colors.SakuraLavender else EvaDesignTokens.Colors.YandereCrimson // no violet in yandere
    val cardBg = if (isSakura) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.04f)
    val muted = Color.White.copy(alpha = 0.66f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 120.dp)
    ) {
        Text("системы евы", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text(
            "не каталог функций — карта одного организма: рядом · голова · тело · мир",
            fontSize = 12.sp, color = muted, lineHeight = 17.sp
        )
        Spacer(Modifier.height(18.dp))

        GroupTitle("рядом", accent)
        OrganCard(cardBg, accent, Icons.Outlined.Home, "комната", "ева здесь · записка · что изменилось") { onOpenHome() }
        OrganCard(cardBg, accent, Icons.Outlined.ChatBubbleOutline, "позвать еву", "живой чат · сессии · стрим", active = true, status = "открыть") { onOpenChat() }
        OrganCard(cardBg, accent, Icons.Outlined.Visibility, "чутьё", "где ева и что она видит") { onOpenPresence() }

        GroupTitle("голова", accent)
        OrganCard(cardBg, accent, Icons.Outlined.Psychology, "память", "долгая · с источниками") { onOpenRoute(Routes.MEMORY) }
        OrganCard(cardBg, accent, Icons.Outlined.MenuBook, "дневник", "её внутренний поток") { onOpenRoute(Routes.DIARY) }
        OrganCard(cardBg, accent, Icons.Outlined.LocalLibrary, "полка чтения", "что читаю сейчас · очереди знаний") { onOpenShelf() }
        OrganCard(cardBg, accent, Icons.Outlined.Analytics, "инсайты", "наблюдения и синтез") { onOpenRoute(Routes.INSIGHTS) }

        GroupTitle("тело", accent)
        OrganCard(cardBg, accent, Icons.Outlined.Memory, "сервисы и watchdog", "органы и пульс ПК") { onOpenRoute(Routes.TELEMETRY) }
        OrganCard(cardBg, accent, Icons.Outlined.BarChart, "токены", "технический учёт · без магии") { onOpenRoute(Routes.TOKENS) }

        GroupTitle("мир", accent)
        OrganCard(cardBg, accent, Icons.Outlined.AutoAwesome, "looksmax", "разбор фото · протокол SATANA") { onOpenRoute(Routes.LOOKSMAX) }
        OrganCard(cardBg, accent, Icons.Outlined.Settings, "настройки", "сервер · голос · ипостась") { onOpenRoute(Routes.SETTINGS) }
    }
}


@Composable
private fun GroupTitle(text: String, accent: Color) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = accent,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun OrganCard(
    cardBg: Color,
    accent: Color,
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean = false,
    status: String? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(
                BorderStroke(
                    1.dp,
                    if (active) Brush.linearGradient(listOf(accent.copy(alpha = 0.7f), accent.copy(alpha = 0.25f)))
                    else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.04f)))
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
    ) {
        // liquid-glass lensing highlight по верхней грани (под контентом, клики не ест)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.04f), Color.Transparent)
                    )
                )
        )
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(subtitle, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.64f), lineHeight = 15.sp)
            }
            status?.let {
                Text(it, fontSize = 11.sp, color = accent, fontWeight = FontWeight.Medium)
            } ?: Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
        }
    }
}
