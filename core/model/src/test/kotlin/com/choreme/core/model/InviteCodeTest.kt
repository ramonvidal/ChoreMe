package com.choreme.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class InviteCodeTest {
    @Test
    fun `generated codes are valid`() {
        val random = Random(42)
        repeat(1_000) { assertTrue(InviteCode.isValid(InviteCode.generate(random))) }
    }

    @Test
    fun `normalize accepts how people type codes`() {
        assertEquals("ABC234", InviteCode.normalize(" abc-234 "))
    }

    @Test
    fun `ambiguous characters are rejected`() {
        assertFalse(InviteCode.isValid("ABC0O1"))
        assertFalse(InviteCode.isValid("ABC23"))
    }
}
