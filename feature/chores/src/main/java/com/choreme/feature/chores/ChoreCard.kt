package com.choreme.feature.chores

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.choreme.core.ui.choreTitle
import com.choreme.core.ui.R as CoreUiR
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import com.choreme.core.ui.AvatarStack
import androidx.compose.ui.draw.clip
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.Recurrence
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.icon
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** A small pill with a room's name, on a card of a task that belongs to that room. */
@Composable
internal fun RoomTag(name: String) {
    Row(
        Modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ItemIcon(CoreUiR.drawable.ic_tab_rooms, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Red is kept for urgent chores only: a small tag. */
@Composable
internal fun UrgentTag() {
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
 * One chore on a white card: grey icon, title, one grey line, and one small button. Who's on it
 * shows as their avatars. Colour means one thing each: a green edge is yours, a red edge urgent.
 * Swipe right for the button's action, left (or tap) for everything else.
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
    /** The room a room task belongs to, shown as a small tag next to the title. */
    roomName: String? = null,
) {
    val me = state.me ?: return
    val overdueDays = ChronoUnit.DAYS.between(chore.dueDate, state.today).toInt()
    val onIt = me.id in chore.team
    val takenByOthers = chore.status == ChoreStatus.CLAIMED && !onIt
    val others = state.teamNames(chore, me.id)
    val team = (listOfNotNull(chore.claimedBy) + chore.helpers.sorted()).mapNotNull { state.members[it] }
    var menu by remember { mutableStateOf(false) }

    val teamText = when {
        onIt && others.isEmpty() -> stringResource(R.string.you_are_on_it)
        onIt -> stringResource(R.string.you_and_on_it, namesLabel(others))
        others.size > 1 -> stringResource(R.string.on_it_many, namesLabel(others))
        takenByOthers -> stringResource(R.string.on_it, others.firstOrNull() ?: stringResource(R.string.someone))
        else -> null
    }
    val line = buildList {
        when {
            // With avatars, "Leo is on it" needs no name: they're in the circles.
            teamText != null -> add(if (onIt) teamText else stringResource(R.string.on_it_short))
            chore.reportedBy == me.id -> add(stringResource(R.string.reported_by_you))
            chore.reportedBy != null -> state.nameOf(chore.reportedBy)?.let { add(stringResource(R.string.reported_by, it)) }
        }
        if (overdueDays > 0) add(pluralStringResource(R.plurals.overdue_days, overdueDays, overdueDays))
        if (overdueDays < 0) add(dayLabel(chore.dueDate, state.today))
        if (chore.recurrence != Recurrence.Once) {
            add(recurrenceLabel(chore.recurrence) + (chore.repeatUntil?.let { " " + untilLabel(it) } ?: ""))
        }
        if (chore.points > 1 && state.pointsOn) add(pluralStringResource(CoreUiR.plurals.points, chore.points, chore.points))
        if (reminder != null) add(stringResource(R.string.reminder_at, reminder.atZone(ZoneId.systemDefault()).format(SHORT_TIME)))
    }.joinToString(" · ")

    // The one thing to do next. "Me too" joins whoever's on it.
    val main = when {
        chore.status == ChoreStatus.OPEN -> R.string.grab to onClaim
        onIt -> R.string.done to onComplete
        else -> R.string.join to onJoin
    }
    val colors = MaterialTheme.colorScheme
    val cardColor = when {
        chore.urgent -> lerp(CardDefaults.cardColors().containerColor, colors.error, 0.06f)
        mine -> lerp(CardDefaults.cardColors().containerColor, colors.secondary, 0.08f)
        else -> CardDefaults.cardColors().containerColor
    }
    val edge = when {
        chore.urgent -> BorderStroke(1.5.dp, colors.error)
        mine -> BorderStroke(1.5.dp, colors.secondary)
        else -> null
    }

    // Swiping only ever triggers; the card springs back and the list updates from the data.
    // The swipe state outlives a change of status, so it reads the current action.
    val action by rememberUpdatedState(main.second)
    val swipe = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> action()
                SwipeToDismissBoxValue.EndToStart -> menu = true
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
        positionalThreshold = { it * 0.35f },
    )

    Box {
        SwipeToDismissBox(
            state = swipe,
            backgroundContent = { SwipeHint(swipe.dismissDirection, stringResource(main.first)) },
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { menu = true },
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = edge,
            ) {
                Box {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ItemIcon(chore.template.icon, Modifier.size(22.dp), tint = colors.onSurfaceVariant)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(choreTitle(chore.title), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            itemVerticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (chore.urgent) UrgentTag()
                            if (roomName != null) RoomTag(roomName)
                            if (team.isNotEmpty() && teamText != null) AvatarStack(team, teamText, ring = cardColor)
                            if (line.isNotEmpty()) {
                                Text(
                                    line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    // Plain for every chore: green is for yours (the edge), red for urgent (the edge and tag).
                    OutlinedButton(
                        onClick = main.second,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = SmallButtonPadding,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
                        border = BorderStroke(1.dp, colors.outline),
                    ) { Text(stringResource(main.first)) }
                }
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

/** Behind a card being swiped: its button's action on the left, "More" on the right. */
@Composable
private fun SwipeHint(direction: SwipeToDismissBoxValue, action: String) {
    val colors = MaterialTheme.colorScheme
    val right = direction == SwipeToDismissBoxValue.StartToEnd
    if (direction == SwipeToDismissBoxValue.Settled) return
    Box(
        Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .background(if (right) colors.secondary else colors.surfaceVariant)
            .padding(horizontal = 20.dp),
        contentAlignment = if (right) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Text(
            if (right) action else stringResource(R.string.swipe_more),
            style = MaterialTheme.typography.titleSmall,
            color = if (right) colors.onSecondary else colors.onSurface,
        )
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
                    Text(choreTitle(chore.title), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
