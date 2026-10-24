package com.choreme.feature.family

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** This feature's only public entry point; everything else stays internal to the module. */
const val FAMILY_ROUTE = "family"

fun NavGraphBuilder.familyScreen() {
    composable(FAMILY_ROUTE) { FamilyScreen() }
}
