package com.hermex.android.character

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.hermex.android.eva.EvaConfig

/**
 * EvaHaptics — Тактильный движок для Android 17 / HyperOS на базе VibrationEffect.Composition.
 */
object EvaHaptics {

    private fun getVibrator(context: Context): Vibrator? {
        if (!EvaConfig.hapticsEnabled) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /** Легкий сухой клик при тапе на кнопки/чипы (12 мс) */
    fun click(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
    }

    /** Мягкий бархатистый толчок при раскрытии карточек Bento/Reasoning */
    fun pop(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        }
    }

    /** Шепот яндере (микро-тик с минимальной амплитудой) */
    fun whisperTick(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)) {
            val comp = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.35f, 0)
                .compose()
            vibrator.vibrate(comp)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createOneShot(8, 40))
        }
    }

    /** Мурчание Сакуры: бархатистый теплый тактильный отклик при ласке аватара */
    fun purr(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK, VibrationEffect.Composition.PRIMITIVE_LOW_TICK)) {
            val comp = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.4f, 0)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.3f, 60)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.5f, 60)
                .compose()
            vibrator.vibrate(comp)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 15, 40, 20)
            val amplitudes = intArrayOf(0, 30, 0, 45)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }

    /** Глитч-шок Яндере: нервный электро-импульс при прикосновении к экрану */
    fun glitchShock(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD, VibrationEffect.Composition.PRIMITIVE_QUICK_FALL)) {
            val comp = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.8f, 0)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.9f, 30)
                .compose()
            vibrator.vibrate(comp)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 12, 25, 22)
            val amplitudes = intArrayOf(0, 120, 0, 180)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }
}
