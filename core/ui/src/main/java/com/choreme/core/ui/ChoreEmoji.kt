package com.choreme.core.ui

import com.choreme.core.model.chore.ChoreTemplate

/** Shown wherever a chore appears: the Today tab and the agenda. */
val ChoreTemplate.emoji: String
    get() = when (this) {
        ChoreTemplate.DISHES -> "🍽️"
        ChoreTemplate.LAUNDRY -> "🧺"
        ChoreTemplate.FOLD_CLOTHES -> "👕"
        ChoreTemplate.CLEAN_KITCHEN -> "🧽"
        ChoreTemplate.CLEAN_BATHROOM -> "🛁"
        ChoreTemplate.TRASH -> "🗑️"
        ChoreTemplate.VACUUM -> "🧹"
        ChoreTemplate.MAKE_BEDS -> "🛏️"
        ChoreTemplate.WATER_PLANTS -> "🪴"
        ChoreTemplate.GROCERIES -> "🛒"
        ChoreTemplate.OTHER -> "✨"
    }
