package com.choreme.feature.chores

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** This feature's only public entry point; everything else stays internal to the module. */
const val TODAY_ROUTE = "today"

fun NavGraphBuilder.todayScreen() {
    composable(TODAY_ROUTE) { TodayScreen() }
}
