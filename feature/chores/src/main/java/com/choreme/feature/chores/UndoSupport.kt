package com.choreme.feature.chores

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.choreme.core.ui.ChoreNames
import com.choreme.core.ui.R as CoreUiR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How long Undo is offered before a deleted chore or room task really goes. */
internal const val UNDO_WINDOW = 5_000L

/**
 * Deletes that wait a few seconds, so Undo can keep the chore: hidden at once (see [ids]), really deleted after
 * [UNDO_WINDOW] unless [undo] comes first. [flush] deletes the waiting ones now (the screen is going away).
 */
internal class PendingDeletes(private val scope: CoroutineScope, private val deleteNow: (String) -> Unit) {
    private val waiting = MutableStateFlow<Map<String, Job>>(emptyMap())
    val ids = waiting.map { it.keys }

    fun delete(id: String) {
        val job = scope.launch {
            delay(UNDO_WINDOW)
            deleteNow(id)
            waiting.update { it - id }
        }
        waiting.update { it + (id to job) }
    }

    fun undo(id: String) {
        waiting.value[id]?.cancel()
        waiting.update { it - id }
    }

    fun flush() {
        waiting.value.forEach { (id, job) -> job.cancel(); deleteNow(id) }
        waiting.value = emptyMap()
    }
}

/** "Done: Dishes · Undo" for a few seconds; true when the person tapped Undo. */
internal suspend fun SnackbarHostState.offerUndo(context: Context, @StringRes message: Int, choreTitle: String): Boolean =
    showSnackbar(
        context.getString(message, ChoreNames.shown(context, choreTitle)),
        actionLabel = context.getString(CoreUiR.string.undo),
        duration = SnackbarDuration.Short,
    ) == SnackbarResult.ActionPerformed
