package com.choreme.feature.chores

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.choreme.core.model.Role
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.ui.NosButton
import com.choreme.core.ui.NosButtonStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.room.FREQUENCY_CHOICES
import com.choreme.core.model.room.LAST_DONE_CHOICES
import com.choreme.core.model.room.Room
import com.choreme.core.model.room.RoomHealth
import com.choreme.core.model.room.RoomOverview
import com.choreme.core.model.room.byHealth
import com.choreme.core.model.room.health
import com.choreme.core.model.room.RoomType
import com.choreme.core.model.room.everyDays
import com.choreme.core.model.room.ideasFor
import com.choreme.core.ui.EmptyState
import com.choreme.core.ui.HeaderLine
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.R as CoreUiR
import com.choreme.core.ui.SectionLabel
import com.choreme.core.ui.TabHeader
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
            item(key = "header") {
                // The sub-tab already says "Rooms": only how many there are, or how many tasks are late.
                if (state.rooms.isNotEmpty()) {
                    val late = state.rooms.sumOf { it.lateCount(state.today) }
                    HeaderLine(
                        if (late > 0) pluralStringResource(R.plurals.rooms_late, late, late)
                        else pluralStringResource(R.plurals.rooms_count, state.rooms.size, state.rooms.size),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
            }
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
                RoomCard(overview, state.today, onClick = { onOpen(overview.room.id) })
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
private fun RoomCard(overview: RoomOverview, today: LocalDate, onClick: () -> Unit) {
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
private fun LastDoneLine(overview: RoomOverview, today: LocalDate) {
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

@Composable
private fun RoomDetail(overview: RoomOverview, state: RoomsUiState, actions: RoomsActions, onBack: () -> Unit, onEdit: () -> Unit) {
    val room = overview.room
    var addingTask by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var assigningId by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.room_back))
            }
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            if (state.canManage) {
                androidx.compose.material3.IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.room_edit))
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemIcon(room.type.icon, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(room.name, style = MaterialTheme.typography.headlineMedium)
            }
            LastDoneLine(overview, state.today)
            if (room.description.isNotBlank()) {
                Text(room.description, style = MaterialTheme.typography.bodyLarge)
            }
            SectionLabel(stringResource(R.string.room_tasks))
            if (overview.tasks.isEmpty()) {
                Text(
                    stringResource(if (state.canManage) R.string.room_no_tasks else R.string.room_no_tasks_kid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth(),
            ) {
                overview.tasks.forEachIndexed { index, task ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    TaskRow(
                        task = task,
                        state = state,
                        actions = actions,
                        onAssign = { assigningId = task.id },
                        onClick = if (state.canManage) ({ editingTaskId = task.id }) else null,
                    )
                }
            }
            if (state.canManage) {
                OutlinedButton(onClick = { addingTask = true }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(18.dp))
                    Text(stringResource(R.string.room_add_task), Modifier.padding(start = 8.dp))
                }
            }
        }
    }
    if (addingTask) {
        TaskDialog(
            room = room,
            task = null,
            onDismiss = { addingTask = false },
            onAdd = { title, template, days, ago ->
                actions.addTask(room.id, title, template, days, ago)
                addingTask = false
            },
        )
    }
    val assigning = overview.tasks.firstOrNull { it.id == assigningId }
    if (assigning != null) {
        PeopleDialog(
            title = stringResource(R.string.who_does_title),
            hint = stringResource(R.string.who_does_hint),
            people = state.atHome,
            initial = assigning.team,
            allowNone = true,
            onConfirm = { people ->
                actions.assign(assigning, people)
                assigningId = null
            },
            onDismiss = { assigningId = null },
        )
    }
    val editingTask = overview.tasks.firstOrNull { it.id == editingTaskId }
    if (editingTask != null) {
        TaskDialog(
            room = room,
            task = editingTask,
            onDismiss = { editingTaskId = null },
            onAdd = { _, _, days, _ ->
                actions.setFrequency(editingTask, days)
                editingTaskId = null
            },
            onDelete = { actions.deleteTask(editingTask); editingTaskId = null },
        )
    }
}

/**
 * One task of a room: what it is, when it was last done and is next due, who is on it, and what this person can do
 * with it, the same as on the Chores list: take it ("I'll do it"), join, let go, say it's done (a kid's waits for a
 * parent's OK), and for parents give it to someone and approve.
 */
