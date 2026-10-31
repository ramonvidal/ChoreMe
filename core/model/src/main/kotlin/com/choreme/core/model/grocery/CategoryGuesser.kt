package com.choreme.core.model.grocery

/** Picks an aisle for a newly typed item, so most people never have to choose one. */
object CategoryGuesser {
    private val keywords: Map<GroceryCategory, List<String>> = mapOf(
        GroceryCategory.PRODUCE to listOf(
            "banana", "maca", "laranja", "limao", "uva", "morango", "melancia", "manga", "abacaxi", "mamao", "abacate", "tomate", "batata", "cebola", "alho", "cenoura", "alface", "pepino", "pimentao", "abobrinha", "brocolis", "couve", "aipim", "mandioca", "coentro", "cheiro-verde", "fruta", "verdura", "legume", "coco",
            "apple", "banana", "orange", "lemon", "lime", "grape", "strawberr", "berr", "melon", "watermelon",
            "pear", "peach", "plum", "kiwi", "mango", "pineapple", "avocado", "tomato", "potato", "onion",
            "garlic", "carrot", "lettuce", "salad", "spinach", "cucumber", "pepper", "zucchini", "eggplant",
            "broccoli", "cauliflower", "cabbage", "celery", "mushroom", "leek", "herb", "parsley", "basil",
            "fruit", "vegetable", "veggie",
        ),
        GroceryCategory.BAKERY to listOf(
            "pao", "bolo", "torrada", "baguete","bread", "baguette", "bagel", "bun", "roll", "croissant", "tortilla", "muffin", "cake", "pastr"),
        GroceryCategory.MEAT_FISH to listOf(
            "frango", "carne", "bife", "porco", "presunto", "bacon", "linguica", "salsicha", "peixe", "camarao", "charque", "calabresa",
            "chicken", "beef", "pork", "ham", "bacon", "sausage", "turkey", "lamb", "steak", "mince", "ground beef",
            "meat", "fish", "salmon", "tuna steak", "cod", "hake", "shrimp", "prawn", "seafood",
        ),
        GroceryCategory.DAIRY_EGGS to listOf(
            "leite", "ovo", "manteiga", "queijo", "iogurte", "requeijao", "creme de leite", "margarina","milk", "egg", "butter", "cheese", "yogurt", "yoghurt", "cream", "kefir"),
        GroceryCategory.PANTRY to listOf(
            "macarrao", "arroz", "feijao", "farinha", "acucar", "sal", "oleo", "azeite", "vinagre", "molho", "cafe", "cha", "aveia", "milho", "fuba", "tapioca", "atum", "sardinha", "lentilha", "tempero", "dende",
            "pasta", "spaghetti", "macaroni", "noodle", "rice", "flour", "sugar", "salt", "oil", "vinegar", "sauce",
            "ketchup", "mayo", "mustard", "honey", "jam", "peanut butter", "cereal", "oat", "granola", "bean",
            "lentil", "chickpea", "tuna", "canned", "can of", "soup", "spice", "coffee", "tea", "cocoa", "stock",
            "broth", "baking",
        ),
        GroceryCategory.FROZEN to listOf(
            "congelado", "sorvete", "gelo","frozen", "ice cream", "pizza", "ice"),
        GroceryCategory.DRINKS to listOf(
            "agua", "suco", "refrigerante", "refri", "cerveja", "vinho", "guarana","water", "juice", "soda", "cola", "beer", "wine", "drink", "lemonade", "sparkling"),
        GroceryCategory.SNACKS to listOf(
            "biscoito", "bolacha", "chocolate", "bala", "doce", "salgadinho", "pipoca", "amendoim","chip", "crisp", "cookie", "biscuit", "chocolate", "candy", "sweet", "snack", "nut", "popcorn", "cracker"),
        GroceryCategory.HOUSEHOLD to listOf(
            "papel higienico", "papel toalha", "detergente", "sabao", "amaciante", "agua sanitaria", "desinfetante", "esponja", "saco de lixo", "pilha", "lampada", "vassoura",
            "toilet paper", "paper towel", "kitchen roll", "napkin", "detergent", "dish soap", "dishwasher",
            "softener", "bleach", "cleaner", "sponge", "trash bag", "bin bag", "garbage bag", "foil", "cling",
            "battery", "batteries", "light bulb",
        ),
        GroceryCategory.PERSONAL_CARE to listOf(
            "xampu", "condicionador", "sabonete", "pasta de dente", "creme dental", "escova de dente", "desodorante", "fralda", "protetor solar", "absorvente",
            "shampoo", "conditioner", "soap", "shower gel", "toothpaste", "toothbrush", "deodorant", "razor",
            "tissue", "diaper", "nappy", "wipes", "sunscreen", "lotion", "floss",
        ),
    )

    private val matcher = KeywordMatcher(keywords)

    fun guess(name: String): GroceryCategory = matcher.match(name) ?: GroceryCategory.OTHER
}
