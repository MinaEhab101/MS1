package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)

    var sfxVolume: Float = 0.8f
    var isVibrationEnabled: Boolean = true

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun playTone(
        sampleRate: Int = 22050,
        durationMs: Int = 350,
        generator: (sampleIndex: Int, totalSamples: Int) -> Short
    ) {
        if (sfxVolume <= 0.01f) return

        scope.launch {
            try {
                val totalSamples = (sampleRate * durationMs / 1000)
                val buffer = ShortArray(totalSamples)

                for (i in 0 until totalSamples) {
                    val rawSample = generator(i, totalSamples)
                    buffer[i] = (rawSample * sfxVolume).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()

                // Clean up after playback
                scope.launch {
                    kotlinx.coroutines.delay(durationMs.toLong() + 50)
                    try {
                        track.stop()
                        track.release()
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    fun playButtonClick() {
        triggerHaptic(20)
        playTone { i, total ->
            val t = i.toDouble() / total
            val freq = 550.0 - (t * 200.0)
            val envelope = (1.0 - t)
            (sin(2.0 * PI * freq * (i / 22050.0)) * envelope * 12000).toInt().toShort()
        }
    }

    fun playDiceRoll() {
        triggerHaptic(50)
        playTone { i, total ->
            val t = i.toDouble() / total
            val clickImpulse = if ((i % 1200) < 180) 1.0 else 0.05
            val noise = (Random.nextDouble() * 2.0 - 1.0)
            val envelope = exp(-t * 4.0)
            ((noise * 18000 * clickImpulse + sin(2.0 * PI * 180.0 * (i / 22050.0)) * 6000) * envelope).toInt().toShort()
        }
    }

    fun playDiceImpact() {
        triggerHaptic(35)
        playTone(durationMs = 90) { i, total ->
            val t = i.toDouble() / total
            val env = exp(-t * 22.0)
            val noise = (Random.nextDouble() * 2.0 - 1.0)
            val freq = 420.0 - (t * 160.0)
            ((sin(2.0 * PI * freq * (i / 22050.0)) * 18000 + noise * 14000) * env).toInt().toShort()
        }
    }

    fun playMovePiece() {
        triggerHaptic(30)
        playTone { i, total ->
            val t = i.toDouble() / total
            val freq = 260.0
            val env = exp(-t * 9.0)
            (sin(2.0 * PI * freq * (i / 22050.0)) * env * 22000).toInt().toShort()
        }
    }

    fun playHitPiece() {
        triggerHaptic(70)
        playTone { i, total ->
            val t = i.toDouble() / total
            val freq = 140.0
            val noise = (Random.nextDouble() * 2.0 - 1.0)
            val env = exp(-t * 7.0)
            ((sin(2.0 * PI * freq * (i / 22050.0)) * 18000 + noise * 10000) * env).toInt().toShort()
        }
    }

    fun playWin() {
        triggerHaptic(120)
        scope.launch {
            val notes = listOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
            for (freq in notes) {
                playTone { i, total ->
                    val t = i.toDouble() / total
                    val env = (1.0 - t)
                    (sin(2.0 * PI * freq * (i / 22050.0)) * env * 20000).toInt().toShort()
                }
                kotlinx.coroutines.delay(120)
            }
        }
    }

    fun playLose() {
        triggerHaptic(80)
        scope.launch {
            val notes = listOf(392.00, 349.23, 311.13, 261.63) // G4, F4, Eb4, C4
            for (freq in notes) {
                playTone { i, total ->
                    val t = i.toDouble() / total
                    val env = (1.0 - t)
                    (sin(2.0 * PI * freq * (i / 22050.0)) * env * 16000).toInt().toShort()
                }
                kotlinx.coroutines.delay(150)
            }
        }
    }

    fun playMatchFound() {
        triggerHaptic(60)
        scope.launch {
            val notes = listOf(440.0, 554.37, 659.25)
            for (freq in notes) {
                playTone { i, total ->
                    val t = i.toDouble() / total
                    val env = (1.0 - t)
                    (sin(2.0 * PI * freq * (i / 22050.0)) * env * 18000).toInt().toShort()
                }
                kotlinx.coroutines.delay(90)
            }
        }
    }

    fun playCheckerSelect() {
        playButtonClick()
    }

    fun playCheckerMove() {
        playMovePiece()
    }

    fun playVictory() {
        playWin()
    }

    fun playBlotHit() {
        playHitPiece()
    }

    fun playNotification() {
        triggerHaptic(30)
        playTone { i, total ->
            val t = i.toDouble() / total
            val freq = if (t < 0.5) 587.33 else 880.0
            val env = exp(-t * 4.0)
            (sin(2.0 * PI * freq * (i / 22050.0)) * env * 18000).toInt().toShort()
        }
    }

    private fun triggerHaptic(durationMs: Long) {
        if (!isVibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
