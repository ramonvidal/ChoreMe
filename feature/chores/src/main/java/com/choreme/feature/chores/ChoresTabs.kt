package com.choreme.feature.chores

import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
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
    // The sub-tabs swipe too; the pager remembers the page across going to another tab and back.
    var savedTab by rememberSaveable { mutableIntStateOf(ChoresTab.CHORES.ordinal) }
    val pager = rememberPagerState(initialPage = savedTab) { ChoresTab.entries.size }
    LaunchedEffect(pager.currentPage) { savedTab = pager.currentPage }
    val scope = rememberCoroutineScope()
    // "Add > Something needs doing" from Home: show the list, where the sheet opens.
    LaunchedEffect(addRequested) { if (addRequested) pager.scrollToPage(ChoresTab.CHORES.ordinal) }
    // The same view model the Rooms sub-tab uses, to count the rooms that are late on its tab.
    val roomsModel: RoomsViewModel = hiltViewModel()
    val rooms by roomsModel.uiState.collectAsStateWithLifecycle()
    val lateRooms = lateRoomCount(rooms.rooms, rooms.today)
    Column(Modifier.fillMaxSize()) {
        ChoresSubTabs(pager.currentPage, lateRooms = lateRooms) { scope.launch { pager.animateScrollToPage(it) } }
        HorizontalPager(pager, Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) { page ->
            when (ChoresTab.entries[page]) {
                ChoresTab.CHORES -> ChoresScreen(
                    onOpenGroceries,
                    openAdd = addRequested,
                    onOpenAddHandled = onAddHandled,
                    rooms = RoomsLink(rooms.rooms.isNotEmpty(), lateRooms),
                    onOpenRooms = { scope.launch { pager.animateScrollToPage(ChoresTab.ROOMS.ordinal) } },
                )
                ChoresTab.ROOMS -> RoomsScreen(roomsModel)
            }
        }
    }
}
