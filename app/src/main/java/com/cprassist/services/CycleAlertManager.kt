package com.cprassist.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.cprassist.data.models.AlertConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Manages the 2-minute cycle alerts during active arrest management.
 *
 * CRITICAL: The alert MUST play at MAXIMUM AUDIBLE VOLUME regardless of current
 * device volume settings. This is a medical emergency application where the alert
 * must be heard in noisy environments.
 *
 * Volume Strategy:
 * 1. Uses USAGE_ALARM audio stream (respects alarm volume, not media)
 * 2. Temporarily boosts alarm stream to maximum
 * 3. Restores previous volume after playback
 * 4. Requests audio focus to ensure priority
 *
 * Android Limitations:
 * - Do Not Disturb mode may still silence the alert unless app is exempted
 * - Some manufacturers may have additional volume restrictions
 * - The app should request DND access permission for full reliability
 */
class CycleAlertManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var vibrator: Vibrator? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var config = AlertConfig()

    private val _currentCycle = MutableStateFlow(1)
    val currentCycle: StateFlow<Int> = _currentCycle.asStateFlow()

    private val _cycleAlertTriggered = MutableSharedFlow<Int>()
    val cycleAlertTriggered: SharedFlow<Int> = _cycleAlertTriggered.asSharedFlow()

    private val _elapsedInCycleMs = MutableStateFlow(0L)
    val elapsedInCycleMs: StateFlow<Long> = _elapsedInCycleMs.asStateFlow()

    private var cycleStartTime: Long = 0L
    private var pausedTime: Long = 0L
    private var isPaused: Boolean = false

    private var alertSamples: ShortArray? = null

    // Store original volume to restore after alert
    private var originalAlarmVolume: Int = 0
    private var maxAlarmVolume: Int = 0

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val ALERT_FREQUENCY_PRIMARY = 1000.0  // 1kHz - penetrating, clear
        private const val ALERT_FREQUENCY_SECONDARY = 1500.0 // 1.5kHz - adds urgency
        private const val ALERT_DURATION_MS = 800 // Short but noticeable
    }

    init {
        initializeAudio()
        initializeVibrator()
        generateAlertSound()
        maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
    }

    private fun initializeAudio() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 4)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
    }

    private fun initializeVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * Generate the alert beep sound - dual-tone for maximum noticeability
     */
    private fun generateAlertSound() {
        val numSamples = (SAMPLE_RATE * ALERT_DURATION_MS) / 1000
        alertSamples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val time = i.toDouble() / SAMPLE_RATE

            // Envelope: quick attack, sustain, quick decay
            val envelope = when {
                i < numSamples / 20 -> i.toDouble() / (numSamples / 20) // 5% attack
                i > numSamples * 19 / 20 -> (numSamples - i).toDouble() / (numSamples / 20) // 5% decay
                else -> 1.0
            }

            // Dual-tone for more penetrating, noticeable alert
            // Primary tone + secondary tone create a distinctive "emergency" sound
            val sample = (
                (sin(2 * PI * ALERT_FREQUENCY_PRIMARY * time) * 0.6 +
                 sin(2 * PI * ALERT_FREQUENCY_SECONDARY * time) * 0.4) *
                Short.MAX_VALUE * envelope * 0.95 // 95% amplitude for headroom
            ).toInt()

            alertSamples!![i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        audioTrack?.write(alertSamples!!, 0, alertSamples!!.size)
    }

    fun start(newConfig: AlertConfig = config) {
        config = newConfig
        _currentCycle.value = 1
        cycleStartTime = System.currentTimeMillis()
        pausedTime = 0L
        isPaused = false

        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                if (!isPaused) {
                    val elapsed = System.currentTimeMillis() - cycleStartTime - pausedTime
                    _elapsedInCycleMs.value = elapsed

                    // Check if cycle completed (2 minutes)
                    if (elapsed >= config.cycleIntervalMs) {
                        triggerCycleAlert()
                        _currentCycle.value++
                        cycleStartTime = System.currentTimeMillis()
                        pausedTime = 0L
                        _elapsedInCycleMs.value = 0L
                    }
                }
                delay(100)
            }
        }
    }

    fun stop() {
        timerJob?.cancel()
        timerJob = null
        _currentCycle.value = 1
        _elapsedInCycleMs.value = 0L
    }

    fun pause() {
        if (!isPaused) {
            isPaused = true
            pausedTime = System.currentTimeMillis()
        }
    }

    fun resume() {
        if (isPaused) {
            val pauseDuration = System.currentTimeMillis() - pausedTime
            pausedTime = pauseDuration
            isPaused = false
        }
    }

    /**
     * Reset for a new arrest (re-arrest scenario)
     */
    fun resetForNewArrest() {
        _currentCycle.value = 1
        cycleStartTime = System.currentTimeMillis()
        pausedTime = 0L
        _elapsedInCycleMs.value = 0L
        isPaused = false
    }

    fun updateConfig(newConfig: AlertConfig) {
        config = newConfig
    }

    /**
     * Trigger the cycle completion alert at MAXIMUM VOLUME
     */
    private suspend fun triggerCycleAlert() {
        if (!config.cycleAlertEnabled) return

        // Emit event for observers
        _cycleAlertTriggered.emit(_currentCycle.value)

        // Play alert at maximum volume
        playMaxVolumeAlert()

        // Strong vibration pattern
        vibrateAlert()
    }

    /**
     * Play the alert sound at MAXIMUM AUDIBLE VOLUME
     *
     * Strategy:
     * 1. Save current alarm volume
     * 2. Set alarm stream to maximum
     * 3. Request audio focus
     * 4. Play the alert
     * 5. Restore original volume
     *
     * This ensures the alert is heard even if the user has turned down volume.
     */
    private fun playMaxVolumeAlert() {
        try {
            // Save current alarm volume
            originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

            // Set to maximum volume
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                maxAlarmVolume,
                0 // No flags - silent volume change
            )

            // Request audio focus for the alarm
            val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .build()
            } else {
                null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }

            // Play the alert
            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.reloadStaticData()
                    track.setVolume(1.0f) // Full track volume
                    track.play()

                    // Schedule volume restoration after playback
                    scope.launch {
                        delay(ALERT_DURATION_MS.toLong() + 100) // Wait for playback + margin
                        restoreVolume()

                        // Abandon audio focus
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                            audioManager.abandonAudioFocusRequest(focusRequest)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // If anything fails, try to restore volume
            restoreVolume()
        }
    }

    /**
     * Restore the original alarm volume
     */
    private fun restoreVolume() {
        try {
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                originalAlarmVolume,
                0
            )
        } catch (e: Exception) {
            // Ignore - best effort
        }
    }

    /**
     * Strong vibration pattern for tactile alert
     */
    private fun vibrateAlert() {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Three strong pulses
                    val pattern = longArrayOf(0, 300, 100, 300, 100, 300)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255) // Maximum amplitude
                    v.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 300, 100, 300, 100, 300), -1)
                }
            }
        } catch (e: Exception) {
            // Fail silently
        }
    }

    /**
     * Manually trigger an alert (for testing)
     */
    fun manualTrigger() {
        scope.launch {
            triggerCycleAlert()
        }
    }

    fun release() {
        stop()
        restoreVolume() // Ensure volume is restored on cleanup
        scope.cancel()
        audioTrack?.release()
        audioTrack = null
    }

    fun getFormattedTimeRemaining(): String {
        val remaining = (config.cycleIntervalMs - _elapsedInCycleMs.value).coerceAtLeast(0)
        val seconds = (remaining / 1000) % 60
        val minutes = (remaining / 1000) / 60
        return String.format("%d:%02d", minutes, seconds)
    }

    fun getFormattedTimeElapsed(): String {
        val elapsed = _elapsedInCycleMs.value
        val seconds = (elapsed / 1000) % 60
        val minutes = (elapsed / 1000) / 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
