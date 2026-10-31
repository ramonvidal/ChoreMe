package com.choreme.core.model.grocery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroceryTranslationsTest {
    @Test
    fun `known groceries show in the phone's language, others as typed`() {
        assertEquals("Leite", GroceryTranslations.inLanguage("Milk", "pt"))
        assertEquals("Papel higiênico", GroceryTranslations.inLanguage("toilet paper", "pt"))
        assertEquals("Milk", GroceryTranslations.inLanguage("leite", "en"))
        assertEquals("Apples", GroceryTranslations.inLanguage("Macas", "en")) // without the accents
        assertEquals("Farinha da vovó", GroceryTranslations.inLanguage("Farinha da vovó", "en"))
        assertTrue(GroceryTranslations.sameItem("Eggs", "Ovos"))
    }

    @Test
    fun `Portuguese names get their picture and aisle too`() {
        assertEquals(GroceryIcon.MILK, GroceryIcons.forItem("Leite", GroceryCategory.OTHER))
        assertEquals(GroceryIcon.NUT, GroceryIcons.forItem("Feijão preto", GroceryCategory.OTHER))
        assertEquals(GroceryCategory.PANTRY, CategoryGuesser.guess("feijao"))
        assertEquals(GroceryCategory.MEAT_FISH, CategoryGuesser.guess("Linguiça calabresa"))
        assertEquals(GroceryCategory.PRODUCE, CategoryGuesser.guess("Salada"))
        assertEquals(GroceryCategory.DAIRY_EGGS, CategoryGuesser.guess("Creme de leite"))
        // English still works as before.
        assertEquals(GroceryCategory.DRINKS, CategoryGuesser.guess("orange juice"))
    }

    @Test
    fun `typing the other language's name finds the family's item`() {
        val milk = GroceryItem(id = "milk", name = "Milk", category = GroceryCategory.DAIRY_EGGS, isNeeded = false, isStaple = false)
        assertEquals(QuickAddResult.MarkNeeded(milk), quickAdd("leite", listOf(milk)))
        assertEquals(listOf(milk), suggestions("lei", listOf(milk)))
    }
}
