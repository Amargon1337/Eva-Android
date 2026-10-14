package com.hermex.android.chat

import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.theme.EvaDesignTokens
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hermex.android.core.network.dto.ModelCatalogGroup
import com.hermex.android.core.network.dto.ModelCatalogOption
import coil3.compose.SubcomposeAsyncImage
import com.hermex.android.core.network.dto.fileTypeIcon
import com.hermex.android.core.util.HermexLog
import com.hermex.android.character.EvaHaptics
import com.hermex.android.ui.theme.HermexRadii

/** [ChatComposer]'s callbacks, grouped so adding a future action (slash commands) doesn't widen
 * [ChatComposer]'s own parameter list. Model selection moved to the chat top bar and profile
 * switching was dropped from the composer entirely (14.09, Ivan's feedback: he never switches
 * profiles, and the bottom strip duplicated controls he doesn't use). */
data class ChatComposerActions(
    val onTextChanged: (String) -> Unit,
    val onSend: () -> Unit,
    val onStop: () -> Unit,
    val onAttachFile: (Uri) -> Unit,
    val onRemoveAttachment: (String) -> Unit,
)

/** The model picker's list/selection data -- kept here (chat package) because the sheet UI
 * itself now lives in the chat top bar, not the composer. */
data class ChatComposerModelSelectorState(
    val modelCatalogGroups: List<ModelCatalogGroup>,
    val currentModel: String?,
    val currentModelProvider: String?,
    val isLoadingModelCatalog: Boolean,
) {
    companion object {
        fun from(uiState: ChatUiState): ChatComposerModelSelectorState = ChatComposerModelSelectorState(
            modelCatalogGroups = uiState.modelCatalogGroups,
            currentModel = uiState.currentModel,
            currentModelProvider = uiState.currentModelProvider,
            isLoadingModelCatalog = uiState.isLoadingModelCatalog,
        )
    }
}

/** The pending-attachment strip's own list data -- separate from [ChatComposerState] for the
 * same reason as the model picker state: it's plain display data, not busy/disabled state. */
data class ChatComposerAttachmentState(
    val pendingAttachments: List<PendingAttachmentUi>,
) {
    companion object {
        fun from(uiState: ChatUiState): ChatComposerAttachmentState = ChatComposerAttachmentState(
            pendingAttachments = uiState.pendingAttachments,
        )
    }
}

/** Caps how wide the composer dock ever gets, so a large tablet's wide-layout right pane doesn't
 * stretch it into an uncomfortably wide, hard-to-scan input. Never binds on phone-scale widths. */
private val ComposerMaxWidth = 840.dp

/**
 * A two-tier composer dock (input row on top, a horizontally-scrollable control strip of
 * Hermex-styled chips below), modeled on the Hermes WebUI/Desktop composer rather than a plain
 * Material `TextField` row: the dock is edge-to-edge with only its top corners rounded (a real
 * dock rising from the bottom of the screen, not a floating margin'd card), and Attach/Profile/
 * Model render as small tonal chips -- with a label when the underlying data already carries one
 * (profile name, model name) -- instead of bare icons.
 */
