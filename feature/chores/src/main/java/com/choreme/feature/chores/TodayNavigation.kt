package com.choreme.feature.chores

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** This feature's only public entry point; everything else stays internal to the module. */
const val TODAY_ROUTE = "today"

/** Set to true on this tab's back stack entry to open its "something needs doing" sheet (Home's Add menu does). */
const val ADD_CHORE_KEY = "add-chore"

/** [onOpenGroceries] lets the app take people to the grocery list without this feature knowing about it. */
fun NavGraphBuilder.todayScreen(onOpenGroceries: () -> Unit) {
    composable(TODAY_ROUTE) { entry ->
        val add by entry.savedStateHandle.getStateFlow(ADD_CHORE_KEY, false).collectAsStateWithLifecycle()
        ChoresTabs(onOpenGroceries, addRequested = add, onAddHandled = { entry.savedStateHandle[ADD_CHORE_KEY] = false })
    }
}
