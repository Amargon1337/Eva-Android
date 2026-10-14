package com.hermex.android.insights

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

data class CategoryCount(val category: String, val count: Int)
data class Doubt(val id: Int, val content: String, val confidence: Double, val status: String)
data class RecentMemory(val id: Int, val content: String, val category: String, val importance: String, val createdAt: String)
data class DayCount(val day: String, val count: Int)
data class Paradox(val id: Int, val type: String, val hypothesis: String, val reason: String, val confidence: Double)
data class WeightPoint(val ts: String, val kg: Double)
data class MemeStat(val title: String, val timesUsed: Int, val mood: String)
data class LooksmaxSummary(val count: Int, val avg: Double, val best: Int, val worst: Int, val last: Int, val trend: Double?)

data class InsightsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val totalMemories: Int = 0,
    val byCategory: List<CategoryCount> = emptyList(),
    val doubts: List<Doubt> = emptyList(),
    val recent: List<RecentMemory> = emptyList(),
    val thoughtsByDay: List<DayCount> = emptyList(),
    val moodMix: List<CategoryCount> = emptyList(),
    val paradoxes: List<Paradox> = emptyList(),
    val weight: List<WeightPoint> = emptyList(),
    val memes: List<MemeStat> = emptyList(),
    val looksmax: LooksmaxSummary? = null,
)

class InsightsViewModel : ViewModel() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun deleteMemory(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            for (base in EvaConfig.getCandidateHosts()) {
                try {
                    val req = Request.Builder().url("$base/api/memories/$id").delete().build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            EvaConfig.baseUrl = base
                            _uiState.value = _uiState.value.let { s ->
                                s.copy(
                                    recent = s.recent.filterNot { it.id == id },
                                    doubts = s.doubts.filterNot { it.id == id },
                                    paradoxes = s.paradoxes.filterNot { it.id == id },
                                    totalMemories = (s.totalMemories - 1).coerceAtLeast(0),
                                )
                            }
                            return@launch
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            var lastErr: String? = null
            for (base in EvaConfig.getCandidateHosts()) {
                try {
                    val req = Request.Builder().url("$base/api/insights?days=30").get().build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) { lastErr = "HTTP ${resp.code}"; return@use }
                        val j = JSONObject(resp.body?.string().orEmpty())
                        EvaConfig.baseUrl = base
                        _uiState.value = InsightsUiState(
                            isLoading = false,
                            totalMemories = j.optInt("total_memories"),
                            byCategory = j.optJSONArray("by_category").toCats(),
                            doubts = j.optJSONArray("doubts").toDoubts(),
                            recent = j.optJSONArray("recent_memories").toRecent(),
                            thoughtsByDay = j.optJSONArray("thoughts_by_day").toDays(),
                            moodMix = j.optJSONArray("mood_mix").toCats(),
                            paradoxes = j.optJSONArray("open_paradoxes").toParadoxes(),
                            weight = j.optJSONArray("weight_history").toWeight(),
                            memes = j.optJSONArray("memes").toMemes(),
                            looksmax = j.optJSONObject("looksmax")?.let { lm ->
                                LooksmaxSummary(
                                    count = lm.optInt("count"),
                                    avg = lm.optDouble("avg_mog", 0.0),
                                    best = lm.optInt("best"),
                                    worst = lm.optInt("worst"),
                                    last = lm.optInt("last"),
                                    trend = if (lm.isNull("trend")) null else lm.optDouble("trend", 0.0),
                                )
                            },
                        )
                        return@launch
                    }
                } catch (e: Exception) { lastErr = e.localizedMessage }
            }
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = lastErr ?: "студия не отвечает")
        }
    }

    private fun JSONArray?.toCats() = this?.let { a ->
        List(a.length()) { i -> a.optJSONObject(i)?.let { CategoryCount(it.optString("category").ifEmpty { it.optString("mood") }, it.optInt("c")) } }
    }?.filterNotNull() ?: emptyList()

    private fun JSONArray?.toDoubts() = this?.let { a ->
        List(a.length()) { i -> a.optJSONObject(i)?.let { Doubt(it.optInt("id"), it.optString("content"), it.optDouble("confidence", 0.5), it.optString("status")) } }
    }?.filterNotNull() ?: emptyList()

    private fun JSONArray?.toRecent() = this?.let { a ->
        List(a.length()) { i -> a.optJSONObject(i)?.let { RecentMemory(it.optInt("id"), it.optString("content"), it.optString("category"), it.optString("importance"), it.optString("created_at").take(10)) } }
    }?.filterNotNull() ?: emptyList()

    private fun JSONArray?.toDays() = this?.let { a ->
        List(a.length()) { i -> a.optJSONObject(i)?.let { DayCount(it.optString("d"), it.optInt("c")) } }
    }?.filterNotNull()?.reversed() ?: emptyList()

    private fun JSONArray?.toParadoxes() = this?.let { a ->
        List(a.length()) { i -> a.optJSONObject(i)?.let { Paradox(it.optInt("id"), it.optString("conflict_type"), it.optString("causal_hypothesis"), it.optString("detected_reason"), it.optDouble("confidence", 0.5)) } }
    }?.filterNotNull() ?: emptyList()

    private fun JSONArray?.toWeight() = this?.let { a ->
        List(a.length()) { i -> a.optJSONObject(i)?.let { WeightPoint(it.optString("ts"), it.optDouble("kg")) } }
    }?.filterNotNull() ?: emptyList()

    private fun JSONArray?.toMemes() = this?.let { a ->
        List(a.length()) { i -> a.optJSONObject(i)?.let { MemeStat(it.optString("title"), it.optInt("times_used"), it.optString("mood", "playful")) } }
    }?.filterNotNull() ?: emptyList()
}
