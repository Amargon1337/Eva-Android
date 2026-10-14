package com.hermex.android.presence

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global observable state holder for currently captured media playing on Android.
 */
object EvaMediaStateHolder {
    private val _currentMedia = MutableStateFlow(MediaContext())
    val currentMedia: StateFlow<MediaContext> = _currentMedia.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    fun updateMedia(media: MediaContext) {
        _currentMedia.value = media
    }

    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }
}
