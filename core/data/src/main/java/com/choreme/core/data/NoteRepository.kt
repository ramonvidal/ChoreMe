package com.choreme.core.data

import android.util.Log
import com.choreme.core.data.remote.FirestoreSchema
import com.choreme.core.data.remote.FirestoreSchema.CREATED_BY
import com.choreme.core.data.remote.FirestoreSchema.TEXT
import com.choreme.core.data.remote.FirestoreSchema.UPDATED_AT
import com.choreme.core.data.remote.documentsFlow
import com.choreme.core.model.Note
import com.choreme.core.model.board
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** The family board. Writes don't wait for the server. */
@Singleton
class NoteRepository @Inject constructor(
    private val firestore: Lazy<FirebaseFirestore>,
) {
    private fun notes(familyId: String) = firestore.get()
        .collection(FirestoreSchema.FAMILIES).document(familyId)
        .collection(FirestoreSchema.NOTES)

    fun observe(familyId: String): Flow<List<Note>> =
        notes(familyId).documentsFlow().map { docs ->
            docs.mapNotNull { doc ->
                Note(
                    id = doc.id,
                    text = doc.getString(TEXT) ?: return@mapNotNull null,
                    createdBy = doc.getString(CREATED_BY).orEmpty(),
                    updatedAt = Instant.ofEpochMilli(doc.getLong(UPDATED_AT) ?: 0L),
                )
            }.board()
        }

    fun add(familyId: String, memberId: String, text: String) {
        notes(familyId).document()
            .set(mapOf(TEXT to text, CREATED_BY to memberId, UPDATED_AT to Instant.now().toEpochMilli()))
            .addOnFailureListener { Log.w("ChoreMe", "Couldn't add note", it) }
    }

    fun edit(familyId: String, note: Note, text: String) {
        notes(familyId).document(note.id)
            .update(mapOf(TEXT to text, UPDATED_AT to Instant.now().toEpochMilli()))
            .addOnFailureListener { Log.w("ChoreMe", "Couldn't edit note", it) }
    }

    fun delete(familyId: String, noteId: String) {
        notes(familyId).document(noteId).delete().addOnFailureListener { Log.w("ChoreMe", "Couldn't delete note", it) }
    }
}
