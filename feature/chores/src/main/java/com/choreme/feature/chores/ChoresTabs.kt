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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.model.room.lateRoomCount
import com.choreme.core.ui.SubTab
import com.choreme.core.ui.SubTabs
import com.choreme.core.ui.R as CoreUiR

private enum class ChoresTab { CHORES, ROOMS }

/** Chores / Rooms under the top edge. */
@Composable
internal fun ChoresSubTabs(selected: Int, lateRooms: Int = 0, onSelect: (Int) -> Unit) {
    SubTabs(
        listOf(
            SubTab(stringResource(R.string.tab_chores), CoreUiR.drawable.ic_tab_chores),
            SubTab(stringResource(R.string.tab_rooms), CoreUiR.drawable.ic_tab_rooms, badge = lateRooms),
        ),
        selected,
        onSelect,
    )
}

/** The Chores tab: the chore list, and the rooms of the house as its second sub-tab. */
@Composable
internal fun ChoresTabs(onOpenGroceries: () -> Unit, addRequested: Boolean, onAddHandled: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(ChoresTab.CHORES) }
    // "Add > Something needs doing" from Home: show the list, where the sheet opens.
    LaunchedEffect(addRequested) { if (addRequested) tab = ChoresTab.CHORES }
    // The same view model the Rooms sub-tab uses, to count the rooms that are late on its tab.
    val roomsModel: RoomsViewModel = hiltViewModel()
    val rooms by roomsModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ChoresSubTabs(tab.ordinal, lateRooms = lateRoomCount(rooms.rooms, rooms.today)) { tab = ChoresTab.entries[it] }
        when (tab) {
            ChoresTab.CHORES -> ChoresScreen(onOpenGroceries, openAdd = addRequested, onOpenAddHandled = onAddHandled)
            ChoresTab.ROOMS -> RoomsScreen(roomsModel)
        }
    }
}
