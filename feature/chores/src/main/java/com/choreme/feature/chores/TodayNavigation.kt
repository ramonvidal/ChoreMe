package com.choreme.feature.chores

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** This feature's only public entry point; everything else stays internal to the module. */
const val TODAY_ROUTE = "today"

/** [onOpenGroceries] lets the app take people to the grocery list without this feature knowing about it. */
fun NavGraphBuilder.todayScreen(onOpenGroceries: () -> Unit) {
    composable(TODAY_ROUTE) { TodayScreen(onOpenGroceries) }
}
