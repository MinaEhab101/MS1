package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class MusicManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var musicJob: Job? = null

    var musicVolume: Float = 0.4f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    var isPlaying: Boolean = false
        private set

    fun startBackgroundMusic() {
        if (isPlaying) return
        isPlaying = true

        musicJob = scope.launch {
            val sampleRate = 22050
            // Relaxing jazz/oriental lounge progression (Am7 - Dm7 - Em7 - Am)
            val chords = listOf(
                listOf(220.0, 261.63, 329.63, 392.0), // A C E G
                listOf(146.83, 220.0, 261.63, 349.23), // D A C F
                listOf(164.81, 246.94, 293.66, 392.0), // E B D G
                listOf(220.0, 261.63, 329.63, 440.0)  // A C E A
            )

            var chordIndex = 0

            while (isActive && isPlaying) {
                if (musicVolume <= 0.01f) {
                    delay(500)
                    continue
                }

                val currentChord = chords[chordIndex % chords.size]
                chordIndex++
                val durationMs = 2800
                val totalSamples = (sampleRate * durationMs / 1000)
                val buffer = ShortArray(totalSamples)

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    var sampleSum = 0.0
                    for (freq in currentChord) {
                        sampleSum += sin(2.0 * PI * freq * t)
                    }
                    val fadeEnvelope = if (i < 4000) {
                        i / 4000.0
                    } else if (i > totalSamples - 4000) {
                        (totalSamples - i) / 4000.0
                    } else {
                        1.0
                    }
                    val amp = (sampleSum / currentChord.size) * fadeEnvelope * musicVolume * 6500.0
                    buffer[i] = amp.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                try {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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

                    delay(durationMs.toLong() - 100)

                    try {
                        track.stop()
                        track.release()
                    } catch (_: Exception) {}
                } catch (_: Exception) {
                    delay(1000)
                }
            }
        }
    }

    fun stopBackgroundMusic() {
        isPlaying = false
        musicJob?.cancel()
        musicJob = null
    }
}
