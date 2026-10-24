package com.choreme.feature.agenda

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** This feature's only public entry point; everything else stays internal to the module. */
const val AGENDA_ROUTE = "agenda"

fun NavGraphBuilder.agendaScreen() {
    composable(AGENDA_ROUTE) { AgendaScreen() }
}
