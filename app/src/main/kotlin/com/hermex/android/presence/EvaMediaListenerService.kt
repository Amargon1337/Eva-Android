package com.hermex.android.presence

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class EvaMediaListenerService : NotificationListenerService() {

    private val tag = "EvaMediaListener"
    private var mediaSessionManager: MediaSessionManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        inspectControllers(controllers)
    }

    override fun onCreate() {
        super.onCreate()
        EvaMediaStateHolder.setServiceConnected(true)
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val compName = ComponentName(this, EvaMediaListenerService::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, compName)
            mediaSessionManager?.getActiveSessions(compName)?.let { inspectControllers(it) }
        } catch (e: SecurityException) {
            Log.w(tag, "MediaSessionManager permission not yet active: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EvaMediaStateHolder.setServiceConnected(false)
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        EvaMediaStateHolder.setServiceConnected(true)
        refreshActiveSessions()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        // Whenever a music player updates its persistent playback notification, refresh sessions
        refreshActiveSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshActiveSessions()
    }

    private fun refreshActiveSessions() {
        try {
            val compName = ComponentName(this, EvaMediaListenerService::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(compName)
            if (controllers != null && controllers.isNotEmpty()) {
                inspectControllers(controllers)
            } else {
                inspectStatusBarNotifications()
            }
        } catch (e: Exception) {
            inspectStatusBarNotifications()
        }
    }

    private fun inspectControllers(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            EvaMediaStateHolder.updateMedia(MediaContext(isPlaying = false))
            return
        }

        var foundPlaying: MediaController? = null
        for (controller in controllers) {
            val state = controller.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) {
                foundPlaying = controller
                break
            }
        }

        val target = foundPlaying ?: controllers.firstOrNull()
        if (target != null) {
            val metadata = target.metadata
            val playbackState = target.playbackState

            val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            val position = playbackState?.position ?: 0L

            val pkg = target.packageName
            val appLabel = getAppName(pkg)

            val media = MediaContext(
                isPlaying = isPlaying,
                title = title,
                artist = artist,
                album = album,
                appLabel = appLabel,
                packageName = pkg,
                durationMs = duration,
                positionMs = position,
            )

            EvaMediaStateHolder.updateMedia(media)
            EvaPresenceDispatcher.dispatchDebounced(applicationContext)
        } else {
            EvaMediaStateHolder.updateMedia(MediaContext(isPlaying = false))
        }
    }

    private fun inspectStatusBarNotifications() {
        try {
            val activeNotifs = activeNotifications ?: return
            for (sbn in activeNotifs) {
                val extras = sbn.notification.extras
                val title = extras.getCharSequence("android.title")?.toString()
                val text = extras.getCharSequence("android.text")?.toString()
                val subText = extras.getCharSequence("android.subText")?.toString()

                val category = sbn.notification.category
                if (category == "transport" || isMusicApp(sbn.packageName)) {
                    val isOngoing = (sbn.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
                    if (title != null) {
                        val media = MediaContext(
                            isPlaying = isOngoing,
                            title = title,
                            artist = text,
                            album = subText,
                            appLabel = getAppName(sbn.packageName),
                            packageName = sbn.packageName,
                        )
                        EvaMediaStateHolder.updateMedia(media)
                        EvaPresenceDispatcher.dispatchDebounced(applicationContext)
                        return
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun isMusicApp(pkg: String?): Boolean {
        if (pkg == null) return false
        val lower = pkg.lowerCase()
        return lower.contains("spotify") || lower.contains("music") ||
                lower.contains("audio") || lower.contains("player") ||
                lower.contains("soundcloud") || lower.contains("vk")
    }

    private fun getAppName(packageName: String?): String {
        if (packageName == null) return "Медиа"
        return try {
            val pm = applicationContext.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            when {
                packageName.contains("spotify") -> "Spotify"
                packageName.contains("youtube") -> "YouTube Music"
                packageName.contains("yandex") -> "Яндекс Музыка"
                packageName.contains("vkmusic") -> "VK Музыка"
                else -> packageName
            }
        }
    }

    private fun String.lowerCase(): String = this.lowercase(java.util.Locale.ROOT)
}
