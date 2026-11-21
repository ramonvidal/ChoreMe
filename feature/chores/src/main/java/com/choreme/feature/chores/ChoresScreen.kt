package com.choreme.feature.chores

import com.choreme.core.ui.ItemIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.choreme.core.ui.AddButton
import com.choreme.core.ui.offerUndo
import com.choreme.core.chores.ChoreChange
import com.choreme.core.ui.TabHeader
import com.choreme.core.ui.ChoreNames
import com.choreme.core.ui.R as CoreUiR
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.choreme.core.ui.EmptyState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.model.chore.Chore
import com.choreme.core.ui.HeaderLine
import com.choreme.core.ui.SectionLabel
import com.choreme.core.ui.NosButton
import java.time.Instant
import java.time.LocalTime
import com.choreme.core.model.chore.SavedChore
import java.time.LocalDateTime
import kotlinx.coroutines.launch

@Composable
internal fun ChoresScreen(
    onOpenGroceries: () -> Unit,
    openAdd: Boolean = false,
    onOpenAddHandled: () -> Unit = {},
    rooms: RoomsLink = RoomsLink(),
    onOpenRooms: () -> Unit = {},
    viewModel: ChoresViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    AskForNotificationPermission()
    ChoresContent(
        rooms = rooms,
        onOpenRooms = onOpenRooms,
        state = state,
        reminders = reminders,
        onOpenGroceries = onOpenGroceries,
        openAdd = openAdd,
        onOpenAddHandled = onOpenAddHandled,
        actions = ChoresActions(
            claim = { viewModel.claim(it) },
            unclaim = { viewModel.unclaim(it) },
            complete = { viewModel.complete(it) },
            join = { viewModel.join(it) },
            assign = { chore, people -> viewModel.assign(chore, people) },
            completeFor = { chore, people -> viewModel.completeFor(chore, people) },
            approve = { viewModel.approve(it) },
            reject = { viewModel.reject(it) },
            create = { viewModel.create(it) },
            update = { chore, draft -> viewModel.update(chore, draft) },
            delete = { viewModel.delete(it) },
            undoDelete = { viewModel.undoDelete(it) },
            undo = { viewModel.undo(it) },
            removeSaved = { viewModel.removeSaved(it) },
            remind = { chore, time -> viewModel.remind(chore, time) },
            cancelReminder = { viewModel.cancelReminder(it) },
            addToCalendar = { viewModel.addToCalendar(it) },
        ),
    )
}

/** How the rooms are doing, for the link to the Rooms sub-tab on the Chores list. */
internal data class RoomsLink(val hasRooms: Boolean = false, val late: Int = 0)

/** "Rooms: 1 late ›" (red) or "Rooms: all on track ›": the Rooms sub-tab, one tap away. */
@Composable
internal fun RoomsLinkRow(rooms: RoomsLink, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (rooms.late > 0) colors.errorContainer else colors.surfaceVariant)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ItemIcon(CoreUiR.drawable.ic_tab_rooms, Modifier.size(18.dp), tint = if (rooms.late > 0) colors.onErrorContainer else colors.onSurfaceVariant)
        Text(
            if (rooms.late > 0) pluralStringResource(R.plurals.rooms_link_late, rooms.late, rooms.late) else stringResource(R.string.rooms_link_ok),
            style = MaterialTheme.typography.bodyMedium,
            color = if (rooms.late > 0) colors.onErrorContainer else colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(stringResource(R.string.rooms_link_open), style = MaterialTheme.typography.labelLarge, color = if (rooms.late > 0) colors.onErrorContainer else colors.onSurface)
    }
}

