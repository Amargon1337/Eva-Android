package com.hermex.android.tokens

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

data class TokenModelUsage(
    val model: String,
    val provider: String,
    val inputTokens: Long,
    val outputTokens: Long,
    val cacheReadTokens: Long,
    val apiCalls: Long
)

data class TokenProviderUsage(
    val provider: String,
    val inputTokens: Long,
    val outputTokens: Long,
    val apiCalls: Long
)

data class TokenUsageSummary(
    val totalInput: Long = 0,
    val totalOutput: Long = 0,
    val totalCalls: Long = 0,
    val uniqueModels: Int = 0,
    val uniqueProviders: Int = 0,
    val byProvider: List<TokenProviderUsage> = emptyList(),
    val byModel: List<TokenModelUsage> = emptyList()
)

data class TokenDayUsage(
    val date: String,
    val tokens: Long,
    val input: Long = 0,
    val output: Long = 0,
)

data class DayModelUsage(
    val model: String,
    val provider: String,
    val input: Long,
    val output: Long,
    val cache: Long,
    val reasoning: Long,
    val calls: Long,
)

data class DaySessionUsage(
    val sessionId: String,
    val title: String,
    val tokens: Long,
    val input: Long = 0,
    val output: Long = 0,
    val cache: Long = 0,
)

data class DayDetail(
    val date: String,
    val models: List<DayModelUsage>,
    val sessions: List<DaySessionUsage>,
)

data class TokensUiState(
    val summary: TokenUsageSummary? = null,
    val daily: List<TokenDayUsage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDate: String? = null,
    val dayDetail: DayDetail? = null,
    val dayDetailLoading: Boolean = false,
    val dayDetailError: String? = null,
)

