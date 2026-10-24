package com.choreme.domain.grocery

/** Listed in the order a typical store is walked. */
enum class GroceryCategory {
    PRODUCE,
    BAKERY,
    MEAT_FISH,
    DAIRY_EGGS,
    PANTRY,
    FROZEN,
    DRINKS,
    SNACKS,
    HOUSEHOLD,
    PERSONAL_CARE,
    OTHER,
}

/**
 * Items stay in the family's catalog and move between "at home" and "to buy",
 * so the weekly shop is a matter of tapping what ran out rather than typing a list.
 */
data class GroceryItem(
    val id: String,
    val name: String,
    val category: GroceryCategory,
    val isNeeded: Boolean,
    /** Bought most weeks; "Restock usuals" puts all of these on the list at once. */
    val isStaple: Boolean,
)
