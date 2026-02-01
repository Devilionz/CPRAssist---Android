package com.cprassist.data.repository

import com.cprassist.data.models.ArrestPhase
import com.cprassist.data.models.EventCategory
import com.cprassist.data.models.EventType
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for EventRepository singleton
 */
class EventRepositoryTest {

    @Before
    fun setup() {
        // Clear any previous state before each test
        EventRepository.clear()
    }

    @After
    fun tearDown() {
        EventRepository.clear()
    }

    @Test
    fun `startNewSession creates session and logs arrest started`() {
        val session = EventRepository.startNewSession()

        assertNotNull(session)
        assertNotNull(session.id)
        assertTrue(session.startTime > 0)
        assertEquals(1, EventRepository.events.value.size)
        assertEquals(EventType.ARREST_STARTED, EventRepository.events.value[0].type)
    }

    @Test
    fun `logEvent adds event to list`() {
        EventRepository.startNewSession()

        EventRepository.logEvent(EventType.ADRENALINE)

        assertEquals(2, EventRepository.events.value.size) // ARREST_STARTED + ADRENALINE
        assertEquals(EventType.ADRENALINE, EventRepository.events.value[1].type)
    }

    @Test
    fun `logEvent sets correct elapsed time`() {
        EventRepository.startNewSession()
        Thread.sleep(100) // Let some time pass

        val event = EventRepository.logEvent(EventType.SHOCK_GIVEN)

        assertTrue(event.elapsedTimeMs >= 100)
    }

    @Test
    fun `logEvent includes cycle number`() {
        EventRepository.startNewSession()
        EventRepository.updateCycle(3)

        val event = EventRepository.logEvent(EventType.VF)

        assertEquals(3, event.cycleNumber)
    }

    @Test
    fun `logEvent with notes stores notes`() {
        EventRepository.startNewSession()

        val event = EventRepository.logEvent(EventType.FLUIDS, "500ml NS")

        assertEquals("500ml NS", event.notes)
    }

    @Test
    fun `lastEvent updates on each log`() {
        EventRepository.startNewSession()

        EventRepository.logEvent(EventType.ADRENALINE)
        assertEquals(EventType.ADRENALINE, EventRepository.lastEvent.value?.type)

        EventRepository.logEvent(EventType.AMIODARONE)
        assertEquals(EventType.AMIODARONE, EventRepository.lastEvent.value?.type)
    }

    @Test
    fun `logCycleComplete logs event and increments cycle`() {
        EventRepository.startNewSession()
        EventRepository.updateCycle(2)

        EventRepository.logCycleComplete()

        val events = EventRepository.events.value
        val cycleEvent = events.find { it.type == EventType.CYCLE_COMPLETED }
        assertNotNull(cycleEvent)
        assertEquals(2, cycleEvent?.cycleNumber)
    }

    @Test
    fun `markROSC creates ROSC event and changes phase`() {
        EventRepository.startNewSession()

        val event = EventRepository.markROSC()

        assertEquals(EventType.ROSC, event.type)
        assertEquals(ArrestPhase.ROSC, EventRepository.getCurrentPhase())
    }

    @Test
    fun `markReArrest increments arrest count and episode`() {
        EventRepository.startNewSession()
        val session = EventRepository.getCurrentSession()
        val initialCount = session?.arrestCount ?: 0

        // First go to ROSC
        EventRepository.markROSC()

        // Then re-arrest
        EventRepository.markReArrest()

        assertEquals(initialCount + 1, session?.arrestCount)
        assertEquals(2, EventRepository.getCurrentEpisode())
    }

    @Test
    fun `markReArrest logs re-arrest event`() {
        EventRepository.startNewSession()
        EventRepository.markROSC()

        val event = EventRepository.markReArrest()

        assertEquals(EventType.RE_ARREST, event.type)
        assertEquals(ArrestPhase.RE_ARREST, event.arrestPhase)
    }

    @Test
    fun `multiple ROSC and re-arrest cycles work correctly`() {
        EventRepository.startNewSession()

        // First ROSC
        EventRepository.markROSC()
        assertEquals(ArrestPhase.ROSC, EventRepository.getCurrentPhase())

        // First re-arrest
        EventRepository.markReArrest()
        assertEquals(2, EventRepository.getCurrentEpisode())

        // Second ROSC
        EventRepository.markROSC()
        assertEquals(ArrestPhase.ROSC, EventRepository.getCurrentPhase())

        // Second re-arrest
        EventRepository.markReArrest()
        assertEquals(3, EventRepository.getCurrentEpisode())

        // Verify event count
        val events = EventRepository.events.value
        val roscCount = events.count { it.type == EventType.ROSC }
        val reArrestCount = events.count { it.type == EventType.RE_ARREST }

        assertEquals(2, roscCount)
        assertEquals(2, reArrestCount)
    }

