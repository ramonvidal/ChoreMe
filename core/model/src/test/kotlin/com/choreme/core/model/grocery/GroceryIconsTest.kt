package com.choreme.core.model.grocery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GroceryIconsTest {
    private fun icon(name: String, category: GroceryCategory = GroceryCategory.OTHER) = GroceryIcons.forItem(name, category)

    @Test
    fun `common items get their own icon`() {
        assertEquals("🥛", icon("Milk"))
        assertEquals("🥚", icon("Eggs"))
        assertEquals("🍞", icon("Bread"))
        assertEquals("🧻", icon("Toilet paper"))
        assertEquals("🍅", icon("Cherry tomatoes"))
    }

    @Test
    fun `the item word wins over describing words`() {
        assertEquals("🧃", icon("Orange juice"))
        assertEquals("🥜", icon("Peanut butter"))
        assertEquals("🍦", icon("Ice cream"))
        assertEquals("🥔", icon("Sweet potatoes"))
        assertEquals("🍚", icon("Rice"))
    }

    @Test
    fun `unknown items fall back to their aisle`() {
        assertEquals("🥕", icon("Kohlrabi", GroceryCategory.PRODUCE))
        assertEquals("🛒", icon("Birthday candles"))
    }

    @Test
    fun `every starter item has its own icon`() {
        StarterItems.forEach { (name, _) ->
            assertNotNull("$name has no icon", GroceryIcons.specific(name))
        }
    }
}
