package com.choreme.feature.chores

import androidx.compose.ui.platform.LocalContext
import com.choreme.core.ui.title
import com.choreme.core.ui.ChoreNames
import com.choreme.core.ui.R as CoreUiR
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
import com.choreme.core.model.chore.ChoreChoice
import com.choreme.core.model.chore.ChoreGroup
import com.choreme.core.model.chore.choreChoices
import com.choreme.core.model.chore.group
import com.choreme.core.model.chore.searchChoices
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
import com.choreme.core.model.Member
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.MemberAvatar
import com.choreme.core.ui.icon
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.Recurrence
import com.choreme.core.model.chore.SavedChore
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import java.time.Instant
import com.choreme.core.model.chore.firstDueDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker

private enum class RepeatKind { ONCE, EVERY_DAYS, WEEKLY, MONTHLY }

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

private fun choiceTitle(choice: ChoreChoice, templateTitles: Map<ChoreTemplate, String>): String = when (choice) {
    is ChoreChoice.BuiltIn -> templateTitles.getValue(choice.template)
    is ChoreChoice.Saved -> choice.saved.title
}

/**
 * The chores to pick from, kept short: what matches the [query] while typing; otherwise the
 * family's most used in one row, then groups (Kitchen, Cleaning, ... and "Your own"), folded
 * closed, one open at a time.
 */
