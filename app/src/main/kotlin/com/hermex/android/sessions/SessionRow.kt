package com.hermex.android.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hermex.android.core.network.dto.SessionSummary
import com.hermex.android.ui.components.LivePulseDot
import com.hermex.android.ui.theme.HermexColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.hermex.android.ui.theme.EvaDesignTokens

@Composable
fun SessionRow(
    session: SessionSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isNested: Boolean = false,
) {
    val paddingStart = if (isNested) 36.dp else 0.dp
    val streamingColor = EvaDesignTokens.Colors.StatusOnline
    val mutedLabel = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier.padding(start = paddingStart),
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = if (isSelected) {
            BorderStroke(EvaDesignTokens.Hairline, MaterialTheme.colorScheme.primary.copy(alpha = 0.50f))
        } else {
            BorderStroke(EvaDesignTokens.Hairline, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f))
        }
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                com.hermex.android.ui.components.EvaInitialAvatar(
                    text = session.title?.takeIf { it.isNotBlank() } ?: "?",
                    size = 38.dp,
                )
            },
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (session.pinned == true) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    if (isNested) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        ) {
                            Text(
                                text = "субагент",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = session.title?.takeIf { it.isNotBlank() } ?: "без названия",
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            supportingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    session.model?.let { model ->
                        val shortModel = model.split("/").lastOrNull() ?: model
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        ) {
                            Text(
                                text = shortModel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    session.workspace?.let { ws ->
                        Text(
                            text = ws,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedLabel,
                        )
                    }
                }
            },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End) {
                    session.lastMessageAt?.let {
                        Text(
                            relativeTimeText(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedLabel,
                        )
                    }
                    if (session.activeStreamId != null) {
                        Spacer(Modifier.width(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LivePulseDot(
                                color = streamingColor,
                                size = 6.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (session.isStreaming == true) "живой" else "пишет",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = streamingColor,
                            )
                        }
                    }
                }
            },
        )
    }
}

/** A small local relative-time formatter -- MVP scope has no need for a date-formatting
 * dependency for this one label. */
internal fun relativeTimeText(epochSeconds: Double): String {
    val nowSeconds = System.currentTimeMillis() / 1000.0
    val diffSeconds = (nowSeconds - epochSeconds).coerceAtLeast(0.0)
    return when {
        diffSeconds < 60 -> "сейчас"
        diffSeconds < 3600 -> "${(diffSeconds / 60).toInt()} мин"
        diffSeconds < 86_400 -> "${(diffSeconds / 3600).toInt()} ч"
        diffSeconds < 604_800 -> "${(diffSeconds / 86_400).toInt()} дн"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date((epochSeconds * 1000).toLong()))
    }
}
