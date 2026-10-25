package com.choreme.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LeavingTest {
    private val mom = Member("mom", "Mom", Role.PARENT)
    private val dad = Member("dad", "Dad", Role.PARENT)
    private val leo = Member("leo", "Leo", Role.KID)

    @Test
    fun `the last one out deletes the family`() {
        assertEquals(LeaveOutcome.DELETE_FAMILY, leaveOutcome(mom, listOf(mom)))
    }

    @Test
    fun `the only parent can't leave the kids behind`() {
        assertEquals(LeaveOutcome.BLOCKED_LAST_PARENT, leaveOutcome(mom, listOf(mom, leo)))
        assertEquals(LeaveOutcome.LEAVE, leaveOutcome(mom, listOf(mom, dad, leo)))
    }

    @Test
    fun `kids can always leave`() {
        assertEquals(LeaveOutcome.LEAVE, leaveOutcome(leo, listOf(mom, leo)))
    }
}
