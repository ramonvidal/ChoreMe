package com.choreme.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChoreTest {
    private val mom = Member(id = "mom", name = "Mom", role = Role.PARENT)
    private val kid = Member(id = "kid", name = "Leo", role = Role.KID)
    private val dishes = Chore(id = "1", title = "Wash dishes")

    @Test
    fun `anyone can grab an open chore`() {
        val grabbed = dishes.claim(kid)
        assertEquals(ChoreStatus.CLAIMED, grabbed.status)
        assertEquals("kid", grabbed.claimedBy)
    }

    @Test(expected = IllegalStateException::class)
    fun `a grabbed chore cannot be grabbed again`() {
        dishes.claim(kid).claim(mom)
    }

    @Test
    fun `parent completing a chore marks it done`() {
        assertEquals(ChoreStatus.DONE, dishes.complete(mom).status)
    }

    @Test
    fun `kid completing a chore waits for approval`() {
        val done = dishes.claim(kid).complete(kid)
        assertEquals(ChoreStatus.PENDING_APPROVAL, done.status)
        assertEquals("kid", done.completedBy)
    }

    @Test
    fun `parent approval finishes a kid's chore`() {
        val approved = dishes.complete(kid).approve(mom)
        assertEquals(ChoreStatus.DONE, approved.status)
        assertEquals("mom", approved.approvedBy)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `kids cannot approve chores`() {
        dishes.complete(kid).approve(kid)
    }

    @Test
    fun `rejected chore goes back to the kid who did it`() {
        val rejected = dishes.complete(kid).reject(mom)
        assertEquals(ChoreStatus.CLAIMED, rejected.status)
        assertEquals("kid", rejected.claimedBy)
    }
}
