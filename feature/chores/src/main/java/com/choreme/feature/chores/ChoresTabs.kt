package com.choreme.feature.chores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.choreme.core.ui.PillTab

private enum class ChoresTab { CHORES, ROOMS }

/** The Chores tab: the chore list, and the rooms of the house as its second sub-tab. */
@Composable
internal fun ChoresTabs(onOpenGroceries: () -> Unit, addRequested: Boolean, onAddHandled: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(ChoresTab.CHORES) }
    // "Add > Something needs doing" from Home: show the list, where the sheet opens.
    LaunchedEffect(addRequested) { if (addRequested) tab = ChoresTab.CHORES }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillTab(stringResource(R.string.tab_chores), tab == ChoresTab.CHORES, { tab = ChoresTab.CHORES })
            PillTab(stringResource(R.string.tab_rooms), tab == ChoresTab.ROOMS, { tab = ChoresTab.ROOMS })
        }
        when (tab) {
            ChoresTab.CHORES -> TodayScreen(onOpenGroceries, openAdd = addRequested, onOpenAddHandled = onAddHandled)
            ChoresTab.ROOMS -> RoomsScreen()
        }
    }
}
