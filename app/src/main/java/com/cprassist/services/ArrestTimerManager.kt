package com.cprassist.services

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Timer state representing the current status
 */
enum class ArrestTimerState {
    STOPPED,
    RUNNING,
    PAUSED,
    ROSC
}

/**
 * Manages timing for cardiac arrest events.
 * Uses Handler for reliable UI updates and SystemClock.elapsedRealtime() for timing.
 */
class ArrestTimerManager {

    // Handler for timer updates on main thread
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    // Current state
    private val _state = MutableStateFlow(ArrestTimerState.STOPPED)
    val state: StateFlow<ArrestTimerState> = _state.asStateFlow()

    // Total arrest timer values (cumulative, doesn't reset on re-arrest)
    private val _totalElapsedMs = MutableStateFlow(0L)
    val totalElapsedMs: StateFlow<Long> = _totalElapsedMs.asStateFlow()

    private val _formattedTotalTime = MutableStateFlow("00:00")
    val formattedTotalTime: StateFlow<String> = _formattedTotalTime.asStateFlow()

    // Episode timer values (resets on re-arrest)
    private val _episodeElapsedMs = MutableStateFlow(0L)
    val episodeElapsedMs: StateFlow<Long> = _episodeElapsedMs.asStateFlow()

    private val _formattedEpisodeTime = MutableStateFlow("00:00")
    val formattedEpisodeTime: StateFlow<String> = _formattedEpisodeTime.asStateFlow()

    // Legacy accessor for backward compatibility
    val elapsedMs: StateFlow<Long> = _totalElapsedMs
    val formattedTime: StateFlow<String> = _formattedTotalTime

    // ROSC timer values
    private val _roscDurationMs = MutableStateFlow(0L)
    val roscDurationMs: StateFlow<Long> = _roscDurationMs.asStateFlow()

    private val _formattedRoscTime = MutableStateFlow("00:00")
    val formattedRoscTime: StateFlow<String> = _formattedRoscTime.asStateFlow()

    // Timing anchors (using SystemClock.elapsedRealtime for reliability)
    private var totalArrestStartTime: Long = 0L  // For total time calculation
    private var episodeStartTime: Long = 0L      // For episode time (resets on re-arrest)
    private var pausedTotalElapsed: Long = 0L
    private var pausedEpisodeElapsed: Long = 0L
    private var roscStartTime: Long = 0L
    private var totalDurationAtRosc: Long = 0L
    private var episodeDurationAtRosc: Long = 0L

    // Accumulated totals
    private var totalROSCTime: Long = 0L

    /**
     * Start the arrest timer (fresh start)
     */
    fun start() {
        stopTimer()

        when (_state.value) {
            ArrestTimerState.STOPPED -> {
                // Fresh start - both timers start from zero
                val now = SystemClock.elapsedRealtime()
                totalArrestStartTime = now
                episodeStartTime = now
                _state.value = ArrestTimerState.RUNNING
                startArrestTimer()
            }
            ArrestTimerState.PAUSED -> {
                // Resume from pause - adjust start times
                val now = SystemClock.elapsedRealtime()
                totalArrestStartTime = now - pausedTotalElapsed
                episodeStartTime = now - pausedEpisodeElapsed
                _state.value = ArrestTimerState.RUNNING
                startArrestTimer()
            }
            ArrestTimerState.ROSC -> {
                // Re-arrest from ROSC
                markReArrest()
            }
            ArrestTimerState.RUNNING -> {
                // Already running, ensure timer is active
                startArrestTimer()
            }
        }
    }

    /**
     * Pause the timer
     */
    fun pause() {
        if (_state.value == ArrestTimerState.RUNNING) {
            stopTimer()
            pausedTotalElapsed = _totalElapsedMs.value
            pausedEpisodeElapsed = _episodeElapsedMs.value
            _state.value = ArrestTimerState.PAUSED
        }
    }

    /**
     * Resume from pause
     */
    fun resume() {
        if (_state.value == ArrestTimerState.PAUSED) {
            start()
        }
    }

    /**
     * Mark ROSC - stops arrest timer, begins tracking ROSC duration
     */
    fun markROSC() {
        stopTimer()

        // Save current arrest durations
        totalDurationAtRosc = _totalElapsedMs.value
        episodeDurationAtRosc = _episodeElapsedMs.value
        roscStartTime = SystemClock.elapsedRealtime()

        _state.value = ArrestTimerState.ROSC
        startRoscTimer()
    }

