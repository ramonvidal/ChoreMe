package com.choreme.data.remote

import com.choreme.domain.session.Remote
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * A missing document read from the local cache is reported as [Remote.Unknown]
 * until the server confirms it, so a slow first sync isn't mistaken for "not there".
 * Metadata changes are included so that confirmation arrives even when the data didn't change.
 */
fun DocumentReference.remoteFlow(): Flow<Remote<DocumentSnapshot>> = callbackFlow {
    val registration = addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
        when {
            // Typically permission denied after being removed from a family.
            error != null -> trySend(Remote.Missing)
            snapshot == null -> Unit
            snapshot.exists() -> trySend(Remote.Present(snapshot))
            snapshot.metadata.isFromCache -> trySend(Remote.Unknown)
            else -> trySend(Remote.Missing)
        }
    }
    awaitClose { registration.remove() }
}

fun Query.documentsFlow(): Flow<List<DocumentSnapshot>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        when {
            error != null -> trySend(emptyList())
            snapshot != null -> trySend(snapshot.documents)
        }
    }
    awaitClose { registration.remove() }
}

inline fun <T, R> Remote<T>.map(transform: (T) -> R): Remote<R> = when (this) {
    Remote.Unknown -> Remote.Unknown
    Remote.Missing -> Remote.Missing
    is Remote.Present -> Remote.Present(transform(value))
}
