package com.hermex.android.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermex.android.eva.EvaConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class MemoryRecordUi(
    val id: Int,
    val content: String,
    val memoryType: String,
    val importance: String,
    val confidence: Double,
    val stability: Double,
    val status: String,
    val category: String,
    val tags: List<String>,
    val createdAt: String,
    val sourceLabel: String,
    val matchScore: Double? = null,
    val matchSource: String? = null,
)

data class MemoryUiState(
    val isLoading: Boolean = true,
    val records: List<MemoryRecordUi> = emptyList(),
    val errorMessage: String? = null,
    val query: String = "",
    val total: Int = 0,
    val category: String = "",
)

class MemoryViewModel : ViewModel() {
    private val deletedIds = mutableSetOf<Int>()
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun search(q: String) {
        _uiState.value = _uiState.value.copy(query = q)
        load()
    }

    fun setCategory(c: String) {
        if (_uiState.value.category == c) return
        _uiState.value = _uiState.value.copy(category = c)
        load()
    }

    /** Soft-delete (archive). Hard purge is deliberate overkill for a companion's memory. */
    fun delete(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            var success = false
            var okBase: String? = null
            for (base in EvaConfig.getCandidateHosts()) {
                try {
                    val req = Request.Builder().url("$base/api/memories/$id").delete().build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) { okBase = base; success = true }
                    }
                    if (success) break
                } catch (_: Exception) { }
            }
            if (success) {
                okBase?.let { EvaConfig.baseUrl = it }
                deletedIds.add(id)
                _uiState.value = _uiState.value.copy(
                    records = _uiState.value.records.filterNot { it.id == id },
                )
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "не получилось забыть #$id — студия не отвечает (проверь tailscale)")
            }
        }
    }
    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            var lastErr: String? = null
            val q = _uiState.value.query.trim()
            for (base in EvaConfig.getCandidateHosts()) {
                try {
                    val cat = _uiState.value.category
                    val url = if (q.isNotEmpty()) "$base/api/memories?search=${java.net.URLEncoder.encode(q, "UTF-8")}&limit=100"
                              else if (cat.isNotEmpty()) "$base/api/memories?category=${java.net.URLEncoder.encode(cat, "UTF-8")}&limit=200"
                              else "$base/api/memories?limit=200"
                    val req = Request.Builder().url(url).get().build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) { lastErr = "HTTP ${resp.code}"; return@use }
                        val body = resp.body?.string().orEmpty()
                        val arr = JSONArray(body)
                        val list = ArrayList<MemoryRecordUi>(arr.length())
                        for (i in 0 until arr.length()) {
                            val o = arr.optJSONObject(i) ?: continue
                            val tags = o.optJSONArray("tags")?.let { t -> List(t.length()) { j -> t.optString(j) } } ?: emptyList()
                            list.add(
                                MemoryRecordUi(
                                    id = o.optInt("id"),
                                    content = o.optString("content"),
                                    memoryType = o.optString("memory_type", "fact"),
                                    importance = o.optString("importance", "MEDIUM"),
                                    confidence = o.optDouble("confidence", 0.8),
                                    stability = o.optDouble("stability", 0.5),
                                    status = o.optString("status", "CONFIRMED"),
                                    category = o.optString("category", "general"),
                                    tags = tags,
                                    createdAt = o.optString("created_at").take(16),
                                    sourceLabel = o.optString("match_source", "").ifEmpty {
                                        if (o.has("source_message_id") && !o.isNull("source_message_id")) "чат" else "наблюдение"
                                    },
                                    matchScore = if (o.has("score")) o.optDouble("score") else null,
                                    matchSource = o.optString("match_source").takeIf { it.isNotEmpty() },
                                )
                            )
                        }
                        EvaConfig.baseUrl = base
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            records = list,
                            total = list.size,
                            errorMessage = null,
                        )
                        return@launch
                    }
                } catch (e: Exception) {
                    lastErr = e.localizedMessage
                }
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = lastErr ?: "студия не отвечает"
            )
        }
    }
}
