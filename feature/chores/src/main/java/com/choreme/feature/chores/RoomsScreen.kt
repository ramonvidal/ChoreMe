package com.choreme.feature.chores

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.room.Room
import com.choreme.core.model.room.RoomHealth
import com.choreme.core.model.room.RoomOverview
import com.choreme.core.model.room.byHealth
import com.choreme.core.model.room.health
import com.choreme.core.model.room.takenBy
import com.choreme.core.model.room.everyDays
import com.choreme.core.ui.EmptyState
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.choreTitle
import com.choreme.core.ui.icon
import java.time.LocalDate

@Composable
internal fun RoomsScreen(viewModel: RoomsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RoomsContent(
        state = state,
        actions = RoomsActions(
            addRoom = { viewModel.addRoom(it) },
            saveRoom = { viewModel.saveRoom(it) },
            deleteRoom = { viewModel.deleteRoom(it) },
            addTask = { room, title, template, days, ago -> viewModel.addTask(room, title, template, days, ago) },
            setFrequency = { task, days -> viewModel.setFrequency(task, days) },
            deleteTask = { viewModel.deleteTask(it) },
            markDone = { viewModel.markDone(it) },
            claim = { viewModel.claim(it) },
            unclaim = { viewModel.unclaim(it) },
            join = { viewModel.join(it) },
            assign = { task, people -> viewModel.assign(task, people) },
            approve = { viewModel.approve(it) },
            reject = { viewModel.reject(it) },
        ),
    )
}

/** What the Rooms tab can ask for; [RoomsScreen] passes the view model's, tests pass their own. */
internal class RoomsActions(
    val addRoom: (Room) -> String? = { null },
    val saveRoom: (Room) -> Unit = {},
    val deleteRoom: (RoomOverview) -> Unit = {},
    val addTask: (roomId: String, title: String, template: ChoreTemplate, everyDays: Int, lastDoneDaysAgo: Int?) -> Unit = { _, _, _, _, _ -> },
    val setFrequency: (Chore, Int) -> Unit = { _, _ -> },
    val deleteTask: (Chore) -> Unit = {},
    val markDone: (Chore) -> Unit = {},
    val claim: (Chore) -> Unit = {},
    val unclaim: (Chore) -> Unit = {},
    val join: (Chore) -> Unit = {},
    val assign: (Chore, Set<String>) -> Unit = { _, _ -> },
    val approve: (Chore) -> Unit = {},
    val reject: (Chore) -> Unit = {},
)

/** The whole tab, drawn from [state] alone, so it can be screenshotted without Firebase. */
@Composable
internal fun RoomsContent(state: RoomsUiState, actions: RoomsActions, openRoomId: String? = null) {
    var openId by rememberSaveable { mutableStateOf(openRoomId) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val open = state.rooms.firstOrNull { it.room.id == openId }

    if (open != null) {
        BackHandler { openId = null }
        RoomDetail(
            overview = open,
            state = state,
            actions = actions,
            onBack = { openId = null },
            onEdit = { editing = open.room.id },
        )
    } else {
        RoomList(state, onOpen = { openId = it }, onAdd = { adding = true })
    }

    if (adding) {
        RoomEditorDialog(
            room = null,
            onDismiss = { adding = false },
            onSave = { room ->
                adding = false
                actions.addRoom(room)?.let { openId = it }
            },
        )
    }
    val editingRoom = state.rooms.firstOrNull { it.room.id == editing }
    if (editingRoom != null) {
        RoomEditorDialog(
            room = editingRoom.room,
            onDismiss = { editing = null },
            onSave = { actions.saveRoom(it); editing = null },
            onDelete = { actions.deleteRoom(editingRoom); editing = null; openId = null },
            taskCount = editingRoom.tasks.size,
        )
    }
}

@Composable
private fun RoomList(state: RoomsUiState, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.rooms.isEmpty() && !state.loading) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.rooms_empty_title),
                        body = stringResource(if (state.canManage) R.string.rooms_empty_body else R.string.rooms_empty_body_kid),
                    )
                }
            }
            // Rooms that need something come first.
            items(state.rooms.byHealth(state.today), key = { it.room.id }) { overview ->
                RoomCard(overview, state.today, taken = overview.takenBy(state.me?.id), onClick = { onOpen(overview.room.id) })
            }
        }
        if (state.canManage) {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.rooms_add)) },
            )
        }
    }
}

@Composable
private fun RoomCard(overview: RoomOverview, today: LocalDate, taken: List<Chore>, onClick: () -> Unit) {
    val room = overview.room
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemIcon(room.type.icon, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(room.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (overview.tasks.isNotEmpty()) HealthChip(overview.health(today))
            }
            LastDoneLine(overview, today)
            if (taken.isNotEmpty()) {
                Text(
                    stringResource(R.string.room_you_took, taken.map { choreTitle(it.title) }.joinToString(", ")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                )
            }
            val late = overview.lateCount(today)
            if (late > 0) {
                Text(
                    pluralStringResource(R.plurals.tasks_late, late, late),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** How the room is doing at a glance: late (red), due today (yellow) or on track (green). */
@Composable
private fun HealthChip(health: RoomHealth) {
    val colors = MaterialTheme.colorScheme
    val (label, container, content) = when (health) {
        RoomHealth.LATE -> Triple(R.string.room_health_late, colors.errorContainer, colors.onErrorContainer)
        RoomHealth.DUE_TODAY -> Triple(R.string.room_health_today, colors.primaryContainer, colors.onPrimaryContainer)
        RoomHealth.OK -> Triple(R.string.room_health_ok, colors.secondaryContainer, colors.onSecondaryContainer)
    }
    Text(
        stringResource(label),
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = Modifier.background(container, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** "Last done 5 days ago", then the task due first: "Next: Clean bathroom, due in 5 days · every 10 days". */
@Composable
internal fun LastDoneLine(overview: RoomOverview, today: LocalDate) {
    val last = overview.lastDoneOn
    val next = overview.next
    val text = when {
        overview.tasks.isEmpty() -> stringResource(R.string.room_no_tasks_short)
        last == null -> stringResource(R.string.room_never_done)
        else -> stringResource(R.string.room_last_done, agoText(last, today))
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (next != null) {
        val every = next.everyDays?.let { everyText(it) }
        val due = dueLabel(next, today).first.replaceFirstChar { it.lowercase() }
        Text(
            stringResource(R.string.room_next, choreTitle(next.title), if (every != null) "$due · $every" else due),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
