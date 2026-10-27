package com.choreme.core.data

import android.util.Log
import com.choreme.core.data.remote.FirestoreSchema
import com.choreme.core.data.remote.FirestoreSchema.AT
import com.choreme.core.data.remote.FirestoreSchema.FROM
import com.choreme.core.data.remote.FirestoreSchema.TEXT
import com.choreme.core.data.remote.documentsFlow
import com.choreme.core.model.FamilyMessage
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Messages to the whole family (see firestore.rules). Sending doesn't wait for the server. */
@Singleton
class MessageRepository @Inject constructor(
    private val firestore: Lazy<FirebaseFirestore>,
) {
    private fun messages(familyId: String) = firestore.get()
        .collection(FirestoreSchema.FAMILIES).document(familyId)
        .collection(FirestoreSchema.MESSAGES)

    /** Also removes this sender's own messages that are older than a week, so the list stays short. */
    fun send(familyId: String, from: String, text: String, now: Instant = Instant.now()) {
        messages(familyId).document()
            .set(mapOf(TEXT to text, FROM to from, AT to now.toEpochMilli()))
            .addOnFailureListener { Log.w("ChoreMe", "Couldn't send message", it) }
        // One field per query, so no extra Firestore index is needed; only our own are removed.
        messages(familyId)
            .whereLessThan(AT, now.minus(FamilyMessage.KEEP).toEpochMilli())
            .get()
            .addOnSuccessListener { old -> old.documents.filter { it.getString(FROM) == from }.forEach { it.reference.delete() } }
    }

    fun delete(familyId: String, messageId: String) {
        messages(familyId).document(messageId).delete().addOnFailureListener { Log.w("ChoreMe", "Couldn't delete message", it) }
    }

    /** The last week's messages. */
    fun observe(familyId: String, now: Instant = Instant.now()): Flow<List<FamilyMessage>> =
        messages(familyId).whereGreaterThan(AT, now.minus(FamilyMessage.KEEP).toEpochMilli()).documentsFlow()
            .map { docs -> docs.mapNotNull { it.toMessage() } }

    /** For background workers: one read from the server. */
    suspend fun since(familyId: String, after: Instant): List<FamilyMessage> =
        messages(familyId).whereGreaterThan(AT, after.toEpochMilli()).get().await().documents.mapNotNull { it.toMessage() }
}

private fun DocumentSnapshot.toMessage(): FamilyMessage? {
    val text = getString(TEXT) ?: return null
    val at = getLong(AT) ?: return null
    return FamilyMessage(id = id, text = text, from = getString(FROM).orEmpty(), at = Instant.ofEpochMilli(at))
}