class TokensViewModel : ViewModel() {
    private val TAG = "TokensViewModel"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val _uiState = MutableStateFlow(TokensUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectDate(date: String) {
        if (_uiState.value.selectedDate == date) {
            _uiState.value = _uiState.value.copy(selectedDate = null, dayDetail = null, dayDetailError = null)
            return
        }
        _uiState.value = _uiState.value.copy(selectedDate = date, dayDetail = null, dayDetailLoading = true, dayDetailError = null)
        viewModelScope.launch(Dispatchers.IO) {
            var lastErr: String? = null
            // primary: /api/token-usage-by-day
            for (base in EvaConfig.getCandidateHosts()) {
                try {
                    val req = Request.Builder().url("$base/api/token-usage-by-day?date=$date&limit=50").get().build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) { lastErr = "HTTP ${resp.code}"; return@use }
                        val body = resp.body?.string().orEmpty()
                        if (body.isBlank()) { lastErr = "пустой ответ"; return@use }
                        val j = JSONObject(body)
                        if (j.has("error")) { lastErr = j.optString("error"); return@use }
                        val models = parseModels(j)
                        val sessions = parseSessions(j)
                        // empty but success is still valid — don't treat as error, but allow fallback if both empty
                        if (models.isEmpty() && sessions.isEmpty() && j.length() == 0) { lastErr = "пустой день"; return@use }
                        EvaConfig.baseUrl = base
                        _uiState.value = _uiState.value.copy(
                            dayDetailLoading = false,
                            dayDetail = DayDetail(date, models, sessions),
                            dayDetailError = null,
                        )
                        return@launch
                    }
                } catch (e: Exception) { lastErr = e.localizedMessage; Log.w(TAG, "by-day $date via $base failed", e) }
            }
            // fallback: если /api/token-usage-by-day не отвечает — пробуем альтернативные эндпоинты и синтетику
            Log.w(TAG, "by-day failed ($lastErr), trying fallback daily detail for $date")
            for (base in EvaConfig.getCandidateHosts()) {
                // пробуем несколько альтернативных путей — сервер может отдавать daily-деталь под другим URL
                val fallbackUrls = listOf(
                    "$base/api/token-usage-daily?date=$date&limit=50",
                    "$base/api/token-usage?date=$date&limit=50&days=1",
                    "$base/api/token-usage-daily?days=1&date=$date",
                ).distinct()
                for (url in fallbackUrls) {
                    try {
                        val req = Request.Builder().url(url).get().build()
                        client.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) return@use
                            val body = resp.body?.string().orEmpty()
                            if (body.isBlank()) return@use
                            val j = JSONObject(body)
                            if (j.has("error")) return@use
                            // daily-фолбэк может вернуть { days:[{date, models:[], sessions:[]}] } или прямой {models,sessions}
                            val directModels = parseModels(j)
                            val directSessions = parseSessions(j)
                            if (directModels.isNotEmpty() || directSessions.isNotEmpty()) {
                                EvaConfig.baseUrl = base
                                _uiState.value = _uiState.value.copy(
                                    dayDetailLoading = false,
                                    dayDetail = DayDetail(date, directModels, directSessions),
                                    dayDetailError = null,
                                )
                                return@launch
                            }
                            // пробуем вложенный days массив
                            j.optJSONArray("days")?.let { arr ->
                                for (i in 0 until arr.length()) {
                                    val o = arr.optJSONObject(i) ?: continue
                                    if (o.optString("date") != date) continue
                                    val m = mutableListOf<DayModelUsage>()
                                    o.optJSONArray("models")?.let { ma ->
                                        for (k in 0 until ma.length()) {
                                            ma.optJSONObject(k)?.let { mo ->
                                                m.add(
                                                    DayModelUsage(
                                                        model = mo.optString("model").ifBlank { mo.optString("name", "?") },
                                                        provider = mo.optString("provider", "?"),
                                                        input = mo.optLongAny("input", "input_tokens", "in"),
                                                        output = mo.optLongAny("output", "output_tokens", "out"),
                                                        cache = mo.optLongAny("cache", "cache_read_tokens", "cache_tokens"),
                                                        reasoning = mo.optLongAny("reasoning", "reasoning_tokens", "thought"),
                                                        calls = mo.optLongAny("calls", "api_calls", "count"),
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    val s = mutableListOf<DaySessionUsage>()
                                    o.optJSONArray("sessions")?.let { sa ->
                                        for (k in 0 until sa.length()) {
                                            sa.optJSONObject(k)?.let { so ->
                                                s.add(
                                                    DaySessionUsage(
                                                        sessionId = so.optStringAny("session_id", "sessionId", "id"),
                                                        title = so.optStringAny("title", "name", "session_title").ifBlank { so.optStringAny("session_id", "id").take(12) },
                                                        tokens = so.optLongAny("tokens", "total_tokens", "total", "count"),
                                                        input = so.optLongAny("input", "input_tokens"),
                                                        output = so.optLongAny("output", "output_tokens"),
                                                        cache = so.optLongAny("cache", "cache_read_tokens"),
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    // даже если моделей нет — отдаём хоть sessions или синтетику
                                    if (m.isNotEmpty() || s.isNotEmpty()) {
                                        EvaConfig.baseUrl = base
                                        _uiState.value = _uiState.value.copy(
                                            dayDetailLoading = false,
                                            dayDetail = DayDetail(date, m, s),
                                            dayDetailError = null,
                                        )
                                        return@launch
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) { Log.w(TAG, "fallback $url failed", e) }
                }
            }
            // последний фолбэк: синтетика из кэша daily баров — хотя бы показать общий объём дня
            val cached = _uiState.value.daily.find { it.date == date }
            if (cached != null && cached.tokens > 0) {
                Log.w(TAG, "using synthetic DayDetail from cached daily for $date: ${cached.tokens}")
                _uiState.value = _uiState.value.copy(
                    dayDetailLoading = false,
                    dayDetail = DayDetail(
                        date = date,
                        models = emptyList(),
                        sessions = listOf(
                            DaySessionUsage(
                                sessionId = "",
                                title = "всего за день",
                                tokens = cached.tokens,
                                input = cached.input,
                                output = cached.output,
                            )
                        )
                    ),
                    dayDetailError = null,
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(dayDetailLoading = false, dayDetailError = lastErr ?: "студия не отвечает")
        }
    }

    private fun parseModels(j: JSONObject): MutableList<DayModelUsage> {
        val models = mutableListOf<DayModelUsage>()
        // основной ключ "models", фолбэк "byModel"
        val arr = j.optJSONArray("models") ?: j.optJSONArray("byModel") ?: j.optJSONArray("by_model")
        arr?.let {
            for (i in 0 until it.length()) {
                val o = it.optJSONObject(i) ?: continue
                models.add(
                    DayModelUsage(
                        model = o.optString("model").ifBlank { o.optString("name", "?") },
                        provider = o.optString("provider").ifBlank { o.optString("provider_name", "?") },
                        input = o.optLongAny("input", "input_tokens", "in", "inputTokens"),
                        output = o.optLongAny("output", "output_tokens", "out", "outputTokens"),
                        cache = o.optLongAny("cache", "cache_read_tokens", "cache_tokens", "cacheReadTokens"),
                        reasoning = o.optLongAny("reasoning", "reasoning_tokens", "reasoningTokens", "thought"),
                        calls = o.optLongAny("calls", "api_calls", "apiCalls", "count"),
                    )
                )
            }
        }
        return models
    }

    private fun parseSessions(j: JSONObject): MutableList<DaySessionUsage> {
        val sessions = mutableListOf<DaySessionUsage>()
        val arr = j.optJSONArray("sessions") ?: j.optJSONArray("bySession") ?: j.optJSONArray("chats")
        arr?.let {
            for (i in 0 until it.length()) {
                val o = it.optJSONObject(i) ?: continue
                sessions.add(
                    DaySessionUsage(
                        sessionId = o.optStringAny("session_id", "sessionId", "id", "chat_id"),
                        title = o.optStringAny("title", "name", "session_title", "chat_title").ifBlank { o.optStringAny("session_id", "sessionId", "id").take(12).ifBlank { "чат" } },
                        tokens = o.optLongAny("tokens", "total_tokens", "total", "count", "sum"),
                        input = o.optLongAny("input", "input_tokens", "in"),
                        output = o.optLongAny("output", "output_tokens", "out"),
                        cache = o.optLongAny("cache", "cache_read_tokens"),
                    )
                )
            }
        }
        return sessions
    }

    private fun JSONObject.optLongAny(vararg keys: String): Long {
        for (k in keys) {
            if (has(k)) {
                val v = opt(k)
                when (v) {
                    is Number -> return v.toLong()
                    is String -> v.toLongOrNull()?.let { return it }
                    else -> {
                        val l = optLong(k, Long.MIN_VALUE)
                        if (l != Long.MIN_VALUE) return l
                    }
                }
            }
        }
        return 0L
    }

    private fun JSONObject.optStringAny(vararg keys: String): String {
        for (k in keys) if (has(k)) { val s = optString(k, ""); if (s.isNotBlank()) return s }
        return ""
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // daily bars from /api/token-usage-daily
            try {
                var daily: List<TokenDayUsage> = emptyList()
                var dErr: String? = null
                for (base in EvaConfig.getCandidateHosts()) {
                    try {
                        val rq = Request.Builder().url("$base/api/token-usage-daily?days=14").get().build()
                        client.newCall(rq).execute().use { resp ->
                            if (!resp.isSuccessful) { dErr = "HTTP ${resp.code}"; return@use }
                            val body = resp.body?.string().orEmpty()
                            val tmp = mutableListOf<TokenDayUsage>()
                            JSONObject(body).optJSONArray("days")?.let { arr ->
                                for (i in 0 until arr.length()) {
                                    arr.optJSONObject(i)?.let { o ->
                                        tmp.add(
                                            TokenDayUsage(
                                                date = o.optString("date"),
                                                tokens = o.optLongAny("tokens", "total_tokens", "total"),
                                                input = o.optLongAny("input", "input_tokens", "in"),
                                                output = o.optLongAny("output", "output_tokens", "out"),
                                            )
                                        )
                                    }
                                }
                            }
                            if (tmp.isNotEmpty()) { daily = tmp; EvaConfig.baseUrl = base; dErr = null }
                            else if (daily.isEmpty() && tmp.isEmpty()) { daily = tmp }
                        }
                        if (daily.isNotEmpty()) break
                    } catch (e: Exception) { dErr = e.localizedMessage }
                }
                _uiState.value = _uiState.value.copy(daily = daily)
            } catch (_: Exception) {}
            try {
                val request = Request.Builder()
                    .url("${EvaConfig.tokenUsageUrl}?days=7")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string().orEmpty())
                        val summary = json.optJSONObject("summary") ?: JSONObject()
                        val providers = mutableListOf<TokenProviderUsage>()
                        json.optJSONArray("providers")?.let { arr ->
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                providers.add(
                                    TokenProviderUsage(
                                        provider = o.optString("provider", "?"),
                                        inputTokens = o.optLongAny("input_tokens", "input", "in"),
                                        outputTokens = o.optLongAny("output_tokens", "output", "out"),
                                        apiCalls = o.optLongAny("api_calls", "apiCalls", "calls")
                                    )
                                )
                            }
                        }
                        val models = mutableListOf<TokenModelUsage>()
                        json.optJSONArray("models")?.let { arr ->
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                models.add(
                                    TokenModelUsage(
                                        model = o.optString("model", "?"),
                                        provider = o.optString("provider", "?"),
                                        inputTokens = o.optLongAny("input_tokens", "input", "in"),
                                        outputTokens = o.optLongAny("output_tokens", "output", "out"),
                                        cacheReadTokens = o.optLongAny("cache_read_tokens", "cache", "cache_tokens"),
                                        apiCalls = o.optLongAny("api_calls", "apiCalls", "calls")
                                    )
                                )
                            }
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            summary = TokenUsageSummary(
                                totalInput = summary.optLongAny("total_input_tokens", "total_input", "input_tokens"),
                                totalOutput = summary.optLongAny("total_output_tokens", "total_output", "output_tokens"),
                                totalCalls = summary.optLongAny("total_api_calls", "total_calls", "api_calls"),
                                uniqueModels = summary.optInt("unique_models", 0),
                                uniqueProviders = summary.optInt("unique_providers", 0),
                                byProvider = providers,
                                byModel = models
                            )
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Студия ответила ${response.code}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Token usage fetch failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Не удалось получить данные. Проверь, что студия Hermes жива."
                )
            }
        }
    }
}
