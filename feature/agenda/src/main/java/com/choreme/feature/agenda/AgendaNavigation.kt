package com.choreme.feature.agenda

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** This feature's only public entry point; everything else stays internal to the module. */
const val AGENDA_ROUTE = "agenda"

/** Set to true on this tab's back stack entry to open its "New event" dialog (Casa's Add menu does it through :app). */
const val ADD_EVENT_KEY = "add_event"

/** The family calendar: the phone's calendars the family shares, as a list or a month, and adding events with alerts. */
fun NavGraphBuilder.agendaScreen() {
    composable(AGENDA_ROUTE) { entry ->
        var addRequest by rememberSaveable { mutableIntStateOf(0) }
        val asked by entry.savedStateHandle.getStateFlow(ADD_EVENT_KEY, false).collectAsStateWithLifecycle()
        LaunchedEffect(asked) {
            if (asked) {
                addRequest++
                entry.savedStateHandle[ADD_EVENT_KEY] = false
            }
        }
        AgendaScreen(addRequest = addRequest)
    }
}
