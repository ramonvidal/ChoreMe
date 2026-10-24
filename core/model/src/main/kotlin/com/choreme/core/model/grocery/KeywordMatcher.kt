package com.choreme.core.model.grocery

/**
 * Finds which keyword best describes an item name. The last word usually names the item
 * ("orange juice" is juice), so the match ending latest wins; on a tie the longer keyword
 * wins ("peanut butter" over "butter"). Keywords match from the start of a word, so
 * "tomato" matches "tomatoes" but "tea" doesn't match "steak".
 */
internal class KeywordMatcher<T>(keywords: Map<T, List<String>>) {
    private val patterns: List<Pair<Regex, T>> = keywords
        .flatMap { (value, words) -> words.map { Regex("\\b${Regex.escape(it)}") to value } }

    fun match(name: String): T? {
        val lower = name.lowercase()
        return patterns
            .mapNotNull { (pattern, value) -> pattern.find(lower)?.let { Triple(it.range.last, it.value.length, value) } }
            .maxWithOrNull(compareBy({ it.first }, { it.second }))
            ?.third
    }
}
