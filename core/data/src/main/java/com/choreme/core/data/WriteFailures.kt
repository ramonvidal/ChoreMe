package com.choreme.core.data

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Why a save didn't go through, without Firebase's own types. */
enum class WriteFailure {
    /** The family's database rules said no (or the data didn't fit them). */
    REFUSED,

    /** Anything else, e.g. the server couldn't be reached for too long. */
    FAILED,
}

/**
 * Saves don't wait for the server (they show at once and sync later), so a save the server
 * refuses would otherwise vanish without a word. Each refused or failed save is reported here;
 * the app shows it as a message.
 */
object WriteFailures {
    private val failures = MutableSharedFlow<WriteFailure>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<WriteFailure> = failures.asSharedFlow()

    internal fun report(what: String, error: Exception) {
        Log.w("ChoreMe", "$what failed", error)
        val code = (error as? FirebaseFirestoreException)?.code
        val refused = code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
            code == FirebaseFirestoreException.Code.INVALID_ARGUMENT ||
            code == FirebaseFirestoreException.Code.FAILED_PRECONDITION
        failures.tryEmit(if (refused) WriteFailure.REFUSED else WriteFailure.FAILED)
    }
}

/** Reports this save to [WriteFailures] if it fails. */
internal fun Task<*>.reportFailure(what: String): Task<*> = addOnFailureListener { WriteFailures.report(what, it) }
