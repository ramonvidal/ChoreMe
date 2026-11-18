package com.choreme.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * The one way to add something on a tab, the same on every tab: a dark "+" with a short label ("Add", "Add a room"),
 * floating at the bottom right. Callers place it there (Modifier.align(Alignment.BottomEnd).padding(16.dp)).
 */
/** False hides every [AddButton]; the Play Store pictures set it so the button doesn't cover the content they show. */
val LocalShowAddButton = staticCompositionLocalOf { true }

@Composable
fun AddButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (!LocalShowAddButton.current) return
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shape = MaterialTheme.shapes.medium,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(label) },
    )
}
