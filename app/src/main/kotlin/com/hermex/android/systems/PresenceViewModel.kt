package com.hermex.android.systems

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermex.android.eva.EvaConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PresenceUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val narrative: String? = null,
    val pcOnline: Boolean = false,
    val pcStatus: String? = null,
    val pcIdleMinutes: Int? = null,
    val pcActiveWindow: String? = null,
    val phoneOnline: Boolean = false,
    val batteryLevel: Int? = null,
    val isCharging: Boolean = false,
    val headphones: Boolean = false,
    val musicTrack: String? = null,
    val musicPlaying: Boolean = false,
    val autonomyPaused: Boolean = false,
    val chatActive: Boolean = false,
    val servicesUp: Int = 0,
    val servicesTotal: Int = 0,
    val lastError: String? = null,
)

class PresenceViewModel : ViewModel() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _uiState = MutableStateFlow(PresenceUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            while (true) {
                delay(7_000)
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            var lastErr: String? = null
            for (base in EvaConfig.getCandidateHosts()) {
                try {
                    val unified = Request.Builder().url("$base/api/context/unified").get().build()
                    client.newCall(unified).execute().use { resp ->
                        if (!resp.isSuccessful) { lastErr = "HTTP ${resp.code}"; return@use }
                        val json = JSONObject(resp.body?.string().orEmpty())
                        parseAndApply(json, base)
                        return@launch
                    }
                } catch (e: Exception) {
                    lastErr = e.localizedMessage
                    Log.d("PresenceVM", "host $base failed: ${e.message}")
                }
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = lastErr?.takeIf { _uiState.value.narrative == null },
                pcOnline = false,
                phoneOnline = _uiState.value.phoneOnline,
            )
        }
    }

    private fun parseAndApply(json: JSONObject, base: String) {
        val pc = json.optJSONObject("pc")
        val phone = json.optJSONObject("phone")
        val media = phone?.optJSONObject("media")
        val batt = phone?.optJSONObject("battery")
        val audio = phone?.optJSONObject("audio")
        val activeWin = pc?.optJSONObject("active_window")

        EvaConfig.baseUrl = base
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = null,
            narrative = json.optString("narrative").takeIf { it.isNotEmpty() },
            pcOnline = pc != null && !pc.has("error"),
            pcStatus = pc?.optString("status")?.takeIf { it.isNotEmpty() },
            pcIdleMinutes = pc?.optDouble("idle_minutes", -1.0)?.takeIf { it >= 0 }?.toInt(),
            pcActiveWindow = activeWin?.optString("title")?.takeIf { it.isNotEmpty() },
            phoneOnline = phone?.optBoolean("is_online", false) ?: false,
            batteryLevel = batt?.optInt("level", -1)?.takeIf { it >= 0 },
            isCharging = batt?.optBoolean("is_charging", false) ?: false,
            headphones = audio?.optBoolean("headphones_connected", false) ?: false,
            musicTrack = media?.let { m ->
                val t = m.optString("title").ifEmpty { null } ?: return@let null
                val a = m.optString("artist").ifEmpty { null }
                if (a != null) "$a — $t" else t
            },
            musicPlaying = media?.optBoolean("is_playing", false) ?: false,
            lastError = json.optString("last_error").takeIf { it.isNotEmpty() },
        )

        // autonomy + services — best effort, don't fail the screen
        try {
            val aut = Request.Builder().url("$base/api/autonomy/status").get().build()
            client.newCall(aut).execute().use { resp ->
                if (resp.isSuccessful) {
                    val a = JSONObject(resp.body?.string().orEmpty())
                    _uiState.value = _uiState.value.copy(
                        autonomyPaused = a.optBoolean("paused", false) || a.optBoolean("chat_active", false),
                        chatActive = a.optBoolean("chat_active", false)
                    )
                }
            }
        } catch (_: Exception) {}
        try {
            val svc = Request.Builder().url("$base/api/services/status").get().build()
            client.newCall(svc).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    // API returns either a top-level array or {"services":[...]}
                    val arr = when {
                        body.trimStart().startsWith("[") -> JSONArray(body)
                        else -> JSONObject(body).optJSONArray("services") ?: JSONArray()
                    }
                    var up = 0
                    for (i in 0 until arr.length()) if (arr.optJSONObject(i)?.optBoolean("running") == true) up++
                    _uiState.value = _uiState.value.copy(servicesUp = up, servicesTotal = arr.length())
                }
            }
        } catch (_: Exception) {}
    }
}
