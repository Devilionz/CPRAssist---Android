package com.cprassist.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Audio prompt types for offline fallback
 */
enum class AudioPromptType {
    START,           // 3 rising tones
    SWITCH_RESCUER,  // Distinctive alert pattern
    ROSC,            // Success tone
    STOP,            // 2 descending tones
    ALERT,           // Single attention-grabbing tone
    RHYTHM_CHECK     // Double beep
}

/**
 * Generates audio prompts programmatically when TTS is unavailable.
 * Provides distinctive, recognizable tones for key CPR events.
 */
class AudioPromptManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val SAMPLE_RATE = 44100

        // Musical notes (frequencies in Hz)
        private const val C4 = 261.63
        private const val D4 = 293.66
        private const val E4 = 329.63
        private const val F4 = 349.23
        private const val G4 = 392.00
        private const val A4 = 440.00
        private const val B4 = 493.88
        private const val C5 = 523.25
        private const val D5 = 587.33
        private const val E5 = 659.25
    }

    /**
     * Play an audio prompt
     */
    fun playPrompt(promptType: AudioPromptType) {
        scope.launch {
            val samples = when (promptType) {
                AudioPromptType.START -> generateStartTone()
                AudioPromptType.SWITCH_RESCUER -> generateSwitchRescuerTone()
                AudioPromptType.ROSC -> generateROSCTone()
                AudioPromptType.STOP -> generateStopTone()
                AudioPromptType.ALERT -> generateAlertTone()
                AudioPromptType.RHYTHM_CHECK -> generateRhythmCheckTone()
            }
            playAudio(samples)
        }
    }

    /**
     * Generate start CPR tone - 3 rising notes
     */
    private fun generateStartTone(): ShortArray {
        val tones = listOf(
            Pair(C4, 150),
            Pair(E4, 150),
            Pair(G4, 300)
        )
        return generateToneSequence(tones)
    }

    /**
     * Generate switch rescuer alert - distinctive urgent pattern
     */
    private fun generateSwitchRescuerTone(): ShortArray {
        val tones = listOf(
            Pair(A4, 100),
            Pair(0.0, 50),  // Silence
            Pair(A4, 100),
            Pair(0.0, 50),
            Pair(E5, 200),
            Pair(0.0, 100),
            Pair(A4, 100),
            Pair(0.0, 50),
            Pair(A4, 100),
            Pair(0.0, 50),
            Pair(E5, 200)
        )
        return generateToneSequence(tones)
    }

    /**
     * Generate ROSC success tone - triumphant ascending
     */
    private fun generateROSCTone(): ShortArray {
        val tones = listOf(
            Pair(C4, 150),
            Pair(E4, 150),
            Pair(G4, 150),
            Pair(C5, 400)
        )
        return generateToneSequence(tones)
    }

    /**
     * Generate stop tone - 2 descending notes
     */
    private fun generateStopTone(): ShortArray {
        val tones = listOf(
            Pair(G4, 200),
            Pair(C4, 300)
        )
        return generateToneSequence(tones)
    }

    /**
     * Generate alert tone - single attention-grabbing beep
     */
    private fun generateAlertTone(): ShortArray {
        val tones = listOf(
            Pair(A4, 500)
        )
        return generateToneSequence(tones)
    }

    /**
     * Generate rhythm check tone - double beep
     */
    private fun generateRhythmCheckTone(): ShortArray {
        val tones = listOf(
            Pair(E5, 100),
            Pair(0.0, 100),
            Pair(E5, 100)
        )
        return generateToneSequence(tones)
    }

    /**
     * Generate a sequence of tones with specified frequencies and durations
     */
    private fun generateToneSequence(tones: List<Pair<Double, Int>>): ShortArray {
        val allSamples = mutableListOf<Short>()

        for ((frequency, durationMs) in tones) {
            val samples = generateTone(frequency, durationMs)
            allSamples.addAll(samples.toList())
        }

        return allSamples.toShortArray()
    }

    /**
     * Generate a single tone
     */
    private fun generateTone(frequency: Double, durationMs: Int): ShortArray {
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val samples = ShortArray(numSamples)

        if (frequency <= 0) {
            // Silence
            return samples
        }

        for (i in 0 until numSamples) {
            val time = i.toDouble() / SAMPLE_RATE

            // ADSR envelope for natural sound
            val attackSamples = numSamples / 10
            val decaySamples = numSamples / 5
            val releaseSamples = numSamples / 5

            val envelope = when {
                i < attackSamples -> i.toDouble() / attackSamples
                i < attackSamples + decaySamples -> {
                    val decayProgress = (i - attackSamples).toDouble() / decaySamples
                    1.0 - (0.3 * decayProgress)
                }
                i > numSamples - releaseSamples -> {
                    (numSamples - i).toDouble() / releaseSamples * 0.7
                }
                else -> 0.7
            }

            // Generate sine wave with harmonics for richer sound
            val fundamental = sin(2 * PI * frequency * time)
            val harmonic2 = sin(4 * PI * frequency * time) * 0.3
            val harmonic3 = sin(6 * PI * frequency * time) * 0.1

            val sample = ((fundamental + harmonic2 + harmonic3) * Short.MAX_VALUE * envelope * 0.7).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return samples
    }

    /**
     * Play audio samples
     */
    private fun playAudio(samples: ShortArray) {
        try {
            val bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(bufferSize, samples.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()

            // Wait for playback to complete
            Thread.sleep((samples.size * 1000L) / SAMPLE_RATE + 100)

            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            // Fail silently
        }
    }

    /**
     * Clean up resources
     */
    fun release() {
        scope.cancel()
    }
}
