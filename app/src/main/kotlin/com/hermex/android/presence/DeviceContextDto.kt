package com.hermex.android.presence

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceContextPayload(
    @SerialName("device_id") val deviceId: String = "redmi_note_13",
    @SerialName("device_name") val deviceName: String = "Redmi Note 13",
    @SerialName("timestamp") val timestamp: Double = System.currentTimeMillis() / 1000.0,
    @SerialName("media") val media: MediaContext? = null,
    @SerialName("battery") val battery: BatteryContext? = null,
    @SerialName("audio") val audio: AudioRouteContext? = null,
    @SerialName("screen") val screen: ScreenContext? = null,
    @SerialName("network") val network: NetworkContext? = null,
)

@Serializable
data class MediaContext(
    @SerialName("is_playing") val isPlaying: Boolean = false,
    @SerialName("title") val title: String? = null,
    @SerialName("artist") val artist: String? = null,
    @SerialName("album") val album: String? = null,
    @SerialName("app_label") val appLabel: String? = null,
    @SerialName("package_name") val packageName: String? = null,
    @SerialName("duration_ms") val durationMs: Long = 0L,
    @SerialName("position_ms") val positionMs: Long = 0L,
)

@Serializable
data class BatteryContext(
    @SerialName("level") val level: Int? = null,
    @SerialName("is_charging") val isCharging: Boolean = false,
    @SerialName("plugged_type") val pluggedType: String? = null,
)

@Serializable
data class AudioRouteContext(
    @SerialName("headphones_connected") val headphonesConnected: Boolean = false,
    @SerialName("route_type") val routeType: String? = null,
    @SerialName("device_name") val deviceName: String? = null,
)

@Serializable
data class ScreenContext(
    @SerialName("is_interactive") val isInteractive: Boolean = true,
    @SerialName("is_locked") val isLocked: Boolean = false,
)

@Serializable
data class NetworkContext(
    @SerialName("type") val type: String? = null,
    @SerialName("ssid") val ssid: String? = null,
    @SerialName("tailscale_active") val tailscaleActive: Boolean = false,
)
