package com.choreme.feature.chores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.room.RoomOverview
import com.choreme.core.model.room.everyDays
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.R as CoreUiR
import com.choreme.core.ui.SectionLabel
import com.choreme.core.ui.choreTitle
import com.choreme.core.ui.icon
import java.time.LocalDate

@Composable
internal fun RoomDetail(overview: RoomOverview, state: RoomsUiState, actions: RoomsActions, onBack: () -> Unit, onEdit: () -> Unit) {
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
internal fun dueLabel(task: Chore, today: LocalDate): Pair<String, Boolean> {
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
