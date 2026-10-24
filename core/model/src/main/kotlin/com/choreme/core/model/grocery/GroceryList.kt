package com.choreme.core.model.grocery

object GroceryNames {
    /** Two names refer to the same item if they only differ in case or spacing. */
    fun key(name: String): String = name.trim().lowercase().replace(Regex("\\s+"), " ")

    /**
     * Document id derived from the name, so two phones adding "Milk" while offline end
     * up with one item instead of two. Null when the name has no usable characters.
     */
    fun documentId(name: String): String? =
        key(name).map { if (it.isLetterOrDigit()) it else '-' }.joinToString("")
            .replace(Regex("-+"), "-").trim('-').take(100).ifEmpty { null }
}

sealed interface QuickAddResult {
    data object Blank : QuickAddResult
    data class AlreadyNeeded(val item: GroceryItem) : QuickAddResult
    data class MarkNeeded(val item: GroceryItem) : QuickAddResult
    data class Create(val name: String, val category: GroceryCategory) : QuickAddResult
}

/** Typing a name you already have puts it back on the list instead of adding a duplicate. */
fun quickAdd(input: String, catalog: List<GroceryItem>): QuickAddResult {
    val name = input.trim().replace(Regex("\\s+"), " ")
    if (name.isEmpty()) return QuickAddResult.Blank
    val existing = catalog.firstOrNull { GroceryNames.key(it.name) == GroceryNames.key(name) }
    return when {
        existing == null -> QuickAddResult.Create(name.replaceFirstChar { it.uppercase() }, CategoryGuesser.guess(name))
        existing.isNeeded -> QuickAddResult.AlreadyNeeded(existing)
        else -> QuickAddResult.MarkNeeded(existing)
    }
}

/** Items at home that match what's being typed, best matches (start of a word) first. */
fun suggestions(query: String, catalog: List<GroceryItem>, limit: Int = 6): List<GroceryItem> {
    val key = GroceryNames.key(query)
    if (key.isEmpty()) return emptyList()
    return catalog
        .filter { !it.isNeeded }
        .mapNotNull { item ->
            val name = GroceryNames.key(item.name)
            when {
                name.startsWith(key) -> 0 to item
                name.split(' ').any { it.startsWith(key) } -> 1 to item
                key in name -> 2 to item
                else -> null
            }
        }
        .sortedWith(compareBy({ it.first }, { it.second.name.lowercase() }))
        .take(limit)
        .map { it.second }
}

data class CategoryGroup(val category: GroceryCategory, val items: List<GroceryItem>)

/** Groups in store order, items alphabetical within each aisle. */
fun groupByCategory(items: List<GroceryItem>): List<CategoryGroup> =
    items.groupBy { it.category }
        .toSortedMap(compareBy { it.ordinal })
        .map { (category, inCategory) -> CategoryGroup(category, inCategory.sortedBy { it.name.lowercase() }) }

/** A first catalog for new families, so the list is useful from the first tap. */
val StarterItems: List<Pair<String, GroceryCategory>> = listOf(
    "Bananas" to GroceryCategory.PRODUCE,
    "Apples" to GroceryCategory.PRODUCE,
    "Tomatoes" to GroceryCategory.PRODUCE,
    "Onions" to GroceryCategory.PRODUCE,
    "Potatoes" to GroceryCategory.PRODUCE,
    "Lettuce" to GroceryCategory.PRODUCE,
    "Bread" to GroceryCategory.BAKERY,
    "Chicken" to GroceryCategory.MEAT_FISH,
    "Ground beef" to GroceryCategory.MEAT_FISH,
    "Milk" to GroceryCategory.DAIRY_EGGS,
    "Eggs" to GroceryCategory.DAIRY_EGGS,
    "Butter" to GroceryCategory.DAIRY_EGGS,
    "Yogurt" to GroceryCategory.DAIRY_EGGS,
    "Cheese" to GroceryCategory.DAIRY_EGGS,
    "Pasta" to GroceryCategory.PANTRY,
    "Rice" to GroceryCategory.PANTRY,
    "Olive oil" to GroceryCategory.PANTRY,
    "Coffee" to GroceryCategory.PANTRY,
    "Cereal" to GroceryCategory.PANTRY,
    "Water" to GroceryCategory.DRINKS,
    "Toilet paper" to GroceryCategory.HOUSEHOLD,
    "Dish soap" to GroceryCategory.HOUSEHOLD,
    "Laundry detergent" to GroceryCategory.HOUSEHOLD,
    "Toothpaste" to GroceryCategory.PERSONAL_CARE,
    "Shampoo" to GroceryCategory.PERSONAL_CARE,
)
