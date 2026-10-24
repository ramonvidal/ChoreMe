package com.choreme.feature.chores

import com.choreme.core.ui.R as CoreUiR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate

private enum class RepeatKind { ONCE, EVERY_DAYS, WEEKLY }

/** Adds a chore ([chore] null) or edits one. Picking a template fills in its title and usual schedule. */
@Composable
internal fun ChoreEditorSheet(
    chore: Chore?,
    today: LocalDate,
    onSave: (ChoreDraft) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val initialRecurrence = chore?.recurrence ?: Recurrence.EveryDays(1)
    var template by remember { mutableStateOf(chore?.template) }
    var title by remember { mutableStateOf(chore?.title.orEmpty()) }
    var titleFromTemplate by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf(initialRecurrence.kind()) }
    var everyDays by remember { mutableIntStateOf((initialRecurrence as? Recurrence.EveryDays)?.days ?: 1) }
    var weekDays by remember { mutableStateOf((initialRecurrence as? Recurrence.Weekly)?.days ?: setOf(DayOfWeek.SATURDAY)) }
    var dueDate by remember { mutableStateOf(chore?.dueDate ?: today) }

    val templateTitles = ChoreTemplate.entries.associateWith { stringResource(it.title) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun pick(picked: ChoreTemplate) {
        template = picked
        // Only replace the title if it's empty or still the previous template's.
        if (picked != ChoreTemplate.OTHER && (title.isBlank() || title == titleFromTemplate)) {
            title = templateTitles.getValue(picked)
            titleFromTemplate = title
        }
        if (chore == null) {
            val recurrence = picked.defaultRecurrence
            kind = recurrence.kind()
            if (recurrence is Recurrence.EveryDays) everyDays = recurrence.days
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
                stringResource(if (chore == null) R.string.add_chore else R.string.edit_chore),
                style = MaterialTheme.typography.titleLarge,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ChoreTemplate.entries.forEach { option ->
                    FilterChip(
                        selected = template == option,
                        onClick = { pick(option) },
                        label = { Text("${option.emoji} ${templateTitles.getValue(option)}") },
                    )
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= MAX_TITLE_LENGTH) title = it },
                label = { Text(stringResource(R.string.editor_title_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.editor_repeat), style = MaterialTheme.typography.labelLarge)
            RepeatOption(stringResource(R.string.repeat_once), kind == RepeatKind.ONCE) { kind = RepeatKind.ONCE }
            RepeatOption(stringResource(R.string.repeat_daily), kind == RepeatKind.EVERY_DAYS && everyDays == 1) {
                kind = RepeatKind.EVERY_DAYS
                everyDays = 1
            }
            RepeatOption(stringResource(R.string.editor_every_n_days), kind == RepeatKind.EVERY_DAYS && everyDays > 1) {
                kind = RepeatKind.EVERY_DAYS
                if (everyDays == 1) everyDays = 2
            }
            if (kind == RepeatKind.EVERY_DAYS && everyDays > 1) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 40.dp)) {
                    OutlinedIconButton(onClick = { if (everyDays > 2) everyDays-- }) {
                        Text("−", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        recurrenceLabel(Recurrence.EveryDays(everyDays)),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    OutlinedIconButton(onClick = { if (everyDays < Recurrence.MAX_DAYS) everyDays++ }) {
                        Text("+", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            RepeatOption(stringResource(R.string.editor_weekdays), kind == RepeatKind.WEEKLY) { kind = RepeatKind.WEEKLY }
            if (kind == RepeatKind.WEEKLY) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 40.dp),
                ) {
                    WEEK.forEach { day ->
                        FilterChip(
                            selected = day in weekDays,
                            onClick = { weekDays = if (day in weekDays) weekDays - day else weekDays + day },
                            label = { Text(day.shortName()) },
                        )
                    }
                }
            }

            Text(stringResource(R.string.editor_starts), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(today, today.plusDays(1)).forEach { date ->
                    FilterChip(
                        selected = dueDate == date,
                        onClick = { dueDate = date },
                        label = { Text(dayLabel(date, today)) },
                    )
                }
                if (dueDate != today && dueDate != today.plusDays(1)) {
                    FilterChip(selected = true, onClick = {}, label = { Text(stringResource(R.string.editor_starts_on, dayLabel(dueDate, today))) })
                }
            }

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
                        onSave(ChoreDraft(title, template ?: ChoreTemplate.OTHER, checkNotNull(recurrence), dueDate))
                    },
                    enabled = title.isNotBlank() && recurrence != null,
                ) { Text(stringResource(CoreUiR.string.save)) }
            }
        }
    }
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
