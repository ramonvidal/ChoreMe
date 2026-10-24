package com.choreme.core.model.grocery

/** An emoji for every item, so the list can be read at a glance. Falls back to the aisle's. */
object GroceryIcons {
    private val items: Map<String, List<String>> = mapOf(
        // Fruit and vegetables
        "🍎" to listOf("apple"), "🍌" to listOf("banana"), "🍊" to listOf("orange", "mandarin", "clementine"),
        "🍋" to listOf("lemon", "lime"), "🍇" to listOf("grape"), "🍓" to listOf("strawberr"),
        "🫐" to listOf("blueberr", "berr"), "🍉" to listOf("watermelon"), "🍈" to listOf("melon"),
        "🍐" to listOf("pear"), "🍑" to listOf("peach", "nectarine"), "🥝" to listOf("kiwi"), "🥭" to listOf("mango"),
        "🍍" to listOf("pineapple"), "🥑" to listOf("avocado"), "🍅" to listOf("tomato"), "🥔" to listOf("potato"),
        "🧅" to listOf("onion"), "🧄" to listOf("garlic"), "🥕" to listOf("carrot"),
        "🥬" to listOf("lettuce", "salad", "spinach", "cabbage"), "🥒" to listOf("cucumber", "zucchini"),
        "🫑" to listOf("pepper"), "🍆" to listOf("eggplant", "aubergine"), "🥦" to listOf("broccoli", "cauliflower"),
        "🍄" to listOf("mushroom"), "🌽" to listOf("corn"), "🌿" to listOf("herb", "parsley", "basil", "mint"),
        // Bakery
        "🍞" to listOf("bread", "toast"), "🥖" to listOf("baguette"), "🥯" to listOf("bagel"),
        "🥐" to listOf("croissant"), "🫓" to listOf("tortilla", "wrap"), "🧁" to listOf("muffin", "cupcake"),
        "🍰" to listOf("cake"),
        // Meat and fish
        "🍗" to listOf("chicken", "turkey"), "🥩" to listOf("beef", "steak", "pork", "lamb", "meat", "mince", "ground beef"),
        "🥓" to listOf("bacon", "ham"), "🌭" to listOf("sausage", "hot dog"), "🐟" to listOf("fish", "salmon", "cod", "hake", "tuna steak"),
        "🦐" to listOf("shrimp", "prawn"),
        // Dairy and eggs
        "🥛" to listOf("milk", "kefir"), "🥚" to listOf("egg"), "🧈" to listOf("butter"), "🧀" to listOf("cheese"),
        "🍶" to listOf("yogurt", "yoghurt", "cream"),
        // Pantry
        "🍝" to listOf("pasta", "spaghetti", "macaroni"), "🍜" to listOf("noodle"), "🍚" to listOf("rice"),
        "🌾" to listOf("flour", "oat"), "🧂" to listOf("salt", "spice"), "🍬" to listOf("sugar"),
        "🫒" to listOf("olive", "oil"), "🍯" to listOf("honey"), "🫙" to listOf("jam"), "🥜" to listOf("peanut butter", "nut"),
        "🥣" to listOf("cereal", "granola", "muesli"), "🫘" to listOf("bean", "lentil", "chickpea"),
        "🥫" to listOf("canned", "can of", "soup", "tuna", "sauce", "ketchup", "mayo", "mustard"),
        "☕" to listOf("coffee"), "🍵" to listOf("tea"), "🍫" to listOf("chocolate", "cocoa"),
        // Frozen
        "🍦" to listOf("ice cream"), "🍕" to listOf("pizza"), "🧊" to listOf("ice", "frozen"),
        // Drinks
        "💧" to listOf("water"), "🧃" to listOf("juice"), "🥤" to listOf("soda", "cola", "lemonade"),
        "🍺" to listOf("beer"), "🍷" to listOf("wine"),
        // Snacks
        "🍪" to listOf("cookie", "biscuit"), "🍿" to listOf("popcorn"), "🥨" to listOf("pretzel", "cracker"),
        "🍟" to listOf("chip", "crisp"), "🍭" to listOf("candy", "sweet"),
        // Household
        "🧻" to listOf("toilet paper", "paper towel", "kitchen roll", "napkin", "tissue"),
        "🧽" to listOf("sponge", "dish soap", "dishwasher"), "🧺" to listOf("detergent", "softener", "laundry"),
        "🧼" to listOf("soap", "cleaner", "bleach"), "🗑️" to listOf("trash bag", "bin bag", "garbage bag"),
        "🔋" to listOf("battery", "batteries"), "💡" to listOf("light bulb", "bulb"), "🧹" to listOf("broom", "mop"),
        // Personal care
        "🪥" to listOf("toothpaste", "toothbrush", "floss"), "🧴" to listOf("shampoo", "conditioner", "shower gel", "lotion", "sunscreen", "deodorant"),
        "🪒" to listOf("razor"), "👶" to listOf("diaper", "nappy", "wipes"),
        // Pets
        "🐾" to listOf("dog food", "cat food", "pet food", "litter"),
    )

    private val matcher = KeywordMatcher(items)

    fun forItem(name: String, category: GroceryCategory): String = specific(name) ?: forCategory(category)

    /** The item's own icon, if its name is one we know. */
    internal fun specific(name: String): String? = matcher.match(name)

    fun forCategory(category: GroceryCategory): String = when (category) {
        GroceryCategory.PRODUCE -> "🥕"
        GroceryCategory.BAKERY -> "🥖"
        GroceryCategory.MEAT_FISH -> "🥩"
        GroceryCategory.DAIRY_EGGS -> "🧀"
        GroceryCategory.PANTRY -> "🥫"
        GroceryCategory.FROZEN -> "🧊"
        GroceryCategory.DRINKS -> "🥤"
        GroceryCategory.SNACKS -> "🍪"
        GroceryCategory.HOUSEHOLD -> "🧹"
        GroceryCategory.PERSONAL_CARE -> "🧴"
        GroceryCategory.OTHER -> "🛒"
    }
}
