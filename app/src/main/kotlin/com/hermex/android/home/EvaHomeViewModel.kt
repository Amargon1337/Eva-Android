package com.hermex.android.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermex.android.eva.EvaConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class EvaHomeUiState(
    val isLoading: Boolean = false,
    val presenceText: String = "Ева на связи",
    val activeApp: String? = null,
    val isNight: Boolean = false,
    val latestThought: String? = null,
    val latestThoughtTime: String? = null,
    val mood: String = "calm",
    val memoryCount: Int = 1284,
    val contradictionsCount: Int = 3,
    val looksmaxCount: Int = 42,
    val pcServicesCount: Int = 4,
    val latestSessionId: String? = null,
    val latestSessionTitle: String? = "Последний диалог",
    val hostLatencyMs: Long = 22L,
    val hostOnline: Boolean = true,
    val diaryCount: Int = 12,
    val currentTrack: String? = null,
    val currentArtist: String? = null,
    val currentAppLabel: String? = null,
    val isMusicPlaying: Boolean = false,
    val headphonesConnected: Boolean = false,
    val batteryLevel: Int? = null,
    val isCharging: Boolean = false,
    val unsurfacedCount: Int = 0,
    val shelfTotal: Int = 0,
    val shelfQueued: Int = 0,
    val narrative: String? = null,
)

class EvaHomeViewModel : ViewModel() {
    private val TAG = "EvaHomeViewModel"
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _uiState = MutableStateFlow(EvaHomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            com.hermex.android.presence.EvaMediaStateHolder.currentMedia.collect { media ->
                _uiState.value = _uiState.value.copy(
                    isMusicPlaying = media.isPlaying,
                    currentTrack = media.title,
                    currentArtist = media.artist,
                    currentAppLabel = media.appLabel,
                )
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val candidates = EvaConfig.getCandidateHosts()

            var activeBase: String? = null
            for (base in candidates) {
                try {
                    val diaryReq = Request.Builder().url("$base/api/autonomy/diary?limit=5").get().build()
                    client.newCall(diaryReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            activeBase = base
                            EvaConfig.baseUrl = base
                            val body = resp.body?.string().orEmpty()
                            val json = JSONObject(body)
                            val thoughts = json.optJSONArray("entries") ?: json.optJSONArray("thoughts")
                            val firstObj = thoughts?.optJSONObject(0)
                            val thoughtText = firstObj?.optString("content")?.ifEmpty { firstObj.optString("thought") }
                            val rawTime = firstObj?.optString("created_at") ?: "Только что"
                            val formattedTime = try {
                                if (rawTime.contains("T")) {
                                    rawTime.split("T")[1].take(5)
                                } else rawTime
                            } catch (_: Exception) { rawTime }

                            val presenceObj = json.optJSONObject("presence")
                                ?: json.optJSONObject("status")?.optJSONObject("presence")
                            val activeWin = presenceObj?.optJSONObject("active_window")
                            val appDesc = activeWin?.optString("title")?.takeIf { it.isNotEmpty() }
                                ?: activeWin?.optString("process")

                            _uiState.value = _uiState.value.copy(
                                latestThought = thoughtText,
                                latestThoughtTime = formattedTime,
                                presenceText = presenceObj?.optString("description", "Иван за ПК") ?: "Ева наблюдает",
                                activeApp = appDesc,
                                isNight = presenceObj?.optBoolean("is_night", false) ?: false,
                                mood = firstObj?.optString("mood", "tender") ?: "tender"
                            )
                        }
                    }
                    if (activeBase != null) break
                } catch (e: Exception) {
                    Log.d(TAG, "Host $base failed for diary: ${e.message}")
                }
            }

            val workingBase = activeBase ?: EvaConfig.baseUrl

            try {
                // Fetch looksmax stats
                val looksReq = Request.Builder().url("$workingBase/api/looksmax/stats").get().build()
                client.newCall(looksReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = JSONObject(resp.body?.string().orEmpty())
                        _uiState.value = _uiState.value.copy(
                            looksmaxCount = json.optInt("total_photos", _uiState.value.looksmaxCount)
                        )
                    }
                }
            } catch (_: Exception) {}

            try {
                val unifiedReq = Request.Builder().url("$workingBase/api/context/unified").get().build()
                client.newCall(unifiedReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = JSONObject(resp.body?.string().orEmpty())
                        val narrative = json.optString("narrative")
                        val currentTrack = json.optString("current_track")
                        val isMusicPlaying = json.optBoolean("music_playing", false)
                        val phoneObj = json.optJSONObject("phone")
                        val battObj = phoneObj?.optJSONObject("battery")
                        val audioObj = phoneObj?.optJSONObject("audio")

                        _uiState.value = _uiState.value.copy(
                            presenceText = if (narrative.isNotEmpty()) narrative else _uiState.value.presenceText,
                            narrative = narrative.takeIf { it.isNotEmpty() },
                            currentTrack = if (currentTrack.isNotEmpty()) currentTrack else _uiState.value.currentTrack,
                            isMusicPlaying = isMusicPlaying || _uiState.value.isMusicPlaying,
                            batteryLevel = battObj?.optInt("level", -1)?.takeIf { it >= 0 } ?: _uiState.value.batteryLevel,
                            isCharging = battObj?.optBoolean("is_charging", false) ?: _uiState.value.isCharging,
                            headphonesConnected = audioObj?.optBoolean("headphones_connected", false) ?: _uiState.value.headphonesConnected,
                        )
                    }
                }
            } catch (_: Exception) {}

            // «мысли не показала» — count of unsurfaced diary thoughts (proactive status)
            try {
                val proReq = Request.Builder().url("$workingBase/api/proactive/status").get().build()
                client.newCall(proReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = JSONObject(resp.body?.string().orEmpty())
                        _uiState.value = _uiState.value.copy(
                            unsurfacedCount = json.optInt("unsurfaced_count", _uiState.value.unsurfacedCount),
                            hostOnline = true,
                        )
                    }
                }
            } catch (_: Exception) {}

            try {
                val shelfReq = Request.Builder().url("$workingBase/api/shelf/stats").get().build()
                client.newCall(shelfReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = JSONObject(resp.body?.string().orEmpty())
                        _uiState.value = _uiState.value.copy(
                            shelfTotal = json.optInt("total", _uiState.value.shelfTotal),
                            shelfQueued = json.optInt("queued", _uiState.value.shelfQueued),
                        )
                    }
                }
            } catch (_: Exception) {}

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
