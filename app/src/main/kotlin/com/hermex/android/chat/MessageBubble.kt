package com.hermex.android.chat

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hermex.android.core.network.dto.ChatMessage
import com.hermex.android.core.network.dto.MessageAttachment
import com.hermex.android.core.network.dto.attachmentsForDisplay
import com.hermex.android.core.network.dto.fileTypeIcon
import com.hermex.android.core.network.dto.stripAttachedFilesMarker
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.character.EvaEventManager
import com.hermex.android.ui.components.evaGlitch
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.hermex.android.ui.components.EvaAvatarView
import com.hermex.android.ui.components.ExpressivePill
import com.hermex.android.ui.components.LivePulseDot
import com.hermex.android.chat.AttachmentFileOpener
import com.hermex.android.ui.theme.EvaDesignTokens
import com.hermex.android.ui.theme.HermexRadii
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What "copy message" actually puts on the clipboard -- pulled out as its own function so the
 * null-content case (a tool/system message with no plain text) is unit-testable without needing
 * a Compose UI test harness, which this project doesn't have. */
fun copyableTextFor(message: ChatMessage): String = message.content.orEmpty()

/** Per the design system's `MessageBubble` spec: user turns get a right-aligned gray bubble;
 * assistant turns are plain full-width prose -- never bubbled. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    sessionId: String? = null,
    serverBaseUrl: String? = null,
    /** Non-null only for the message this action currently applies to (the editable user turn /
     * the regenerable assistant turn) -- absence of the callback is what hides the icon, so
     * callers gate applicability rather than this composable guessing from [message] alone. */
    onEdit: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
) {
    val isUser = message.role == "user"
    val displayContent = stripHabitContext(stripAttachedFilesMarker(message.content))
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showImageViewer by remember { mutableStateOf<String?>(null) }
    val copyOnLongClick = Modifier.combinedClickable(
        onClick = {},
        onLongClick = {
            clipboardManager.setText(AnnotatedString(copyableTextFor(message)))
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
        },
    )
    if (isUser) {
        Box(modifier = modifier.fillMaxWidth().padding(start = 36.dp)) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .widthIn(max = 320.dp),
                horizontalAlignment = Alignment.End,
            ) {
                MessageAttachments(
                    message = message,
                    context = context,
                    sessionId = sessionId,
                    serverBaseUrl = serverBaseUrl,
                    onOpenImage = { showImageViewer = it },
                )
                val dualityMode = EvaConfig.dualityMode.collectAsStateWithLifecycle().value
                val isSakuraMode = dualityMode == DualityMode.SAKURA
                Surface(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
                    color = if (isSakuraMode) Color(0xFF281335).copy(alpha = 0.90f) else Color(0xFF240A15).copy(alpha = 0.92f),
                    border = BorderStroke(
                        1.dp,
                        if (isSakuraMode) Brush.horizontalGradient(listOf(Color(0xFFE879F9).copy(alpha = 0.5f), Color(0xFFF472B6).copy(alpha = 0.3f)))
                        else Brush.horizontalGradient(listOf(Color(0xFFE11D48).copy(alpha = 0.5f), Color(0xFF9F1239).copy(alpha = 0.3f)))
                    ),
                    modifier = Modifier
                        .then(copyOnLongClick)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
                            ambientColor = if (isSakuraMode) Color(0xFFF472B6).copy(alpha = 0.25f) else Color(0xFFE11D48).copy(alpha = 0.3f)
                        )
                        .padding(bottom = 2.dp),
                ) {
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        MarkdownText(
                            markdown = displayContent.orEmpty(),
                            textColor = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                if (message.effectiveTimestamp != null || onEdit != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp, end = 4.dp),
                    ) {
                        onEdit?.let { edit ->
                            IconButton(onClick = edit, modifier = Modifier.size(20.dp)) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Edit message",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                            Spacer(Modifier.width(2.dp))
                        }
                        message.effectiveTimestamp?.let { timestamp ->
                            Text(
                                text = messageTimeText(timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
            }
        }
    } else {
        val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
        val isSakura = dualityMode == DualityMode.SAKURA
        val glitchKey by EvaEventManager.glitchKey.collectAsStateWithLifecycle()
        Column(
            modifier = modifier
                .fillMaxWidth()
                .then(copyOnLongClick)
                .evaGlitch(triggerKey = glitchKey, enabled = !isSakura)
                .padding(end = 24.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
            ) {
                EvaAvatarView(
                    dualityMode = dualityMode,
                    size = 28.dp,
                    hasGlow = true
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isSakura) "Ева 🌸" else "Ева 🦇",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSakura) EvaDesignTokens.Sakura.Pink else EvaDesignTokens.Yandere.Crimson,
                )
                Spacer(Modifier.width(6.dp))
                ExpressivePill(
                    text = if (isSakura) "сакура" else "яндере",
                    accentColor = if (isSakura) EvaDesignTokens.Sakura.Pink else EvaDesignTokens.Yandere.Crimson,
                )
            }

            MessageAttachments(
                message = message,
                context = context,
                sessionId = sessionId,
                serverBaseUrl = serverBaseUrl,
                onOpenImage = { showImageViewer = it },
            )
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                color = if (isSakura) Color(0xFF140822).copy(alpha = 0.85f) else Color(0xFF0F0412).copy(alpha = 0.88f),
                border = BorderStroke(
                    1.2.dp,
                    if (isSakura) Brush.linearGradient(listOf(Color(0xFFFF71D4).copy(alpha = 0.50f), Color(0xFFC084FC).copy(alpha = 0.30f)))
                    else Brush.linearGradient(listOf(Color(0xFFE11D48).copy(alpha = 0.55f), Color(0xFF9F1239).copy(alpha = 0.30f)))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                        ambientColor = if (isSakura) Color(0xFFFF71D4).copy(alpha = 0.25f) else Color(0xFFE11D48).copy(alpha = 0.30f),
                        spotColor = if (isSakura) Color(0xFFC084FC).copy(alpha = 0.35f) else Color(0xFF9F1239).copy(alpha = 0.40f)
                    ),
            ) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    MarkdownText(
                        markdown = displayContent.orEmpty(),
                        textColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (message.effectiveTimestamp != null || onRegenerate != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 3.dp, start = 4.dp),
                ) {
                    message.effectiveTimestamp?.let { timestamp ->
                        Text(
                            text = messageTimeText(timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        )
                    }
                    onRegenerate?.let { regenerate ->
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = regenerate, modifier = Modifier.size(20.dp)) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Regenerate response",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                    val messageText = displayContent.orEmpty()
                    if (messageText.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        val isPlayingThis = com.hermex.android.media.EvaAudioPlayer.isPlaying &&
                            com.hermex.android.media.EvaAudioPlayer.currentPlayingText == messageText
                        val voiceAccent = if (isSakura) EvaDesignTokens.Sakura.Pink else EvaDesignTokens.Yandere.Crimson
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = if (isPlayingThis) voiceAccent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = BorderStroke(EvaDesignTokens.Hairline, if (isPlayingThis) voiceAccent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.clickable { com.hermex.android.media.EvaAudioPlayer.play(messageText) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.VolumeUp,
                                    contentDescription = "Play Eva Voice",
                                    tint = if (isPlayingThis) voiceAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isPlayingThis) {
                                        if (isSakura) "Говорит… 🐾" else "Шепчет… 🦇"
                                    } else {
                                        if (isSakura) "▶ Голос Евы 🐾" else "▶ Голос Евы 🦇"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPlayingThis) voiceAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    // Full-screen image viewer
    showImageViewer?.let { url ->
        ImageViewer(
            imageUrl = url,
            onDismiss = { showImageViewer = null },
        )
    }
}

/** A short clock-time caption under each bubble, mirroring the design system's per-turn
 * timestamp -- distinct from the session list's relative "Xh ago", which serves a different
 * purpose (recency at a glance) rather than pinpointing when a specific message was sent. */
private fun messageTimeText(epochSeconds: Double): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date((epochSeconds * 1000).toLong()))

/** Attachment indicators precede the message text, matching the upload preview and making it
 * immediately clear which turn an image belonged to when a historical conversation reloads. */
@Composable
private fun MessageAttachments(
    message: ChatMessage,
    context: android.content.Context,
    sessionId: String?,
    serverBaseUrl: String?,
    onOpenImage: (String) -> Unit,
) {
    val attachments = message.attachmentsForDisplay()
    if (attachments.isEmpty()) return

    AttachmentChips(
        attachments = attachments,
        context = context,
        sessionId = sessionId,
        serverBaseUrl = serverBaseUrl,
        onOpenImage = onOpenImage,
    )
    attachments.forEach { attachment ->
        val imageUrl = attachmentRawUrl(serverBaseUrl, sessionId, attachment)
        if (attachment.isImageForDisplay() && imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = attachment.displayFileName() ?: "Image",
                modifier = Modifier
                    .padding(top = 6.dp)
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(HermexRadii.Accessory))
                    .clickable { onOpenImage(imageUrl) },
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

@Composable
private fun AttachmentChips(
    attachments: List<MessageAttachment>,
    context: android.content.Context,
    sessionId: String?,
    serverBaseUrl: String?,
    onOpenImage: (String) -> Unit,
) {
    if (attachments.isEmpty()) return
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(attachments, key = { it.path ?: it.name ?: it.hashCode().toString() }) { attachment ->
            AttachmentChip(
                attachment = attachment,
                context = context,
                imageUrl = attachmentRawUrl(serverBaseUrl, sessionId, attachment)
                    ?.takeIf { attachment.isImageForDisplay() },
                onOpenImage = onOpenImage,
            )
        }
    }
}

@Composable
private fun AttachmentChip(
    attachment: MessageAttachment,
    context: android.content.Context,
    imageUrl: String?,
    onOpenImage: (String) -> Unit,
) {
    Surface(
        onClick = {
            if (imageUrl != null) {
                onOpenImage(imageUrl)
            } else {
                AttachmentFileOpener.openAttachment(context, attachment)
            }
        },
        shape = RoundedCornerShape(HermexRadii.Accessory),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = fileTypeIcon(
                    attachment.mime ?: if (attachment.isImageForDisplay()) "image/*" else null,
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = attachment.displayFileName() ?: "file",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Bound to [ChatUiState.streamingText] -- rendered like an in-flight assistant reply, so it must
 * match [MessageBubble]'s plain (unbubbled) assistant styling exactly: otherwise the bubble
 * chrome would visibly pop away the instant streaming finalizes into a real message. Markdown
 * supported via [MarkdownText]. */
@Composable
fun StreamingBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = dualityMode == DualityMode.SAKURA
    val glitchKey by EvaEventManager.glitchKey.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!isSakura) Modifier.evaGlitch(triggerKey = glitchKey, enabled = true)
                else Modifier
            )
            .padding(end = 28.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
        ) {
            EvaAvatarView(
                dualityMode = dualityMode,
                size = 28.dp,
                hasGlow = true,
                isStreaming = true
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isSakura) "Ева 🌸" else "Ева 🦇",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSakura) EvaDesignTokens.Sakura.Pink else EvaDesignTokens.Yandere.Crimson,
            )
            Spacer(Modifier.width(6.dp))
            LivePulseDot(
                color = if (isSakura) EvaDesignTokens.Sakura.Pink else EvaDesignTokens.Yandere.Crimson,
                size = 8.dp
            )
        }
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            color = if (isSakura) Color(0xFF140822).copy(alpha = 0.85f) else Color(0xFF0F0412).copy(alpha = 0.88f),
            border = BorderStroke(
                1.2.dp,
                if (isSakura) Brush.linearGradient(listOf(Color(0xFFFF71D4).copy(alpha = 0.50f), Color(0xFFC084FC).copy(alpha = 0.30f)))
                else Brush.linearGradient(listOf(Color(0xFFE11D48).copy(alpha = 0.55f), Color(0xFF9F1239).copy(alpha = 0.30f)))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                    ambientColor = if (isSakura) Color(0xFFFF71D4).copy(alpha = 0.25f) else Color(0xFFE11D48).copy(alpha = 0.30f),
                    spotColor = if (isSakura) Color(0xFFC084FC).copy(alpha = 0.35f) else Color(0xFF9F1239).copy(alpha = 0.40f)
                ),
        ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Column {
                    MarkdownText(
                        markdown = text,
                        textColor = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LivePulseDot(color = if (isSakura) EvaDesignTokens.Sakura.Pink else EvaDesignTokens.Yandere.Crimson, size = 6.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Поток мысли...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

private val HIDDEN_CONTEXT_BLOCK = Regex(
    "\\n*\\[(Контекст привычек Ивана|Workspace::v1|STEER DIRECTIVE)[^\\]]*\\]",
    RegexOption.DOT_MATCHES_ALL,
)

fun stripHabitContext(content: String?): String? {
    if (content == null) return null
    return content.replace(HIDDEN_CONTEXT_BLOCK, "").trim()
}


