package com.hermex.android.looksmax

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermex.android.eva.EvaConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

enum class LooksmaxCategory(
    val id: String,
    val title: String,
    val emoji: String,
    val subtitle: String,
    val guidance: String
) {
    FACE_FRONT("face_front", "Анфас", "🗿", "Симметрия, отечность, кожа", "Держи телефон на уровне глаз, ровный свет"),
    FACE_PROFILE("face_profile", "Профиль", "📐", "Угол челюсти, осанка шеи", "Повернись на 90 градусов, подбородок ровно")
}

data class CategoryStat(
    val category: String,
    val count: Int,
    val latestFile: String?
)

data class PhotoReview(
    val category: String,
    val filename: String,
    val moggingScore: Int,
    val reviewText: String,
    val timestamp: String
)

data class LooksmaxPhotoItem(
    val category: String,
    val filename: String,
    val sizeBytes: Long,
    val timestamp: Double,
    val dateFormatted: String,
    val moggingScore: Int?,
    val reviewText: String?,
    val url: String
)

data class LooksmaxTermItem(
    val termEn: String,
    val titleRu: String,
    val category: String, // "Bones", "Eyes", "Posture", "Skin"
    val targetNorm: String,
    val descriptionRu: String,
    val highRoiTip: String
)

data class WeightEntry(
    val timestamp: String,
    val kg: Double
)

val DEFAULT_SATANA_TERMS = listOf(
    LooksmaxTermItem(
        termEn = "Canthal Tilt (CT)",
        titleRu = "Наклон глазной щели",
        category = "Eyes",
        targetNorm = "+2° .. +6° (положительный)",
        descriptionRu = "Угол наклона между внутренним (медиальным) и внешним (латеральным) уголком глаза. Положительный наклон создает взгляд хищника (hunter eyes), отрицательный придает взгляду усталость и опущенность.",
        highRoiTip = "Депуффинг подглазничной зоны, сон строго на спине, ледяные патчи."
    ),
    LooksmaxTermItem(
        termEn = "Gonial Angle",
        titleRu = "Гониальный угол челюсти",
        category = "Bones",
        targetNorm = "120° .. 125°",
        descriptionRu = "Угол между телом нижней челюсти и её восходящей ветвью. Чёткий острый угол создает мужественную рамку лица и подчеркивает линию подбородка в профиль.",
        highRoiTip = "Низкий процент подкожного жира, гипертрофия жевательных мышц (masseters), твердая пища."
    ),
    LooksmaxTermItem(
        termEn = "Midface Ratio",
        titleRu = "Пропорция средней трети лица",
        category = "Bones",
        targetNorm = "0.95 .. 1.05 (компактная)",
        descriptionRu = "Отношение расстояния между зрачками (IPD) к расстоянию от бровей до основания носа. Компактная средняя треть визуально делает лицо гармоничным и молодым.",
        highRoiTip = "Осанка головы, правильное положение языка, подтягивающее ткани кверху."
    ),
    LooksmaxTermItem(
        termEn = "Mewing Tongue Posture",
        titleRu = "Мьюинг (позиция языка)",
        category = "Posture",
        targetNorm = "100% прилегание к нёбу",
        descriptionRu = "Анатомически верное положение: всё тело языка (включая заднюю треть) прижато к нёбу при закрытых зубах и дыхании через нос. Не давит на зубы.",
        highRoiTip = "Подтягивает подъязычную кость (hyoid bone), мгновенно убирает провисание кожи шеи."
    ),
    LooksmaxTermItem(
        termEn = "Forward Head Posture (FHP)",
        titleRu = "Выдвижение шеи вперёд",
        category = "Posture",
        targetNorm = "0° (ухо строго над плечом)",
        descriptionRu = "Синдром компьютерной шеи. Смещает голову вперед, визуально уничтожает угол челюсти в профиль и создает складки под подбородком даже при низком жире.",
        highRoiTip = "Chin tucks (втягивание подбородка назад к стене), монитор на уровне глаз."
    ),
    LooksmaxTermItem(
        termEn = "Zygomatic Projection",
        titleRu = "Проекция скуловой дуги",
        category = "Bones",
        targetNorm = "Шире бигониальной линии",
        descriptionRu = "Ширина и выступ скул. Высокие скулы дают прочную костную поддержку щекам, формируя впалые щеки (hollow cheeks) и V-образный силуэт лица.",
        highRoiTip = "Снижение задержки внеклеточной жидкости, достаточная гидратация."
    ),
    LooksmaxTermItem(
        termEn = "Hunter Eyes & Hooding",
        titleRu = "Глаза охотника & Нависание века",
        category = "Eyes",
        targetNorm = "Минимальный показ верхнего века",
        descriptionRu = "Глубокая посадка глаз в глазнице с компактной щелью и низкими бровями. Отсутствие открытого верхнего века (upper eyelid exposure).",
        highRoiTip = "Укрепление круговой мышцы глаз, контроль отёков, качественный глубокий сон."
    ),
    LooksmaxTermItem(
        termEn = "Depuffing & Sodium Balance",
        titleRu = "Депуффинг & Баланс натрия/калия",
        category = "Skin",
        targetNorm = "Соотношение K:Na > 2:1",
        descriptionRu = "Контроль задержки внеклеточной воды в тканях лица. Даже мощная костная структура скрывается под слоем отёка при избытке соли и дефиците калия.",
        highRoiTip = "2.5-3 л чистой воды, отказ от пересоленной пищи на ночь, калий из шпината и бананов."
    )
)

