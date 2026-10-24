package com.choreme.domain.grocery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroceryListTest {
    private fun item(name: String, needed: Boolean = false, category: GroceryCategory = GroceryCategory.OTHER) =
        GroceryItem(id = name, name = name, category = category, isNeeded = needed, isStaple = false)

    private val catalog = listOf(item("Milk"), item("Eggs", needed = true), item("Almond milk"), item("Mineral water"))

    @Test
    fun `typing an item at home puts it back on the list`() {
        assertEquals(QuickAddResult.MarkNeeded(catalog[0]), quickAdd("  milk ", catalog))
    }

    @Test
    fun `typing an item already on the list changes nothing`() {
        assertEquals(QuickAddResult.AlreadyNeeded(catalog[1]), quickAdd("EGGS", catalog))
    }

    @Test
    fun `new items are capitalized and get a guessed aisle`() {
        assertEquals(QuickAddResult.Create("Peanut butter", GroceryCategory.PANTRY), quickAdd("peanut   butter", catalog))
        assertEquals(QuickAddResult.Blank, quickAdd("   ", catalog))
    }

    @Test
    fun `suggestions prefer the start of the name and skip items already needed`() {
        assertEquals(listOf("Milk", "Almond milk"), suggestions("mil", catalog).map { it.name })
        assertEquals(listOf("Mineral water"), suggestions("ater", catalog).map { it.name })
        assertEquals(emptyList<GroceryItem>(), suggestions("egg", catalog))
    }

    @Test
    fun `groups follow store order`() {
        val groups = groupByCategory(
            listOf(
                item("Soap", category = GroceryCategory.PERSONAL_CARE),
                item("pears", category = GroceryCategory.PRODUCE),
                item("Apples", category = GroceryCategory.PRODUCE),
            ),
        )
        assertEquals(listOf(GroceryCategory.PRODUCE, GroceryCategory.PERSONAL_CARE), groups.map { it.category })
        assertEquals(listOf("Apples", "pears"), groups[0].items.map { it.name })
    }

    @Test
    fun `document ids are stable across spelling variations`() {
        assertEquals("ground-beef", GroceryNames.documentId(" Ground  Beef"))
        assertEquals("café-con-leche", GroceryNames.documentId("Café con leche"))
        assertEquals("a-b", GroceryNames.documentId("a/b"))
        assertNull(GroceryNames.documentId("!!!"))
    }
}
