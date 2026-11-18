package com.choreme.core.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

/**
 * "Done: Dishes · Undo" for a few seconds after a step on a chore, on any tab; true when the person tapped Undo.
 * [message] takes the chore's name, shown in this phone's language.
 */
suspend fun SnackbarHostState.offerUndo(context: Context, @StringRes message: Int, choreTitle: String): Boolean =
    showSnackbar(
        context.getString(message, ChoreNames.shown(context, choreTitle)),
        actionLabel = context.getString(R.string.undo),
        duration = SnackbarDuration.Short,
    ) == SnackbarResult.ActionPerformed
