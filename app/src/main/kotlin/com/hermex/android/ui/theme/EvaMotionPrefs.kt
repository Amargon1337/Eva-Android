package com.hermex.android.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * EvaMotionPrefs -- accessibility gate for infinite/ambient motion (audit P1-4).
 *
 * Respects the Android system "Remove animations" / developer "Animator duration scale = 0"
 * setting: when the user has disabled animations system-wide, infinite ambient effects
 * (evaBreathe, LivePulseDot, petals) must render as static frames instead.
 *
 * Read is cheap (a ContentResolver lookup per composition) and only happens where ambient
 * motion actually exists, so no flow plumbing is needed.
 */
object EvaMotionPrefs {

    /** True when the system animator duration scale is 0 (user opted out of animations). */
    @Composable
    fun reducedMotion(): Boolean {
        val resolver = LocalContext.current.contentResolver
        return try {
            Settings.Global.getFloat(
                resolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }
}
