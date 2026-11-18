package com.choreme.feature.chores

import com.choreme.core.ui.title
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.icon
import com.choreme.core.model.chore.ChoreTemplate

internal fun choiceTitle(choice: ChoreChoice, templateTitles: Map<ChoreTemplate, String>): String = when (choice) {
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
internal val ChoreGroup.label: Int
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
