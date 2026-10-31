package com.choreme.core.model.chore

import com.choreme.core.model.NameKeys
import java.text.Normalizer

/** The editor's groups, so the ready-made and saved chores aren't one long list. */
enum class ChoreGroup { KITCHEN, CLEANING, LAUNDRY, BINS, CARE, ERRANDS }

/** Null for [ChoreTemplate.OTHER]: a typed chore that fits no group goes under "Your own". */
val ChoreTemplate.group: ChoreGroup?
    get() = when (this) {
        ChoreTemplate.DISHES, ChoreTemplate.CLEAN_KITCHEN -> ChoreGroup.KITCHEN
        ChoreTemplate.VACUUM, ChoreTemplate.CLEAN_BATHROOM, ChoreTemplate.TIDY_UP, ChoreTemplate.MAKE_BEDS -> ChoreGroup.CLEANING
        ChoreTemplate.LAUNDRY, ChoreTemplate.FOLD_CLOTHES -> ChoreGroup.LAUNDRY
        ChoreTemplate.TRASH, ChoreTemplate.ORGANIC_WASTE, ChoreTemplate.RECYCLING -> ChoreGroup.BINS
        ChoreTemplate.TAKE_MEDICINE, ChoreTemplate.FEED_PETS, ChoreTemplate.WATER_PLANTS -> ChoreGroup.CARE
        ChoreTemplate.GROCERIES -> ChoreGroup.ERRANDS
        ChoreTemplate.OTHER -> null
    }

/** Something to pick when adding a chore: a ready-made one, or one the family typed and saved. */
sealed interface ChoreChoice {
    /** The ready-made chore it is, or is closest to; [ChoreTemplate.OTHER] when nothing fits. */
    val template: ChoreTemplate

    data class BuiltIn(override val template: ChoreTemplate) : ChoreChoice

    data class Saved(val saved: SavedChore) : ChoreChoice {
        override val template: ChoreTemplate
            get() = saved.template.takeIf { it != ChoreTemplate.OTHER } ?: guessTemplate(saved.title) ?: ChoreTemplate.OTHER
    }
}

val ChoreChoice.group: ChoreGroup? get() = template.group

/** Every ready-made chore (but "Something else"), then the saved ones. */
fun choreChoices(saved: List<SavedChore>): List<ChoreChoice> =
    ChoreTemplate.entries.filter { it != ChoreTemplate.OTHER }.map { ChoreChoice.BuiltIn(it) } + saved.map { ChoreChoice.Saved(it) }

/**
 * The ready-made chore a typed name is closest to, from words in English, Portuguese and
 * German ("Lixo Bio" is organic waste, "Tomar remedio Ramon" medicine). Only the first
 * three words count, so "Email the landlord about the bathroom" isn't a cleaning chore.
 */
fun guessTemplate(title: String): ChoreTemplate? {
    val words = plain(title).split(' ').filter { it.isNotEmpty() }.take(3)
    if (words.isEmpty()) return null
    val start = words.joinToString(" ")
    return KEYWORDS.firstOrNull { (_, keys) ->
        keys.any { key -> if (' ' in key) start.contains(key) else words.any { it.startsWith(key) } }
    }?.first
}

/**
 * What the family adds most, for the top of the editor: how often each choice was done, plus
 * one for each time it's on the list. A ready-made chore counts the chores made from it whose
 * name isn't one of the saved ones (those count for the saved one).
 */
fun mostUsed(choices: List<ChoreChoice>, chores: List<Chore>, limit: Int = MOST_USED): List<ChoreChoice> {
    val savedKeys = choices.filterIsInstance<ChoreChoice.Saved>().map { NameKeys.key(it.saved.title) }.toSet()
    fun uses(choice: ChoreChoice): Int = when (choice) {
        is ChoreChoice.Saved -> chores.filter { NameKeys.key(it.title) == NameKeys.key(choice.saved.title) }
        is ChoreChoice.BuiltIn -> chores.filter { it.template == choice.template && NameKeys.key(it.title) !in savedKeys }
    }.sumOf { it.doneCount + 1 }
    return choices.map { it to uses(it) }.filter { it.second > 0 }.sortedByDescending { it.second }.take(limit).map { it.first }
}

/**
 * The choices matching what's being typed: by name ("lixo" finds "Lixo Bio"), or by a word
 * for its kind of chore ("lix" also finds "Take out trash"). [titleOf] names the ready-made
 * ones in the app's language.
 */
fun searchChoices(query: String, choices: List<ChoreChoice>, titleOf: (ChoreChoice) -> String): List<ChoreChoice> {
    val q = plain(query)
    if (q.length < 2) return emptyList()
    // Words for a kind of chore count from three letters, so "ca" doesn't find cats and beds.
    val kinds = if (q.length < 3) emptySet() else KEYWORDS.filter { (_, keys) -> keys.any { it.startsWith(q) || q.startsWith(it) } }.map { it.first }.toSet()
    return choices.filter { plain(titleOf(it)).contains(q) || it.template in kinds }
}

/** Lowercase, without accents or punctuation: "Remédio" and "remedio" are the same word. */
private fun plain(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

// The most specific first: "lixo bio" is organic waste before it's trash.
private val KEYWORDS: List<Pair<ChoreTemplate, List<String>>> = listOf(
    ChoreTemplate.ORGANIC_WASTE to listOf("organic", "compost", "bio", "organico", "lixo bio", "lixo organico", "biomull"),
    ChoreTemplate.RECYCLING to listOf("recycl", "plastic", "glass", "paper", "reciclagem", "reciclavel", "vidro", "papel", "lixo plastico", "gelber sack", "altpapier"),
    ChoreTemplate.TAKE_MEDICINE to listOf("medicine", "medication", "pill", "tablet", "remedio", "medicamento", "comprimido", "medikament", "tablette"),
    ChoreTemplate.FEED_PETS to listOf("feed", "pet", "fish", "dog", "cat", "peixe", "cachorro", "gato", "racao", "alimentar", "futter", "hund", "katze"),
    ChoreTemplate.TIDY_UP to listOf("tidy", "living room", "arrumar", "sala", "organizar", "aufraumen", "wohnzimmer"),
    ChoreTemplate.DISHES to listOf("dish", "louca", "prato", "geschirr", "spulmaschine"),
    ChoreTemplate.FOLD_CLOTHES to listOf("fold", "dobrar", "falten"),
    ChoreTemplate.LAUNDRY to listOf("laundry", "roupa", "wasche", "maquina de lavar"),
    ChoreTemplate.CLEAN_KITCHEN to listOf("kitchen", "cozinha", "kuche"),
    ChoreTemplate.CLEAN_BATHROOM to listOf("bathroom", "toilet", "banheiro", "banho", "badezimmer"),
    ChoreTemplate.VACUUM to listOf("vacuum", "hoover", "aspirar", "aspirador", "staubsaug", "saugen"),
    ChoreTemplate.MAKE_BEDS to listOf("bed", "cama", "bett"),
    ChoreTemplate.WATER_PLANTS to listOf("plant", "water the", "planta", "regar", "pflanze", "giessen"),
    ChoreTemplate.GROCERIES to listOf("grocer", "shopping", "compras", "mercado", "supermercado", "einkauf"),
    ChoreTemplate.TRASH to listOf("trash", "garbage", "rubbish", "bin", "lixo", "mull", "restmull"),
)

const val MOST_USED = 6
