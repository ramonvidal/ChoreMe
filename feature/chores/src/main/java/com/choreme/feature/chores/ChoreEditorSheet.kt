package com.choreme.feature.chores

import com.choreme.core.ui.R as CoreUiR
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.icon
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.Recurrence
import com.choreme.core.model.chore.SavedChore
import java.time.DayOfWeek
import java.time.LocalDate

private enum class RepeatKind { ONCE, EVERY_DAYS, WEEKLY }

/**
 * Reports something that needs doing ([chore] null) or edits a chore. Picking a template
 * fills in the title. Repeating is an extra, folded away under "Repeat".
 */
@Composable
internal fun ChoreEditorSheet(
    chore: Chore?,
    today: LocalDate,
    onSave: (ChoreDraft) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    savedChores: List<SavedChore> = emptyList(),
    canRemoveSaved: (SavedChore) -> Boolean = { false },
    onRemoveSaved: (SavedChore) -> Unit = {},
    /** Parents choose what a chore is worth; kids' reports are worth [Chore.POINTS] first. */
    canSetPoints: Boolean = false,
) {
    var removing by remember { mutableStateOf<SavedChore?>(null) }
    val initialRecurrence = chore?.recurrence ?: Recurrence.Once
    var template by remember { mutableStateOf(chore?.template) }
    var title by remember { mutableStateOf(chore?.title.orEmpty()) }
    var titleFromTemplate by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf(initialRecurrence.kind()) }
    var everyDays by remember { mutableIntStateOf((initialRecurrence as? Recurrence.EveryDays)?.days ?: 1) }
    var weekDays by remember { mutableStateOf((initialRecurrence as? Recurrence.Weekly)?.days ?: setOf(DayOfWeek.SATURDAY)) }
    var dueDate by remember { mutableStateOf(chore?.dueDate ?: today) }
    var showRepeat by remember { mutableStateOf(initialRecurrence != Recurrence.Once) }
    var notifyWhenDone by remember { mutableStateOf(chore?.notifyWhenDone ?: true) }
    var urgent by remember { mutableStateOf(chore?.urgent ?: false) }
    var points by remember { mutableIntStateOf(chore?.points ?: Chore.POINTS.first) }

    val templateTitles = ChoreTemplate.entries.associateWith { stringResource(it.title) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun pick(picked: ChoreTemplate) {
        template = picked
        // Only replace the title if it's empty or still the previous template's.
        if (picked != ChoreTemplate.OTHER && (title.isBlank() || title == titleFromTemplate)) {
            title = templateTitles.getValue(picked)
            titleFromTemplate = title
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (chore == null) R.string.add_chore_title else R.string.edit_chore),
                style = MaterialTheme.typography.titleLarge,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ChoreTemplate.entries.forEach { option ->
                    FilterChip(
                        selected = template == option,
                        onClick = { pick(option) },
                        leadingIcon = { ItemIcon(option.icon, Modifier.size(FilterChipDefaults.IconSize)) },
                        label = { Text(templateTitles.getValue(option)) },
                    )
                }
            }
            if (savedChores.isNotEmpty()) {
                Text(stringResource(R.string.saved_chores), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    savedChores.forEach { saved ->
                        SavedChoreChip(
                            title = saved.title,
                            selected = template == ChoreTemplate.OTHER && title == saved.title,
                            onClick = {
                                template = ChoreTemplate.OTHER
                                title = saved.title
                                titleFromTemplate = saved.title
                                if (canSetPoints) points = saved.points
                            },
                            onLongClick = { if (canRemoveSaved(saved)) removing = saved },
                        )
                    }
                }
                Text(stringResource(R.string.saved_chore_hint), style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= MAX_TITLE_LENGTH) title = it },
                label = { Text(stringResource(R.string.editor_title_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            if (canSetPoints) PointsRow(points, onPoints = { points = it })

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = urgent, onValueChange = { urgent = it }, role = Role.Switch),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ItemIcon(CoreUiR.drawable.ic_priority_high, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.editor_urgent))
                    Text(stringResource(R.string.editor_urgent_hint), style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = urgent,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.error,
                        checkedThumbColor = MaterialTheme.colorScheme.onError,
                    ),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = notifyWhenDone, onValueChange = { notifyWhenDone = it }, role = Role.Switch),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.editor_notify_when_done), modifier = Modifier.weight(1f))
                Switch(checked = notifyWhenDone, onCheckedChange = null)
            }

            // Most chores are a one-off "this needs doing now"; repeating is there for those who want it.
            TextButton(onClick = { showRepeat = !showRepeat }) {
                Text(
                    when {
                        showRepeat -> stringResource(R.string.editor_repeat_hide)
                        kind == RepeatKind.ONCE -> stringResource(R.string.editor_repeat_show)
                        else -> stringResource(R.string.editor_repeats, currentRecurrenceLabel(kind, everyDays, weekDays))
                    },
                )
            }
            if (showRepeat) RepeatSection(
                kind = kind,
                everyDays = everyDays,
                weekDays = weekDays,
                dueDate = dueDate,
                today = today,
                onKind = { kind = it },
                onEveryDays = { everyDays = it },
                onWeekDays = { weekDays = it },
                onDueDate = { dueDate = it },
            )

            val recurrence = when (kind) {
                RepeatKind.ONCE -> Recurrence.Once
                RepeatKind.EVERY_DAYS -> Recurrence.EveryDays(everyDays)
                RepeatKind.WEEKLY -> weekDays.takeIf { it.isNotEmpty() }?.let { Recurrence.Weekly(it) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.editor_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val due = if (chore == null && recurrence == Recurrence.Once) today else dueDate
                        onSave(ChoreDraft(title, template ?: ChoreTemplate.OTHER, checkNotNull(recurrence), due, notifyWhenDone, urgent, points))
                    },
                    enabled = title.isNotBlank() && recurrence != null,
                ) { Text(stringResource(CoreUiR.string.save)) }
            }
        }
    }

    removing?.let { saved ->
        AlertDialog(
            onDismissRequest = { removing = null },
            text = { Text(stringResource(R.string.remove_saved_chore, saved.title)) },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveSaved(saved)
                    removing = null
                }) { Text(stringResource(CoreUiR.string.remove)) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(CoreUiR.string.cancel)) } },
        )
    }
}

