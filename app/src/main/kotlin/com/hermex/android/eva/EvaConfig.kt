package com.hermex.android.eva

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hermex.android.core.storage.AppearancePreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder

enum class DualityMode(val label: String, val badge: String, val desc: String) {
    SAKURA("🌸 Сакура", "сакура", "Нежная, заботливая, мотивирующая"),
    YANDERE("🦇 Яндере", "яндере", "Одержимая, строгая, ревнивая к калориям");

    companion object {
        /** Safe parse for persisted values -- unknown/blank falls back to SAKURA (matches NoOp default). */
        fun fromStoredNameOrDefault(stored: String?): DualityMode =
            entries.firstOrNull { it.name == stored } ?: SAKURA
    }
}

object EvaConfig {
    const val DEFAULT_TAILSCALE_MAGIC_DNS = "http://home-pc.tail3a7e5e.ts.net:8765"
    const val DEFAULT_TAILSCALE_URL = "http://100.118.134.36:8765"
    const val DEFAULT_LOCAL_WIFI_URL = "http://192.168.100.117:8765"

    var baseUrl by mutableStateOf(DEFAULT_LOCAL_WIFI_URL)

    fun getCandidateHosts(): List<String> = listOf(
        baseUrl,
        DEFAULT_LOCAL_WIFI_URL,
        DEFAULT_TAILSCALE_URL,
        DEFAULT_TAILSCALE_MAGIC_DNS
    ).distinct()

    fun syncWithServerUrl(serverUrl: String) {
        if (serverUrl.isBlank()) return
        try {
            val uri = java.net.URI(serverUrl)
            val host = uri.host
            if (!host.isNullOrBlank()) {
                val candidate = "http://$host:8765"
                if (!baseUrl.contains("192.168.100.117") || candidate.contains("192.168.100.117")) {
                    baseUrl = candidate
                }
            }
        } catch (_: Exception) {}
    }

    fun toggleNetworkHost() {
        baseUrl = when {
            baseUrl.contains("home-pc.tail3a7e5e.ts.net") -> DEFAULT_TAILSCALE_URL
            baseUrl.contains("100.118.134.36") -> DEFAULT_LOCAL_WIFI_URL
            else -> DEFAULT_TAILSCALE_MAGIC_DNS
        }
    }

    // --- Duality persistence (audit P0-1): wired once from AppContainer at startup. ---
    private var dualityPersistence: AppearancePreferencesStore? = null
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Called once by [com.hermex.android.AppContainer]: restores the persisted mode and
     * enables write-through so mode survives cold starts. Safe to call before or after
     * first UI read -- the StateFlow starts at SAKURA and updates when the disk read lands. */
    fun attachDualityPersistence(store: AppearancePreferencesStore) {
        dualityPersistence = store
        persistenceScope.launch { _dualityMode.value = store.loadDualityMode() }
    }

    private val _dualityMode = MutableStateFlow(DualityMode.SAKURA)
    val dualityMode = _dualityMode.asStateFlow()

    var hapticsEnabled by mutableStateOf(true)
    var autoVoicePlay by mutableStateOf(false)
    var streamPulseAnimation by mutableStateOf(true)

    fun setMode(mode: DualityMode) {
        _dualityMode.value = mode
        dualityPersistence?.let { store ->
            persistenceScope.launch { store.setDualityMode(mode) }
        }
    }

    fun toggleMode() {
        setMode(if (_dualityMode.value == DualityMode.SAKURA) DualityMode.YANDERE else DualityMode.SAKURA)
    }

    suspend fun measureLatencyMs(): Pair<Boolean, Long> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val url = java.net.URL(diaryUrl + "?limit=1")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            val code = conn.responseCode
            val duration = System.currentTimeMillis() - start
            conn.disconnect()
            (code in 200..299) to duration
        } catch (_: Throwable) {
            false to (System.currentTimeMillis() - start)
        }
    }

    val looksmaxPhotosUrl: String
        get() = "$baseUrl/api/looksmax/photos"

    fun looksmaxPhotoFileUrl(category: String, filename: String): String =
        "$baseUrl/api/looksmax/photo-file/$category/$filename"

    fun looksmaxDeletePhotoUrl(category: String, filename: String): String =
        "$baseUrl/api/looksmax/photo/$category/$filename"

    val looksmaxUploadUrl: String
        get() = "$baseUrl/api/looksmax/upload"

    val looksmaxStatsUrl: String
        get() = "$baseUrl/api/looksmax/stats"

    val looksmaxReviewUrl: String
        get() = "$baseUrl/api/looksmax/photo/review"

    val looksmaxChatUrl: String
        get() = "$baseUrl/api/looksmax/chat"

    val looksmaxWeightUrl: String
        get() = "$baseUrl/api/looksmax/weight"

    val looksmaxCheatsheetUrl: String
        get() = "$baseUrl/api/looksmax/mewing-cheatsheet"

    val tokenUsageUrl: String
        get() = "$baseUrl/api/token-usage"

    val tokenUsageDailyUrl: String
        get() = "$baseUrl/api/token-usage-daily"

    val tokenUsageByDayUrl: String
        get() = "$baseUrl/api/token-usage-by-day"

    val insightsUrl: String
        get() = "$baseUrl/api/insights"

    val memoriesListUrl: String
        get() = "$baseUrl/api/memories"

    fun memoriesSearchUrl(query: String): String =
        "$baseUrl/api/memories?search=${java.net.URLEncoder.encode(query, "UTF-8")}&limit=100"

    val diaryUrl: String
        get() = "$baseUrl/api/autonomy/diary"

    val revealThoughtUrl: String
        get() = "$baseUrl/api/autonomy/reveal-thought"

    val servicesStatusUrl: String
        get() = "$baseUrl/api/services/status"

    val unifiedContextUrl: String
        get() = "$baseUrl/api/context/unified"

    val autonomyStatusUrl: String
        get() = "$baseUrl/api/autonomy/status"

    val shelfStatsUrl: String
        get() = "$baseUrl/api/shelf/stats"

    val shelfItemsUrl: String
        get() = "$baseUrl/api/shelf/items"

    fun shelfItemUrl(id: Int): String = "$baseUrl/api/shelf/items/$id"

    fun shelfActionUrl(id: Int, action: String): String = "$baseUrl/api/shelf/items/$id/$action"

    val shelfAddUrlUrl: String
        get() = "$baseUrl/api/shelf/add-url"

    val shelfPauseUrl: String
        get() = "$baseUrl/api/shelf/pause"

    val systemDesktopUrl: String
        get() = "$baseUrl/api/system/desktop"

    fun ttsUrl(text: String, mood: String? = null): String {
        val encodedText = URLEncoder.encode(text, "UTF-8")
        val currentMood = mood ?: if (_dualityMode.value == DualityMode.SAKURA) "cute" else "yandere"
        return "$baseUrl/api/media/tts?text=$encodedText&mood=$currentMood"
    }

    fun serviceActionUrl(service: String, action: String): String {
        return "$baseUrl/api/services/$service/$action"
    }
}

