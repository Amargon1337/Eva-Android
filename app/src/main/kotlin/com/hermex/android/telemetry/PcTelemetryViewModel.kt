package com.hermex.android.telemetry

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
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ServiceInfo(
    val name: String,
    val title: String,
    val status: String,
    val port: Int?,
    val pid: Int?
)

data class DesktopSystemInfo(
    val cpuPercent: Double,
    val ramPercent: Double,
    val gpuPercent: Double?,
    val activeWindow: String?,
    val uptimeHours: Double?
)

data class PcTelemetryUiState(
    val isLoading: Boolean = false,
    val desktopInfo: DesktopSystemInfo? = null,
    val services: List<ServiceInfo> = emptyList(),
    val errorMessage: String? = null
)

class PcTelemetryViewModel : ViewModel() {
    private val TAG = "PcTelemetryViewModel"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _uiState = MutableStateFlow(PcTelemetryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // 1. Fetch system desktop info
                val deskReq = Request.Builder().url(EvaConfig.systemDesktopUrl).get().build()
                client.newCall(deskReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val json = JSONObject(body)
                        val activeWin = if (json.has("active_window")) {
                            val aw = json.get("active_window")
                            if (aw is JSONObject) {
                                aw.optString("title", "")
                            } else {
                                aw.toString()
                            }
                        } else null

                        val desk = DesktopSystemInfo(
                            cpuPercent = json.optDouble("cpu_percent", 0.0),
                            ramPercent = json.optDouble("ram_percent", 0.0),
                            gpuPercent = json.optDouble("gpu_percent").takeIf { !it.isNaN() },
                            activeWindow = activeWin?.takeIf { it.isNotEmpty() },
                            uptimeHours = json.optDouble("uptime_hours").takeIf { !it.isNaN() }
                        )
                        _uiState.value = _uiState.value.copy(desktopInfo = desk)
                    }
                }

                // 2. Fetch services status
                val srvReq = Request.Builder().url(EvaConfig.servicesStatusUrl).get().build()
                client.newCall(srvReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val json = JSONObject(body)
                        val arr = json.optJSONArray("services") ?: JSONArray()
                        val list = mutableListOf<ServiceInfo>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val isRunning = obj.optBoolean("running", false)
                            val statusStr = if (isRunning) "running" else "stopped"
                            val id = obj.optString("id", "")
                            val name = obj.optString("name", id)
                            val uptime = obj.optString("uptime_formatted", "")
                            val title = if (uptime.isNotEmpty() && uptime != "—") "$name ($uptime)" else name

                            list.add(
                                ServiceInfo(
                                    name = id,
                                    title = title,
                                    status = statusStr,
                                    port = obj.optInt("port").takeIf { it > 0 },
                                    pid = obj.optInt("pid").takeIf { it > 0 }
                                )
                            )
                        }

                        val watchdogs = json.optJSONArray("internal_watchdogs") ?: JSONArray()
                        for (i in 0 until watchdogs.length()) {
                            val wobj = watchdogs.optJSONObject(i) ?: continue
                            val wstatus = wobj.optString("status", "")
                            val isRunning = wstatus.equals("HEALTHY", ignoreCase = true)
                            list.add(
                                ServiceInfo(
                                    name = wobj.optString("id", ""),
                                    title = "🛡️ " + wobj.optString("name", ""),
                                    status = if (isRunning) "running" else "stopped",
                                    port = null,
                                    pid = null
                                )
                            )
                        }

                        _uiState.value = _uiState.value.copy(services = list)
                    }
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed telemetry refresh", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Ошибка связи с ПК"
                )
            }
        }
    }

    fun restartService(serviceName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = EvaConfig.serviceActionUrl(serviceName, "restart")
                val req = Request.Builder().url(url).post("{}".toRequestBody()).build()
                client.newCall(req).execute().close()
                refreshAll()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart $serviceName", e)
            }
        }
    }
}
