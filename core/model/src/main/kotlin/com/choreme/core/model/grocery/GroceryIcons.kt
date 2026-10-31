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
        GroceryIcon.APPLE to listOf("maca", "pera", "pessego", "kiwi", "manga", "abacaxi", "mamao", "fruta", "apple", "pear", "peach", "nectarine", "kiwi", "mango", "pineapple", "fruit"),
        GroceryIcon.BANANA to listOf("banana", "banana"),
        GroceryIcon.CITRUS to listOf("laranja", "tangerina", "mexerica", "orange", "mandarin", "clementine", "grapefruit"),
        GroceryIcon.LEMON to listOf("limao", "limoes", "lemon", "lime"),
        GroceryIcon.GRAPE to listOf("uva", "grape"),
        GroceryIcon.CHERRY to listOf("cereja", "morango", "amora", "tomate", "cherr", "strawberr", "blueberr", "raspberr", "berr", "tomato"),
        GroceryIcon.MELON to listOf("melancia", "melao", "watermelon", "melon"),
        GroceryIcon.AVOCADO to listOf("abacate", "avocado"),
        GroceryIcon.CARROT to listOf("cenoura", "batata", "cebola", "alho", "beterraba", "aipim", "mandioca", "inhame", "carrot", "potato", "onion", "garlic", "leek", "beet"),
        GroceryIcon.PEPPER to listOf("pimenta", "pimentao", "pepino", "abobrinha", "berinjela", "quiabo", "jilo", "pepper", "cucumber", "zucchini", "eggplant", "aubergine", "chili"),
        GroceryIcon.MUSHROOM to listOf("cogumelo", "mushroom"),
        GroceryIcon.SALAD to listOf("alface", "salada", "espinafre", "repolho", "brocolis", "couve", "verdura", "legume", "lettuce", "salad", "spinach", "cabbage", "broccoli", "cauliflower", "vegetable"),
        GroceryIcon.LEAF to listOf("cheiro-verde", "coentro", "salsa", "manjericao", "hortela", "cebolinha", "herb", "parsley", "basil", "mint", "coriander", "cilantro"),
        // Bakery
        GroceryIcon.BREAD to listOf("pao", "torrada", "bread", "toast", "bagel", "tortilla", "wrap", "bun"),
        GroceryIcon.BAGUETTE to listOf("baguete", "croissant", "baguette", "croissant"),
        GroceryIcon.CAKE to listOf("bolo", "cake", "muffin", "cupcake"),
        // Meat and fish
        GroceryIcon.MEAT to listOf("frango", "carne", "bife", "porco", "presunto", "bacon", "peru", "costela", "charque", "carne-seca", 
            "chicken", "turkey", "beef", "steak", "pork", "lamb", "meat", "mince", "ground beef", "bacon", "ham",
        ),
        GroceryIcon.SAUSAGE to listOf("linguica", "salsicha", "calabresa", "sausage", "hot dog"),
        GroceryIcon.FISH to listOf("peixe", "camarao", "salmao", "bacalhau", "sardinha", "fish", "salmon", "cod", "hake", "tuna steak", "shrimp", "prawn"),
        // Dairy and eggs
        GroceryIcon.MILK to listOf("leite", "iogurte", "creme de leite", "requeijao", "milk", "kefir", "yogurt", "yoghurt", "cream"),
        GroceryIcon.EGG to listOf("ovo", "egg"),
        GroceryIcon.CHEESE to listOf("queijo", "manteiga", "margarina", "cheese", "butter"),
        // Pantry
        GroceryIcon.NOODLES to listOf("macarrao", "arroz", "espaguete", "miojo", "pasta", "spaghetti", "macaroni", "noodle", "rice"),
        GroceryIcon.CEREAL to listOf("cereal", "granola", "aveia", "cereal", "granola", "muesli"),
        GroceryIcon.WHEAT to listOf("farinha", "milho", "fuba", "tapioca", "goma", "flour", "oat", "corn"),
        GroceryIcon.SALT to listOf("sal", "acucar", "tempero", "salt", "spice", "sugar"),
        GroceryIcon.BOTTLE to listOf("azeite", "oleo", "vinagre", "mel", "geleia", "dende", "olive", "oil", "vinegar", "honey", "jam"),
        GroceryIcon.NUT to listOf("feijao", "lentilha", "grao-de-bico", "amendoim", "castanha", "peanut butter", "nut", "bean", "lentil", "chickpea"),
        GroceryIcon.SOUP to listOf("molho", "sopa", "atum", "enlatado", "maionese", "mostarda", "extrato de tomate", "canned", "can of", "soup", "tuna", "sauce", "ketchup", "mayo", "mustard"),
        GroceryIcon.COFFEE to listOf("cafe", "coffee"),
        GroceryIcon.TEA to listOf("cha", "tea"),
        GroceryIcon.CHOCOLATE to listOf("chocolate", "achocolatado", "cacau", "chocolate", "cocoa"),
        // Frozen
        GroceryIcon.ICE_CREAM to listOf("sorvete", "picole", "ice cream"),
        GroceryIcon.PIZZA to listOf("pizza", "pizza"),
        GroceryIcon.FROZEN to listOf("gelo", "congelado", "ice", "frozen"),
        // Drinks
        GroceryIcon.WATER to listOf("agua", "water"),
        GroceryIcon.JUICE to listOf("suco", "refrigerante", "refri", "guarana", "juice", "soda", "cola", "lemonade"),
        GroceryIcon.BEER to listOf("cerveja", "beer"),
        GroceryIcon.WINE to listOf("vinho", "wine"),
        // Snacks
        GroceryIcon.COOKIE to listOf("biscoito", "bolacha", "cookie", "biscuit", "pretzel", "cracker"),
        GroceryIcon.CHIPS to listOf("salgadinho", "batata frita", "pipoca", "chip", "crisp", "popcorn"),
        GroceryIcon.CANDY to listOf("bala", "doce", "brigadeiro", "candy", "sweet"),
        // Household
        GroceryIcon.TOILET_PAPER to listOf("papel higienico", "papel toalha", "guardanapo", "lenco", "toilet paper", "paper towel", "kitchen roll", "napkin", "tissue"),
        GroceryIcon.BUCKET to listOf("esponja", "detergente", "vassoura", "rodo", "sponge", "dish soap", "dishwasher", "broom", "mop"),
        GroceryIcon.LAUNDRY to listOf("sabao em po", "sabao", "amaciante", "detergent", "softener", "laundry"),
        GroceryIcon.SPRAY to listOf("sabonete", "desinfetante", "agua sanitaria", "multiuso", "soap", "cleaner", "bleach"),
        GroceryIcon.TRASH to listOf("saco de lixo", "trash bag", "bin bag", "garbage bag"),
        GroceryIcon.BATTERY to listOf("pilha", "bateria", "battery", "batteries"),
        GroceryIcon.BULB to listOf("lampada", "light bulb", "bulb"),
        // Personal care
        GroceryIcon.DENTAL to listOf("pasta de dente", "creme dental", "escova de dente", "fio dental", "toothpaste", "toothbrush", "floss"),
        GroceryIcon.LOTION to listOf("xampu", "shampoo", "condicionador", "creme", "protetor solar", "desodorante", "hidratante", "shampoo", "conditioner", "shower gel", "lotion", "sunscreen", "deodorant"),
        GroceryIcon.RAZOR to listOf("barbeador", "gilete", "lamina", "razor"),
        GroceryIcon.DIAPER to listOf("fralda", "lenco umedecido", "diaper", "nappy", "wipes"),
        // Pets
        GroceryIcon.PAW to listOf("racao", "areia de gato", "dog food", "cat food", "pet food", "litter"),
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
