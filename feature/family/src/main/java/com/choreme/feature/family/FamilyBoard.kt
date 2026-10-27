package com.choreme.feature.family

import com.choreme.core.ui.R as CoreUiR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.choreme.core.model.Member
import com.choreme.core.model.Note
import com.choreme.core.model.chore.MemberWeek
import com.choreme.core.ui.ItemIcon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The family board: short notes everyone sees. Tap one you may change to edit it. */
@Composable
internal fun NotesSection(notes: List<Note>, members: List<Member>, me: Member?, onAdd: (String) -> Unit, onEdit: (Note, String) -> Unit, onDelete: (Note) -> Unit) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) } // note id, or "" for a new one
    val names = members.associate { it.id to it.name }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (notes.isEmpty()) Text(stringResource(R.string.notes_empty), style = MaterialTheme.typography.bodySmall)
        notes.forEach { note ->
            val editable = me != null && note.canEdit(me)
            Card(Modifier.fillMaxWidth().clickable(enabled = editable) { editing = note.id }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    ItemIcon(CoreUiR.drawable.ic_sticky_note_2, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(note.text, style = MaterialTheme.typography.bodyLarge)
                        names[note.createdBy]?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        OutlinedButton(
            onClick = { editing = "" },
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) { Text(stringResource(R.string.notes_add)) }
    }

    val id = editing ?: return
    val note = notes.firstOrNull { it.id == id }
    NoteDialog(
        initial = note?.text.orEmpty(),
        onSave = { text ->
            if (note == null) onAdd(text) else onEdit(note, text)
            editing = null
        },
        onDelete = note?.let { { onDelete(it); editing = null } },
        onDismiss = { editing = null },
    )
}

@Composable
private fun NoteDialog(initial: String, onSave: (String) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (onDelete == null) R.string.notes_new else R.string.notes_edit)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= Note.MAX_LENGTH) text = it },
                placeholder = { Text(stringResource(R.string.notes_placeholder)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 3,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text(stringResource(CoreUiR.string.save)) } },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(CoreUiR.string.remove), color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) }
            }
        },
    )
}

/** For parents: the points each person earned in the last 7 days, to keep things fair. */
@Composable
internal fun WeekSection(week: List<MemberWeek>) {
    Card(Modifier.fillMaxWidth()) {
        week.forEachIndexed { index, (member, done) ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(member.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    val points = done.sumOf { it.points }
                    Text(pluralStringResource(CoreUiR.plurals.points, points, points), style = MaterialTheme.typography.titleSmall)
                }
                if (done.isNotEmpty()) {
                    val titles = done.map { it.title }.distinct()
                    Text(
                        titles.take(MAX_TITLES).joinToString(", ") + if (titles.size > MAX_TITLES) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private const val MAX_TITLES = 4

/** "Away until Sun 2 Nov", or null when home. */
@Composable
internal fun awayLabel(member: Member, today: LocalDate): String? {
    if (!member.isAway(today)) return null
    val until = checkNotNull(member.awayUntil).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    return stringResource(R.string.away_until, until)
}

/** Pick the last day away (from today on); "Back home" ends it early. */
@Composable
internal fun AwayDialog(member: Member, today: LocalDate, onSet: (LocalDate?) -> Unit, onDismiss: () -> Unit) {
    val todayMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = (member.awayUntil ?: today.plusDays(DEFAULT_DAYS)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { picker.selectedDateMillis?.let { onSet(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } },
                enabled = picker.selectedDateMillis != null,
            ) { Text(stringResource(R.string.away_set)) }
        },
        dismissButton = {
            Row {
                if (member.isAway(today)) TextButton(onClick = { onSet(null) }) { Text(stringResource(R.string.away_back)) }
                TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) }
            }
        },
    ) {
        Text(
            stringResource(R.string.away_title, member.name),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
        )
        Text(
            stringResource(R.string.away_hint),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        DatePicker(state = picker, title = null, headline = null, showModeToggle = false)
    }
}

private const val DEFAULT_DAYS = 7L
