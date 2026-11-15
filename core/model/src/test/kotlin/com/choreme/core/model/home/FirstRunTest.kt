package com.choreme.core.model.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunTest {
    private val fresh = SetupFacts(hasPlace = false, sharing = false, notificationsAllowed = false, hasTrip = false, isParent = true)

    @Test
    fun `a parent gets all four steps in order`() {
        assertEquals(
            listOf(FirstStep.SAVE_HOME, FirstStep.SHARE_LOCATION, FirstStep.ALLOW_NOTIFICATIONS, FirstStep.PLAN_TRIP),
            firstRunSteps(fresh).map { it.step },
        )
    }

    @Test
    fun `a kid is only asked for location and notifications`() {
        assertEquals(
            listOf(FirstStep.SHARE_LOCATION, FirstStep.ALLOW_NOTIFICATIONS),
            firstRunSteps(fresh.copy(isParent = false)).map { it.step },
        )
    }

    @Test
    fun `steps tick as things get done`() {
        val steps = firstRunSteps(fresh.copy(hasPlace = true, notificationsAllowed = true))
        assertEquals(listOf(true, false, true, false), steps.map { it.done })
    }

    @Test
    fun `the list is shown until everything is done or hidden`() {
        assertTrue(showFirstRun(firstRunSteps(fresh), hidden = false))
        assertFalse(showFirstRun(firstRunSteps(fresh), hidden = true))
        val all = SetupFacts(hasPlace = true, sharing = true, notificationsAllowed = true, hasTrip = true, isParent = true)
        assertFalse(showFirstRun(firstRunSteps(all), hidden = false))
    }
}
