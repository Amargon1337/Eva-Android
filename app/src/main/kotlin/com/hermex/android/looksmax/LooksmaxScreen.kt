package com.hermex.android.looksmax

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.hermex.android.R
import com.hermex.android.character.EvaHaptics
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import com.hermex.android.ui.components.ExpressivePill
import com.hermex.android.ui.components.HermexBentoCard
import com.hermex.android.ui.components.LivePulseDot
import com.hermex.android.ui.components.EvaPetalCanvas
import com.hermex.android.ui.components.evaGlitch
import com.hermex.android.ui.theme.EvaDesignTokens
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LooksmaxScreen(
    viewModel: LooksmaxViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = dualityMode == DualityMode.SAKURA
    val screenBg = if (isSakura) Color(0xFF180E1C) else Color(0xFF140508)
    val context = LocalContext.current
    val smokingStats by SmokingTracker.stats.collectAsStateWithLifecycle()

    val primaryAccent = if (isSakura) EvaDesignTokens.Colors.SakuraAccent else EvaDesignTokens.Colors.YandereCrimson
    val secondaryAccent = MaterialTheme.colorScheme.secondary

    var currentTempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showAnalysisResult by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var photoToDelete by remember { mutableStateOf<LooksmaxPhotoItem?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val cat = uiState.activeCategoryForCamera
        val uri = currentTempPhotoUri
        if (success && cat != null && uri != null) {
            viewModel.uploadPhoto(context, uri, cat)
        }
        viewModel.clearCameraCategory()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val cat = uiState.activeCategoryForCamera
        if (uri != null && cat != null) {
            try {
                val tempPick = File.createTempFile("gallery_pick_", ".jpg", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(tempPick).use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.uploadPhoto(context, tempPick, cat)
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка доступа к фото: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.clearCameraCategory()
    }

    fun launchCamera(category: LooksmaxCategory) {
        try {
            val tempFile = File.createTempFile("looksmax_", ".jpg", context.cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            currentTempPhotoUri = uri
            viewModel.prepareCameraCapture(category)
            cameraLauncher.launch(uri)
        } catch (t: Throwable) {
            Toast.makeText(context, "Не удалось открыть камеру: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchGallery(category: LooksmaxCategory) {
        try {
            viewModel.prepareCameraCapture(category)
            galleryLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        } catch (t: Throwable) {
            Toast.makeText(context, "Не удалось открыть галерею: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.uploadSuccessMessage) {
        uiState.uploadSuccessMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LOOKSMAX",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 1.2.sp,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "PROJECT LIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAnalysisResult = true }) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = "Analysis Result",
                            tint = primaryAccent
                        )
                    }
                    IconButton(onClick = { viewModel.refreshStats() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = primaryAccent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .then(if (!isSakura) Modifier.evaGlitch(triggerKey = Unit) else Modifier)
        ) {
            // Ambient Radial Glow matching Home screen
            Box(
                modifier = Modifier
                    .size(420.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-60).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isSakura) Color(0x55F472B6) else Color(0x55E11D48),
                                if (isSakura) Color(0x20E879F9) else Color(0x209F1239),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )


            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Top Bento Hero Banner (Frosted Glass)
                item {
                    Spacer(Modifier.height(4.dp))
                    HermexBentoCard(
                        cornerRadius = 24.dp,
                        borderColor = primaryAccent.copy(alpha = 0.35f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ExpressivePill(
                                text = dualityMode.badge,
                                accentColor = primaryAccent,
                                onClick = { EvaConfig.toggleMode() }
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "Ева отслеживает твой прогресс",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (dualityMode == DualityMode.SAKURA)
                                "Делай замеры лица и веса. Я рядом и помогу стать лучшей версией себя! 🌸"
                            else
                                "Не смей пропускать отчеты! Я вижу каждый твой селф и каждую складку. Докажи, что держишь дисциплину! 🦇",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(Modifier.height(16.dp))

                        // Bento Metric Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BentoMetricTile(
                                label = "Всего кадров",
                                value = "${uiState.totalPhotos}",
                                accentColor = primaryAccent,
                                modifier = Modifier.weight(1f)
                            )
                            BentoMetricTile(
                                label = "Могг-скор",
                                value = "${uiState.lastReview?.moggingScore ?: 79}%",
                                accentColor = secondaryAccent,
                                modifier = Modifier.weight(1f)
                            )
                            BentoMetricTile(
                                label = "Сигареты",
                                value = if (smokingStats.lastTimeText.isNotEmpty()) smokingStats.lastTimeText else "${smokingStats.totalToday} шт",
                                accentColor = primaryAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            onClick = {
                                EvaHaptics.click(context)
                                showAnalysisResult = true
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = primaryAccent.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.50f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("ПОЛНЫЙ АНАЛИЗ PSL MOGG", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("График Гаусса, углы челюсти и симметрия", fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f))
                                    }
                                }
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // 2. WEIGHT TODAY Card (Live Data & Functional Manual Entry)
                item {
                    val currentWeight = uiState.weightHistory.lastOrNull()?.kg ?: 58.0
                    val weightDeltaStr = if (uiState.weightHistory.size >= 2) {
                        val diff = currentWeight - uiState.weightHistory[uiState.weightHistory.size - 2].kg
                        val sign = if (diff > 0) "+" else ""
                        "($sign${String.format(java.util.Locale.US, "%.1f", diff)} kg)"
                    } else "(-0.4 kg)"

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (dualityMode == DualityMode.SAKURA) Color(0xFF241026) else Color(0xFF1E070D),
                        border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "WEIGHT TODAY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${String.format(java.util.Locale.US, "%.1f", currentWeight)} kg",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = weightDeltaStr,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        EvaHaptics.click(context)
                                        showWeightDialog = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Manual entry",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Cigarette Monitor (Live Frequency & Sleep Inference)
                item {
                    CigaretteTrackerCard(
                        stats = smokingStats,
                        accentColor = primaryAccent,
                        isSakura = isSakura,
                        onAdd = {
                            EvaHaptics.whisperTick(context)
                            SmokingTracker.logCigarette()
                        },
                        onUndo = {
                            EvaHaptics.click(context)
                            SmokingTracker.removeLastCigarette()
                        }
                    )
                }

                // 4. Photo Capture Block -- streamlined action buttons (model detects angle automatically)
                item {
                    UnifiedCaptureCard(
                        primaryAccent = primaryAccent,
                        onCameraClick = {
                            EvaHaptics.click(context)
                            launchCamera(LooksmaxCategory.FACE_FRONT)
                        },
                        onGalleryClick = {
                            EvaHaptics.click(context)
                            launchGallery(LooksmaxCategory.FACE_FRONT)
                        }
                    )
                }

                // 4. Animated Upload / Review Progress Indicator
                if (uiState.isUploading || uiState.isReviewing) {
                    item {
                        HermexBentoCard(
                            cornerRadius = 18.dp,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            borderColor = primaryAccent.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = primaryAccent,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        if (uiState.isUploading) "Отправка кадра в looksmax..." else "Ева разглядывает кадр и считает могг...",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        if (uiState.isUploading) "Сжатие до 2048px и загрузка на ПК" else "Vision-модель смотрит фото по метрикам SATANA",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Upload Error Card
                uiState.uploadErrorMessage?.let { err ->
                    item {
                        HermexBentoCard(
                            cornerRadius = 18.dp,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    err,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 6. Fresh AI review card with mogging score
                uiState.lastReview?.let { review ->
                    item {
                        ReviewCard(review = review, accent = primaryAccent)
                    }
                }

                // 7. PHOTO DATABASE (БАЗА СНИМКОВ)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "БАЗА СНИМКОВ (${uiState.photosList.size})",
                                color = primaryAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Все кадры с ПК · нажми для PSL отчёта",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                EvaHaptics.click(context)
                                viewModel.fetchPhotos()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Обновить базу",
                                tint = primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                item {
                    if (uiState.isLoadingPhotos && uiState.photosList.isEmpty()) {
                        HermexBentoCard(
                            cornerRadius = 18.dp,
                            borderColor = primaryAccent.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = primaryAccent,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Загрузка базы снимков с ПК...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else if (uiState.photosList.isEmpty()) {
                        HermexBentoCard(
                            cornerRadius = 18.dp,
                            borderColor = primaryAccent.copy(alpha = 0.25f)
                        ) {
                            Text(
                                "Снимков пока нет. Сделай первый кадр анфас или профиль через кнопку сканирования выше 📸",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.photosList, key = { "${it.category}_${it.filename}" }) { photo ->
                                PhotoDatabaseCard(
                                    photo = photo,
                                    accentColor = primaryAccent,
                                    onClick = {
                                        EvaHaptics.click(context)
                                        viewModel.selectPhotoForAnalysis(photo)
                                        showAnalysisResult = true
                                    },
                                    onDelete = {
                                        EvaHaptics.click(context)
                                        photoToDelete = photo
                                    }
                                )
                            }
                        }
                    }
                }

                // 8. INTERACTIVE SATANA TERMINOLOGY GUIDE (ШПАРГАЛКА SATANA)
                item {
                    Column(modifier = Modifier.padding(start = 4.dp, top = 10.dp)) {
                        Text(
                            text = "ШПАРГАЛКА SATANA · PSL МЕТРИКИ",
                            color = primaryAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Костная структура, мягкие ткани и high-ROI привычки",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Filter chips row
                item {
                    val filters = listOf(
                        "ALL" to "Все",
                        "Bones" to "🦴 Кости",
                        "Eyes" to "👁️ Глаза",
                        "Posture" to "🧍 Осанка",
                        "Skin" to "✨ Кожа"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filters) { (key, label) ->
                            val isSelected = uiState.selectedCheatsheetFilter.equals(key, ignoreCase = true)
                            Surface(
                                onClick = {
                                    EvaHaptics.click(context)
                                    viewModel.setCheatsheetFilter(key)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) primaryAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) primaryAccent else Color.White.copy(alpha = 0.15f)
                                )
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Filtered term cards
                val filteredTerms = if (uiState.selectedCheatsheetFilter.equals("ALL", ignoreCase = true)) {
                    uiState.cheatsheetTerms
                } else {
                    uiState.cheatsheetTerms.filter { it.category.equals(uiState.selectedCheatsheetFilter, ignoreCase = true) }
                }

                items(filteredTerms, key = { it.termEn }) { term ->
                    SatanaTermCard(term = term, accent = primaryAccent)
                }

                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    if (showAnalysisResult) {
        val targetPhoto = uiState.selectedPhotoForAnalysis 
            ?: uiState.photosList.firstOrNull()
        val scoreVal = when {
            targetPhoto?.moggingScore != null -> String.format(java.util.Locale.US, "%.1f", targetPhoto.moggingScore.toDouble() / 10.0)
            uiState.lastReview != null -> String.format(java.util.Locale.US, "%.1f", uiState.lastReview!!.moggingScore.toDouble() / 10.0)
            else -> "8.2"
        }
        val percentileVal = when {
            targetPhoto?.moggingScore != null -> viewModel.getCalculatedPercentile(targetPhoto.moggingScore)
            uiState.lastReview != null -> viewModel.getCalculatedPercentile(uiState.lastReview!!.moggingScore)
            else -> 79
        }
        val photoUrlVal = targetPhoto?.url 
            ?: uiState.lastReview?.let { EvaConfig.looksmaxPhotoFileUrl(it.category, it.filename) }
        val categoryVal = targetPhoto?.category ?: uiState.lastReview?.category
        val dateVal = targetPhoto?.dateFormatted ?: uiState.lastReview?.timestamp
        val reviewTextVal = targetPhoto?.reviewText ?: uiState.lastReview?.reviewText

        LooksmaxResultDialog(
            score = scoreVal,
            percentile = percentileVal,
            photoUrl = photoUrlVal,
            photoCategory = categoryVal,
            photoDate = dateVal,
            reviewText = reviewTextVal,
            onDismiss = {
                showAnalysisResult = false
                viewModel.clearSelectedAnalysisPhoto()
            }
        )
    }

    photoToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isSakura) Color(0xFF221128) else Color(0xFF1C060B),
            title = {
                Text("Удалить снимок?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    "Снимок ${target.filename} (${target.dateFormatted}) будет безвозвратно удалён из базы looksmax на ПК.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePhoto(target)
                        photoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Удалить", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) {
                    Text("Отмена", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    if (showWeightDialog) {
        val currentWeight = uiState.weightHistory.lastOrNull()?.kg ?: 58.0
        var manualWeightInput by remember { mutableStateOf(String.format(java.util.Locale.US, "%.1f", currentWeight)) }
        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (dualityMode == DualityMode.SAKURA) Color(0xFF221128) else Color(0xFF1C060B),
            title = {
                Text("Записать текущий вес", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Укажите вес в килограммах:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                    OutlinedTextField(
                        value = manualWeightInput,
                        onValueChange = { manualWeightInput = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primaryAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(-0.5, -0.1, +0.1, +0.5).forEach { step ->
                            Surface(
                                onClick = {
                                    val cur = manualWeightInput.replace(',', '.').toDoubleOrNull() ?: currentWeight
                                    val next = String.format(java.util.Locale.US, "%.1f", cur + step)
                                    manualWeightInput = next
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = primaryAccent.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = if (step > 0) "+$step" else "$step",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val kg = manualWeightInput.replace(',', '.').toDoubleOrNull()
                        if (kg != null) {
                            viewModel.saveWeight(kg)
                            showWeightDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сохранить", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWeightDialog = false }) {
                    Text("Отмена", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@Composable
private fun ReviewCard(review: PhotoReview, accent: Color) {
    HermexBentoCard(
        cornerRadius = 22.dp,
        borderColor = accent.copy(alpha = 0.45f)
    ) {
        val scoreColor = when {
            review.moggingScore >= 75 -> Color(0xFF10B981)
            review.moggingScore >= 60 -> accent
            review.moggingScore >= 45 -> Color(0xFFF59E0B)
            else -> Color(0xFFEF4444)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.12f))
                    .border(2.dp, scoreColor.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${review.moggingScore}%", color = scoreColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Объективный могг-скор", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                Text(
                    when {
                        review.moggingScore >= 75 -> "Уверенный Hunter / Могг 💜"
                        review.moggingScore >= 60 -> "Выше среднего, сильная база 🐾"
                        review.moggingScore >= 45 -> "Средний уровень, есть зоны роста 🦇"
                        review.moggingScore > 0 -> "Слабый ракурс / отёк, делай гуаша 🩸"
                        else -> "Могг не посчитан"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            review.reviewText,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "📄 ${review.filename} · ${review.timestamp}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun BentoMetricTile(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)) {
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CigaretteTrackerCard(
    stats: SmokingTracker.SmokingStats,
    accentColor: Color,
    isSakura: Boolean,
    onAdd: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSakura) Color(0xFF241026) else Color(0xFF1E070D),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "NICOTINE // СЧЁТЧИК СИГАРЕТ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${stats.totalToday} шт",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "(${stats.lastHourCount} за последний час)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (stats.lastHourCount >= 3) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (stats.totalToday > 0) {
                        TextButton(
                            onClick = onUndo,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text("-1", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Button(
                        onClick = onAdd,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("+1 СИГАРЕТА", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🐾 ${stats.remark}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun UnifiedCaptureCard(
    primaryAccent: Color,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    HermexBentoCard(
        cornerRadius = 22.dp,
        borderColor = primaryAccent.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onCameraClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryAccent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("СНЯТЬ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            FilledTonalButton(
                onClick = onGalleryClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White.copy(alpha = 0.10f),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("ЗАГРУЗИТЬ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PhotoDatabaseCard(
    photo: LooksmaxPhotoItem,
    accentColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val scoreColor = when {
        photo.moggingScore == null -> Color.White.copy(alpha = 0.5f)
        photo.moggingScore >= 75 -> Color(0xFF10B981)
        photo.moggingScore >= 60 -> accentColor
        photo.moggingScore >= 45 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E0F21),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .width(150.dp)
            .height(210.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = photo.url,
                contentDescription = photo.filename,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top gradient overlay for contrast with chips
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                        )
                    )
            )

            // Top Bar: Angle Pill + Delete Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "📸 Кадр",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Удалить",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Bottom gradient overlay for score and date
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = scoreColor.copy(alpha = 0.20f),
                    border = BorderStroke(0.5.dp, scoreColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = if (photo.moggingScore != null) "${photo.moggingScore}% MOGG" else "—% (Оценить)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = scoreColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = photo.dateFormatted.ifEmpty { photo.filename.take(10) },
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SatanaTermCard(
    term: LooksmaxTermItem,
    accent: Color
) {
    HermexBentoCard(
        cornerRadius = 18.dp,
        borderColor = accent.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = term.termEn,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = term.titleRu,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f))
            ) {
                Text(
                    text = term.targetNorm,
                    color = Color(0xFF10B981),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = term.descriptionRu,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = accent.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp).offset(y = 1.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "High-ROI Действие",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = term.highRoiTip,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
