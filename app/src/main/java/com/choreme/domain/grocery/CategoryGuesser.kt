package com.choreme.domain.grocery

/** Picks an aisle for a newly typed item, so most people never have to choose one. */
object CategoryGuesser {
    private val keywords: Map<GroceryCategory, List<String>> = mapOf(
        GroceryCategory.PRODUCE to listOf(
            "apple", "banana", "orange", "lemon", "lime", "grape", "strawberr", "berr", "melon", "watermelon",
            "pear", "peach", "plum", "kiwi", "mango", "pineapple", "avocado", "tomato", "potato", "onion",
            "garlic", "carrot", "lettuce", "salad", "spinach", "cucumber", "pepper", "zucchini", "eggplant",
            "broccoli", "cauliflower", "cabbage", "celery", "mushroom", "leek", "herb", "parsley", "basil",
            "fruit", "vegetable", "veggie",
        ),
        GroceryCategory.BAKERY to listOf("bread", "baguette", "bagel", "bun", "roll", "croissant", "tortilla", "muffin", "cake", "pastr"),
        GroceryCategory.MEAT_FISH to listOf(
            "chicken", "beef", "pork", "ham", "bacon", "sausage", "turkey", "lamb", "steak", "mince", "ground beef",
            "meat", "fish", "salmon", "tuna steak", "cod", "hake", "shrimp", "prawn", "seafood",
        ),
        GroceryCategory.DAIRY_EGGS to listOf("milk", "egg", "butter", "cheese", "yogurt", "yoghurt", "cream", "kefir"),
        GroceryCategory.PANTRY to listOf(
            "pasta", "spaghetti", "macaroni", "noodle", "rice", "flour", "sugar", "salt", "oil", "vinegar", "sauce",
            "ketchup", "mayo", "mustard", "honey", "jam", "peanut butter", "cereal", "oat", "granola", "bean",
            "lentil", "chickpea", "tuna", "canned", "can of", "soup", "spice", "coffee", "tea", "cocoa", "stock",
            "broth", "baking",
        ),
        GroceryCategory.FROZEN to listOf("frozen", "ice cream", "pizza", "ice"),
        GroceryCategory.DRINKS to listOf("water", "juice", "soda", "cola", "beer", "wine", "drink", "lemonade", "sparkling"),
        GroceryCategory.SNACKS to listOf("chip", "crisp", "cookie", "biscuit", "chocolate", "candy", "sweet", "snack", "nut", "popcorn", "cracker"),
        GroceryCategory.HOUSEHOLD to listOf(
            "toilet paper", "paper towel", "kitchen roll", "napkin", "detergent", "dish soap", "dishwasher",
            "softener", "bleach", "cleaner", "sponge", "trash bag", "bin bag", "garbage bag", "foil", "cling",
            "battery", "batteries", "light bulb",
        ),
        GroceryCategory.PERSONAL_CARE to listOf(
            "shampoo", "conditioner", "soap", "shower gel", "toothpaste", "toothbrush", "deodorant", "razor",
            "tissue", "diaper", "nappy", "wipes", "sunscreen", "lotion", "floss",
        ),
    )

    private val patterns: List<Pair<Regex, GroceryCategory>> = keywords
        .flatMap { (category, words) -> words.map { Regex("\\b${Regex.escape(it)}") to category } }

    /**
     * The last word usually names the item ("orange juice" is a drink), so the match
     * ending latest wins; on a tie the longer keyword wins ("peanut butter" over "butter").
     */
    fun guess(name: String): GroceryCategory {
        val lower = name.lowercase()
        return patterns
            .mapNotNull { (pattern, category) -> pattern.find(lower)?.let { Triple(it.range.last, it.value.length, category) } }
            .maxWithOrNull(compareBy({ it.first }, { it.second }))
            ?.third ?: GroceryCategory.OTHER
    }
}
