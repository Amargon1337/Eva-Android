package com.hermex.android.presence

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

object EvaContextCollector {

    fun collectFullSnapshot(context: Context): DeviceContextPayload {
        val media = getLiveMediaContext(context)
        val battery = getBatteryContext(context)
        val audio = getAudioRouteContext(context)
        val screen = getScreenContext(context)
        val network = getNetworkContext(context)

        return DeviceContextPayload(
            deviceId = "redmi_note_13",
            deviceName = "Redmi Note 13",
            timestamp = System.currentTimeMillis() / 1000.0,
            media = media,
            battery = battery,
            audio = audio,
            screen = screen,
            network = network,
        )
    }

    fun getLiveMediaContext(context: Context): MediaContext {
        try {
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? android.media.session.MediaSessionManager
                ?: return EvaMediaStateHolder.currentMedia.value
            val compName = android.content.ComponentName(context, EvaMediaListenerService::class.java)
            val controllers = msm.getActiveSessions(compName)
            if (!controllers.isNullOrEmpty()) {
                var foundPlaying: android.media.session.MediaController? = null
                for (controller in controllers) {
                    if (controller.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING) {
                        foundPlaying = controller
                        break
                    }
                }
                val target = foundPlaying ?: controllers.firstOrNull()
                if (target != null) {
                    val metadata = target.metadata
                    val state = target.playbackState
                    val isPlaying = state?.state == android.media.session.PlaybackState.STATE_PLAYING
                    val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
                        ?: metadata?.getString(android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                    val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
                        ?: metadata?.getString(android.media.MediaMetadata.METADATA_KEY_AUTHOR)
                    val album = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM)
                    val duration = metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                    val position = state?.position ?: 0L
                    val pkg = target.packageName

                    if (!title.isNullOrBlank() || isPlaying) {
                        val media = MediaContext(
                            isPlaying = isPlaying,
                            title = title,
                            artist = artist,
                            album = album,
                            appLabel = resolveAppLabel(context, pkg),
                            packageName = pkg,
                            durationMs = duration,
                            positionMs = position
                        )
                        EvaMediaStateHolder.updateMedia(media)
                        return media
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("EvaContextCollector", "Error querying active sessions: ${e.message}")
        }
        return EvaMediaStateHolder.currentMedia.value
    }

    fun resolveAppLabel(context: Context, packageName: String?): String {
        if (packageName == null) return "Медиа"
        return try {
            val pm = context.packageManager
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

    fun getBatteryContext(context: Context): BatteryContext {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)

            val level = batteryStatus?.let { intent ->
                val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (rawLevel >= 0 && scale > 0) (rawLevel * 100) / scale else null
            }

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val plugType = when (chargePlug) {
                BatteryManager.BATTERY_PLUGGED_USB -> "usb"
                BatteryManager.BATTERY_PLUGGED_AC -> "ac"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
                else -> null
            }

            BatteryContext(
                level = level,
                isCharging = isCharging,
                pluggedType = plugType,
            )
        } catch (e: Exception) {
            BatteryContext()
        }
    }

    fun getAudioRouteContext(context: Context): AudioRouteContext {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return AudioRouteContext()

            var headphones = false
            var routeType: String? = null
            var deviceName: String? = null

            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLE_HEADSET -> {
                        headphones = true
                        routeType = "bluetooth_a2dp"
                        deviceName = device.productName?.toString()
                        break
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        headphones = true
                        routeType = "wired"
                        deviceName = device.productName?.toString()
                        break
                    }
                }
            }

            AudioRouteContext(
                headphonesConnected = headphones,
                routeType = routeType,
                deviceName = deviceName,
            )
        } catch (e: Exception) {
            AudioRouteContext()
        }
    }

    fun getScreenContext(context: Context): ScreenContext {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

            val isInteractive = powerManager?.isInteractive ?: true
            val isLocked = keyguardManager?.isDeviceLocked ?: false

            ScreenContext(
                isInteractive = isInteractive,
                isLocked = isLocked,
            )
        } catch (e: Exception) {
            ScreenContext()
        }
    }

    fun getNetworkContext(context: Context): NetworkContext {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return NetworkContext()

            val activeNetwork = cm.activeNetwork ?: return NetworkContext()
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkContext()

            val type = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
                else -> "other"
            }

            val isVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

            NetworkContext(
                type = type,
                ssid = null,
                tailscaleActive = isVpn,
            )
        } catch (e: Exception) {
            NetworkContext()
        }
    }
}
