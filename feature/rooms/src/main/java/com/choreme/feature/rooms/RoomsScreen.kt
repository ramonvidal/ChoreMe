package com.choreme.feature.rooms

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.room.FREQUENCY_CHOICES
import com.choreme.core.model.room.LAST_DONE_CHOICES
import com.choreme.core.model.room.Room
import com.choreme.core.model.room.RoomOverview
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
                TabHeader(stringResource(R.string.rooms_title), Modifier.padding(top = 16.dp, bottom = 4.dp)) {
                    if (state.rooms.isNotEmpty()) {
                        val late = state.rooms.sumOf { it.lateCount(state.today) }
                        HeaderLine(
                            if (late > 0) pluralStringResource(R.plurals.rooms_late, late, late)
                            else pluralStringResource(R.plurals.rooms_count, state.rooms.size, state.rooms.size),
                        )
                    }
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
            items(state.rooms, key = { it.room.id }) { overview ->
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

/** The room's own photo, or the standard sketch for its kind. */
@Composable
internal fun RoomPicture(room: Room, modifier: Modifier = Modifier) {
    val bitmap = remember(room.photo) { room.photo?.toBitmap() }
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        RoomSketch(room.type, modifier)
    }
}

@Composable
private fun RoomCard(overview: RoomOverview, today: LocalDate, onClick: () -> Unit) {
    val room = overview.room
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        RoomPicture(room, Modifier.fillMaxWidth().aspectRatio(CARD_RATIO))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemIcon(room.type.icon, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(room.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box {
            RoomPicture(room, Modifier.fillMaxWidth().aspectRatio(DETAIL_RATIO))
            // Round buttons on a solid color, so they stand out from any picture.
            val buttonColors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
            FilledIconButton(onClick = onBack, Modifier.padding(12.dp), colors = buttonColors) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.room_back))
            }
            if (state.canManage) {
                FilledIconButton(onClick = onEdit, Modifier.align(Alignment.TopEnd).padding(12.dp), colors = buttonColors) {
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
                        today = state.today,
                        onDone = { actions.markDone(task) },
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

@Composable
private fun TaskRow(task: Chore, today: LocalDate, onDone: () -> Unit, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ItemIcon(task.template.icon, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(choreTitle(task.title), style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val every = task.everyDays?.let { everyText(it) }
            val last = task.lastDoneOn?.let { stringResource(R.string.room_last_done, agoText(it, today)) } ?: stringResource(R.string.room_never_done)
            Text(
                if (every != null) "$last · $every" else last,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DueText(task, today)
        }
        TextButton(onClick = onDone, enabled = task.isActive) { Text(stringResource(R.string.room_done)) }
    }
}

/** When a task is due ("Due in 3 days", "2 days late") and whether that's late. */
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
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(room?.name.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(room?.type ?: RoomType.BATHROOM) }
    var description by rememberSaveable { mutableStateOf(room?.description.orEmpty()) }
    var photo by remember { mutableStateOf(room?.photo) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var photoFailed by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val read = readRoomPhoto(context, uri)
            photoFailed = read == null
            if (read != null) photo = read
        }
    }
    val preview = Room(id = room?.id.orEmpty(), name = name, type = type, photo = photo)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (room == null) R.string.room_new else R.string.room_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RoomPicture(preview, Modifier.fillMaxWidth().aspectRatio(CARD_RATIO).clip(MaterialTheme.shapes.medium))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text(stringResource(if (photo == null) R.string.room_photo_add else R.string.room_photo_change))
                    }
                    if (photo != null) TextButton(onClick = { photo = null }) { Text(stringResource(R.string.room_photo_remove)) }
                }
                if (photoFailed) {
                    Text(stringResource(R.string.room_photo_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
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
                onClick = { onSave((room ?: Room(id = "", name = "")).copy(name = name.trim(), type = type, description = description.trim(), photo = photo)) },
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

// The sketches are drawn on a 100 x 60 grid.
private const val CARD_RATIO = 100f / 60f
private const val DETAIL_RATIO = 100f / 60f
