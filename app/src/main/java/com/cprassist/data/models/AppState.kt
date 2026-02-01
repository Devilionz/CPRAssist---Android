package com.cprassist.data.models

/**
 * Represents the current state of the arrest management
 */
sealed class ArrestState {
    /**
     * No active arrest - waiting for confirmation
     */
    object Idle : ArrestState()

    /**
     * Active cardiac arrest with ongoing CPR
     */
    data class Active(
        val session: ArrestSession,
        val currentCycle: Int = 1,
        val cycleStartTime: Long = System.currentTimeMillis(),
        val isPaused: Boolean = false,
        val pauseStartTime: Long? = null,
        val totalPausedTime: Long = 0L
    ) : ArrestState() {
        /**
         * Get elapsed time in current arrest, excluding paused time
         */
        fun getElapsedTime(): Long {
            val now = System.currentTimeMillis()
            val rawElapsed = now - session.startTime - totalPausedTime
            return if (isPaused && pauseStartTime != null) {
                rawElapsed - (now - pauseStartTime)
            } else {
                rawElapsed
            }
        }

        /**
         * Get time elapsed in current 2-minute cycle
         */
        fun getCycleElapsed(): Long {
            val now = System.currentTimeMillis()
            val rawElapsed = now - cycleStartTime
            return if (isPaused && pauseStartTime != null) {
                val pauseDuration = now - pauseStartTime
                rawElapsed - pauseDuration
            } else {
                rawElapsed
            }
        }
    }

    /**
     * Return of Spontaneous Circulation achieved
     */
    data class ROSC(
        val session: ArrestSession,
        val roscTime: Long = System.currentTimeMillis(),
        val roscStartTime: Long = System.currentTimeMillis(),
        val previousArrestDuration: Long
    ) : ArrestState() {
        /**
         * Duration since ROSC was achieved
         */
        fun getROSCDuration(): Long {
            return System.currentTimeMillis() - roscStartTime
        }
    }

    /**
     * Event has ended, viewing summary
     */
    data class Completed(
        val session: ArrestSession
    ) : ArrestState()
}

/**
 * State for CPR Guidance Mode
 */
sealed class CPRGuidanceState {
    object Idle : CPRGuidanceState()

    data class Active(
        val startTime: Long = System.currentTimeMillis(),
        val currentPhase: CPRPhase = CPRPhase.CALL_EMERGENCY,
        val compressionCount: Int = 0,
        val currentCycle: Int = 1,
        val isMetronomeRunning: Boolean = false
    ) : CPRGuidanceState() {
        fun getElapsedTime(): Long = System.currentTimeMillis() - startTime
    }

    object Paused : CPRGuidanceState()
}

/**
 * Phases of CPR guidance for bystanders
 */
enum class CPRPhase(val instruction: String, val ttsPrompt: String) {
    CALL_EMERGENCY(
        "CALL 911 NOW",
        "Call 9 1 1 now if you haven't already. Put the phone on speaker."
    ),
    CHECK_RESPONSE(
        "CHECK FOR RESPONSE",
        "Tap the person's shoulders and shout. Are you okay?"
    ),
    START_COMPRESSIONS(
        "START COMPRESSIONS",
        "Place the heel of your hand on the center of the chest. Push hard and fast."
    ),
    COMPRESSIONS_ACTIVE(
        "PUSH HARD & FAST",
        ""
    ),
    SWITCH_RESCUER(
        "SWITCH RESCUER",
        "Switch rescuer now if possible. You've been doing compressions for 2 minutes."
    ),
    AED_INSTRUCTION(
        "USE AED IF AVAILABLE",
        "If an A E D is available, turn it on and follow the voice prompts."
    )
}

/**
 * Metronome configuration
 */
data class MetronomeConfig(
    val bpm: Int = 110, // Default 110 BPM (middle of 100-120 range)
    val isEnabled: Boolean = true,
    val useVibration: Boolean = true,
    val useSound: Boolean = true,
    val volumePercent: Int = 100
) {
    init {
        require(bpm in 60..180) { "BPM must be between 60 and 180" }
        require(volumePercent in 0..100) { "Volume must be between 0 and 100" }
    }

    /**
     * Interval between beats in milliseconds
     */
    val intervalMs: Long get() = (60_000L / bpm)
}

/**
 * Alert configuration
 */
data class AlertConfig(
    val cycleAlertEnabled: Boolean = true,
    val cycleAlertDurationMs: Long = 1000L, // 1 second beep
    val cycleIntervalMs: Long = 120_000L, // 2 minutes
    val alertVolume: Int = 100
)

/**
 * UI state for Professional Mode
 */
data class ProfessionalModeUIState(
    val arrestState: ArrestState = ArrestState.Idle,
    val metronomeConfig: MetronomeConfig = MetronomeConfig(),
    val alertConfig: AlertConfig = AlertConfig(),
    val lastEvent: TimestampedEvent? = null,
    val eventCount: Int = 0,
    val formattedElapsedTime: String = "00:00",      // Total arrest time (cumulative)
    val formattedEpisodeTime: String = "00:00",      // Episode time (resets on re-arrest)
    val formattedCycleTime: String = "00:00",
    val formattedRoscTime: String = "00:00",
    val isMetronomeRunning: Boolean = false,
    val currentEpisode: Int = 1                       // Current arrest episode number
)

/**
 * UI state for CPR Guidance Mode
 */
data class CPRGuidanceUIState(
    val state: CPRGuidanceState = CPRGuidanceState.Idle,
    val metronomeConfig: MetronomeConfig = MetronomeConfig(),
    val currentInstruction: String = "",
    val compressionCount: Int = 0,
    val formattedElapsedTime: String = "00:00",
    val showSwitchAlert: Boolean = false,
    val isMetronomeRunning: Boolean = false
)
