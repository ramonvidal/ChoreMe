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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.model.Member
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.Recurrence
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.icon
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

@Composable
internal fun TodayScreen(onOpenGroceries: () -> Unit, viewModel: ChoresViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Chore?>(null) }
    var adding by remember { mutableStateOf(false) }
    var doneByFor by remember { mutableStateOf<Chore?>(null) }

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
            if (state.groceriesToBuy > 0) {
                item(key = "groceries") {
                    TextButton(onClick = onOpenGroceries) {
                        ItemIcon(CoreUiR.drawable.ic_shopping_cart, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.groceries_link, state.groceriesToBuy))
                    }
                }
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

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
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

@Composable
private fun ChoreHeading(chore: Chore, subtitle: String?, late: Boolean = false) {
    val accent = if (chore.urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically) {
        ItemIcon(chore.template.icon, Modifier.size(28.dp), tint = accent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (chore.urgent) UrgentLabel()
            Text(chore.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (late) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
            }
        }
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

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
) {
    val me = state.me ?: return
    val overdueDays = ChronoUnit.DAYS.between(chore.dueDate, state.today).toInt()
    val claimedByMe = chore.claimedBy == me.id
    val claimedByOther = chore.status == ChoreStatus.CLAIMED && !claimedByMe

    val details = buildList {
        if (overdueDays > 0) add(pluralStringResource(R.plurals.overdue_days, overdueDays, overdueDays))
        when (chore.reportedBy) {
            null -> Unit
            me.id -> add(stringResource(R.string.reported_by_you))
            else -> state.nameOf(chore.reportedBy)?.let { add(stringResource(R.string.reported_by, it)) }
        }
        if (chore.recurrence != Recurrence.Once) add(recurrenceLabel(chore.recurrence))
        when {
            claimedByMe -> add(stringResource(R.string.you_are_on_it))
            claimedByOther -> add(stringResource(R.string.on_it, state.nameOf(chore.claimedBy) ?: stringResource(R.string.someone)))
        }
    }.joinToString(" · ")

    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = state.isParent, onClick = onEdit),
        colors = if (chore.urgent) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoreHeading(chore, details, late = overdueDays > 0)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Kids can only act on chores that are free or theirs; parents can always finish one.
                val canFinish = !claimedByOther || state.isParent
                if (chore.status == ChoreStatus.OPEN) OutlinedButton(onClick = onClaim) { Text(stringResource(R.string.grab)) }
                if (canFinish) Button(onClick = onComplete) { Text(stringResource(R.string.done)) }
                if (claimedByMe) TextButton(onClick = onUnclaim) { Text(stringResource(R.string.let_go)) }
                if (state.isParent && state.kids.isNotEmpty()) TextButton(onClick = onDoneBy) { Text(stringResource(R.string.done_by)) }
                TextButton(onClick = onCalendar) {
                    ItemIcon(CoreUiR.drawable.ic_calendar_add_on, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_to_calendar))
                }
            }
        }
    }
}

@Composable
internal fun AwaitingCard(chore: Chore, state: ChoresUiState, onApprove: () -> Unit, onReject: () -> Unit) {
    val who = state.nameOf(chore.completedBy) ?: stringResource(R.string.someone)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoreHeading(chore, stringResource(R.string.finished_by, who))
            if (state.isParent) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onApprove) { Text(stringResource(CoreUiR.string.approve)) }
                    FilledTonalButton(onClick = onReject) { Text(stringResource(R.string.not_yet)) }
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
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIcon(
            if (done) CoreUiR.drawable.ic_check_circle else chore.template.icon,
            Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Text(chore.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(trailing, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
