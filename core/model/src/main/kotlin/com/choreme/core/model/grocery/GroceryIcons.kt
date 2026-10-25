package com.choreme.core.model.grocery

/** A small picture for a grocery item. `:core:ui` draws each one. */
enum class GroceryIcon {
    APPLE, BANANA, CITRUS, LEMON, GRAPE, CHERRY, MELON, AVOCADO, CARROT, PEPPER, MUSHROOM, SALAD, LEAF,
    BREAD, BAGUETTE, CAKE,
    MEAT, SAUSAGE, FISH,
    MILK, EGG, CHEESE,
    NOODLES, CEREAL, WHEAT, SALT, BOTTLE, NUT, SOUP, COFFEE, TEA, CHOCOLATE,
    ICE_CREAM, PIZZA, FROZEN,
    WATER, JUICE, BEER, WINE,
    COOKIE, CHIPS, CANDY,
    TOILET_PAPER, BUCKET, LAUNDRY, SPRAY, TRASH, BATTERY, BULB,
    DENTAL, LOTION, RAZOR, DIAPER,
    PAW,
    BASKET,
}

/** A picture for every item, so the list can be read at a glance. Falls back to the aisle's. */
object GroceryIcons {
    private val items: Map<GroceryIcon, List<String>> = mapOf(
        // Fruit and vegetables
        GroceryIcon.APPLE to listOf("apple", "pear", "peach", "nectarine", "kiwi", "mango", "pineapple", "fruit"),
        GroceryIcon.BANANA to listOf("banana"),
        GroceryIcon.CITRUS to listOf("orange", "mandarin", "clementine", "grapefruit"),
        GroceryIcon.LEMON to listOf("lemon", "lime"),
        GroceryIcon.GRAPE to listOf("grape"),
        GroceryIcon.CHERRY to listOf("cherr", "strawberr", "blueberr", "raspberr", "berr", "tomato"),
        GroceryIcon.MELON to listOf("watermelon", "melon"),
        GroceryIcon.AVOCADO to listOf("avocado"),
        GroceryIcon.CARROT to listOf("carrot", "potato", "onion", "garlic", "leek", "beet"),
        GroceryIcon.PEPPER to listOf("pepper", "cucumber", "zucchini", "eggplant", "aubergine", "chili"),
        GroceryIcon.MUSHROOM to listOf("mushroom"),
        GroceryIcon.SALAD to listOf("lettuce", "salad", "spinach", "cabbage", "broccoli", "cauliflower", "vegetable"),
        GroceryIcon.LEAF to listOf("herb", "parsley", "basil", "mint", "coriander", "cilantro"),
        // Bakery
        GroceryIcon.BREAD to listOf("bread", "toast", "bagel", "tortilla", "wrap", "bun"),
        GroceryIcon.BAGUETTE to listOf("baguette", "croissant"),
        GroceryIcon.CAKE to listOf("cake", "muffin", "cupcake"),
        // Meat and fish
        GroceryIcon.MEAT to listOf(
            "chicken", "turkey", "beef", "steak", "pork", "lamb", "meat", "mince", "ground beef", "bacon", "ham",
        ),
        GroceryIcon.SAUSAGE to listOf("sausage", "hot dog"),
        GroceryIcon.FISH to listOf("fish", "salmon", "cod", "hake", "tuna steak", "shrimp", "prawn"),
        // Dairy and eggs
        GroceryIcon.MILK to listOf("milk", "kefir", "yogurt", "yoghurt", "cream"),
        GroceryIcon.EGG to listOf("egg"),
        GroceryIcon.CHEESE to listOf("cheese", "butter"),
        // Pantry
        GroceryIcon.NOODLES to listOf("pasta", "spaghetti", "macaroni", "noodle", "rice"),
        GroceryIcon.CEREAL to listOf("cereal", "granola", "muesli"),
        GroceryIcon.WHEAT to listOf("flour", "oat", "corn"),
        GroceryIcon.SALT to listOf("salt", "spice", "sugar"),
        GroceryIcon.BOTTLE to listOf("olive", "oil", "vinegar", "honey", "jam"),
        GroceryIcon.NUT to listOf("peanut butter", "nut", "bean", "lentil", "chickpea"),
        GroceryIcon.SOUP to listOf("canned", "can of", "soup", "tuna", "sauce", "ketchup", "mayo", "mustard"),
        GroceryIcon.COFFEE to listOf("coffee"),
        GroceryIcon.TEA to listOf("tea"),
        GroceryIcon.CHOCOLATE to listOf("chocolate", "cocoa"),
        // Frozen
        GroceryIcon.ICE_CREAM to listOf("ice cream"),
        GroceryIcon.PIZZA to listOf("pizza"),
        GroceryIcon.FROZEN to listOf("ice", "frozen"),
        // Drinks
        GroceryIcon.WATER to listOf("water"),
        GroceryIcon.JUICE to listOf("juice", "soda", "cola", "lemonade"),
        GroceryIcon.BEER to listOf("beer"),
        GroceryIcon.WINE to listOf("wine"),
        // Snacks
        GroceryIcon.COOKIE to listOf("cookie", "biscuit", "pretzel", "cracker"),
        GroceryIcon.CHIPS to listOf("chip", "crisp", "popcorn"),
        GroceryIcon.CANDY to listOf("candy", "sweet"),
        // Household
        GroceryIcon.TOILET_PAPER to listOf("toilet paper", "paper towel", "kitchen roll", "napkin", "tissue"),
        GroceryIcon.BUCKET to listOf("sponge", "dish soap", "dishwasher", "broom", "mop"),
        GroceryIcon.LAUNDRY to listOf("detergent", "softener", "laundry"),
        GroceryIcon.SPRAY to listOf("soap", "cleaner", "bleach"),
        GroceryIcon.TRASH to listOf("trash bag", "bin bag", "garbage bag"),
        GroceryIcon.BATTERY to listOf("battery", "batteries"),
        GroceryIcon.BULB to listOf("light bulb", "bulb"),
        // Personal care
        GroceryIcon.DENTAL to listOf("toothpaste", "toothbrush", "floss"),
        GroceryIcon.LOTION to listOf("shampoo", "conditioner", "shower gel", "lotion", "sunscreen", "deodorant"),
        GroceryIcon.RAZOR to listOf("razor"),
        GroceryIcon.DIAPER to listOf("diaper", "nappy", "wipes"),
        // Pets
        GroceryIcon.PAW to listOf("dog food", "cat food", "pet food", "litter"),
    )

    private val matcher = KeywordMatcher(items)

    fun forItem(name: String, category: GroceryCategory): GroceryIcon = specific(name) ?: forCategory(category)

    /** The item's own icon, if its name is one we know. */
    internal fun specific(name: String): GroceryIcon? = matcher.match(name)

    fun forCategory(category: GroceryCategory): GroceryIcon = when (category) {
        GroceryCategory.PRODUCE -> GroceryIcon.CARROT
        GroceryCategory.BAKERY -> GroceryIcon.BAGUETTE
        GroceryCategory.MEAT_FISH -> GroceryIcon.MEAT
        GroceryCategory.DAIRY_EGGS -> GroceryIcon.CHEESE
        GroceryCategory.PANTRY -> GroceryIcon.SOUP
        GroceryCategory.FROZEN -> GroceryIcon.FROZEN
        GroceryCategory.DRINKS -> GroceryIcon.JUICE
        GroceryCategory.SNACKS -> GroceryIcon.COOKIE
        GroceryCategory.HOUSEHOLD -> GroceryIcon.SPRAY
        GroceryCategory.PERSONAL_CARE -> GroceryIcon.LOTION
        GroceryCategory.OTHER -> GroceryIcon.BASKET
    }
}