@Composable
fun ChatComposer(
    composerState: ChatComposerState,
    attachmentState: ChatComposerAttachmentState,
    actions: ChatComposerActions,
    modifier: Modifier = Modifier,
) {
    // Telegram-style single-row composer (14.09, Ivan's request): [+] [input field] [mic/send].
    // The old bottom chip strip (Attach/Default/Model/Voice) and the quick-prompt ribbon are
    // gone -- model selection lives in the chat top bar now, attachments hang off the "+",
    // and voice input is the trailing button when the field is empty.
    val context = LocalContext.current
    var lastLoggedHeightPx by remember { mutableStateOf(-1) }
    var showCommandPicker by remember { mutableStateOf(false) }
    var commandQuery by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    val voiceHandler = remember { VoiceInputHandler(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val recordAudioPermission = android.Manifest.permission.RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            voiceHandler.startListening(
                onResult = { text ->
                    actions.onTextChanged(composerState.text + text)
                    isRecording = false
                },
                onError = {
                    isRecording = false
                },
            )
            isRecording = true
        } else {
            // Permission denied -- show education Snackbar
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Microphone permission is required for voice input. Please enable it in Settings.",
                    actionLabel = "Settings",
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    // Open app settings
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }
    // Clean up SpeechRecognizer on dispose
    DisposableEffect(Unit) {
        onDispose {
            voiceHandler.stopListening()
        }
    }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        SnackbarHost(hostState = snackbarHostState)
        Surface(
            modifier = Modifier
                .widthIn(max = ComposerMaxWidth)
                .onGloballyPositioned { coordinates ->
                    val heightPx = coordinates.size.height
                    if (heightPx != lastLoggedHeightPx) {
                        lastLoggedHeightPx = heightPx
                        HermexLog.d("Composer", "measured height=${heightPx}px")
                    }
                },
            color = Color.Transparent,
            border = null,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            tonalElevation = 0.dp,
        ) {
            // Rounded-display safe bottom padding (14.09): on gesture-nav phones like the Redmi
            // Note 13 the nav bar inset can report 0 when hints are hidden, letting the dock's
            // content physically clip against the rounded bottom corners. navigationBarsPadding
            // alone isn't enough -- take the max of it and a hard 12dp floor.
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {

                if (attachmentState.pendingAttachments.isNotEmpty()) {
                    PendingAttachmentStrip(
                        attachments = attachmentState.pendingAttachments,
                        onRemove = actions.onRemoveAttachment,
                    )
                }

                if (showCommandPicker) {
                    val suggestions = CommandRegistry.filter(commandQuery)
                    if (suggestions.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.09f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            shadowElevation = 8.dp,
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                            ) {
                                items(suggestions) { suggestion ->
                                    ListItem(
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        headlineContent = {
                                            Text(
                                                text = suggestion.command,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = suggestion.description,
                                                color = Color.White.copy(alpha = 0.70f),
                                                fontSize = 12.sp
                                            )
                                        },
                                        leadingContent = {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = suggestion.icon,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.clickable {
                                            actions.onTextChanged(suggestion.command + " ")
                                            showCommandPicker = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                val primaryAccent = MaterialTheme.colorScheme.primary
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    // Telegram-style capsule container
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.07f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Left inside capsule: Menu button (replaces emoji/stickers, opens slash commands)
                            IconButton(
                                onClick = {
                                    showCommandPicker = !showCommandPicker
                                    if (showCommandPicker && !commandQuery.startsWith("/")) {
                                        commandQuery = "/"
                                    }
                                },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Команды",
                                    tint = if (showCommandPicker) primaryAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.size(22.dp),
                                )
                            }

                            // Center inside capsule: BasicTextField
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (composerState.text.isEmpty()) {
                                    Text(
                                        text = "позвать Еву…",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                            fontSize = 15.sp,
                                        ),
                                    )
                                }
                                BasicTextField(
                                    value = composerState.text,
                                    onValueChange = { newValue ->
                                        actions.onTextChanged(newValue)
                                        if (newValue.startsWith("/") && !newValue.contains(" ")) {
                                            showCommandPicker = true
                                            commandQuery = newValue
                                        } else if (!newValue.startsWith("/") && showCommandPicker && commandQuery.startsWith("/")) {
                                            showCommandPicker = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = composerState.isTextFieldEnabled,
                                    maxLines = 5,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp,
                                    ),
                                    cursorBrush = SolidColor(primaryAccent),
                                )
                            }

                            // Right inside capsule: Paperclip icon button (Telegram style)
                            AttachFileIconButton(
                                enabled = composerState.isAttachButtonEnabled,
                                isUploading = composerState.isUploadingAttachment,
                                onAttachFile = actions.onAttachFile,
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Right outside capsule: Circular action button (Mic / Send / Stop / Spinner)
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            composerState.showStopButton -> {
                                val haptic = LocalHapticFeedback.current
                                FilledIconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        actions.onStop()
                                    },
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black,
                                    ),
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Stop",
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                            composerState.showSendingSpinner -> CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.5.dp,
                                color = primaryAccent,
                            )
                            composerState.text.isNotBlank() -> FilledIconButton(
                                onClick = {
                                    EvaHaptics.click(context)
                                    actions.onSend()
                                },
                                shape = CircleShape,
                                enabled = composerState.canSend,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = primaryAccent,
                                    contentColor = Color.White,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                ),
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            else -> FilledIconButton(
                                onClick = {
                                    if (isRecording) {
                                        voiceHandler.stopListening()
                                        isRecording = false
                                    } else {
                                        val permissionCheck = ContextCompat.checkSelfPermission(context, recordAudioPermission)
                                        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            voiceHandler.startListening(
                                                onResult = { text ->
                                                    actions.onTextChanged(composerState.text + text)
                                                    isRecording = false
                                                },
                                                onError = { isRecording = false },
                                            )
                                            isRecording = true
                                        } else {
                                            permissionLauncher.launch(recordAudioPermission)
                                        }
                                    }
                                },
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else primaryAccent,
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    if (isRecording) Icons.Filled.Close else Icons.Filled.Mic,
                                    contentDescription = if (isRecording) "Stop listening" else "Voice input",
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Telegram-style paperclip attachment button inside the composer capsule. */
@Composable
private fun AttachFileIconButton(
    enabled: Boolean,
    isUploading: Boolean,
    onAttachFile: (Uri) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onAttachFile)
    }

    Box(contentAlignment = Alignment.Center) {
        IconButton(
            onClick = { if (enabled && !isUploading) launcher.launch(arrayOf("*/*")) },
            enabled = enabled && !isUploading,
            modifier = Modifier.size(40.dp),
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    Icons.Filled.AttachFile,
                    contentDescription = "Attach file",
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun PendingAttachmentStrip(
    attachments: List<PendingAttachmentUi>,
    onRemove: (String) -> Unit,
) {
    val context = LocalContext.current
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(attachments, key = { it.id }) { attachment ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(HermexRadii.Accessory),
            ) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (attachment.isImage == true && attachment.path != null) {
                        // Thumbnail for images
                        Box(
                            modifier = Modifier
                                .size(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            SubcomposeAsyncImage(
                                model = attachment.path,
                                contentDescription = attachment.name ?: "Attachment thumbnail",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(HermexRadii.Accessory)),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceContainerLow,
                                                RoundedCornerShape(HermexRadii.Accessory),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                },
                                error = {
                                    Icon(
                                        imageVector = fileTypeIcon(attachment.mime),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = fileTypeIcon(attachment.mime),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.CenterVertically),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Column {
                        Text(
                            text = attachment.name ?: "attachment",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        attachment.size?.let { size ->
                            Text(
                                text = Formatter.formatShortFileSize(context, size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { onRemove(attachment.id) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove ${attachment.name ?: "attachment"}", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/** Model picker bottom sheet -- 14.09: moved from the composer's bottom chip into the chat top
 * bar (Ivan's request: model selection belongs at the top, like the model name subtitle). The
 * sheet UI itself is unchanged; it's now invoked from [ChatScreen]'s top-bar model chip. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    modelSelectorState: ChatComposerModelSelectorState,
    isUpdatingComposerConfiguration: Boolean,
    onOpenModelPicker: () -> Unit,
    onSelectModel: (ModelCatalogOption) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // The visible trigger: a compact tonal chip showing the current model. When the catalog is
    // empty and loading, it degrades to a plain spinner chip rather than showing a blank label.
    ComposerModelChip(
        currentModel = modelSelectorState.currentModel,
        isLoading = isUpdatingComposerConfiguration,
        onClick = {
            searchQuery = ""
            showSheet = true
            onOpenModelPicker()
        },
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Выбор нейросети",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (modelSelectorState.currentModel != null) "Активна: ${modelSelectorState.currentModel}" else "Выберите модель для диалога",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val totalCount = modelSelectorState.modelCatalogGroups.sumOf { it.models.size }
                    if (totalCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$totalCount доступно",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск модели или провайдера...", style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    ),
                )

                Spacer(Modifier.height(12.dp))

                val filteredGroups = remember(modelSelectorState.modelCatalogGroups, searchQuery) {
                    if (searchQuery.isBlank()) {
                        modelSelectorState.modelCatalogGroups
                    } else {
                        modelSelectorState.modelCatalogGroups.mapNotNull { group ->
                            val matching = group.models.filter {
                                it.displayName.contains(searchQuery, ignoreCase = true) ||
                                it.id.contains(searchQuery, ignoreCase = true) ||
                                (it.providerId ?: "").contains(searchQuery, ignoreCase = true)
                            }
                            if (matching.isNotEmpty()) group.copy(models = matching) else null
                        }
                    }
                }

                when {
                    modelSelectorState.isLoadingModelCatalog && modelSelectorState.modelCatalogGroups.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                        }
                    }
                    filteredGroups.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "Нет доступных моделей" else "Ничего не найдено",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filteredGroups.forEach { group ->
                                item(key = "header_${group.name}") {
                                    Text(
                                        text = group.name.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }
                                items(group.models, key = { "${it.providerId}_${it.id}_${it.displayName}" }) { option ->
                                    val isSelected = option.matchesSelection(modelSelectorState.currentModel, modelSelectorState.currentModelProvider)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                                        },
                                        border = if (isSelected) {
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                        } else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showSheet = false
                                                onSelectModel(option)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = option.displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                option.providerId?.let { provider ->
                                                    Text(
                                                        text = provider,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The compact chip shown in the chat top bar that opens [ModelPickerSheet]. */
@Composable
private fun ComposerModelChip(
    currentModel: String?,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(HermexRadii.Accessory),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(5.dp))
            Text(
                text = currentModel ?: "модель",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 96.dp),
            )
        }
    }
}

