package com.choreme.core.data

import com.choreme.core.data.remote.FirestoreSchema
import com.choreme.core.data.remote.FirestoreSchema.DESCRIPTION
import com.choreme.core.data.remote.FirestoreSchema.NAME
import com.choreme.core.data.remote.FirestoreSchema.PHOTO
import com.choreme.core.data.remote.FirestoreSchema.TYPE
import com.choreme.core.data.remote.documentsFlow
import com.choreme.core.model.room.Room
import com.choreme.core.model.room.RoomPhoto
import com.choreme.core.model.room.RoomType
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The house's rooms. A room's picture is a small JPEG kept in the room's own document (Cloud
 * Storage needs a paid plan). Its tasks are chores that carry the room's id, so they are
 * written through [ChoreRepository]. Writes don't wait for the server.
 */
@Singleton
class RoomRepository @Inject constructor(
    private val firestore: Lazy<FirebaseFirestore>,
) {
    private fun rooms(familyId: String) = firestore.get()
        .collection(FirestoreSchema.FAMILIES).document(familyId)
        .collection(FirestoreSchema.ROOMS)

    fun observe(familyId: String): Flow<List<Room>> =
        rooms(familyId).documentsFlow().map { docs -> docs.mapNotNull { it.toRoom() } }

    /** Adds [room] (its id is ignored) and returns the new room's id. */
    fun add(familyId: String, room: Room): String {
        val ref = rooms(familyId).document()
        ref.set(room.toFields()).reportFailure("Couldn't add room")
        return ref.id
    }

    fun update(familyId: String, room: Room) {
        rooms(familyId).document(room.id).set(room.toFields()).reportFailure("Couldn't save room")
    }

    fun delete(familyId: String, roomId: String) {
        rooms(familyId).document(roomId).delete().reportFailure("Couldn't delete room")
    }
}

private fun Room.toFields(): Map<String, Any?> = mapOf(
    NAME to name,
    TYPE to type.name,
    DESCRIPTION to description,
    PHOTO to photo?.let { Blob.fromBytes(it.jpeg) },
)

private fun DocumentSnapshot.toRoom(): Room? {
    val name = getString(NAME) ?: return null
    return Room(
        id = id,
        name = name,
        type = runCatching { RoomType.valueOf(getString(TYPE).orEmpty()) }.getOrDefault(RoomType.OTHER),
        description = getString(DESCRIPTION).orEmpty(),
        photo = getBlob(PHOTO)?.toBytes()?.let(::RoomPhoto),
    )
}