data class LooksmaxUiState(
    val isLoadingStats: Boolean = false,
    val isUploading: Boolean = false,
    val uploadSuccessMessage: String? = null,
    val uploadErrorMessage: String? = null,
    val totalPhotos: Int = 0,
    val stats: Map<String, CategoryStat> = emptyMap(),
    val selectedCaptureAngle: LooksmaxCategory = LooksmaxCategory.FACE_FRONT,
    val activeCategoryForCamera: LooksmaxCategory? = null,
    val isReviewing: Boolean = false,
    val lastReview: PhotoReview? = null,
    val weightHistory: List<WeightEntry> = emptyList(),
    val isWeightBusy: Boolean = false,
    val weightSavedMessage: String? = null,
    val photosList: List<LooksmaxPhotoItem> = emptyList(),
    val isLoadingPhotos: Boolean = false,
    val selectedPhotoForAnalysis: LooksmaxPhotoItem? = null,
    val cheatsheetTerms: List<LooksmaxTermItem> = DEFAULT_SATANA_TERMS,
    val selectedCheatsheetFilter: String = "ALL"
)

class LooksmaxViewModel : ViewModel() {
    private val TAG = "LooksmaxViewModel"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val _uiState = MutableStateFlow(LooksmaxUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshStats()
        fetchPhotos()
    }

    fun setCaptureAngle(category: LooksmaxCategory) {
        _uiState.value = _uiState.value.copy(selectedCaptureAngle = category)
    }

    fun prepareCameraCapture(category: LooksmaxCategory) {
        _uiState.value = _uiState.value.copy(activeCategoryForCamera = category)
    }

    fun clearCameraCategory() {
        _uiState.value = _uiState.value.copy(activeCategoryForCamera = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            uploadSuccessMessage = null,
            uploadErrorMessage = null
        )
    }

    fun setCheatsheetFilter(category: String) {
        _uiState.value = _uiState.value.copy(selectedCheatsheetFilter = category)
    }

    fun selectPhotoForAnalysis(photo: LooksmaxPhotoItem) {
        _uiState.value = _uiState.value.copy(selectedPhotoForAnalysis = photo)
        if (photo.moggingScore == null || photo.reviewText == null) {
            val cat = if (photo.category == "face_profile") LooksmaxCategory.FACE_PROFILE else LooksmaxCategory.FACE_FRONT
            requestReview(cat, photo.filename)
        }
    }

    fun clearSelectedAnalysisPhoto() {
        _uiState.value = _uiState.value.copy(selectedPhotoForAnalysis = null)
    }