@Composable
internal fun ChoicePicker(
    choices: List<ChoreChoice>,
    popular: List<ChoreChoice>,
    query: String,
    titleOf: (ChoreChoice) -> String,
    isSelected: (ChoreChoice) -> Boolean,
    onPick: (ChoreChoice) -> Unit,
    onLongPress: (ChoreChoice) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val chip: @Composable (ChoreChoice) -> Unit = { choice ->
        ChoiceChip(
            title = titleOf(choice),
            icon = choice.template.icon,
            selected = isSelected(choice),
            onClick = { onPick(choice) },
            onLongClick = { onLongPress(choice) },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (query.isNotBlank()) {
            val found = searchChoices(query, choices, titleOf).take(MAX_FOUND)
            if (found.isEmpty()) {
                Text(stringResource(R.string.editor_new_chore_hint), style = MaterialTheme.typography.bodySmall)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    found.forEach { chip(it) }
                }
            }
            return@Column
        }
        if (popular.isNotEmpty()) {
            Text(stringResource(R.string.editor_most_used), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                popular.forEach { chip(it) }
            }
        }
        val groups = ChoreGroup.entries.map { group -> group.name to choices.filter { it.group == group } } +
            ("OWN" to choices.filter { it.group == null })
        groups.filter { it.second.isNotEmpty() }.forEach { (key, inGroup) ->
            val group = ChoreGroup.entries.firstOrNull { it.name == key }
            GroupRow(
                title = stringResource(group?.label ?: R.string.group_own),
                icon = group?.icon ?: ChoreTemplate.OTHER.icon,
                count = inGroup.size,
                open = open == key,
                onToggle = { open = if (open == key) null else key },
            )
            AnimatedVisibility(open == key) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        inGroup.forEach { chip(it) }
                    }
                    if (inGroup.any { it is ChoreChoice.Saved }) {
                        Text(stringResource(R.string.saved_chore_hint), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/** One group, folded: its icon, name and how many chores, and an arrow. */
@Composable
private fun GroupRow(title: String, @DrawableRes icon: Int, count: Int, open: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onToggle).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIcon(icon, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(count.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
    }
}

/** A chore to pick: its icon and name on one line (long names end in "…"); long-press for a saved one's options. */
@Composable
private fun ChoiceChip(title: String, @DrawableRes icon: Int, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline),
        modifier = Modifier.widthIn(max = 240.dp).clip(shape).combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemIcon(icon, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@get:StringRes
private val ChoreGroup.label: Int
    get() = when (this) {
        ChoreGroup.KITCHEN -> R.string.group_kitchen
        ChoreGroup.CLEANING -> R.string.group_cleaning
        ChoreGroup.LAUNDRY -> R.string.group_laundry
        ChoreGroup.BINS -> R.string.group_bins
        ChoreGroup.CARE -> R.string.group_care
        ChoreGroup.ERRANDS -> R.string.group_errands
    }

@get:DrawableRes
private val ChoreGroup.icon: Int
    get() = when (this) {
        ChoreGroup.KITCHEN -> ChoreTemplate.DISHES.icon
        ChoreGroup.CLEANING -> ChoreTemplate.VACUUM.icon
        ChoreGroup.LAUNDRY -> ChoreTemplate.LAUNDRY.icon
        ChoreGroup.BINS -> ChoreTemplate.TRASH.icon
        ChoreGroup.CARE -> ChoreTemplate.TAKE_MEDICINE.icon
        ChoreGroup.ERRANDS -> ChoreTemplate.GROCERIES.icon
    }

private const val MAX_FOUND = 12

@Composable
private fun RepeatSection(
    kind: RepeatKind,
    everyDays: Int,
    weekDays: Set<DayOfWeek>,
    monthDay: Int,
    dueDate: LocalDate,
    firstDue: LocalDate,
    repeatUntil: LocalDate?,
    today: LocalDate,
    onKind: (RepeatKind) -> Unit,
    onEveryDays: (Int) -> Unit,
    onWeekDays: (Set<DayOfWeek>) -> Unit,
    onMonthDay: (Int) -> Unit,
    onDueDate: (LocalDate) -> Unit,
    onRepeatUntil: (LocalDate?) -> Unit,
) {
    var pickingEnd by remember { mutableStateOf(false) }
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
        RepeatOption(stringResource(R.string.editor_monthly), kind == RepeatKind.MONTHLY) { onKind(RepeatKind.MONTHLY) }
        if (kind == RepeatKind.MONTHLY) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 40.dp)) {
                OutlinedIconButton(onClick = { onMonthDay(if (monthDay > 1) monthDay - 1 else 31) }) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text(stringResource(R.string.editor_month_day, ordinal(monthDay)), modifier = Modifier.padding(horizontal = 16.dp))
                OutlinedIconButton(onClick = { onMonthDay(if (monthDay < 31) monthDay + 1 else 1) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        if (kind != RepeatKind.ONCE) {
            Text(stringResource(R.string.editor_starts), style = MaterialTheme.typography.labelLarge)
            if (kind == RepeatKind.MONTHLY) {
                // Monthly starts on the next of its days; nothing to choose.
                Text(stringResource(R.string.editor_starts_on, dayLabel(firstDue, today)), modifier = Modifier.padding(start = 4.dp))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(today, today.plusDays(1)).forEach { date ->
                        FilterChip(selected = dueDate == date, onClick = { onDueDate(date) }, label = { Text(dayLabel(date, today)) })
                    }
                    if (firstDue != today && firstDue != today.plusDays(1)) {
                        FilterChip(selected = true, onClick = {}, label = { Text(stringResource(R.string.editor_starts_on, dayLabel(firstDue, today))) })
                    }
                }
            }
            // "Until the end of next year": the last day it can be due.
            Text(stringResource(R.string.editor_ends), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = repeatUntil == null, onClick = { onRepeatUntil(null) }, label = { Text(stringResource(R.string.editor_ends_never)) })
                FilterChip(
                    selected = repeatUntil != null,
                    onClick = { pickingEnd = true },
                    label = { Text(repeatUntil?.let { stringResource(R.string.editor_ends_on, endLabel(it)) } ?: stringResource(R.string.editor_ends_pick)) },
                )
            }
        }
    }

    if (pickingEnd) {
        val start = maxOf(firstDue, today)
        val utcStart = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = (repeatUntil ?: start.withDayOfYear(1).plusYears(2).minusDays(1))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= utcStart
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingEnd = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        picker.selectedDateMillis?.let { onRepeatUntil(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                        pickingEnd = false
                    },
                    enabled = picker.selectedDateMillis != null,
                ) { Text(stringResource(CoreUiR.string.save)) }
            },
            dismissButton = { TextButton(onClick = { pickingEnd = false }) { Text(stringResource(CoreUiR.string.cancel)) } },
        ) { DatePicker(state = picker) }
    }
}

/** "31 Dec 2027". */
private fun endLabel(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

@Composable
private fun currentRecurrenceLabel(kind: RepeatKind, everyDays: Int, weekDays: Set<DayOfWeek>, monthDay: Int): String = when (kind) {
    RepeatKind.ONCE -> recurrenceLabel(Recurrence.Once)
    RepeatKind.EVERY_DAYS -> recurrenceLabel(Recurrence.EveryDays(everyDays))
    RepeatKind.WEEKLY -> if (weekDays.isEmpty()) recurrenceLabel(Recurrence.Once) else recurrenceLabel(Recurrence.Weekly(weekDays))
    RepeatKind.MONTHLY -> recurrenceLabel(Recurrence.Monthly(monthDay))
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
    is Recurrence.Monthly -> RepeatKind.MONTHLY
}

// Matches the limit in firestore.rules.
private const val MAX_TITLE_LENGTH = 60
