package com.choreme.core.model.grocery

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryGuesserTest {
    private fun guess(name: String) = CategoryGuesser.guess(name)

    @Test
    fun `common items land in the right aisle`() {
        assertEquals(GroceryCategory.DAIRY_EGGS, guess("Eggs"))
        assertEquals(GroceryCategory.PRODUCE, guess("Cherry tomatoes"))
        assertEquals(GroceryCategory.HOUSEHOLD, guess("Toilet paper"))
        assertEquals(GroceryCategory.PERSONAL_CARE, guess("Toothpaste"))
        assertEquals(GroceryCategory.DRINKS, guess("Orange juice"))
    }

    @Test
    fun `the more specific keyword wins`() {
        assertEquals(GroceryCategory.PANTRY, guess("Peanut butter"))
        assertEquals(GroceryCategory.FROZEN, guess("Ice cream"))
        assertEquals(GroceryCategory.PRODUCE, guess("Eggplant"))
    }

    @Test
    fun `keywords match whole words, not fragments`() {
        // "tea" must not match inside "steak".
        assertEquals(GroceryCategory.MEAT_FISH, guess("Steak"))
        assertEquals(GroceryCategory.OTHER, guess("Birthday candles"))
    }
}
