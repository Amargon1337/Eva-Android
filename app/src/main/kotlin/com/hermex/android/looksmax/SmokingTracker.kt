package com.hermex.android.looksmax

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Tracks smoked cigarettes timestamps, persists them across app launches,
 * calculates smoking frequency (e.g. cigarettes today, in the last hour),
 * and provides contextual intelligence for Eva's personality and chat prompts.
 */
object SmokingTracker {
    private const val PREFS_NAME = "hermex_smoking_tracker"
    private const val KEY_TIMESTAMPS = "cigarette_timestamps"
    private const val KEY_LAST_WEIGHT_KG = "last_weight_kg"
    private const val KEY_LAST_WEIGHT_TS = "last_weight_ts"

    private var prefs: SharedPreferences? = null

    data class SmokingStats(
        val totalToday: Int = 0,
        val lastHourCount: Int = 0,
        val lastTimestamp: Long? = null,
        val minutesSinceLast: Long? = null,
        val lastTimeText: String = "",
        val wakeTimestamp: Long? = null,
        val lateNightCount: Int = 0,
        val longestGapTodayHours: Double = 0.0,
        val remark: String = "",
        val lastWeightKg: Double = 0.0,
        val lastWeightDaysAgo: Int = -1,
    )

    private val _stats = MutableStateFlow(SmokingStats())
    val stats: StateFlow<SmokingStats> = _stats.asStateFlow()

