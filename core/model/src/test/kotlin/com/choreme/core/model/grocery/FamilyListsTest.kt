package com.choreme.core.model.grocery

import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyListsTest {
    @Test
    fun `list names are compared loosely, item ids are per list`() {
        val lists = listOf(FamilyList("pharmacy", "Pharmacy", "mom"))
        assertEquals(true, FamilyLists.isDuplicate(" pharmacy", lists))
        assertEquals(false, FamilyLists.isDuplicate("Hardware store", lists))
        assertEquals("toothpaste", FamilyLists.itemDocumentId(null, "Toothpaste"))
        assertEquals("pharmacy--toothpaste", FamilyLists.itemDocumentId("pharmacy", "Toothpaste"))
    }
}