    fun getCalculatedPercentile(score: Int): Int {
        val scores = _uiState.value.photosList.mapNotNull { it.moggingScore }
        if (scores.isEmpty()) return 79
        val belowOrEqual = scores.count { it <= score }
        val p = (belowOrEqual.toDouble() / scores.size * 100).toInt()
        return p.coerceIn(15, 95)
    }

    fun refreshStats() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoadingStats = true)
            try {
                val request = Request.Builder()
                    .url(EvaConfig.looksmaxStatsUrl)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        val json = JSONObject(body)
                        val total = json.optInt("total_photos", 0)
                        val statsJson = json.optJSONObject("categories") ?: JSONObject()

                        val parsedMap = mutableMapOf<String, CategoryStat>()
                        val keys = statsJson.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val item = statsJson.optJSONObject(key)
                            if (item != null) {
                                parsedMap[key] = CategoryStat(
                                    category = key,
                                    count = item.optInt("count", 0),
                                    latestFile = item.optString("latest_file", "").takeIf { it.isNotEmpty() }
                                )
                            }
                        }

                        val weights = mutableListOf<WeightEntry>()
                        val whArr = json.optJSONArray("weight_history") ?: JSONArray()
                        for (i in 0 until whArr.length()) {
                            val o = whArr.optJSONObject(i) ?: continue
                            weights.add(WeightEntry(o.optString("ts"), o.optDouble("kg", 0.0)))
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoadingStats = false,
                            totalPhotos = total,
                            stats = parsedMap,
                            weightHistory = weights
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoadingStats = false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching looksmax stats", e)
                _uiState.value = _uiState.value.copy(isLoadingStats = false)
            }
        }
    }

    fun fetchPhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoadingPhotos = true)
            try {
                val request = Request.Builder()
                    .url(EvaConfig.looksmaxPhotosUrl)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        val json = JSONObject(body)
                        val arr = json.optJSONArray("photos") ?: JSONArray()
                        val list = mutableListOf<LooksmaxPhotoItem>()
                        for (i in 0 until arr.length()) {
                            val o = arr.optJSONObject(i) ?: continue
                            list.add(
                                LooksmaxPhotoItem(
                                    category = o.optString("category", "face_front"),
                                    filename = o.optString("filename", ""),
                                    sizeBytes = o.optLong("size_bytes", 0L),
                                    timestamp = o.optDouble("timestamp", 0.0),
                                    dateFormatted = o.optString("date_formatted", ""),
                                    moggingScore = if (o.has("mogging_score") && !o.isNull("mogging_score")) o.optInt("mogging_score") else null,
                                    reviewText = if (o.has("review_text") && !o.isNull("review_text")) o.optString("review_text") else null,
                                    url = EvaConfig.looksmaxPhotoFileUrl(o.optString("category"), o.optString("filename"))
                                )
                            )
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoadingPhotos = false,
                            photosList = list
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoadingPhotos = false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch photos", e)
                _uiState.value = _uiState.value.copy(isLoadingPhotos = false)
            }
        }
    }

    fun deletePhoto(item: LooksmaxPhotoItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(EvaConfig.looksmaxDeletePhotoUrl(item.category, item.filename))
                    .delete()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            photosList = _uiState.value.photosList.filter { it.filename != item.filename },
                            uploadSuccessMessage = "Фото ${item.filename} удалено из базы",
                            selectedPhotoForAnalysis = if (_uiState.value.selectedPhotoForAnalysis?.filename == item.filename) null else _uiState.value.selectedPhotoForAnalysis
                        )
                        refreshStats()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete photo", e)
                _uiState.value = _uiState.value.copy(uploadErrorMessage = "Не удалось удалить фото: ${e.localizedMessage}")
            }
        }
    }

    /** Trigger the AI review for a saved photo; result lands in lastReview. */
    fun requestReview(category: LooksmaxCategory, filename: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isReviewing = true)
            try {
                val payload = JSONObject()
                    .put("category", category.id)
                    .put("filename", filename)
                    .put("filepath", "")
                val request = Request.Builder()
                    .url(EvaConfig.looksmaxReviewUrl)
                    .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string().orEmpty())
                        val review = PhotoReview(
                            category = json.optString("category", category.id),
                            filename = filename,
                            moggingScore = json.optInt("mogging_score", 0),
                            reviewText = json.optString("review_text", ""),
                            timestamp = json.optString("timestamp", "")
                        )
                        // Also update item in photosList
                        val updatedList = _uiState.value.photosList.map {
                            if (it.filename == filename) it.copy(moggingScore = review.moggingScore, reviewText = review.reviewText) else it
                        }
                        val updatedSelected = _uiState.value.selectedPhotoForAnalysis?.let {
                            if (it.filename == filename) it.copy(moggingScore = review.moggingScore, reviewText = review.reviewText) else it
                        }
                        _uiState.value = _uiState.value.copy(
                            isReviewing = false,
                            lastReview = review,
                            photosList = updatedList,
                            selectedPhotoForAnalysis = updatedSelected
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isReviewing = false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Review request failed", e)
                _uiState.value = _uiState.value.copy(isReviewing = false)
            }
        }
    }

    /** Manual weight entry — just a number, no photo. */
    fun saveWeight(kg: Double) {
        if (kg !in 20.0..300.0) {
            _uiState.value = _uiState.value.copy(weightSavedMessage = "Число должно быть в кг, дурашка, а не в пудах 🦇")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isWeightBusy = true, weightSavedMessage = null)
            try {
                val payload = JSONObject().put("weight_kg", kg)
                val request = Request.Builder()
                    .url(EvaConfig.looksmaxWeightUrl)
                    .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string().orEmpty())
                        SmokingTracker.recordWeightEntry(kg)
                        val delta = json.optDouble("delta_kg", Double.NaN)
                        val deltaTxt = when {
                            delta.isNaN() -> "первое взвешивание в базе"
                            delta == 0.0 -> "без изменений"
                            else -> "${if (delta > 0) "+" else ""}${String.format("%.1f", delta)} кг"
                        }
                        _uiState.value = _uiState.value.copy(
                            isWeightBusy = false,
                            weightSavedMessage = "⚖️ $kg кг записано ($deltaTxt)"
                        )
                        refreshStats()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isWeightBusy = false,
                            weightSavedMessage = "Сервер не принял: HTTP ${response.code}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Weight save failed", e)
                _uiState.value = _uiState.value.copy(
                    isWeightBusy = false,
                    weightSavedMessage = "Связь с ПК потеряна: ${e.localizedMessage}"
                )
            }
        }
    }

    fun uploadPhoto(context: Context, imageSource: Any, category: LooksmaxCategory, notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isUploading = true,
                uploadErrorMessage = null,
                uploadSuccessMessage = null
            )
            try {
                val bytes = ImageCompressor.compress(context, imageSource)
                if (imageSource is File && imageSource.name.startsWith("gallery_pick_")) {
                    imageSource.delete()
                }
                if (bytes == null || bytes.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        uploadErrorMessage = "Не удалось обработать изображение. Возможно, файл повреждён или недоступен."
                    )
                    return@launch
                }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("category", category.id)
                    .addFormDataPart("notes", notes)
                    .addFormDataPart(
                        "file",
                        "photo.jpg",
                        bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url(EvaConfig.looksmaxUploadUrl)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val savedName = try {
                            JSONObject(response.body?.string().orEmpty()).optString("filename", "")
                        } catch (_: Exception) { "" }
                        _uiState.value = _uiState.value.copy(
                            isUploading = false,
                            uploadSuccessMessage = "${category.emoji} ${category.title} сохранено в Eva Looksmax!"
                        )
                        refreshStats()
                        fetchPhotos()
                        if (savedName.isNotEmpty()) {
                            requestReview(category, savedName)
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isUploading = false,
                            uploadErrorMessage = "Ошибка сервера: HTTP ${response.code}"
                        )
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Upload failed safely caught", t)
                val userMsg = when (t) {
                    is OutOfMemoryError -> "Недостаточно памяти для обработки снимка сверхвысокого разрешения."
                    is SecurityException -> "Нет разрешения на чтение выбранного файла галереи."
                    else -> "Ошибка отправки: ${t.localizedMessage ?: "Проверьте связь с ПК"}"
                }
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    uploadErrorMessage = userMsg
                )
            }
        }
    }
}
