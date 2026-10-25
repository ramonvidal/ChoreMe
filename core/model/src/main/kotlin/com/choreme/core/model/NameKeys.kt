package com.choreme.core.model

/** Names typed by people, compared and stored the same way everywhere (groceries, saved chores). */
object NameKeys {
    /** Two names refer to the same thing if they only differ in case or spacing. */
    fun key(name: String): String = name.trim().lowercase().replace(Regex("\\s+"), " ")

    /**
     * A document id derived from the name, so two phones adding "Milk" while offline end up
     * with one entry instead of two. Null when the name has no usable characters.
     */
    fun documentId(name: String): String? =
        key(name).map { if (it.isLetterOrDigit()) it else '-' }.joinToString("")
            .replace(Regex("-+"), "-").trim('-').take(100).ifEmpty { null }
}
