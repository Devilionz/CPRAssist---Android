package com.cprassist

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * Application class for CPR Assist.
 * Initializes notification channels and global resources.
 */
class CPRAssistApplication : Application() {

    companion object {
        const val METRONOME_CHANNEL_ID = "metronome_channel"
        const val ALERT_CHANNEL_ID = "alert_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    /**
     * Create notification channels for Android O+
     */
    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Metronome channel - low importance, no sound (audio is handled by AudioTrack)
        val metronomeChannel = NotificationChannel(
            METRONOME_CHANNEL_ID,
            "CPR Metronome",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the CPR metronome running in the background"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }

        // Alert channel - high importance for 2-minute alerts
        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "CPR Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important alerts during CPR such as 2-minute cycle reminders"
            setShowBadge(true)
            enableVibration(true)
        }

        notificationManager.createNotificationChannels(
            listOf(metronomeChannel, alertChannel)
        )
    }
}