    /**
     * Mark re-arrest - total timer continues, episode timer resets
     */
    fun markReArrest() {
        // Stop any running timer
        stopTimer()

        // Calculate ROSC duration and add to total
        if (roscStartTime > 0) {
            totalROSCTime += SystemClock.elapsedRealtime() - roscStartTime
        }

        val now = SystemClock.elapsedRealtime()

        // Total timer: continues from where it was
        val savedTotalDuration = totalDurationAtRosc
        totalArrestStartTime = now - savedTotalDuration

        // Episode timer: RESETS to zero for new episode
        episodeStartTime = now

        // Reset ROSC values
        _roscDurationMs.value = 0L
        _formattedRoscTime.value = "00:00"
        roscStartTime = 0L

        // Update state to RUNNING
        _state.value = ArrestTimerState.RUNNING

        // Immediately update displays
        _totalElapsedMs.value = savedTotalDuration
        _formattedTotalTime.value = formatDuration(savedTotalDuration)
        _episodeElapsedMs.value = 0L
        _formattedEpisodeTime.value = "00:00"

        // Start the arrest timer
        startArrestTimer()
    }

    /**
     * Stop and reset the timer completely
     */
    fun stop() {
        stopTimer()
        _state.value = ArrestTimerState.STOPPED
        _totalElapsedMs.value = 0L
        _formattedTotalTime.value = "00:00"
        _episodeElapsedMs.value = 0L
        _formattedEpisodeTime.value = "00:00"
        _roscDurationMs.value = 0L
        _formattedRoscTime.value = "00:00"
        totalArrestStartTime = 0L
        episodeStartTime = 0L
        pausedTotalElapsed = 0L
        pausedEpisodeElapsed = 0L
        roscStartTime = 0L
        totalDurationAtRosc = 0L
        episodeDurationAtRosc = 0L
        totalROSCTime = 0L
    }

    /**
     * Get total arrest duration (excluding ROSC time)
     */
    fun getTotalArrestDuration(): Long = _totalElapsedMs.value

    /**
     * Get current episode duration
     */
    fun getEpisodeDuration(): Long = _episodeElapsedMs.value

    /**
     * Get total ROSC duration across all ROSC periods
     */
    fun getTotalROSCDuration(): Long {
        val currentRosc = if (_state.value == ArrestTimerState.ROSC && roscStartTime > 0) {
            SystemClock.elapsedRealtime() - roscStartTime
        } else {
            0L
        }
        return totalROSCTime + currentRosc
    }

    fun isInROSC(): Boolean = _state.value == ArrestTimerState.ROSC
    fun isRunning(): Boolean = _state.value == ArrestTimerState.RUNNING
    fun isPaused(): Boolean = _state.value == ArrestTimerState.PAUSED

    private fun stopTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
    }

    private fun startArrestTimer() {
        stopTimer()

        val totalStart = totalArrestStartTime
        val episodeStart = episodeStartTime
        if (totalStart == 0L) return

        val runnable = object : Runnable {
            override fun run() {
                val now = SystemClock.elapsedRealtime()

                // Update total elapsed time
                val totalElapsed = now - totalStart
                _totalElapsedMs.value = totalElapsed
                _formattedTotalTime.value = formatDuration(totalElapsed)

                // Update episode elapsed time
                val episodeElapsed = now - episodeStart
                _episodeElapsedMs.value = episodeElapsed
                _formattedEpisodeTime.value = formatDuration(episodeElapsed)

                // Always reschedule - stopTimer() will remove callbacks when needed
                handler.postDelayed(this, 100)
            }
        }
        timerRunnable = runnable
        // Execute immediately first, then schedule repeats
        runnable.run()
    }

    private fun startRoscTimer() {
        stopTimer()

        val startTime = roscStartTime
        if (startTime == 0L) return

        val runnable = object : Runnable {
            override fun run() {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                _roscDurationMs.value = elapsed
                _formattedRoscTime.value = formatDuration(elapsed)
                // Always reschedule - stopTimer() will remove callbacks when needed
                handler.postDelayed(this, 100)
            }
        }
        timerRunnable = runnable
        // Execute immediately first, then schedule repeats
        runnable.run()
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun release() {
        stopTimer()
    }
}
