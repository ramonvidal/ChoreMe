package com.choreme.feature.groceries

import com.choreme.core.ui.R as CoreUiR
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.choreme.core.model.grocery.CategoryGroup
import com.choreme.core.model.grocery.GroceryItem
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.drawable

/**
 * In the store: the item's picture, its name, and a round tick to tap once it's in the cart.
 * Long-press to edit. Rows sit together in one card per aisle.
 */
@Composable
internal fun ToBuyRow(item: GroceryItem, large: Boolean, onBought: () -> Unit, onEdit: () -> Unit) {
    var ticked by remember(item.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .heightIn(min = if (large) 64.dp else 54.dp)
            .combinedClickable(
                onClickLabel = stringResource(R.string.grocery_mark_bought),
                onLongClickLabel = stringResource(R.string.grocery_edit_item),
                onClick = { ticked = true; onBought() },
                onLongClick = onEdit,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIcon(item.icon, Modifier.size(if (large) 26.dp else 22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(18.dp))
        FlowRow(
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(groceryName(item.name), style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
            if (item.isStaple) UsualTag()
        }
        Spacer(Modifier.width(12.dp))
        TickCircle(ticked, large)
    }
}

/** An empty ring that fills green with a tick when tapped. */
@Composable
private fun TickCircle(ticked: Boolean, large: Boolean) {
    val size = if (large) 30.dp else 26.dp
    if (ticked) {
        ItemIcon(CoreUiR.drawable.ic_check_circle, Modifier.size(size), tint = MaterialTheme.colorScheme.secondary)
    } else {
        Box(Modifier.size(size).padding(2.dp).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape))
    }
}

/** Swipe a row either way to tick it off (with Undo, like a tap). */
@Composable
internal fun SwipeToBuy(onBought: () -> Unit, content: @Composable () -> Unit) {
    val bought by rememberUpdatedState(onBought)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) bought()
            value != SwipeToDismissBoxValue.Settled
        },
        positionalThreshold = { it * 0.35f },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondary).padding(horizontal = 20.dp),
                contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                ItemIcon(CoreUiR.drawable.ic_check_circle, tint = MaterialTheme.colorScheme.onSecondary)
            }
        },
    ) { content() }
}

/** "usual": bought most weeks, so "Restock usuals" puts it back. Words, not a star nobody can read. */
@Composable
private fun UsualTag() {
    Text(
        stringResource(R.string.grocery_usual_tag),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

@Composable
internal fun AtHomeGroup(group: CategoryGroup, onNeeded: (GroceryItem) -> Unit, onEdit: (GroceryItem) -> Unit) {
    Column {
        CategoryHeader(group.category)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            group.items.forEach { item ->
                ItemTile(item, onClick = { onNeeded(item) }, onLongClick = { onEdit(item) })
            }
        }
    }
}

/** At home: a big tile to tap when it runs out. Long-press to edit. */
@Composable
internal fun ItemTile(item: GroceryItem, onClick: () -> Unit, onLongClick: () -> Unit) {
    Text(
        groceryName(item.name),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    )
}

/** A suggestion while typing: tap to put it on the list. */
@Composable
internal fun ItemChip(item: GroceryItem, onClick: () -> Unit, onLongClick: () -> Unit) {
    val shape = CircleShape
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clip(shape).combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 40.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemIcon(item.icon, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(6.dp))
            Text(groceryName(item.name), style = MaterialTheme.typography.bodyMedium)
            if (item.isStaple) {
                Spacer(Modifier.width(6.dp))
                UsualTag()
            }
        }
    }
}
