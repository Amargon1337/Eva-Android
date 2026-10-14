package com.hermex.android.shelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermex.android.eva.EvaConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ShelfKnowledge(
    val id: Int,
    val classification: String,
    val text: String,
    val consolidate: String,
    val memoryId: Int?,
)

data class ShelfItem(
    val id: Int,
    val title: String,
    val kind: String,
    val source: String,
    val status: String,
    val summary: String,
    val charCount: Int,
    val addedAt: String,
    val lastError: String?,
    val important: Boolean,
    val counts: Map<String, Int>,
    val knowledge: List<ShelfKnowledge> = emptyList(),
)

data class ShelfUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val items: List<ShelfItem> = emptyList(),
    val total: Int = 0,
    val queued: Int = 0,
    val reading: Int? = null,
    val digested: Int = 0,
    val consolidated: Int = 0,
    val knowledgeTotal: Int = 0,
    val knowledgeToReview: Int = 0,
    val workerPaused: Boolean = false,
    val chatYield: Boolean = false,
    val expandedId: Int? = null,
)

class ShelfViewModel : ViewModel() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // consolidate = LLM call, slow is normal
        .build()
    private val json = "application/json; charset=utf-8".toMediaType()

    private val _uiState = MutableStateFlow(ShelfUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            while (true) {
                delay(12_000)
                if (_uiState.value.expandedId == null) refresh(silent = true)
            }
        }
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!silent) _uiState.value = _uiState.value.copy(isLoading = true)
            var lastErr: String? = null
            for (base in EvaConfig.getCandidateHosts()) {
                try {
                    val st = Request.Builder().url("$base/api/shelf/stats").get().build()
                    client.newCall(st).execute().use { resp ->
                        if (!resp.isSuccessful) { lastErr = "HTTP ${resp.code}"; return@use }
                        val stats = JSONObject(resp.body?.string().orEmpty())
                        EvaConfig.baseUrl = base
                        val itemsReq = Request.Builder().url("$base/api/shelf/items?limit=100").get().build()
                        val items = client.newCall(itemsReq).execute().use { ir ->
                            if (!ir.isSuccessful) error("HTTP ${ir.code}")
                            parseItems(ir.body?.string().orEmpty())
                        }
                        _uiState.value = ShelfUiState(
                            isLoading = false,
                            errorMessage = null,
                            items = items,
                            total = stats.optInt("total"),
                            queued = stats.optInt("queued"),
                            reading = stats.optInt("current_item").takeIf { stats.has("current_item") && !stats.isNull("current_item") },
                            digested = stats.optInt("digested"),
                            consolidated = stats.optInt("consolidated"),
                            knowledgeTotal = stats.optInt("knowledge_total"),
                            knowledgeToReview = stats.optInt("knowledge_to_review"),
                            workerPaused = stats.optBoolean("paused"),
                            chatYield = stats.optBoolean("chat_yield"),
                            expandedId = _uiState.value.expandedId,
                        )
                        return@launch
                    }
                } catch (e: Exception) {
                    lastErr = e.localizedMessage
                }
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = if (silent) _uiState.value.errorMessage else (lastErr ?: "нет связи со студией")
            )
        }
    }

    private fun parseItems(body: String): List<ShelfItem> {
        val arr = JSONArray(body)
        return List(arr.length()) { i ->
            val o = arr.optJSONObject(i) ?: return@List null
            ShelfItem(
                id = o.optInt("id"),
                title = o.optString("title").ifBlank { o.optString("source_ref") },
                kind = o.optString("kind"),
                source = o.optString("source_ref"),
                status = o.optString("status"),
                summary = o.optString("summary"),
                charCount = o.optInt("char_count"),
                addedAt = o.optString("added_at").take(16),
                lastError = o.optString("last_error").takeIf { it.isNotEmpty() },
                important = o.optInt("important") == 1,
                counts = o.optJSONObject("counts")?.let { c ->
                    c.keys().asSequence().associateWith { c.optInt(it) }
                } ?: emptyMap(),
            )
        }.filterNotNull()
    }

    fun toggleExpand(id: Int) {
        val cur = _uiState.value
        if (cur.expandedId == id) {
            _uiState.value = cur.copy(expandedId = null)
            return
        }
        _uiState.value = cur.copy(expandedId = id)
        val cached = cur.items.firstOrNull { it.id == id }?.knowledge
        if (!cached.isNullOrEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(EvaConfig.shelfItemUrl(id)).get().build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val o = JSONObject(resp.body?.string().orEmpty())
                    val kn = o.optJSONArray("knowledge") ?: JSONArray()
                    val list = List(kn.length()) { j ->
                        kn.optJSONObject(j)?.let { k ->
                            ShelfKnowledge(
                                id = k.optInt("id"),
                                classification = k.optString("classification"),
                                text = k.optString("text"),
                                consolidate = k.optString("consolidate"),
                                memoryId = k.optString("memory_id").takeIf { s -> s != "null" && s.isNotEmpty() }?.toIntOrNull()
                                    ?: if (k.isNull("memory_id")) null else k.optInt("memory_id"),
                            )
                        }
                    }.filterNotNull()
                    _uiState.value = _uiState.value.copy(
                        items = _uiState.value.items.map { if (it.id == id) it.copy(knowledge = list) else it }
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun addUrl(url: String, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val body = JSONObject().put("url", url).toString()
                    .toRequestBody(json)
                val req = Request.Builder().url(EvaConfig.shelfAddUrlUrl).post(body).build()
                client.newCall(req).execute().use { resp ->
                    val txt = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) {
                        onResult(null)
                        refresh()
                    } else {
                        onResult(JSONObject(txt).optString("detail", "HTTP ${resp.code}"))
                    }
                }
            } catch (e: Exception) {
                onResult(e.localizedMessage)
            }
        }
    }

    fun action(id: Int, action: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url(EvaConfig.shelfActionUrl(id, action))
                    .post("".toRequestBody(json))
                    .build()
                client.newCall(req).execute().close()
                if (action == "consolidate") _uiState.value = _uiState.value.copy(expandedId = null)
                refresh(silent = true)
            } catch (_: Exception) {}
        }
    }

    fun togglePause() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Request.Builder().url(EvaConfig.shelfPauseUrl).post("".toRequestBody(json)).build().let {
                    client.newCall(it).execute().close()
                }
                refresh(silent = true)
            } catch (_: Exception) {}
        }
    }
}
