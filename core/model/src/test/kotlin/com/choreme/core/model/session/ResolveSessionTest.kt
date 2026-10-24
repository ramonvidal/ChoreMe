package com.choreme.core.model.session

import com.choreme.core.model.Role
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveSessionTest {
    private val parent = Account(uid = "u1", isAnonymous = false, displayName = "Ramon Vidal")
    private val kid = Account(uid = "k1", isAnonymous = true, displayName = null)
    private val pointer = Remote.Present(FamilyPointer("fam1", "Vidals"))

    @Test
    fun `no account means signed out`() {
        assertEquals(SessionState.SignedOut, resolveSession(null, Remote.Missing, Remote.Missing, Remote.Missing))
    }

    @Test
    fun `waits for the server before deciding there is no family`() {
        assertEquals(SessionState.Loading, resolveSession(parent, Remote.Unknown, Remote.Missing, Remote.Missing))
        assertEquals(SessionState.Loading, resolveSession(parent, pointer, Remote.Unknown, Remote.Missing))
    }

    @Test
    fun `new parent is offered to create or join, with their first name`() {
        assertEquals(
            SessionState.NoFamily(isKid = false, suggestedName = "Ramon", requestDeclined = false),
            resolveSession(parent, Remote.Missing, Remote.Missing, Remote.Missing),
        )
    }

    @Test
    fun `kid device is flagged as a kid`() {
        val state = resolveSession(kid, Remote.Missing, Remote.Missing, Remote.Missing) as SessionState.NoFamily
        assertEquals(true, state.isKid)
        assertEquals("", state.suggestedName)
    }

    @Test
    fun `pending request waits for approval`() {
        assertEquals(
            SessionState.AwaitingApproval("fam1", "Vidals"),
            resolveSession(kid, pointer, Remote.Missing, Remote.Present(Unit)),
        )
    }

    @Test
    fun `membership wins over everything else`() {
        assertEquals(
            SessionState.InFamily("fam1", "kid-leo", Role.KID),
            resolveSession(kid, pointer, Remote.Present(Membership("kid-leo", Role.KID)), Remote.Present(Unit)),
        )
    }

    @Test
    fun `request gone without membership means declined or removed`() {
        val state = resolveSession(kid, pointer, Remote.Missing, Remote.Missing) as SessionState.NoFamily
        assertEquals(true, state.requestDeclined)
    }
}
