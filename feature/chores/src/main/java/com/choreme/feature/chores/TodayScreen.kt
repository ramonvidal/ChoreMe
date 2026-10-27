package com.choreme.feature.chores

import com.choreme.core.ui.R as CoreUiR
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.model.Member
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.Recurrence
import com.choreme.core.ui.HeaderLine
import com.choreme.core.ui.SectionLabel
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.MemberAvatar
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
    var assigning by remember { mutableStateOf<Chore?>(null) }
    var remindFor by remember { mutableStateOf<Chore?>(null) }
    var showHome by remember { mutableStateOf(false) }
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
                    item(key = "home") { HomeCard(state.scene, onClick = { showHome = true }) }
                    val board = state.board
                    val choreCard: @Composable (Chore, Boolean) -> Unit = { chore, mine ->
                        ChoreCard(
                            chore = chore,
                            state = state,
                            onClaim = { viewModel.claim(chore) },
                            onUnclaim = { viewModel.unclaim(chore) },
                            onComplete = { viewModel.complete(chore) },
                            onDoneBy = { doneByFor = chore },
                            onJoin = { viewModel.join(chore) },
                            onAssign = { assigning = chore },
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
                            mine = mine,
                        )
                    }
                    // What you said you'd do comes first, so it isn't forgotten.
                    if (board.mine.isNotEmpty()) {
                        item(key = "mine-title") { SectionLabel(stringResource(R.string.section_yours)) }
                        items(board.mine, key = { "mine-${it.id}" }) { chore -> choreCard(chore, true) }
                    }
                    if (board.awaitingApproval.isNotEmpty()) {
                        item(key = "awaiting-title") { SectionLabel(stringResource(R.string.section_awaiting)) }
                        items(board.awaitingApproval, key = { "awaiting-${it.id}" }) { chore ->
                            AwaitingCard(chore, state, onApprove = { viewModel.approve(chore) }, onReject = { viewModel.reject(chore) })
                        }
                    }

                    if (board.toDo.isNotEmpty() || board.mine.isEmpty()) {
                        item(key = "todo-title") { SectionLabel(stringResource(R.string.section_to_do)) }
                    }
                    if (board.toDo.isEmpty() && board.mine.isEmpty()) {
                        item(key = "todo-empty") {
                            Text(
                                stringResource(R.string.nothing_to_do),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                    items(board.toDo, key = { "todo-${it.id}" }) { chore -> choreCard(chore, false) }

                    if (board.upcoming.isNotEmpty()) {
                        item(key = "upcoming-title") { SectionLabel(stringResource(R.string.section_upcoming)) }
                        item(key = "upcoming") {
                            CompactList(board.upcoming, trailing = { dayLabel(it.dueDate, state.today) }, onClick = { if (state.isParent) editing = it })
                        }
                    }

                    if (board.doneToday.isNotEmpty()) {
                        item(key = "done-title") { SectionLabel(stringResource(R.string.section_done_today)) }
                        item(key = "done-today") {
                            CompactList(
                                board.doneToday,
                                trailing = {
                                    namesLabel((listOfNotNull(it.lastDoneBy) + it.lastHelpers.sorted()).mapNotNull(state::nameOf))
                                        .ifEmpty { stringResource(R.string.someone) }
                                },
                                onClick = { if (state.isParent) editing = it },
                                done = true,
                            )
                        }
                    }
                }
            }
        }

        // Anyone who notices something can add it, kids included.
        if (state.me != null) {
            // Black, so it never looks like a chore's own button.
            FloatingActionButton(
                onClick = { adding = true },
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_chore)) }
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
            canSetPoints = state.isParent,
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
            canSetPoints = state.isParent,
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
    if (showHome) HomeDialog(state.scene, state.week, onDismiss = { showHome = false })
    doneByFor?.let { chore ->
        PeopleDialog(
            title = stringResource(R.string.done_by_title),
            hint = stringResource(R.string.done_by_hint),
            people = state.atHome,
            initial = chore.team,
            allowNone = false,
            onConfirm = { people ->
                viewModel.completeFor(chore, people)
                doneByFor = null
            },
            onDismiss = { doneByFor = null },
        )
    }
    assigning?.let { chore ->
        PeopleDialog(
            title = stringResource(R.string.who_does_title),
            hint = stringResource(R.string.who_does_hint),
            people = state.atHome,
            initial = chore.team,
            allowNone = true,
            onConfirm = { people ->
                viewModel.assign(chore, people)
                assigning = null
            },
            onDismiss = { assigning = null },
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

/** "Chores", and one grey line: what needs doing and what's on the grocery list. */
@Composable
internal fun ChoresHeader(state: ChoresUiState, onOpenGroceries: () -> Unit) {
    val toDo = state.board.toDo.size
    TabHeader(title = stringResource(R.string.chores_title), modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
        Row {
            HeaderLine(pluralStringResource(R.plurals.summary_to_do, toDo, toDo))
            val mine = state.board.mine.size
            if (mine > 0) {
                HeaderLine(" · ")
                Text(
                    pluralStringResource(R.plurals.summary_yours, mine, mine),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (state.groceriesToBuy > 0) {
                HeaderLine(" · ")
                HeaderLine(
                    stringResource(R.string.groceries_link, state.groceriesToBuy),
                    modifier = Modifier.clickable(onClick = onOpenGroceries),
                )
            }
        }
    }
}


@Composable
private fun EmptyChores(isParent: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (isParent) R.string.empty_body_parent else R.string.empty_body_kid),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Red is kept for urgent chores only: a small tag. */
@Composable
private fun UrgentTag() {
    Text(
        stringResource(R.string.urgent),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/**
 * One chore on a white card: grey icon, title, one grey line, and one small button. Tapping the
 * card opens everything else (done by, reminders, calendar, edit).
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
    onJoin: () -> Unit = {},
    onAssign: () -> Unit = {},
    reminder: Instant? = null,
    onRemind: () -> Unit = {},
    /** One the person said they'd do: green edge and tint, so it stands out. */
    mine: Boolean = false,
) {
    val me = state.me ?: return
    val overdueDays = ChronoUnit.DAYS.between(chore.dueDate, state.today).toInt()
    val onIt = me.id in chore.team
    val takenByOthers = chore.status == ChoreStatus.CLAIMED && !onIt
    val others = state.teamNames(chore, me.id)
    var menu by remember { mutableStateOf(false) }

    val line = buildList {
        when {
            onIt && others.isEmpty() -> add(stringResource(R.string.you_are_on_it))
            onIt -> add(stringResource(R.string.you_and_on_it, namesLabel(others)))
            takenByOthers -> add(
                if (others.size > 1) stringResource(R.string.on_it_many, namesLabel(others))
                else stringResource(R.string.on_it, others.firstOrNull() ?: stringResource(R.string.someone)),
            )
            chore.reportedBy == me.id -> add(stringResource(R.string.reported_by_you))
            chore.reportedBy != null -> state.nameOf(chore.reportedBy)?.let { add(stringResource(R.string.reported_by, it)) }
        }
        if (overdueDays > 0) add(pluralStringResource(R.plurals.overdue_days, overdueDays, overdueDays))
        if (overdueDays < 0) add(dayLabel(chore.dueDate, state.today))
        if (chore.recurrence != Recurrence.Once) add(recurrenceLabel(chore.recurrence))
        if (chore.points > 1) add(pluralStringResource(CoreUiR.plurals.points, chore.points, chore.points))
        if (reminder != null) add(stringResource(R.string.reminder_at, reminder.atZone(ZoneId.systemDefault()).format(SHORT_TIME)))
    }.joinToString(" · ")

    // The one thing to do next; green when urgent, plain otherwise. "Me too" joins whoever's on it.
    val main = when {
        chore.status == ChoreStatus.OPEN -> R.string.grab to onClaim
        onIt -> R.string.done to onComplete
        else -> R.string.join to onJoin
    }

    Box {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { menu = true },
            colors = if (mine) {
                CardDefaults.cardColors(containerColor = lerp(CardDefaults.cardColors().containerColor, MaterialTheme.colorScheme.primary, 0.08f))
            } else {
                CardDefaults.cardColors()
            },
            border = if (mine) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ItemIcon(chore.template.icon, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(chore.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (chore.urgent) UrgentTag()
                        if (line.isNotEmpty()) {
                            Text(
                                line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (chore.urgent) {
                    Button(onClick = main.second, shape = MaterialTheme.shapes.small, contentPadding = SmallButtonPadding) {
                        Text(stringResource(main.first))
                    }
                } else {
                    OutlinedButton(
                        onClick = main.second,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = SmallButtonPadding,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) { Text(stringResource(main.first)) }
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            // Parents can always finish one; kids only one that's free or theirs.
            if (chore.status == ChoreStatus.OPEN || (takenByOthers && state.isParent)) {
                DropdownMenuItem(text = { Text(stringResource(R.string.done_already)) }, onClick = { menu = false; onComplete() })
            }
            if (onIt) {
                DropdownMenuItem(text = { Text(stringResource(R.string.let_go)) }, onClick = { menu = false; onUnclaim() })
            }
            if (state.isParent && state.atHome.size > 1) {
                DropdownMenuItem(text = { Text(stringResource(R.string.who_does)) }, onClick = { menu = false; onAssign() })
                DropdownMenuItem(text = { Text(stringResource(R.string.done_by)) }, onClick = { menu = false; onDoneBy() })
            }
            DropdownMenuItem(
                text = { Text(stringResource(if (reminder == null) R.string.remind_me else R.string.change_reminder)) },
                onClick = { menu = false; onRemind() },
            )
            DropdownMenuItem(text = { Text(stringResource(R.string.add_to_calendar)) }, onClick = { menu = false; onCalendar() })
            if (state.isParent) {
                DropdownMenuItem(text = { Text(stringResource(R.string.edit_chore)) }, onClick = { menu = false; onEdit() })
            }
        }
    }
}

private val SmallButtonPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)

@Composable
internal fun AwaitingCard(chore: Chore, state: ChoresUiState, onApprove: () -> Unit, onReject: () -> Unit) {
    val who = state.nameOf(chore.completedBy) ?: stringResource(R.string.someone)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ItemIcon(chore.template.icon, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(chore.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.finished_by, who),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.isParent) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 36.dp)) {
                    Button(onClick = onApprove, shape = MaterialTheme.shapes.small) { Text(stringResource(CoreUiR.string.approve)) }
                    OutlinedButton(
                        onClick = onReject,
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) { Text(stringResource(R.string.not_yet)) }
                }
            } else {
                Text(
                    stringResource(R.string.waiting_for_parent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 36.dp),
                )
            }
        }
    }
}

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
            ItemIcon(CoreUiR.drawable.ic_check_circle, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
        }
        Text(chore.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(trailing, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Pick several people: who's doing a chore together, or who did it (e.g. kids without a
 * phone). Everyone picked gets the chore's points.
 */
@Composable
private fun PeopleDialog(
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