/** What the Chores tab can ask for; [ChoresScreen] passes the view model's, tests pass their own. */
internal class ChoresActions(
    val claim: (Chore) -> Unit = {},
    val unclaim: (Chore) -> Unit = {},
    /** Each step that can be undone returns what it changed, for the Undo button. */
    val complete: (Chore) -> ChoreChange? = { null },
    val join: (Chore) -> Unit = {},
    val assign: (Chore, Set<String>) -> Unit = { _, _ -> },
    val completeFor: (Chore, Set<String>) -> ChoreChange? = { _, _ -> null },
    val approve: (Chore) -> ChoreChange? = { null },
    val undo: (ChoreChange) -> Unit = {},
    val reject: (Chore) -> Unit = {},
    /** Returns the chore already on the list with that name, if any. */
    val create: (ChoreDraft) -> Chore? = { null },
    val update: (Chore, ChoreDraft) -> Unit = { _, _ -> },
    /** Deleting waits a few seconds, so [undoDelete] can keep the chore. */
    val delete: (Chore) -> Unit = {},
    val undoDelete: (Chore) -> Unit = {},
    val removeSaved: (SavedChore) -> Unit = {},
    val remind: (Chore, LocalTime) -> LocalDateTime = { _, time -> LocalDateTime.now().with(time) },
    val cancelReminder: (Chore) -> Unit = {},
    val addToCalendar: suspend (Chore) -> CalendarResult? = { null },
)