    private val timestamps = mutableListOf<Long>()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadTimestamps()
            recalculate()
        }
    }

    @Synchronized
    fun logCigarette() {
        val now = System.currentTimeMillis()
        timestamps.add(now)
        saveTimestamps()
        recalculate()
    }

    @Synchronized
    fun removeLastCigarette() {
        if (timestamps.isNotEmpty()) {
            timestamps.removeAt(timestamps.lastIndex)
            saveTimestamps()
            recalculate()
        }
    }

    @Synchronized
    fun resetToday() {
        val startOfDay = getStartOfDayMillis()
        timestamps.removeAll { it >= startOfDay }
        saveTimestamps()
        recalculate()
    }

    @Synchronized
    fun clearAll() {
        timestamps.clear()
        saveTimestamps()
        recalculate()
    }

    fun getStats(): SmokingStats {
        recalculate()
        return _stats.value
    }

    private fun getStartOfDayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Synchronized
    private fun recalculate() {
        val now = System.currentTimeMillis()
        val startOfDay = getStartOfDayMillis()

        // Prune older than 7 days
        val sevenDaysAgo = now - (7L * 24 * 3600_000L)
        timestamps.removeAll { it < sevenDaysAgo }

        val todayList = timestamps.filter { it >= startOfDay }
        val lastTs = timestamps.lastOrNull()
        val minutesSince = if (lastTs != null) (now - lastTs) / 60_000L else null
        val lastTimeText = if (lastTs != null) {
            val c = Calendar.getInstance().apply { timeInMillis = lastTs }
            String.format(java.util.Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
        } else ""

        // Wake marker: the cigarette that ENDS a long sleep gap (>=4h) is a
        // wake-up event — Ivan smokes right after waking, even a temporary one.
        var wakeTs: Long? = null
        var longestGapHours = 0.0
        val sorted = timestamps.sorted()
        for (i in 1 until sorted.size) {
            val gap = sorted[i] - sorted[i - 1]
            if (gap >= 4L * 3600_000L) {
                if (sorted[i] >= startOfDay) wakeTs = sorted[i]
                longestGapHours = maxOf(longestGapHours, gap / 3600_000.0)
            }
        }
        if (minutesSince != null && minutesSince >= 240) {
            longestGapHours = maxOf(longestGapHours, minutesSince / 60.0)
        }

        // Late-night activity: cigarettes between 00:00 and 05:00 = still not asleep.
        val lateNight = todayList.count {
            val h = Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY)
            h < 5
        }

        val remark = when {
            minutesSince == null -> "Ни одной сигареты в базе. Логировать можно кнопкой «Сигарета» 🐾"
            minutesSince >= 360 -> "Последняя была в $lastTimeText, пауза ${minutesSince / 60} ч — скорее всего спишь 💤"
            minutesSince >= 180 -> "Последняя в $lastTimeText (${minutesSince} мин назад) — длинная пауза ✨"
            else -> "Последняя в $lastTimeText (${minutesSince} мин назад), сегодня ${todayList.size} шт."
        }

        val wKg = (prefs?.getFloat(KEY_LAST_WEIGHT_KG, 0f) ?: 0f).toDouble()
        val wTs = prefs?.getLong(KEY_LAST_WEIGHT_TS, 0L) ?: 0L
        val wDays = if (wTs > 0) ((now - wTs) / 86_400_000L).toInt() else -1

        _stats.value = SmokingStats(
            totalToday = todayList.size,
            lastHourCount = timestamps.count { it >= now - 3600_000L },
            lastTimestamp = lastTs,
            minutesSinceLast = minutesSince,
            lastTimeText = lastTimeText,
            wakeTimestamp = wakeTs,
            lateNightCount = lateNight,
            longestGapTodayHours = longestGapHours,
            remark = remark,
            lastWeightKg = wKg,
            lastWeightDaysAgo = wDays,
        )
    }

    private fun loadTimestamps() {
        val raw = prefs?.getString(KEY_TIMESTAMPS, null) ?: return
        timestamps.clear()
        raw.split(",").forEach {
            it.trim().toLongOrNull()?.let { ts -> timestamps.add(ts) }
        }
        timestamps.sort()
    }

    private fun saveTimestamps() {
        val raw = timestamps.joinToString(",")
        prefs?.edit()?.putString(KEY_TIMESTAMPS, raw)?.apply()
    }

    /**
     * Caches the last weight Ivan logged in Hermex (called after a successful
     * POST to /api/looksmax/weight) so it can ride along into Eva's chat context,
     * same channel as the smoking telemetry.
     */
    @Synchronized
    fun recordWeightEntry(kg: Double) {
        prefs?.edit()
            ?.putFloat(KEY_LAST_WEIGHT_KG, kg.toFloat())
            ?.putLong(KEY_LAST_WEIGHT_TS, System.currentTimeMillis())
            ?.apply()
    }

    /**
     * Produces a concise contextual prompt instruction for Eva to ground her persona.
     */
    fun buildEvaSmokingContext(): String {
        recalculate()
        val current = _stats.value
        val today = current.totalToday
        val minSince = current.minutesSinceLast

        val parts = mutableListOf<String>()

        if (today > 0 || current.lastTimestamp != null) {
            val sb = StringBuilder()
            sb.append("сегодня ${today} шт")
            if (current.lastTimeText.isNotEmpty()) {
                sb.append("; последняя в ${current.lastTimeText}")
                if (minSince != null) sb.append(" ($minSince мин назад)")
            }
            // Wake marker: first cigarette after a >=4h gap = he woke up.
            current.wakeTimestamp?.let { ws ->
                val c = Calendar.getInstance().apply { timeInMillis = ws }
                sb.append("; проснулся в ${String.format(java.util.Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))} (первая сигарета после сна)")
            }
            if (current.longestGapTodayHours >= 4.0) {
                sb.append("; самый длинный сон сегодня ~${String.format(java.util.Locale.US, "%.0f", current.longestGapTodayHours)} ч")
            }
            if (current.lateNightCount > 0) {
                sb.append("; ${current.lateNightCount} шт. курилось глубокой ночью (00-05) — не спалось")
            }
            // Ivan's explicit rule: bursts around the Morzik walk are NORMAL, do not worry.
            sb.append("; НОРМАЛЬНО, что подряд несколько сигарет (дорога к Морзику/у дома/гулянка) — это не стресс-сигнал, не драматизируй и не выпытывай. Долгий перерыв = спит.")
            parts.add("Курение: $sb")
        }

        val wKg = prefs?.getFloat(KEY_LAST_WEIGHT_KG, -1f)?.takeIf { it > 0f }
        val wTs = prefs?.getLong(KEY_LAST_WEIGHT_TS, 0L)?.takeIf { it > 0L }
        if (wKg != null && wTs != null) {
            val daysSince = (System.currentTimeMillis() - wTs) / (24 * 3600_000L)
            val whenTxt = when {
                daysSince < 1 -> "сегодня"
                daysSince == 1L -> "вчера"
                else -> "$daysSince дн. назад"
            }
            parts.add("Вес (последний замер Ивана в Hermex): ${String.format(java.util.Locale.US, "%.1f", wKg)} кг, записан $whenTxt")
        }

        if (parts.isEmpty()) {
            return ""
        }
        return "[Контекст привычек Ивана: ${parts.joinToString(" | ")}]"
    }
}
