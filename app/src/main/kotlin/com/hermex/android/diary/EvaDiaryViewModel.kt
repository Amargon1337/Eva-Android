package com.hermex.android.diary

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermex.android.eva.EvaConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DiaryEntry(
    val id: String,
    val timestamp: String,
    val thought: String,
    val mood: String,
    val isPrivate: Boolean,
    val activeWindow: String?,
    val revealed: Boolean = false,
    val proactivePrompt: String? = null
)

data class PcPresence(
    val status: String,
    val description: String,
    val activeApp: String?,
    val isNight: Boolean
)

data class DiaryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val entries: List<DiaryEntry> = emptyList(),
    val activeMood: String = "mysterious",
    val presence: PcPresence? = null,
    val totalThoughts: Int = 0,
    val autonomyRunning: Boolean = false,
    val autonomyPaused: Boolean = false,
    val lastSpark: String? = null,
)

class EvaDiaryViewModel : ViewModel() {
    private val TAG = "EvaDiaryViewModel"
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchDiary()
    }

    fun toggleReveal(id: String) {
        val current = _uiState.value.entries
        val target = current.find { it.id == id } ?: return
        val newRevealed = !target.revealed

        _uiState.value = _uiState.value.copy(
            entries = current.map {
                if (it.id == id) it.copy(revealed = newRevealed) else it
            }
        )

        if (newRevealed && target.isPrivate) {
            // Notify server that user opened the private thought
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val targetIntId = id.toIntOrNull()
                    if (targetIntId != null) {
                        val body = JSONObject().put("thought_id", targetIntId).toString()
                        val req = Request.Builder()
                            .url(EvaConfig.revealThoughtUrl)
                            .post(body.toRequestBody("application/json".toMediaTypeOrNull()))
                            .build()
                        client.newCall(req).execute().close()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun fetchDiary() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val request = Request.Builder()
                    .url(EvaConfig.diaryUrl)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        val json = JSONObject(body)

                        // Server returns "entries" (list of thoughts) or "thoughts"
                        val thoughtsArray = json.optJSONArray("entries")
                            ?: json.optJSONArray("thoughts")
                            ?: JSONArray()

                        // Extract presence if returned
                        val presenceObj = json.optJSONObject("presence")
                            ?: json.optJSONObject("status")?.optJSONObject("presence")
                        val pcPresence = presenceObj?.let { p ->
                            val activeWin = p.optJSONObject("active_window")
                            val winDesc = activeWin?.optString("title")?.takeIf { it.isNotEmpty() }
                                ?: activeWin?.optString("process")
                            PcPresence(
                                status = p.optString("status", "ACTIVE"),
                                description = p.optString("description", "ПК активен"),
                                activeApp = winDesc,
                                isNight = p.optBoolean("is_night", false)
                            )
                        }

                        val list = mutableListOf<DiaryEntry>()
                        for (i in 0 until thoughtsArray.length()) {
                            val obj = thoughtsArray.optJSONObject(i) ?: continue
                            val thoughtText = obj.optString("content").ifEmpty {
                                obj.optString("content_preview").ifEmpty {
                                    obj.optString("thought", "")
                                }
                            }
                            if (thoughtText.isBlank()) continue

                            val rawTime = obj.optString("created_at").ifEmpty {
                                obj.optString("timestamp", "Только что")
                            }
                            val formattedTime = try {
                                if (rawTime.contains("T")) {
                                    val odt = java.time.OffsetDateTime.parse(rawTime)
                                    val local = odt.atZoneSameInstant(java.time.ZoneId.systemDefault())
                                    val hourMin = local.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                                    val shortDate = local.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM"))
                                    "$hourMin · $shortDate"
                                } else rawTime
                            } catch (_: Exception) {
                                try {
                                    if (rawTime.contains("T")) {
                                        val parts = rawTime.split("T")
                                        val datePart = parts[0]
                                        val timePart = parts[1].substringBefore("+").substringBefore("Z").substringBefore(".")
                                        val hourMin = timePart.take(5)
                                        val shortDate = if (datePart.count { it == '-' } == 2) {
                                            val dParts = datePart.split("-")
                                            "${dParts[2]}.${dParts[1]}"
                                        } else datePart
                                        "$hourMin · $shortDate"
                                    } else rawTime
                                } catch (_: Exception) { rawTime }
                            }

                            val isPriv = obj.optInt("is_private", 0) == 1 || obj.optBoolean("is_private", false)
                            val isVeiled = obj.optBoolean("is_veiled", false)
                            val source = obj.optString("trigger_source", "").ifEmpty {
                                obj.optString("thought_type", "")
                            }

                            list.add(
                                DiaryEntry(
                                    id = obj.optString("id", i.toString()),
                                    timestamp = formattedTime,
                                    thought = thoughtText,
                                    mood = obj.optString("mood", "calm"),
                                    isPrivate = isPriv || isVeiled,
                                    activeWindow = source.takeIf { it.isNotEmpty() },
                                    revealed = !(isPriv || isVeiled)
                                )
                            )
                        }

                        val mood = if (list.isNotEmpty()) list.first().mood else json.optString("current_mood", "mysterious")
                        val statusObj = json.optJSONObject("status")

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            entries = list,
                            activeMood = mood,
                            presence = pcPresence,
                            totalThoughts = json.optInt("total_thoughts", list.size),
                            autonomyRunning = statusObj?.optBoolean("running", false) ?: false,
                            autonomyPaused = statusObj?.optBoolean("paused", false) ?: false,
                            lastSpark = statusObj?.optString("last_spark")?.takeIf { it.isNotEmpty() },
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "HTTP ${response.code}: Сервер Евы вернул ошибку при запросе мыслей"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Primary host ${EvaConfig.baseUrl} failed, trying fallback...", e)
                // Attempt automatic fallback across configured network targets
                var fallbackSuccess = false
                val fallbackHosts = listOf(
                    EvaConfig.DEFAULT_TAILSCALE_URL,
                    EvaConfig.DEFAULT_LOCAL_WIFI_URL,
                    EvaConfig.DEFAULT_TAILSCALE_MAGIC_DNS
                ).filter { it != EvaConfig.baseUrl }

                for (altHost in fallbackHosts) {
                    try {
                        val altUrl = "$altHost/api/autonomy/diary"
                        val req = Request.Builder().url(altUrl).get().build()
                        client.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                EvaConfig.baseUrl = altHost
                                Log.i(TAG, "Successfully auto-switched EvaConfig to $altHost")
                                fetchDiary()
                                fallbackSuccess = true
                                return@launch
                            }
                        }
                    } catch (_: Exception) {}
                }

                if (!fallbackSuccess) {
                    val msg = if (e.localizedMessage?.contains("Failed to connect") == true || e.localizedMessage?.contains("Connection refused") == true) {
                        "Не удалось подключиться к ${EvaConfig.baseUrl}. Проверьте доступность порта 8765 на ПК через Tailscale."
                    } else {
                        e.localizedMessage ?: "Сетевая ошибка"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = msg
                    )
                }
            }
        }
    }
}
