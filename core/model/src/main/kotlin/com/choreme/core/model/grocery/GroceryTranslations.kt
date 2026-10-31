package com.choreme.core.model.grocery

import java.text.Normalizer

/**
 * Common grocery names in the app's languages. An item's name is saved as text, in whatever
 * language its phone showed ("Milk" from the starter list), so a known one is shown in each
 * phone's language: "Leite" in Baianês, "Milk" in English. Names nobody listed stay as typed.
 */
object GroceryTranslations {
    // English to Brazilian Portuguese.
    private val PAIRS: List<Pair<String, String>> = listOf(
        // The starter list
        "Bananas" to "Bananas", "Apples" to "Maçãs", "Tomatoes" to "Tomates", "Onions" to "Cebolas",
        "Potatoes" to "Batatas", "Lettuce" to "Alface", "Bread" to "Pão", "Chicken" to "Frango",
        "Ground beef" to "Carne moída", "Milk" to "Leite", "Eggs" to "Ovos", "Butter" to "Manteiga",
        "Yogurt" to "Iogurte", "Cheese" to "Queijo", "Pasta" to "Macarrão", "Rice" to "Arroz",
        "Olive oil" to "Azeite", "Coffee" to "Café", "Cereal" to "Cereal", "Water" to "Água",
        "Toilet paper" to "Papel higiênico", "Dish soap" to "Detergente", "Laundry detergent" to "Sabão em pó",
        "Toothpaste" to "Pasta de dente", "Shampoo" to "Xampu",
        // Other everyday things
        "Beans" to "Feijão", "Sugar" to "Açúcar", "Salt" to "Sal", "Flour" to "Farinha de trigo",
        "Cassava flour" to "Farinha de mandioca", "Oil" to "Óleo", "Vinegar" to "Vinagre", "Tea" to "Chá",
        "Oranges" to "Laranjas", "Limes" to "Limões", "Lemons" to "Limões-sicilianos", "Garlic" to "Alho",
        "Carrots" to "Cenouras", "Grapes" to "Uvas", "Watermelon" to "Melancia", "Mango" to "Manga",
        "Pineapple" to "Abacaxi", "Papaya" to "Mamão", "Avocado" to "Abacate", "Corn" to "Milho",
        "Coconut" to "Coco", "Peppers" to "Pimentões", "Cucumber" to "Pepino", "Fish" to "Peixe",
        "Shrimp" to "Camarão", "Ham" to "Presunto", "Sausages" to "Linguiça", "Beef" to "Carne",
        "Cream" to "Creme de leite", "Juice" to "Suco", "Beer" to "Cerveja", "Wine" to "Vinho",
        "Soda" to "Refrigerante", "Cookies" to "Biscoitos", "Chocolate" to "Chocolate", "Ice cream" to "Sorvete",
        "Soap" to "Sabonete", "Paper towels" to "Papel toalha", "Trash bags" to "Saco de lixo",
        "Sponges" to "Esponjas", "Bleach" to "Água sanitária", "Fabric softener" to "Amaciante",
        "Deodorant" to "Desodorante", "Conditioner" to "Condicionador", "Diapers" to "Fraldas",
        "Batteries" to "Pilhas",
    )

    private val toPortuguese = PAIRS.associate { (en, pt) -> key(en) to pt }
    private val toEnglish = PAIRS.associate { (en, pt) -> key(pt) to en }

    /** [name] in [language] ("en" or "pt") when it's a known grocery; otherwise as typed. */
    fun inLanguage(name: String, language: String): String = when (language) {
        "pt" -> toPortuguese[key(name)] ?: name
        else -> toEnglish[key(name)] ?: name
    }

    /** Both spellings, to tell that "Milk" and "Leite" are the same item. */
    fun sameItem(a: String, b: String): Boolean = key(inLanguage(a, "en")) == key(inLanguage(b, "en"))

    private fun key(name: String): String = plainName(GroceryNames.key(name))
}

/** Lowercase without accents, so "Maçãs" and "macas" read the same. */
internal fun plainName(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
