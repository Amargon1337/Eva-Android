package com.hermex.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hermex.android.eva.EvaConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object EvaAudioPlayer {
    private const val TAG = "EvaAudioPlayer"
    private var mediaPlayer: MediaPlayer? = null

    var isPlaying by mutableStateOf(false)
        private set

    var currentPlayingText by mutableStateOf<String?>(null)
        private set

    var isBuffering by mutableStateOf(false)
        private set

    fun play(text: String, mood: String? = null) {
        if (isPlaying && currentPlayingText == text) {
            stop()
            return
        }

        stop()
        val url = EvaConfig.ttsUrl(text, mood)
        Log.d(TAG, "Playing TTS from: $url")
        currentPlayingText = text
        isBuffering = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(url)
                    setOnPreparedListener { mp ->
                        this@EvaAudioPlayer.isBuffering = false
                        this@EvaAudioPlayer.isPlaying = true
                        mp.start()
                    }
                    setOnCompletionListener {
                        stop()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                        stop()
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize playback", e)
                stop()
            }
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPlaying = false
        isBuffering = false
        currentPlayingText = null
    }
}
