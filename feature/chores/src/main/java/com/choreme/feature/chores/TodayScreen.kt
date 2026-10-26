package com.choreme.feature.chores

import com.choreme.core.ui.R as CoreUiR
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.model.DayPart
import com.choreme.core.model.Member
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.Recurrence
import com.choreme.core.model.dayPart
import com.choreme.core.ui.FamilyRow
import com.choreme.core.ui.IconBubble
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.MemberAvatar
import com.choreme.core.ui.SummaryChip
import com.choreme.core.ui.TabHeader
import com.choreme.core.ui.icon
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

@Composable
internal fun TodayScreen(onOpenGroceries: () -> Unit, viewModel: ChoresViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Chore?>(null) }
    var adding by remember { mutableStateOf(false) }
    var doneByFor by remember { mutableStateOf<Chore?>(null) }
    var remindFor by remember { mutableStateOf<Chore?>(null) }
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AskForNotificationPermission()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header") {
                ChoresHeader(state, onOpenGroceries)
            }
            when {
                state.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                !state.hasChores -> item(key = "empty") { EmptyChores(state.isParent) }
                else -> {
                    val board = state.board
                    if (board.awaitingApproval.isNotEmpty()) {
                        item(key = "awaiting-title") { SectionTitle(stringResource(R.string.section_awaiting)) }
                        items(board.awaitingApproval, key = { "awaiting-${it.id}" }) { chore ->
                            AwaitingCard(chore, state, onApprove = { viewModel.approve(chore) }, onReject = { viewModel.reject(chore) })
                        }
                    }

                    item(key = "todo-title") { SectionTitle(stringResource(R.string.section_to_do)) }
                    if (board.toDo.isEmpty()) {
                        item(key = "todo-empty") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ItemIcon(CoreUiR.drawable.ic_celebration, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.nothing_to_do), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    items(board.toDo, key = { "todo-${it.id}" }) { chore ->
                        ChoreCard(
                            chore = chore,
                            state = state,
                            onClaim = { viewModel.claim(chore) },
                            onUnclaim = { viewModel.unclaim(chore) },
                            onComplete = { viewModel.complete(chore) },
                            onDoneBy = { doneByFor = chore },
                            onEdit = { editing = chore },
                            reminder = reminders[chore.id],
                            onRemind = { remindFor = chore },
                            onCalendar = {
                                scope.launch {
                                    when (val result = viewModel.addToCalendar(chore)) {
                                        is CalendarResult.Added ->
                                            snackbar.showSnackbar(context.getString(R.string.added_to_calendar, result.calendarName))
                                        is CalendarResult.OpenApp -> try {
                                            context.startActivity(result.intent)
                                        } catch (e: ActivityNotFoundException) {
                                            snackbar.showSnackbar(context.getString(R.string.no_calendar_app))
                                        }
                                    }
                                }
                            },
                        )
                    }

                    if (board.upcoming.isNotEmpty()) {
                        item(key = "upcoming-title") { SectionTitle(stringResource(R.string.section_upcoming)) }
                        items(board.upcoming, key = { "upcoming-${it.id}" }) { chore ->
                            CompactRow(chore, trailing = dayLabel(chore.dueDate, state.today), onClick = { if (state.isParent) editing = chore })
                        }
                    }

                    if (board.doneToday.isNotEmpty()) {
                        item(key = "done-title") { SectionTitle(stringResource(R.string.section_done_today)) }
                        items(board.doneToday, key = { "done-${it.id}" }) { chore ->
                            CompactRow(
                                chore,
                                trailing = state.nameOf(chore.lastDoneBy) ?: stringResource(R.string.someone),
                                onClick = { if (state.isParent) editing = chore },
                                done = true,
                            )
                        }
                    }
                }
            }
        }

        // Anyone who notices something can add it, kids included.
        if (state.me != null) {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                // Dark and round, so it never looks like a chore's own "I'll do it" button.
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shape = CircleShape,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_chore)) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
    }

    if (adding) {
        ChoreEditorSheet(
            chore = null,
            today = state.today,
            savedChores = state.savedChores,
            canRemoveSaved = state::canRemove,
            onRemoveSaved = viewModel::removeSaved,
            onSave = { draft ->
                adding = false
                viewModel.create(draft)?.let { existing ->
                    scope.launch { snackbar.showSnackbar(context.getString(R.string.already_on_list, existing.title)) }
                }
            },
            onDelete = null,
            onDismiss = { adding = false },
        )
    }
    editing?.let { chore ->
        ChoreEditorSheet(
            chore = chore,
            today = state.today,
            onSave = {
                viewModel.update(chore, it)
                editing = null
            },
            onDelete = {
                viewModel.delete(chore)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
    remindFor?.let { chore ->
        RemindDialog(
            hasReminder = chore.id in reminders,
            onSet = { time ->
                val at = viewModel.remind(chore, time)
                remindFor = null
                scope.launch { snackbar.showSnackbar(context.getString(R.string.reminder_set, at.format(SHORT_TIME))) }
            },
            onRemove = {
                viewModel.cancelReminder(chore)
                remindFor = null
            },
            onDismiss = { remindFor = null },
        )
    }
    doneByFor?.let { chore ->
        DoneByDialog(
            kids = state.kids,
            onPick = { kid ->
                viewModel.completeFor(chore, kid)
                doneByFor = null
            },
            onDismiss = { doneByFor = null },
        )
    }
}

/** "Remind me at…": a time today (or tomorrow, if it's passed), on this phone only. */
@Composable
private fun RemindDialog(hasReminder: Boolean, onSet: (LocalTime) -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
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

private val SHORT_TIME: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/** Reminders need the notification permission from Android 13 on; asked once, the first time Today opens. */
@Composable
private fun AskForNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/** "Good evening, Ramon", what's waiting, and the family as circles. */
@Composable
internal fun ChoresHeader(state: ChoresUiState, onOpenGroceries: () -> Unit) {
    val me = state.me
    val greeting = when (dayPart(LocalTime.now())) {
        DayPart.MORNING -> R.string.greeting_morning
        DayPart.AFTERNOON -> R.string.greeting_afternoon
        DayPart.EVENING -> R.string.greeting_evening
    }
    Column(Modifier.padding(top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TabHeader(
            title = if (me != null && me.name.isNotBlank()) stringResource(greeting, me.name) else stringResource(R.string.chores_title),
            eyebrow = state.today.format(DateTimeFormatter.ofPattern("EEEE d MMMM")),
        ) {
            val toDo = state.board.toDo.size
            SummaryChip(pluralStringResource(R.plurals.summary_to_do, toDo, toDo))
            if (state.groceriesToBuy > 0) {
                SummaryChip(
                    stringResource(R.string.groceries_link, state.groceriesToBuy),
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onOpenGroceries),
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        if (state.members.size > 1) {
            FamilyRow(
                members = state.members.values.sortedWith(compareBy({ it.id != me?.id }, { it.name.lowercase() })),
                meId = me?.id,
                youLabel = stringResource(R.string.you),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun EmptyChores(isParent: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(if (isParent) R.string.empty_body_parent else R.string.empty_body_kid))
        }
    }
}

/** Who it's about (on it, or noticed it), as their circle and a short line. */
@Composable
private fun ChoreMeta(person: Member?, text: String, late: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (person != null) MemberAvatar(person, size = 18.dp)
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = if (late) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (late) FontWeight.ExtraBold else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Red is kept for urgent chores only, so it always means "drop everything". */
@Composable
private fun UrgentLabel() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ItemIcon(CoreUiR.drawable.ic_priority_high, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
        Text(
            stringResource(R.string.urgent).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.error,
            letterSpacing = 0.8.sp,
        )
    }
}

/**
 * One chore, with one main button ("I'll do it" or "Done"); everything else is in the ⋯ menu.
 * Urgent ones are red.
 */
@Composable
internal fun ChoreCard(
    chore: Chore,
    state: ChoresUiState,
    onClaim: () -> Unit,
    onUnclaim: () -> Unit,
    onComplete: () -> Unit,
    onDoneBy: () -> Unit,
    onEdit: () -> Unit,
    onCalendar: () -> Unit,
    reminder: Instant? = null,
    onRemind: () -> Unit = {},
) {
    val me = state.me ?: return
    val overdueDays = ChronoUnit.DAYS.between(chore.dueDate, state.today).toInt()
    val claimedByMe = chore.claimedBy == me.id
    val claimedByOther = chore.status == ChoreStatus.CLAIMED && !claimedByMe
    // Kids can only act on chores that are free or theirs; parents can always finish one.
    val canFinish = !claimedByOther || state.isParent
    var menu by remember { mutableStateOf(false) }

    val person = state.members[chore.claimedBy ?: chore.reportedBy]
    val line = buildList {
        when {
            claimedByMe -> add(stringResource(R.string.you_are_on_it))
            claimedByOther -> add(stringResource(R.string.on_it, state.nameOf(chore.claimedBy) ?: stringResource(R.string.someone)))
            chore.reportedBy == me.id -> add(stringResource(R.string.reported_by_you))
            chore.reportedBy != null -> state.nameOf(chore.reportedBy)?.let { add(stringResource(R.string.reported_by, it)) }
        }
        if (overdueDays > 0 && !chore.urgent) add(pluralStringResource(R.plurals.overdue_days, overdueDays, overdueDays))
        if (chore.recurrence != Recurrence.Once) add(recurrenceLabel(chore.recurrence))
        if (reminder != null) add(stringResource(R.string.reminder_at, reminder.atZone(ZoneId.systemDefault()).format(SHORT_TIME)))
    }.joinToString(" · ")

    val urgent = chore.urgent
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = state.isParent, onClick = onEdit),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (urgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBubble(
                    chore.template.icon,
                    container = if (urgent) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.primaryContainer,
                    tint = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (urgent) UrgentLabel()
                    Text(chore.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (line.isNotEmpty()) ChoreMeta(person, line, late = overdueDays > 0 && !urgent)
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (chore.status == ChoreStatus.OPEN && canFinish) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.done_already)) }, onClick = { menu = false; onComplete() })
                        }
                        if (claimedByMe) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.let_go)) }, onClick = { menu = false; onUnclaim() })
                        }
                        if (state.isParent && state.kids.isNotEmpty()) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.done_by)) }, onClick = { menu = false; onDoneBy() })
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(if (reminder == null) R.string.remind_me else R.string.change_reminder)) },
                            leadingIcon = { ItemIcon(CoreUiR.drawable.ic_alarm, Modifier.size(20.dp)) },
                            onClick = { menu = false; onRemind() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_to_calendar)) },
                            leadingIcon = { ItemIcon(CoreUiR.drawable.ic_calendar_add_on, Modifier.size(20.dp)) },
                            onClick = { menu = false; onCalendar() },
                        )
                        if (state.isParent) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.edit_chore)) }, onClick = { menu = false; onEdit() })
                        }
                    }
                }
            }
            // The one thing to do next.
            val main = when {
                chore.status == ChoreStatus.OPEN -> R.string.grab to onClaim
                canFinish -> R.string.done to onComplete
                else -> null
            }
            if (main != null) {
                Button(
                    onClick = main.second,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = if (urgent) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                    } else if (main.first == R.string.done) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                ) { Text(stringResource(main.first), style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)) }
            }
        }
    }
}

