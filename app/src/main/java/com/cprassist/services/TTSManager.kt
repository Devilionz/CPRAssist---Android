package com.cprassist.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.Locale
import java.util.UUID

/**
 * Priority levels for TTS messages
 */
enum class TTSPriority {
    LOW,      // General instructions
    NORMAL,   // Standard prompts
    HIGH,     // Important alerts
    CRITICAL  // Emergency instructions - interrupts current speech
}

/**
 * A queued TTS message
 */
data class TTSMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val priority: TTSPriority = TTSPriority.NORMAL
)

/**
 * Manages Text-to-Speech functionality for CPR guidance.
 * Supports priority queuing and interruption for critical messages.
 */
class TTSManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val messageQueue = LinkedList<TTSMessage>()
    private var currentMessage: TTSMessage? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var speechRate: Float = 0.9f // Slightly slower for clarity
    private var pitch: Float = 1.0f

    /**
     * Predefined voice prompts for CPR guidance
     */
    object Prompts {
        // Initial prompts
        const val CALL_911 = "Call 9 1 1 now. Put the phone on speaker if possible."
        const val CHECK_RESPONSE = "Tap their shoulders firmly and shout, Are you okay?"
        const val NO_RESPONSE = "If they're not responding and not breathing normally, start C P R."

        // Compression prompts
        const val START_COMPRESSIONS = "Place the heel of your hand on the center of their chest, between the nipples. Push hard and fast."
        const val PUSH_HARD = "Push hard. The chest should go down at least 2 inches."
        const val PUSH_FAST = "Push fast. Aim for 100 to 120 compressions per minute."
        const val FULL_RECOIL = "Allow the chest to fully rise between compressions."
        const val KEEP_GOING = "You're doing great. Keep going."

        // Cycle prompts
        const val SWITCH_RESCUER = "Two minutes. Switch rescuers now if someone else can help."
        const val CONTINUE_CPR = "Continue C P R. Don't stop unless help arrives or they start breathing."

        // AED prompts
        const val GET_AED = "If an A E D is available, have someone bring it immediately."
        const val AED_ARRIVED = "Turn on the A E D and follow its voice instructions."

        // Encouragement
        const val DOING_WELL = "You're doing well. Each compression helps blood reach the brain."
        const val DONT_STOP = "Don't stop. C P R doubles or triples their chance of survival."
    }

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)

            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                _isInitialized.value = true

                // Configure TTS parameters
                tts?.setSpeechRate(speechRate)
                tts?.setPitch(pitch)

                // Set up progress listener
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) {
                        scope.launch {
                            _isSpeaking.value = true
                        }
                    }

                    override fun onDone(utteranceId: String) {
                        scope.launch {
                            _isSpeaking.value = false
                            currentMessage = null
                            processNextInQueue()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String) {
                        scope.launch {
                            _isSpeaking.value = false
                            currentMessage = null
                            processNextInQueue()
                        }
                    }

                    override fun onError(utteranceId: String, errorCode: Int) {
                        scope.launch {
                            _isSpeaking.value = false
                            currentMessage = null
                            processNextInQueue()
                        }
                    }
                })
            }
        }
    }

    /**
     * Speak a message with the given priority
     */
    fun speak(text: String, priority: TTSPriority = TTSPriority.NORMAL) {
        if (!_isInitialized.value) return

        val message = TTSMessage(text = text, priority = priority)

        when (priority) {
            TTSPriority.CRITICAL -> {
                // Stop current speech and speak immediately
                stopSpeaking()
                messageQueue.clear()
                speakNow(message)
            }
            TTSPriority.HIGH -> {
                // Add to front of queue
                messageQueue.addFirst(message)
                if (!_isSpeaking.value) {
                    processNextInQueue()
                }
            }
            else -> {
                // Add to queue normally
                messageQueue.addLast(message)
                if (!_isSpeaking.value) {
                    processNextInQueue()
                }
            }
        }
    }

    /**
     * Speak a message immediately
     */
    private fun speakNow(message: TTSMessage) {
        currentMessage = message
        tts?.speak(
            message.text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            message.id
        )
    }

    /**
     * Process the next message in the queue
     */
    private fun processNextInQueue() {
        if (messageQueue.isEmpty()) return

        // Sort by priority and take the highest priority message
        messageQueue.sortByDescending { it.priority.ordinal }
        val nextMessage = messageQueue.pollFirst() ?: return

        speakNow(nextMessage)
    }

    /**
     * Stop speaking and clear the queue
     */
    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
        currentMessage = null
    }

    /**
     * Clear the message queue without stopping current speech
     */
    fun clearQueue() {
        messageQueue.clear()
    }

    /**
     * Set speech rate (0.5 = half speed, 1.0 = normal, 2.0 = double speed)
     */
    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(speechRate)
    }

    /**
     * Set pitch (0.5 = lower, 1.0 = normal, 2.0 = higher)
     */
    fun setPitch(newPitch: Float) {
        pitch = newPitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(pitch)
    }

    /**
     * Speak the 2-minute switch rescuer prompt
     */
    fun speakSwitchRescuer() {
        speak(Prompts.SWITCH_RESCUER, TTSPriority.HIGH)
    }

    /**
     * Speak initial CPR guidance
     */
    fun speakInitialGuidance() {
        speak(Prompts.CALL_911, TTSPriority.HIGH)
    }

    /**
     * Speak compression instructions
     */
    fun speakStartCompressions() {
        speak(Prompts.START_COMPRESSIONS, TTSPriority.HIGH)
    }

    /**
     * Speak periodic encouragement
     */
    fun speakEncouragement() {
        val encouragements = listOf(
            Prompts.KEEP_GOING,
            Prompts.DOING_WELL,
            Prompts.DONT_STOP,
            Prompts.PUSH_HARD,
            Prompts.FULL_RECOIL
        )
        speak(encouragements.random(), TTSPriority.LOW)
    }

    /**
     * Check if TTS is available on this device
     */
    fun isAvailable(): Boolean = _isInitialized.value

    /**
     * Clean up resources
     */
    fun shutdown() {
        scope.cancel()
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isInitialized.value = false
    }
}
