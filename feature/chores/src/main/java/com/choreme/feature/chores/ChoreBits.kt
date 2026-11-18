package com.choreme.feature.chores

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.choreme.core.ui.choreTitle
import com.choreme.core.ui.R as CoreUiR
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.choreme.core.model.Member
import com.choreme.core.model.chore.Chore
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.MemberAvatar
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Finishing a chore: its points (0 when the family plays without points), or, for a kid, that it's gone to a parent. [at] tells two apart. */
internal data class Cheer(val points: Int, val waiting: Boolean, val at: Long)

/** A dark pill that pops up with a bouncing tick for a moment, then fades. */
@Composable
internal fun CheerBubble(cheer: Cheer?, onGone: () -> Unit, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(cheer) }
    if (cheer != null) shown = cheer
    LaunchedEffect(cheer) {
        if (cheer != null) {
            delay(CHEER_MILLIS)
            onGone()
        }
    }
    AnimatedVisibility(
        visible = cheer != null,
        enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 400f)) + fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val current = shown ?: return@AnimatedVisibility
        val tick = remember(current.at) { Animatable(0f) }
        LaunchedEffect(current.at) { tick.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f)) }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.inverseSurface, shadowElevation = 6.dp) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                ItemIcon(
                    CoreUiR.drawable.ic_check_circle,
                    Modifier.size(22.dp).graphicsLayer { scaleX = tick.value; scaleY = tick.value },
                    tint = MaterialTheme.colorScheme.inversePrimary,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (current.waiting) stringResource(R.string.cheer_waiting)
                    else if (current.points == 0) stringResource(R.string.cheer_done)
                    else pluralStringResource(R.plurals.cheer_points, current.points, current.points),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
    }
}

private const val CHEER_MILLIS = 1_800L

/** "Remind me at…": a time today (or tomorrow, if it's passed), on this phone only. */
@Composable
internal fun RemindDialog(hasReminder: Boolean, onSet: (LocalTime) -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val inAnHour = LocalTime.now().plusHours(1)
    val picker = rememberTimePickerState(inAnHour.hour, 0, android.text.format.DateFormat.is24HourFormat(context))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remind_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.remind_hint), style = MaterialTheme.typography.bodySmall)
                TimePicker(state = picker)
            }
        },
        confirmButton = { TextButton(onClick = { onSet(LocalTime.of(picker.hour, picker.minute)) }) { Text(stringResource(CoreUiR.string.save)) } },
        dismissButton = {
            Row {
                if (hasReminder) TextButton(onClick = onRemove) { Text(stringResource(CoreUiR.string.remove)) }
                TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) }
            }
        },
    )
}

internal val SHORT_TIME: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/** Short rows inside one white card ("Coming up", "Done today"). */
@Composable
internal fun CompactList(rows: List<Chore>, trailing: @Composable (Chore) -> String, onClick: (Chore) -> Unit, done: Boolean = false) {
    Card(Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, chore ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            CompactRow(chore, trailing(chore), onClick = { onClick(chore) }, done = done)
        }
    }
}

@Composable
internal fun CompactRow(chore: Chore, trailing: String, onClick: () -> Unit, done: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (done) {
            ItemIcon(CoreUiR.drawable.ic_check_circle, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
        }
        Text(choreTitle(chore.title), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (chore.urgent && !done) {
            UrgentTag()
            Spacer(Modifier.width(8.dp))
        }
        Text(trailing, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Pick several people: who's doing a chore together, or who did it (e.g. kids without a
 * phone). Everyone picked gets the chore's points.
 */
@Composable
internal fun PeopleDialog(
    title: String,
    hint: String,
    people: List<Member>,
    initial: Set<String>,
    allowNone: Boolean,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by remember { mutableStateOf(initial.filter { id -> people.any { it.id == id } }.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(hint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                people.forEach { person ->
                    val checked = person.id in picked
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = checked, role = Role.Checkbox) { picked = if (it) picked + person.id else picked - person.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Spacer(Modifier.width(12.dp))
                        MemberAvatar(person, size = 28.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(person.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(picked) }, enabled = allowNone || picked.isNotEmpty()) {
                Text(stringResource(CoreUiR.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } },
    )
}

/** "Leo", "Leo and Mia", "Leo, Mia and Ana". */
@Composable
internal fun namesLabel(names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names.single()
    else -> stringResource(R.string.names_and, names.dropLast(1).joinToString(", "), names.last())
}
