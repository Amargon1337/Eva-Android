package com.hermex.android.presence

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object EvaPresenceDispatcher {

    private const val TAG = "EvaPresenceDispatcher"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var debounceJob: Job? = null
    private var heartbeatJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    // Host fallback list: try active local Wi-Fi first, then Tailscale
    private val hosts = listOf(
        "http://192.168.100.117:8765",
        "http://100.118.134.36:8765",
    )
    private var currentHostIndex = 0

    fun startHeartbeat(context: Context) {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            while (true) {
                dispatchNow(context.applicationContext)
                val isPlaying = EvaMediaStateHolder.currentMedia.value.isPlaying
                // Send more frequently if actively playing, otherwise quiet heartbeat
                delay(if (isPlaying) 15_000L else 45_000L)
            }
        }
    }

    fun dispatchDebounced(context: Context) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(1000L)
            dispatchNow(context.applicationContext)
        }
    }

    fun dispatchNow(context: Context) {
        scope.launch {
            try {
                val payload = EvaContextCollector.collectFullSnapshot(context)
                val jsonString = json.encodeToString(payload)
                val body = jsonString.toRequestBody("application/json".toMediaType())

                var success = false
                for (i in hosts.indices) {
                    val idx = (currentHostIndex + i) % hosts.size
                    val base = hosts[idx]
                    val url = "$base/api/context/device"
                    try {
                        val request = Request.Builder()
                            .url(url)
                            .post(body)
                            .build()
                        client.newCall(request).execute().use { resp ->
                            if (resp.isSuccessful) {
                                currentHostIndex = idx
                                success = true
                                return@use
                            }
                        }
                        if (success) break
                    } catch (e: Exception) {
                        Log.w(TAG, "Host $base failed: ${e.message}")
                    }
                }
                if (success) {
                    Log.i(TAG, "Sent presence to Eva: ${payload.media?.artist} - ${payload.media?.title} (playing=${payload.media?.isPlaying})")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to dispatch presence: ${e.message}")
            }
        }
    }
}