/** The whole tab, drawn from [state] alone, so it can be screenshotted without Firebase. */
@Composable
internal fun ChoresContent(
    state: ChoresUiState,
    reminders: Map<String, Instant>,
    onOpenGroceries: () -> Unit,
    actions: ChoresActions,
    openAdd: Boolean = false,
    onOpenAddHandled: () -> Unit = {},
    rooms: RoomsLink = RoomsLink(),
    onOpenRooms: () -> Unit = {},
) {
    var cheer by remember { mutableStateOf<Cheer?>(null) }
    val haptics = LocalHapticFeedback.current
    // A small "well done" each time a chore is finished: a tick, the points, a little buzz.
    fun finished(chore: Chore, approved: Boolean = false) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        cheer = Cheer(if (state.pointsOn) chore.points else 0, waiting = !approved && !state.isParent, at = System.nanoTime())
    }
    var editing by remember { mutableStateOf<Chore?>(null) }
    var adding by remember { mutableStateOf(false) }
    // Asked for from Home's Add menu.
    LaunchedEffect(openAdd) { if (openAdd) { adding = true; onOpenAddHandled() } }
    var doneByFor by remember { mutableStateOf<Chore?>(null) }
    var assigning by remember { mutableStateOf<Chore?>(null) }
    var remindFor by remember { mutableStateOf<Chore?>(null) }
    var showLater by rememberSaveable { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // "Done · Undo" for a few seconds after a step, for the wrong tap (often a kid's).
    fun offerUndo(message: Int, chore: Chore, change: ChoreChange) {
        scope.launch { if (snackbar.offerUndo(context, message, chore.title)) actions.undo(change) }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header") {
                ChoresHeader(state, onOpenGroceries, onAdd = { adding = true })
            }
            // The way to the Rooms sub-tab, with how they're doing, right under the header.
            if (rooms.hasRooms) item(key = "rooms-link") { RoomsLinkRow(rooms, onOpenRooms) }
            when {
                state.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                !state.hasChores -> item(key = "empty") { EmptyChores(state.isParent) }
                else -> {
                    val board = state.board
                    val choreCard: @Composable (Chore, Boolean, String?) -> Unit = { chore, mine, roomName ->
                        ChoreCard(
                            chore = chore,
                            state = state,
                            onClaim = { actions.claim(chore) },
                            onUnclaim = { actions.unclaim(chore) },
                            onComplete = {
                                val change = actions.complete(chore)
                                finished(chore)
                                change?.let { offerUndo(R.string.undo_done, chore, it) }
                            },
                            onDoneBy = { doneByFor = chore },
                            onJoin = { actions.join(chore) },
                            onAssign = { assigning = chore },
                            onEdit = { editing = chore },
                            reminder = reminders[chore.id],
                            onRemind = { remindFor = chore },
                            onCalendar = {
                                scope.launch {
                                    when (val result = actions.addToCalendar(chore)) {
                                        is CalendarResult.Added ->
                                            snackbar.showSnackbar(context.getString(R.string.added_to_calendar, result.calendarName))
                                        is CalendarResult.OpenApp -> try {
                                            context.startActivity(result.intent)
                                        } catch (e: ActivityNotFoundException) {
                                            snackbar.showSnackbar(context.getString(R.string.no_calendar_app))
                                        }
                                        null -> Unit
                                    }
                                }
                            },
                            mine = mine,
                            roomName = roomName,
                        )
                    }
                    // What you said you'd do comes first, so it isn't forgotten.
                    // The tasks you took from a room are here too, tagged with the room.
                    if (state.yours.isNotEmpty()) {
                        item(key = "mine-title") { SectionLabel(stringResource(R.string.section_yours)) }
                        items(state.yours, key = { "mine-${it.chore.id}" }) { task -> choreCard(task.chore, true, task.room?.name) }
                    }
                    if (board.awaitingApproval.isNotEmpty()) {
                        item(key = "awaiting-title") { SectionLabel(stringResource(R.string.section_awaiting)) }
                        items(board.awaitingApproval, key = { "awaiting-${it.id}" }) { chore ->
                            AwaitingCard(chore, state, onApprove = {
                                    val change = actions.approve(chore)
                                    finished(chore, approved = true)
                                    change?.let { offerUndo(R.string.undo_approved, chore, it) }
                                }, onReject = { actions.reject(chore) })
                        }
                    }

                    if (board.toDo.isNotEmpty() || state.yours.isEmpty()) {
                        item(key = "todo-title") { SectionLabel(stringResource(R.string.section_to_do)) }
                    }
                    if (board.toDo.isEmpty() && state.yours.isEmpty()) {
                        item(key = "todo-empty") { EmptyState(title = stringResource(R.string.nothing_to_do), body = stringResource(R.string.nothing_to_do_body)) }
                    }
                    items(board.toDo, key = { "todo-${it.id}" }) { chore -> choreCard(chore, false, null) }

                    if (board.upcoming.isNotEmpty()) {
                        item(key = "upcoming-title") { SectionLabel(stringResource(R.string.section_upcoming)) }
                        item(key = "upcoming") {
                            CompactList(board.upcoming, trailing = { dayLabel(it.dueDate, state.today) }, onClick = { if (state.isParent) editing = it })
                        }
                    }

                    // Further out (next month's, say): folded, so it's there without taking room.
                    if (board.later.isNotEmpty()) {
                        item(key = "later-title") {
                            Row(
                                Modifier.fillMaxWidth().clickable { showLater = !showLater },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SectionLabel(stringResource(R.string.section_later, board.later.size), Modifier.weight(1f))
                                Icon(
                                    if (showLater) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (showLater) {
                            item(key = "later") {
                                CompactList(board.later, trailing = { dayLabel(it.dueDate, state.today) }, onClick = { if (state.isParent) editing = it })
                            }
                        }
                    }

                    if (board.doneToday.isNotEmpty()) {
                        item(key = "done-title") { SectionLabel(stringResource(R.string.section_done_today)) }
                        item(key = "done-today") {
                            CompactList(
                                board.doneToday,
                                trailing = {
                                    namesLabel((listOfNotNull(it.lastDoneBy) + it.lastHelpers.sorted()).mapNotNull(state::nameOf))
                                        .ifEmpty { stringResource(R.string.someone) }
                                },
                                onClick = { if (state.isParent) editing = it },
                                done = true,
                            )
                        }
                    }
                }
            }
        }

        // Adding sits where it sits on every tab.
        if (state.me != null) AddButton(stringResource(R.string.add_chore_short), { adding = true }, Modifier.align(Alignment.BottomEnd).padding(16.dp))
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
        CheerBubble(cheer, onGone = { cheer = null }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 152.dp))
    }

    if (adding) {
        ChoreEditorSheet(
            chore = null,
            today = state.today,
            choices = state.choices,
            popular = state.popular,
            canRemoveSaved = state::canRemove,
            onRemoveSaved = actions.removeSaved,
            canSetPoints = state.isParent && state.pointsOn,
            people = if (state.isParent) state.atHome else emptyList(),
            onSave = { draft ->
                adding = false
                actions.create(draft)?.let { existing ->
                    scope.launch { snackbar.showSnackbar(context.getString(R.string.already_on_list, existing.title)) }
                }
            },
            onDelete = null,
            onDismiss = { adding = false },
        )
    }
    editing?.let { chore ->
        ChoreEditorSheet(
            chore = chore,
            today = state.today,
            choices = state.choices,
            onSave = {
                actions.update(chore, it)
                editing = null
            },
            canSetPoints = state.isParent && state.pointsOn,
            people = if (state.isParent) state.atHome else emptyList(),
            onDelete = {
                actions.delete(chore)
                editing = null
                scope.launch { if (snackbar.offerUndo(context, R.string.undo_deleted, chore.title)) actions.undoDelete(chore) }
            },
            onDismiss = { editing = null },
        )
    }
    remindFor?.let { chore ->
        RemindDialog(
            hasReminder = chore.id in reminders,
            onSet = { time ->
                val at = actions.remind(chore, time)
                remindFor = null
                scope.launch { snackbar.showSnackbar(context.getString(R.string.reminder_set, at.format(SHORT_TIME))) }
            },
            onRemove = {
                actions.cancelReminder(chore)
                remindFor = null
            },
            onDismiss = { remindFor = null },
        )
    }
    doneByFor?.let { chore ->
        PeopleDialog(
            title = stringResource(R.string.done_by_title),
            hint = stringResource(R.string.done_by_hint),
            people = state.atHome,
            initial = chore.team,
            allowNone = false,
            onConfirm = { people ->
                val change = actions.completeFor(chore, people)
                doneByFor = null
                change?.let { offerUndo(R.string.undo_done, chore, it) }
            },
            onDismiss = { doneByFor = null },
        )
    }
    assigning?.let { chore ->
        PeopleDialog(
            title = stringResource(R.string.who_does_title),
            hint = stringResource(R.string.who_does_hint),
            people = state.atHome,
            initial = chore.team,
            allowNone = true,
            onConfirm = { people ->
                actions.assign(chore, people)
                assigning = null
            },
            onDismiss = { assigning = null },
        )
    }
}

/** Reminders need the notification permission from Android 13 on; asked once, the first time Today opens. */
@Composable
private fun AskForNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/**
 * The top of the chore list: how many need doing and what is yours (the day's whole summary is on Home), and the one
 * button: add a chore.
 */
@Composable
internal fun ChoresHeader(state: ChoresUiState, onOpenGroceries: () -> Unit, onAdd: () -> Unit) {
    val toDo = state.board.toDo.size
    // Yours counts the room tasks you took too, as the Yours list shows them. Groceries are Casa's to mention.
    val mine = state.yours.size
    val parts = buildList {
        if (mine > 0) add(pluralStringResource(R.plurals.summary_yours, mine, mine))
    }
    TabHeader(
        title = if (toDo + mine == 0) stringResource(R.string.hero_all_done) else pluralStringResource(R.plurals.summary_to_do, toDo, toDo),
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        subtitle = if (parts.isNotEmpty()) ({ HeaderLine(parts.joinToString(" · ")) }) else null,
    )
}

@Composable
private fun EmptyChores(isParent: Boolean) {
    EmptyState(
        title = stringResource(R.string.empty_title),
        body = stringResource(if (isParent) R.string.empty_body_parent else R.string.empty_body_kid),
    )
}