/** "Points: 1 2 3 4 5": harder chores are worth more in the week's score and the garden. */
@Composable
private fun PointsRow(points: Int, onPoints: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ItemIcon(CoreUiR.drawable.ic_star, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.editor_points))
                Text(stringResource(R.string.editor_points_hint), style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 32.dp)) {
            Chore.POINTS.forEach { value ->
                FilterChip(selected = points == value, onClick = { onPoints(value) }, label = { Text(value.toString()) })
            }
        }
    }
}

/** Like a FilterChip, plus long-press (to remove it from the saved list). */
@Composable
private fun SavedChoreChip(title: String, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.clip(shape).combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemIcon(ChoreTemplate.OTHER.icon, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title)
        }
    }
}

@Composable
private fun RepeatSection(
    kind: RepeatKind,
    everyDays: Int,
    weekDays: Set<DayOfWeek>,
    dueDate: LocalDate,
    today: LocalDate,
    onKind: (RepeatKind) -> Unit,
    onEveryDays: (Int) -> Unit,
    onWeekDays: (Set<DayOfWeek>) -> Unit,
    onDueDate: (LocalDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RepeatOption(stringResource(R.string.repeat_once), kind == RepeatKind.ONCE) { onKind(RepeatKind.ONCE) }
        RepeatOption(stringResource(R.string.repeat_daily), kind == RepeatKind.EVERY_DAYS && everyDays == 1) {
            onKind(RepeatKind.EVERY_DAYS)
            onEveryDays(1)
        }
        RepeatOption(stringResource(R.string.editor_every_n_days), kind == RepeatKind.EVERY_DAYS && everyDays > 1) {
            onKind(RepeatKind.EVERY_DAYS)
            if (everyDays == 1) onEveryDays(2)
        }
        if (kind == RepeatKind.EVERY_DAYS && everyDays > 1) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 40.dp)) {
                OutlinedIconButton(onClick = { if (everyDays > 2) onEveryDays(everyDays - 1) }) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text(recurrenceLabel(Recurrence.EveryDays(everyDays)), modifier = Modifier.padding(horizontal = 16.dp))
                OutlinedIconButton(onClick = { if (everyDays < Recurrence.MAX_DAYS) onEveryDays(everyDays + 1) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        RepeatOption(stringResource(R.string.editor_weekdays), kind == RepeatKind.WEEKLY) { onKind(RepeatKind.WEEKLY) }
        if (kind == RepeatKind.WEEKLY) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 40.dp)) {
                WEEK.forEach { day ->
                    FilterChip(
                        selected = day in weekDays,
                        onClick = { onWeekDays(if (day in weekDays) weekDays - day else weekDays + day) },
                        label = { Text(day.shortName()) },
                    )
                }
            }
        }
        if (kind != RepeatKind.ONCE) {
            Text(stringResource(R.string.editor_starts), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(today, today.plusDays(1)).forEach { date ->
                    FilterChip(selected = dueDate == date, onClick = { onDueDate(date) }, label = { Text(dayLabel(date, today)) })
                }
                if (dueDate != today && dueDate != today.plusDays(1)) {
                    FilterChip(selected = true, onClick = {}, label = { Text(stringResource(R.string.editor_starts_on, dayLabel(dueDate, today))) })
                }
            }
        }
    }
}

@Composable
private fun currentRecurrenceLabel(kind: RepeatKind, everyDays: Int, weekDays: Set<DayOfWeek>): String = when (kind) {
    RepeatKind.ONCE -> recurrenceLabel(Recurrence.Once)
    RepeatKind.EVERY_DAYS -> recurrenceLabel(Recurrence.EveryDays(everyDays))
    RepeatKind.WEEKLY -> if (weekDays.isEmpty()) recurrenceLabel(Recurrence.Once) else recurrenceLabel(Recurrence.Weekly(weekDays))
}

@Composable
private fun RepeatOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

private fun Recurrence.kind() = when (this) {
    Recurrence.Once -> RepeatKind.ONCE
    is Recurrence.EveryDays -> RepeatKind.EVERY_DAYS
    is Recurrence.Weekly -> RepeatKind.WEEKLY
}

// Matches the limit in firestore.rules.
private const val MAX_TITLE_LENGTH = 60
