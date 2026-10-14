package com.hermex.android.character

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * EvaEventManager — Диспетчер процедурных реакций и характера Евы.
 * Защищает от спама, учитывает экраны, музыку и простой пользователя.
 */
object EvaEventManager {

    enum class CharacterEvent {
        NONE,
        GLITCH_MICRO,
        SAKURA_PETAL_GUST,
        EYE_CONTACT_BLINK,
        MUSIC_REACTION,
        INACTIVITY_OBSERVE
    }

    private val _currentEvent = MutableStateFlow(CharacterEvent.NONE)
    val currentEvent = _currentEvent.asStateFlow()

    private val _glitchKey = MutableStateFlow(0)
    val glitchKey = _glitchKey.asStateFlow()

    private val _petalKey = MutableStateFlow(0)
    val petalKey = _petalKey.asStateFlow()

    private var lastGlitchTimestamp: Long = 0
    private var lastPetalTimestamp: Long = 0

    // Anti-spam cooldowns (минимальный интервал между микро-эффектами)
    private const val GLITCH_COOLDOWN_MS = 120_000L // 2 минуты
    private const val PETAL_COOLDOWN_MS = 60_000L   // 1 минута

    fun triggerMicroGlitch(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (force || (now - lastGlitchTimestamp > GLITCH_COOLDOWN_MS)) {
            lastGlitchTimestamp = now
            _glitchKey.value = _glitchKey.value + 1
            _currentEvent.value = CharacterEvent.GLITCH_MICRO
        }
    }

    fun triggerPetalGust(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (force || (now - lastPetalTimestamp > PETAL_COOLDOWN_MS)) {
            lastPetalTimestamp = now
            _petalKey.value = _petalKey.value + 1
            _currentEvent.value = CharacterEvent.SAKURA_PETAL_GUST
        }
    }

    fun clearEvent() {
        _currentEvent.value = CharacterEvent.NONE
    }
}
