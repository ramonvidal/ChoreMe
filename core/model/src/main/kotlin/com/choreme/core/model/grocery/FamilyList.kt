package com.choreme.core.model.grocery

/**
 * A shopping list besides groceries: pharmacy, hardware store, packing for a trip. Items work
 * like grocery items; the grocery list itself has no document (items without a list are on it).
 */
data class FamilyList(val id: String, val name: String, val createdBy: String)

object FamilyLists {
    const val MAX_NAME = 40

    /** Same rule as item names, so "Pharmacy" and "pharmacy " are one list. */
    fun isDuplicate(name: String, lists: List<FamilyList>): Boolean =
        lists.any { GroceryNames.key(it.name) == GroceryNames.key(name) }

    /** Item ids come from their names; on another list they get the list's id in front, so names can repeat across lists. */
    fun itemDocumentId(listId: String?, name: String): String? {
        val base = GroceryNames.documentId(name) ?: return null
        return if (listId == null) base else "$listId--$base"
    }
}