@Composable
private fun TaskRow(task: Chore, state: RoomsUiState, actions: RoomsActions, onAssign: () -> Unit, onClick: (() -> Unit)?) {
    val me = state.me
    val isParent = me?.role == Role.PARENT
    val mine = me != null && me.id in task.team
    val waiting = task.status == ChoreStatus.PENDING_APPROVAL
    val takenByOthers = task.status == ChoreStatus.CLAIMED && !mine
    val others = (listOfNotNull(task.claimedBy) + task.helpers.sorted()).filter { it != me?.id }.mapNotNull { state.members[it]?.name }
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ItemIcon(task.template.icon, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(choreTitle(task.title), style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val every = task.everyDays?.let { everyText(it) }
            val last = task.lastDoneOn?.let { stringResource(R.string.room_last_done, agoText(it, today = state.today)) } ?: stringResource(R.string.room_never_done)
            Text(
                if (every != null) "$last · $every" else last,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DueText(task, state.today)
            val who = when {
                waiting -> task.completedBy?.let { state.members[it]?.name }?.let { stringResource(R.string.finished_by, it) } ?: stringResource(R.string.waiting_for_parent)
                mine && others.isEmpty() -> stringResource(R.string.you_are_on_it)
                mine -> stringResource(R.string.you_and_on_it, others.joinToString(", "))
                takenByOthers && others.size == 1 -> stringResource(R.string.on_it, others.first())
                takenByOthers -> stringResource(R.string.on_it_many, others.joinToString(", "))
                else -> null
            }
            who?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
        }
        when {
            waiting -> if (isParent) NosButton(stringResource(CoreUiR.string.approve), { actions.approve(task) }, style = NosButtonStyle.Tonal)
            task.status == ChoreStatus.OPEN -> NosButton(stringResource(R.string.grab), { actions.claim(task) }, style = NosButtonStyle.Tonal)
            mine -> NosButton(stringResource(R.string.room_done), { actions.markDone(task) }, style = NosButtonStyle.Tonal)
            takenByOthers -> NosButton(stringResource(R.string.join), { actions.join(task) }, style = NosButtonStyle.Tonal)
        }
        val canFinish = task.isActive && !mine
        if (canFinish || (mine && task.isActive) || (isParent && (task.isActive || waiting))) {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.swipe_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (canFinish) DropdownMenuItem(text = { Text(stringResource(R.string.room_done)) }, onClick = { menu = false; actions.markDone(task) })
                    if (mine && task.isActive) DropdownMenuItem(text = { Text(stringResource(R.string.let_go)) }, onClick = { menu = false; actions.unclaim(task) })
                    if (isParent && task.isActive) DropdownMenuItem(text = { Text(stringResource(R.string.who_does)) }, onClick = { menu = false; onAssign() })
                    if (isParent && waiting) DropdownMenuItem(text = { Text(stringResource(R.string.not_yet)) }, onClick = { menu = false; actions.reject(task) })
                }
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun dueLabel(task: Chore, today: LocalDate): Pair<String, Boolean> {
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, task.dueDate).toInt()
    return when {
        days < 0 -> pluralStringResource(R.plurals.task_late_by, -days, -days) to true
        days == 0 -> stringResource(R.string.task_due_today) to false
        else -> pluralStringResource(R.plurals.task_due_in, days, days) to false
    }
}

@Composable
private fun DueText(task: Chore, today: LocalDate) {
    if (!task.isActive) {
        Text(stringResource(R.string.room_waiting_approval), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val (text, late) = dueLabel(task, today)
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (late) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RoomEditorDialog(
    room: Room?,
    onDismiss: () -> Unit,
    onSave: (Room) -> Unit,
    onDelete: (() -> Unit)? = null,
    taskCount: Int = 0,
) {
    var name by rememberSaveable { mutableStateOf(room?.name.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(room?.type ?: RoomType.BATHROOM) }
    var description by rememberSaveable { mutableStateOf(room?.description.orEmpty()) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (room == null) R.string.room_new else R.string.room_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(Room.MAX_NAME) },
                    label = { Text(stringResource(R.string.room_name)) },
                    placeholder = { Text(stringResource(R.string.room_name_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.room_type), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoomType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(stringResource(option.label)) },
                            leadingIcon = { ItemIcon(option.icon, Modifier.size(18.dp)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(Room.MAX_DESCRIPTION) },
                    label = { Text(stringResource(R.string.room_description)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.room_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onSave((room ?: Room(id = "", name = "")).copy(name = name.trim(), type = type, description = description.trim())) },
            ) { Text(stringResource(CoreUiR.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } },
    )
    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.room_delete_title, room?.name.orEmpty())) },
            text = { Text(pluralStringResource(R.plurals.room_delete_body, taskCount, taskCount)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.room_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(CoreUiR.string.cancel)) } },
        )
    }
}

/** Adds a task to the room (a suggestion or a typed name, how often, when it was last done), or changes how often one repeats. */
@Composable
private fun TaskDialog(
    room: Room,
    task: Chore?,
    onDismiss: () -> Unit,
    onAdd: (title: String, template: ChoreTemplate, everyDays: Int, lastDoneDaysAgo: Int?) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf(task?.title.orEmpty()) }
    var template by rememberSaveable { mutableStateOf(task?.template ?: ChoreTemplate.TIDY_UP) }
    var days by rememberSaveable { mutableStateOf(task?.everyDays ?: 7) }
    var lastIndex by rememberSaveable { mutableStateOf(LAST_DONE_CHOICES.lastIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task == null) stringResource(R.string.room_add_task) else choreTitle(task.title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (task == null) {
                    Text(stringResource(R.string.task_ideas), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ideasFor(room.type).forEach { idea ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    title = context.getString(idea.label)
                                    template = idea.template
                                    days = idea.everyDays
                                },
                                label = { Text(stringResource(idea.label)) },
                                leadingIcon = { ItemIcon(idea.template.icon, Modifier.size(18.dp)) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(60) },
                        label = { Text(stringResource(R.string.task_name)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(stringResource(R.string.task_repeat), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (FREQUENCY_CHOICES + listOfNotNull(days.takeIf { it !in FREQUENCY_CHOICES })).sorted().forEach { option ->
                        FilterChip(selected = days == option, onClick = { days = option }, label = { Text(everyChoice(option)) })
                    }
                }
                if (task == null) {
                    Text(stringResource(R.string.task_last_done), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LAST_DONE_CHOICES.forEachIndexed { index, ago ->
                            FilterChip(
                                selected = lastIndex == index,
                                onClick = { lastIndex = index },
                                label = {
                                    Text(
                                        when (ago) {
                                            null -> stringResource(R.string.task_last_unknown)
                                            0 -> stringResource(R.string.ago_today)
                                            1 -> stringResource(R.string.ago_yesterday)
                                            else -> pluralStringResource(R.plurals.ago_days, ago, ago)
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.task_remove), color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = { onAdd(title, template, days, LAST_DONE_CHOICES[lastIndex]) },
            ) { Text(stringResource(if (task == null) CoreUiR.string.add else CoreUiR.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } },
    )
}