    @Test
    fun `endSession sets end time`() {
        EventRepository.startNewSession()
        val session = EventRepository.getCurrentSession()
        assertNull(session?.endTime)

        EventRepository.endSession()

        assertNotNull(session?.endTime)
    }

    @Test
    fun `getEventCount returns correct count`() {
        EventRepository.startNewSession()
        EventRepository.logEvent(EventType.ADRENALINE)
        EventRepository.logEvent(EventType.ADRENALINE)
        EventRepository.logEvent(EventType.ADRENALINE)
        EventRepository.logEvent(EventType.SHOCK_GIVEN)

        assertEquals(3, EventRepository.getEventCount(EventType.ADRENALINE))
        assertEquals(1, EventRepository.getEventCount(EventType.SHOCK_GIVEN))
        assertEquals(0, EventRepository.getEventCount(EventType.AMIODARONE))
    }

    @Test
    fun `getEventsByCategory returns correct events`() {
        EventRepository.startNewSession()
        EventRepository.logEvent(EventType.VF)
        EventRepository.logEvent(EventType.ASYSTOLE)
        EventRepository.logEvent(EventType.ADRENALINE)

        val rhythmEvents = EventRepository.getEventsByCategory(EventCategory.RHYTHM)

        assertEquals(2, rhythmEvents.size)
        assertTrue(rhythmEvents.all { it.type.category == EventCategory.RHYTHM })
    }

    @Test
    fun `clear removes all data`() {
        EventRepository.startNewSession()
        EventRepository.logEvent(EventType.ADRENALINE)

        EventRepository.clear()

        assertNull(EventRepository.getCurrentSession())
        assertTrue(EventRepository.events.value.isEmpty())
        assertNull(EventRepository.lastEvent.value)
    }

    @Test
    fun `generateTextSummary returns valid string`() {
        EventRepository.startNewSession()
        EventRepository.logEvent(EventType.VF)
        EventRepository.logEvent(EventType.SHOCK_GIVEN)

        val summary = EventRepository.generateTextSummary()

        assertTrue(summary.contains("CARDIAC ARREST EVENT SUMMARY"))
        assertTrue(summary.contains("VF"))
        assertTrue(summary.contains("Shock Given"))
    }

    @Test
    fun `generateJsonExport returns valid JSON`() {
        EventRepository.startNewSession()
        EventRepository.logEvent(EventType.ADRENALINE)

        val json = EventRepository.generateJsonExport()

        assertTrue(json.contains("session_id"))
        assertTrue(json.contains("events"))
        assertTrue(json.contains("ADRENALINE"))
    }

    @Test
    fun `generateJsonExport includes arrest phase information`() {
        EventRepository.startNewSession()
        EventRepository.markROSC()
        EventRepository.markReArrest()
        EventRepository.logEvent(EventType.ADRENALINE)

        val json = EventRepository.generateJsonExport()

        assertTrue(json.contains("arrest_phase"))
        assertTrue(json.contains("arrest_episode"))
        assertTrue(json.contains("arrest_episodes"))
    }

    @Test
    fun `session tracks total arrest duration`() {
        EventRepository.startNewSession()
        Thread.sleep(100)

        val session = EventRepository.getCurrentSession()
        val duration = session?.getTotalArrestDuration() ?: 0

        assertTrue(duration >= 100)
    }

    @Test
    fun `hasActiveSession returns correct state`() {
        assertFalse(EventRepository.hasActiveSession())

        EventRepository.startNewSession()
        assertTrue(EventRepository.hasActiveSession())

        EventRepository.endSession()
        assertFalse(EventRepository.hasActiveSession())
    }

    @Test
    fun `events have correct arrest phase during ROSC`() {
        EventRepository.startNewSession()

        // Log event during normal arrest
        EventRepository.logEvent(EventType.ADRENALINE)

        // Go to ROSC and log event
        EventRepository.markROSC()

        val events = EventRepository.events.value
        val adrenalineEvent = events.find { it.type == EventType.ADRENALINE }
        val roscEvent = events.find { it.type == EventType.ROSC }

        assertEquals(ArrestPhase.ARREST, adrenalineEvent?.arrestPhase)
        assertEquals(ArrestPhase.ROSC, roscEvent?.arrestPhase)
    }
}
