package com.hermex.android.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.character.EvaHaptics
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.components.LivePulseDot
import com.hermex.android.ui.theme.EvaDesignTokens

/**
 * Collapsible Material 3 Expressive Bento card for streaming reasoning with live pulse dot,
 * adaptive SQLite/Glitch persona labels, and 120 Hz spring expansion.
 */
@Composable
fun ReasoningBlock(
    text: String,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val context = LocalContext.current
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = dualityMode == DualityMode.SAKURA

    val accentColor = if (isSakura) EvaDesignTokens.Sakura.Pink else EvaDesignTokens.Yandere.Crimson
    val headerLabel = if (isSakura) "🌸 Мысли Евы · Память SQLite" else "🦇 Сканирование намерений Ивана · GLITCH_SYNC"
    val borderColor = if (isSakura) EvaDesignTokens.Sakura.Border else EvaDesignTokens.Yandere.Wine.copy(alpha = 0.45f)
    val containerBg = if (isSakura) {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
    } else {
        EvaDesignTokens.Yandere.Surface.copy(alpha = 0.85f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                EvaHaptics.click(context)
                expanded = !expanded
            },
        shape = RoundedCornerShape(EvaDesignTokens.Shapes.Card),
        color = containerBg,
        border = BorderStroke(EvaDesignTokens.Hairline, borderColor),
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .animateContentSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LivePulseDot(color = accentColor, size = 7.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = headerLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
                Spacer(Modifier.width(8.dp))
                val wordCount = text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                Text(
                    text = "$wordCount слов",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                key(text) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
