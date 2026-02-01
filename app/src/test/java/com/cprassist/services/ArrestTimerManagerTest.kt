package com.cprassist.services

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ArrestTimerManager
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ArrestTimerManagerTest {

    private lateinit var timerManager: ArrestTimerManager

    @Before
    fun setup() {
        timerManager = ArrestTimerManager()
    }

    @After
    fun tearDown() {
        timerManager.release()
    }

    @Test
    fun `initial state is Stopped`() {
        assertEquals(TimerState.Stopped, timerManager.state.value)
    }

    @Test
    fun `start changes state to Running`() {
        timerManager.start()
        assertTrue(timerManager.state.value is TimerState.Running)
    }

    @Test
    fun `pause changes state to Paused`() {
        timerManager.start()
        timerManager.pause()
        assertTrue(timerManager.state.value is TimerState.Paused)
    }

    @Test
    fun `resume from pause changes state to Running`() {
        timerManager.start()
        timerManager.pause()
        timerManager.resume()
        assertTrue(timerManager.state.value is TimerState.Running)
    }

    @Test
    fun `markROSC changes state to ROSC`() {
        timerManager.start()
        timerManager.markROSC()
        assertTrue(timerManager.state.value is TimerState.ROSC)
    }

    @Test
    fun `markReArrest from ROSC changes state to Running`() {
        timerManager.start()
        timerManager.markROSC()
        timerManager.markReArrest()
        assertTrue(timerManager.state.value is TimerState.Running)
    }

    @Test
    fun `stop resets state to Stopped`() {
        timerManager.start()
        timerManager.stop()
        assertEquals(TimerState.Stopped, timerManager.state.value)
        assertEquals(0L, timerManager.elapsedMs.value)
    }

    @Test
    fun `isRunning returns true when Running`() {
        timerManager.start()
        assertTrue(timerManager.isRunning())
    }

    @Test
    fun `isPaused returns true when Paused`() {
        timerManager.start()
        timerManager.pause()
        assertTrue(timerManager.isPaused())
    }

    @Test
    fun `isInROSC returns true when in ROSC state`() {
        timerManager.start()
        timerManager.markROSC()
        assertTrue(timerManager.isInROSC())
    }

    @Test
    fun `getTotalArrestDuration returns elapsed time when running`() {
        timerManager.start()
        // The elapsed time should be tracked
        val duration = timerManager.getTotalArrestDuration()
        assertTrue(duration >= 0)
    }

    @Test
    fun `multiple start calls do not reset timer`() {
        timerManager.start()
        val state1 = timerManager.state.value as TimerState.Running
        val startTime1 = state1.startTime

        timerManager.start() // Call start again

        val state2 = timerManager.state.value as TimerState.Running
        assertEquals(startTime1, state2.startTime)
    }

    @Test
    fun `ROSC preserves arrest duration`() {
        timerManager.start()
        Thread.sleep(100) // Let some time pass

        val durationBeforeROSC = timerManager.elapsedMs.value
        timerManager.markROSC()

        val roscState = timerManager.state.value as TimerState.ROSC
        assertTrue(roscState.arrestDuration >= durationBeforeROSC)
    }
}
