package com.cprassist.ui.accessibility

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * Accessibility utilities for CPR Assist
 *
 * These utilities help make the app usable with screen readers
 * and other accessibility services.
 */

/**
 * Minimum touch target size as per accessibility guidelines (48dp)
 */
val MinTouchTargetSize = 48.dp

/**
 * Larger touch target for emergency buttons (56dp)
 */
val EmergencyTouchTargetSize = 56.dp

/**
 * Modifier to ensure minimum touch target size
 */
fun Modifier.minTouchTarget(): Modifier = this.sizeIn(
    minWidth = MinTouchTargetSize,
    minHeight = MinTouchTargetSize
)

/**
 * Modifier for emergency buttons with larger touch targets
 */
fun Modifier.emergencyTouchTarget(): Modifier = this.sizeIn(
    minWidth = EmergencyTouchTargetSize,
    minHeight = EmergencyTouchTargetSize
)

/**
 * Modifier for elements that announce updates (like timers)
 */
fun Modifier.announceUpdates(
    description: String
): Modifier = this.semantics {
    contentDescription = description
    liveRegion = LiveRegionMode.Polite
}

/**
 * Modifier for critical announcements (like ROSC)
 */
fun Modifier.announceCritical(
    description: String
): Modifier = this.semantics {
    contentDescription = description
    liveRegion = LiveRegionMode.Assertive
}

/**
 * Modifier to mark element as a heading
 */
fun Modifier.accessibilityHeading(): Modifier = this.semantics {
    heading()
}

/**
 * Modifier to add state description for buttons
 */
fun Modifier.buttonState(
    action: String,
    state: String? = null
): Modifier = this.semantics {
    contentDescription = action
    if (state != null) {
        stateDescription = state
    }
}

/**
 * Content descriptions for common actions
 */
object ContentDescriptions {
    // Mode selection
    const val CPR_GUIDANCE_BUTTON = "Start CPR guidance mode for bystanders. Provides voice instructions and metronome."
    const val PROFESSIONAL_MODE_BUTTON = "Start professional mode for trained responders. Includes event logging and timers."

    // Professional mode
    const val CONFIRM_ARREST_BUTTON = "Confirm cardiac arrest. This will start all timers and the metronome."
    const val ROSC_BUTTON = "Mark return of spontaneous circulation. Stops arrest timer."
    const val RE_ARREST_BUTTON = "Mark re-arrest. Resumes arrest timer."

    // Event buttons
    fun eventButton(eventName: String) = "Log $eventName intervention"

    // Timer
    fun timerDescription(time: String) = "Arrest time: $time"
    fun cycleDescription(cycle: Int) = "Current cycle: $cycle"

    // Navigation
    const val BACK_BUTTON = "Go back"
    const val SETTINGS_BUTTON = "Open settings"
    const val HISTORY_BUTTON = "View session history"

    // Metronome
    fun metronomeDescription(bpm: Int, isRunning: Boolean): String {
        return if (isRunning) {
            "Metronome running at $bpm beats per minute"
        } else {
            "Metronome stopped"
        }
    }

    // Alerts
    const val SWITCH_RESCUER_ALERT = "Time to switch rescuers. You have been doing compressions for 2 minutes."
}

/**
 * Haptic feedback patterns for accessibility
 */
object HapticPatterns {
    // Standard button press
    val BUTTON_PRESS = longArrayOf(0, 30)

    // Confirmation feedback
    val CONFIRM = longArrayOf(0, 50, 50, 50)

    // Alert pattern
    val ALERT = longArrayOf(0, 100, 100, 100, 100, 100)

    // Success (ROSC)
    val SUCCESS = longArrayOf(0, 100, 50, 100, 50, 200)

    // Error/critical
    val CRITICAL = longArrayOf(0, 200, 100, 200)
}

/**
 * Announcement strings for TalkBack
 */
object Announcements {
    const val CPR_STARTED = "CPR guidance started. Follow the voice prompts and metronome."
    const val ARREST_CONFIRMED = "Cardiac arrest confirmed. Arrest timer started."
    const val ROSC_ACHIEVED = "ROSC achieved! Return of spontaneous circulation. Monitor patient."
    const val RE_ARREST = "Re-arrest detected. Resuming CPR protocol."
    const val SWITCH_RESCUER = "Two minutes elapsed. Switch rescuers if possible."

    fun eventLogged(eventName: String) = "$eventName logged"
    fun cycleCompleted(cycle: Int) = "Cycle $cycle completed"
}
