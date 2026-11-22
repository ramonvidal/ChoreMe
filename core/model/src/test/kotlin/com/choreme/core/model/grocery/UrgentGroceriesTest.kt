package com.choreme.core.model.grocery

import org.junit.Assert.assertEquals
import org.junit.Test

class UrgentGroceriesTest {
    private fun item(name: String, needed: Boolean, urgent: Boolean) =
        GroceryItem(id = name, name = name, category = GroceryCategory.OTHER, isNeeded = needed, isStaple = false, urgent = urgent)

    @Test
    fun `only urgent items still to buy count, by name`() {
        val items = listOf(item("Nappies", true, true), item("Milk", true, false), item("bread", false, true), item("Aspirin", true, true))
        assertEquals(listOf("Aspirin", "Nappies"), items.urgentToBuy().map { it.name })
    }
}
