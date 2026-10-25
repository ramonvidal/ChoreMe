package com.choreme.core.model.grocery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GroceryIconsTest {
    private fun icon(name: String, category: GroceryCategory = GroceryCategory.OTHER) = GroceryIcons.forItem(name, category)

    @Test
    fun `common items get their own icon`() {
        assertEquals(GroceryIcon.MILK, icon("Milk"))
        assertEquals(GroceryIcon.EGG, icon("Eggs"))
        assertEquals(GroceryIcon.BREAD, icon("Bread"))
        assertEquals(GroceryIcon.TOILET_PAPER, icon("Toilet paper"))
        assertEquals(GroceryIcon.CHERRY, icon("Cherry tomatoes"))
    }

    @Test
    fun `the item word wins over describing words`() {
        assertEquals(GroceryIcon.JUICE, icon("Orange juice"))
        assertEquals(GroceryIcon.NUT, icon("Peanut butter"))
        assertEquals(GroceryIcon.ICE_CREAM, icon("Ice cream"))
        assertEquals(GroceryIcon.CARROT, icon("Sweet potatoes"))
        assertEquals(GroceryIcon.NOODLES, icon("Rice"))
        assertEquals(GroceryIcon.MILK, icon("Oat milk"))
    }

    @Test
    fun `unknown items fall back to their aisle`() {
        assertEquals(GroceryIcon.CARROT, icon("Kohlrabi", GroceryCategory.PRODUCE))
        assertEquals(GroceryIcon.BASKET, icon("Birthday candles"))
    }

    @Test
    fun `every starter item has its own icon`() {
        StarterItems.forEach { (name, _) ->
            assertNotNull("$name has no icon", GroceryIcons.specific(name))
        }
    }
}
