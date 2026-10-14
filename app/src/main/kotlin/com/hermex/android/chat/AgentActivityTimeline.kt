package com.hermex.android.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermex.android.ui.components.ExpressivePill
import com.hermex.android.ui.components.LivePulseDot

/**
 * Connected vertical Agent Activity Timeline for tool calls.
 * Displays sequential tool execution as an interactive, connected node tree with
 * duration metrics, status indicators, and collapsible parameter/result inspectors.
 */
@Composable
fun AgentActivityTimeline(
    toolCalls: List<ToolCallUi>,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (toolCalls.isEmpty()) return

    val anyInProgress = toolCalls.any { !it.isComplete }
    val anyError = toolCalls.any { it.isError }
    val totalDuration = toolCalls.mapNotNull { it.durationSeconds }.sum()

    // Automatically keep expanded if tools are currently running
    var expanded by remember(toolCalls.size) {
        mutableStateOf(initiallyExpanded || anyInProgress)
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "arrowRotation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.70f),
        border = BorderStroke(
            1.dp,
            if (anyInProgress) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Summary Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    anyInProgress -> LivePulseDot(color = MaterialTheme.colorScheme.primary, size = 10.dp)
                    anyError -> Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Failed",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    else -> Icon(
                        Icons.Filled.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (toolCalls.size == 1) {
                                friendlyToolName(toolCalls.first().name)
                            } else {
                                "Цепочка действий (${toolCalls.size})"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        if (totalDuration > 0.05) {
                            Spacer(Modifier.width(8.dp))
                            ExpressivePill(
                                text = "%.1fs".format(totalDuration),
                                accentColor = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    if (!expanded && toolCalls.size > 1) {
                        Text(
                            text = toolCalls.joinToString(" → ") { friendlyToolName(it.name) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(arrowRotation),
                )
            }

            // Timeline Tree Body
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 4.dp)
                ) {
                    toolCalls.forEachIndexed { index, toolCall ->
                        val isLast = index == toolCalls.lastIndex
                        TimelineNodeItem(
                            toolCall = toolCall,
                            isLast = isLast,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineNodeItem(
    toolCall: ToolCallUi,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    var stepExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Vertical Timeline Column (Node Dot + Connecting Line)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = when {
                            !toolCall.isComplete -> MaterialTheme.colorScheme.primary
                            toolCall.isError -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                        },
                        shape = CircleShape
                    )
            )

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f))
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Node Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 4.dp else 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { stepExpanded = !stepExpanded }
            ) {
                Text(
                    text = friendlyToolName(toolCall.name),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                toolCall.durationSeconds?.let { duration ->
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "%.2fs".format(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }

                if (!toolCall.rawArgs.isNullOrBlank() || !toolCall.preview.isNullOrBlank()) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (stepExpanded) "свернуть" else "детали",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                    )
                }
            }

            // Expanded Details (Arguments & Results)
            AnimatedVisibility(
                visible = stepExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    toolCall.rawArgs?.takeIf { it.isNotBlank() }?.let { args ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Входные параметры",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = args,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    toolCall.preview?.takeIf { it.isNotBlank() }?.let { preview ->
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Результат",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = preview,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 12,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Translates internal tool identifiers into human-friendly Russian descriptions with icons. */
private fun friendlyToolName(rawName: String?): String {
    val name = rawName?.trim().orEmpty()
    return when {
        name.isEmpty() -> "Действие"
        name.contains("memory", ignoreCase = true) -> "🧠 Память: $name"
        name.contains("file", ignoreCase = true) || name.contains("read", ignoreCase = true) -> "📄 Документ: $name"
        name.contains("write", ignoreCase = true) || name.contains("edit", ignoreCase = true) -> "✏️ Запись: $name"
        name.contains("search", ignoreCase = true) || name.contains("web", ignoreCase = true) -> "🌐 Поиск: $name"
        name.contains("bash", ignoreCase = true) || name.contains("exec", ignoreCase = true) || name.contains("terminal", ignoreCase = true) -> "⚙️ Терминал: $name"
        name.contains("looksmax", ignoreCase = true) || name.contains("image", ignoreCase = true) -> "📸 Анализ фото: $name"
        else -> "⚡ $name"
    }
}
