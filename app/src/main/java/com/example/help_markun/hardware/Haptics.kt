package com.example.help_markun.hardware

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** 聴覚に頼らない通知のための振動パターン */
class Haptics(context: Context) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    /** BLE 一致：強めの「ドン・ドン・ドン」 */
    fun alert() = play(longArrayOf(0, 180, 120, 180, 120, 360))

    /** カード読み取り：短い「トン」 */
    fun tick() = play(longArrayOf(0, 60))

    private fun play(pattern: LongArray) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(pattern, -1)
        }
    }
}
