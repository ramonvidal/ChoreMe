package com.choreme.feature.chores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.room.FREQUENCY_CHOICES
import com.choreme.core.model.room.LAST_DONE_CHOICES
import com.choreme.core.model.room.Room
import com.choreme.core.model.room.RoomType
import com.choreme.core.model.room.everyDays
import com.choreme.core.model.room.ideasFor
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.R as CoreUiR
import com.choreme.core.ui.choreTitle
import com.choreme.core.ui.icon

@Composable
internal fun RoomEditorDialog(
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
internal fun TaskDialog(
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