@Composable
internal fun AwaitingCard(chore: Chore, state: ChoresUiState, onApprove: () -> Unit, onReject: () -> Unit) {
    val who = state.nameOf(chore.completedBy) ?: stringResource(R.string.someone)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBubble(
                    chore.template.icon,
                    container = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(chore.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    ChoreMeta(state.members[chore.completedBy], stringResource(R.string.finished_by, who), late = false)
                }
            }
            if (state.isParent) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    ) { Text(stringResource(CoreUiR.string.approve)) }
                    OutlinedButton(onClick = onReject, modifier = Modifier.heightIn(min = 48.dp), shape = MaterialTheme.shapes.small) {
                        Text(stringResource(R.string.not_yet))
                    }
                }
            } else {
                Text(stringResource(R.string.waiting_for_parent), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
internal fun CompactRow(chore: Chore, trailing: String, onClick: () -> Unit, done: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIcon(
            if (done) CoreUiR.drawable.ic_check_circle else chore.template.icon,
            Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.width(12.dp))
        Text(chore.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(trailing, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** For kids without a phone: a parent records which kid did it. */
@Composable
private fun DoneByDialog(kids: List<Member>, onPick: (Member) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.done_by_title)) },
        text = {
            Column {
                kids.forEach { kid ->
                    TextButton(onClick = { onPick(kid) }, modifier = Modifier.fillMaxWidth()) { Text(kid.name) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } },
    )
}
