package com.choreme.feature.chores

import androidx.compose.ui.platform.LocalContext
import com.choreme.core.ui.title
import com.choreme.core.ui.ChoreNames
import com.choreme.core.ui.R as CoreUiR
import com.choreme.core.model.chore.ChoreChoice
import com.choreme.core.model.chore.choreChoices
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.choreme.core.model.Member
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.MemberAvatar
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.Recurrence
import com.choreme.core.model.chore.SavedChore
import java.time.DayOfWeek
import java.time.LocalDate
import com.choreme.core.model.chore.firstDueDate
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
    /** Ready-made and saved chores to pick from, and the family's most used ones. */
    choices: List<ChoreChoice> = choreChoices(emptyList()),
    popular: List<ChoreChoice> = emptyList(),
    canRemoveSaved: (SavedChore) -> Boolean = { false },
    onRemoveSaved: (SavedChore) -> Unit = {},
    /** Parents choose what a chore is worth; kids' reports are worth [Chore.POINTS] first. */
    canSetPoints: Boolean = false,
    /** Who a parent can give it to (everyone at home); empty hides the choice, as for kids. */
    people: List<Member> = emptyList(),
) {
    val context = LocalContext.current
    var removing by remember { mutableStateOf<SavedChore?>(null) }
    val initialRecurrence = chore?.recurrence ?: Recurrence.Once
    var template by remember { mutableStateOf(chore?.template) }
    var title by remember { mutableStateOf(chore?.title?.let { ChoreNames.shown(context, it) }.orEmpty()) }
    var titleFromTemplate by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf(initialRecurrence.kind()) }
    var everyDays by remember { mutableIntStateOf((initialRecurrence as? Recurrence.EveryDays)?.days ?: 1) }
    var weekDays by remember { mutableStateOf((initialRecurrence as? Recurrence.Weekly)?.days ?: setOf(DayOfWeek.SATURDAY)) }
    var monthDay by remember { mutableIntStateOf((initialRecurrence as? Recurrence.Monthly)?.day ?: today.dayOfMonth) }
    var repeatUntil by remember { mutableStateOf(chore?.repeatUntil) }
    var dueDate by remember { mutableStateOf(chore?.dueDate ?: today) }
    var showRepeat by remember { mutableStateOf(initialRecurrence != Recurrence.Once) }
    var notifyWhenDone by remember { mutableStateOf(chore?.notifyWhenDone ?: true) }
    var urgent by remember { mutableStateOf(chore?.urgent ?: false) }
    var points by remember { mutableIntStateOf(chore?.points ?: Chore.POINTS.first) }
    var forWhom by remember { mutableStateOf(chore?.assignedTo?.ifEmpty { null } ?: chore?.team.orEmpty()) }

    val templateTitles = ChoreTemplate.entries.associateWith { stringResource(it.title) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun pick(picked: ChoreTemplate) {
        template = picked
        // Only replace the title if it's empty or still the previous template's.
        if (picked != ChoreTemplate.OTHER) {
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
            // Typing first: most of the time two or three letters and a tap is enough.
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= MAX_TITLE_LENGTH) title = it },
                label = { Text(stringResource(R.string.editor_title_label)) },
                placeholder = { Text(stringResource(R.string.editor_title_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            ChoicePicker(
                choices = choices,
                popular = popular,
                // While typing a new chore, only what matches; otherwise "Most used" and the groups.
                query = if (chore == null && title != titleFromTemplate) title else "",
                titleOf = { choice -> choiceTitle(choice, templateTitles) },
                isSelected = { choice -> title.isNotBlank() && title == choiceTitle(choice, templateTitles) },
                onPick = { choice ->
                    when (choice) {
                        is ChoreChoice.BuiltIn -> pick(choice.template)
                        is ChoreChoice.Saved -> {
                            template = choice.template
                            title = choice.saved.title
                            titleFromTemplate = choice.saved.title
                            if (canSetPoints) points = choice.saved.points
                        }
                    }
                },
                onLongPress = { choice -> if (choice is ChoreChoice.Saved && canRemoveSaved(choice.saved)) removing = choice.saved },
            )

            if (people.isNotEmpty() && (chore == null || chore.isActive)) {
                ForWhomRow(people, forWhom, onChange = { forWhom = it })
            }
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
                        else -> stringResource(R.string.editor_repeats, currentRecurrenceLabel(kind, everyDays, weekDays, monthDay))
                    },
                )
            }
            val recurrence = when (kind) {
                RepeatKind.ONCE -> Recurrence.Once
                RepeatKind.EVERY_DAYS -> Recurrence.EveryDays(everyDays)
                RepeatKind.WEEKLY -> weekDays.takeIf { it.isNotEmpty() }?.let { Recurrence.Weekly(it) }
                RepeatKind.MONTHLY -> Recurrence.Monthly(monthDay)
            }
            // The first time it's due: "the 5th" starts on the next 5th; a changed repeat starts afresh.
            val firstDue = when {
                recurrence == null -> dueDate
                chore != null && recurrence == chore.recurrence -> dueDate
                // Changing an existing chore's repeat counts from today, unless a start was picked.
                chore != null && dueDate == chore.dueDate -> recurrence.firstDueDate(today)
                else -> recurrence.firstDueDate(dueDate)
            }
            if (showRepeat) RepeatSection(
                kind = kind,
                everyDays = everyDays,
                weekDays = weekDays,
                monthDay = monthDay,
                dueDate = dueDate,
                firstDue = firstDue,
                repeatUntil = repeatUntil,
                today = today,
                onKind = { kind = it },
                onEveryDays = { everyDays = it },
                onWeekDays = { weekDays = it },
                onMonthDay = { monthDay = it },
                onDueDate = { dueDate = it },
                onRepeatUntil = { repeatUntil = it },
            )
            val endsTooSoon = recurrence != Recurrence.Once && repeatUntil?.isBefore(firstDue) == true
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
                        val due = if (chore == null && recurrence == Recurrence.Once) today else firstDue
                        onSave(
                            ChoreDraft(
                                title, template ?: ChoreTemplate.OTHER, checkNotNull(recurrence), due, notifyWhenDone, urgent, points, forWhom,
                                repeatUntil = repeatUntil,
                            ),
                        )
                    },
                    enabled = title.isNotBlank() && recurrence != null && !endsTooSoon,
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

/**
 * "For: (R) Ramon (L) Leo (M) Mia": who's doing it, picked straight away. Several can do it
 * together; nobody picked means anyone can grab it.
 */
@Composable
internal fun ForWhomRow(people: List<Member>, picked: Set<String>, onChange: (Set<String>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.editor_for))
        Text(
            stringResource(if (picked.isEmpty()) R.string.editor_for_anyone else R.string.editor_for_hint),
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            people.forEach { person ->
                val selected = person.id in picked
                FilterChip(
                    selected = selected,
                    onClick = { onChange(if (selected) picked - person.id else picked + person.id) },
                    leadingIcon = { MemberAvatar(person, size = 20.dp) },
                    label = { Text(person.name) },
                )
            }
        }
    }
}

/** "Points: 1 2 3 4 5": harder chores are worth more in the week's score and the garden. */
@Composable
private fun PointsRow(points: Int, onPoints: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ItemIcon(CoreUiR.drawable.ic_star, tint = MaterialTheme.colorScheme.secondary)
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

// Matches the limit in firestore.rules.
private const val MAX_TITLE_LENGTH = 60
