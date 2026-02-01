package com.cprassist.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.cprassist.data.models.MetronomeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Foreground service that provides a metronome for CPR compressions.
 * Uses AudioTrack for low-latency audio playback.
 */
class MetronomeService : Service() {

    private val binder = MetronomeBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var metronomeJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var vibrator: Vibrator? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _beatCount = MutableStateFlow(0L)
    val beatCount: StateFlow<Long> = _beatCount.asStateFlow()

    private var config = MetronomeConfig()

    // Pre-generated click sound
    private lateinit var clickSamples: ShortArray

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "metronome_channel"
        private const val SAMPLE_RATE = 44100
        private const val CLICK_DURATION_MS = 30 // Short, sharp click

        fun createIntent(context: Context): Intent {
            return Intent(context, MetronomeService::class.java)
        }
    }

    inner class MetronomeBinder : Binder() {
        fun getService(): MetronomeService = this@MetronomeService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeAudio()
        initializeVibrator()
        generateClickSound()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        stop()
        serviceScope.cancel()
        audioTrack?.release()
        super.onDestroy()
    }

    /**
     * Initialize AudioTrack for low-latency playback
     */
    private fun initializeAudio() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
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
            .setBufferSizeInBytes(minBufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
    }

    /**
     * Initialize vibrator service
     */
    private fun initializeVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * Generate a sharp click sound for the metronome beat
     */
    private fun generateClickSound() {
        val numSamples = (SAMPLE_RATE * CLICK_DURATION_MS) / 1000
        clickSamples = ShortArray(numSamples)

        // Generate a 880Hz sine wave with quick attack and decay
        val frequency = 880.0 // A5 note - clear and audible

        for (i in 0 until numSamples) {
            val time = i.toDouble() / SAMPLE_RATE

            // Envelope: quick attack, quick decay
            val envelope = if (i < numSamples / 4) {
                i.toDouble() / (numSamples / 4) // Attack
            } else {
                1.0 - ((i - numSamples / 4).toDouble() / (numSamples * 3 / 4)) // Decay
            }.coerceIn(0.0, 1.0)

            val sample = (sin(2 * PI * frequency * time) * Short.MAX_VALUE * envelope).toInt()
            clickSamples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        // Write the static buffer
        audioTrack?.write(clickSamples, 0, clickSamples.size)
    }

    /**
     * Start the metronome with the given configuration
     */
    fun start(newConfig: MetronomeConfig = config) {
        if (_isRunning.value) {
            // Update config while running
            config = newConfig
            return
        }

        config = newConfig
        _isRunning.value = true
        _beatCount.value = 0

        metronomeJob = serviceScope.launch {
            while (isActive && _isRunning.value) {
                playBeat()
                _beatCount.value++
                delay(config.intervalMs)
            }
        }
    }

    /**
     * Stop the metronome
     */
    fun stop() {
        _isRunning.value = false
        metronomeJob?.cancel()
        metronomeJob = null
        audioTrack?.stop()
    }

    /**
     * Update metronome configuration (BPM, volume, etc.)
     */
    fun updateConfig(newConfig: MetronomeConfig) {
        config = newConfig
        if (!config.isEnabled && _isRunning.value) {
            stop()
        }
    }

    /**
     * Reset beat counter
     */
    fun resetBeatCount() {
        _beatCount.value = 0
    }

    /**
     * Play a single metronome beat (sound and/or vibration)
     */
    private fun playBeat() {
        if (config.useSound) {
            playClickSound()
        }

        if (config.useVibration) {
            vibrate()
        }
    }

    /**
     * Play the click sound
     */
    private fun playClickSound() {
        try {
            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.reloadStaticData()

                    // Set volume based on config
                    val volume = config.volumePercent / 100f
                    track.setVolume(volume)

                    track.play()
                }
            }
        } catch (e: Exception) {
            // Fail silently - audio issues shouldn't crash the app
        }
    }

    /**
     * Trigger haptic feedback
     */
    private fun vibrate() {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(
                        VibrationEffect.createOneShot(
                            30L, // Short vibration
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(30L)
                }
            }
        } catch (e: Exception) {
            // Fail silently
        }
    }

    /**
     * Create the notification channel for Android O+
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "CPR Metronome",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the CPR metronome running"
            setShowBadge(false)
            setSound(null, null)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Create foreground service notification
     */
    private fun createNotification(): Notification {
        // Intent to open the app when notification is tapped
        val pendingIntent = packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CPR Assist Active")
            .setContentText("Metronome running at ${config.bpm} BPM")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
