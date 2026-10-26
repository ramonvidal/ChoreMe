package com.choreme.core.data

import android.util.Log
import com.choreme.core.data.remote.FirestoreSchema
import com.choreme.core.data.remote.FirestoreSchema.AT
import com.choreme.core.data.remote.FirestoreSchema.KIND
import com.choreme.core.data.remote.FirestoreSchema.MEMBER_ID
import com.choreme.core.data.remote.documentsFlow
import com.choreme.core.model.Announcement
import com.choreme.core.model.AnnouncementKind
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Lazy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** "At the shop", "on my way home": one document per person and kind (see firestore.rules). */
@Singleton
class AnnouncementRepository @Inject constructor(
    private val firestore: Lazy<FirebaseFirestore>,
) {
    private fun announcements(familyId: String) = firestore.get()
        .collection(FirestoreSchema.FAMILIES).document(familyId)
        .collection(FirestoreSchema.ANNOUNCEMENTS)

    private fun ref(familyId: String, memberId: String, kind: AnnouncementKind) =
        announcements(familyId).document("${memberId}_${kind.name}")

    fun announce(familyId: String, memberId: String, kind: AnnouncementKind, at: Instant = Instant.now()) {
        ref(familyId, memberId, kind)
            .set(mapOf(MEMBER_ID to memberId, KIND to kind.name, AT to at.toEpochMilli()))
            .addOnFailureListener { Log.w("ChoreMe", "Couldn't share $kind", it) }
    }

    fun clear(familyId: String, memberId: String, kind: AnnouncementKind) {
        ref(familyId, memberId, kind).delete().addOnFailureListener { Log.w("ChoreMe", "Couldn't clear $kind", it) }
    }

    fun observe(familyId: String): Flow<List<Announcement>> =
        announcements(familyId).documentsFlow().map { docs -> docs.mapNotNull { it.toAnnouncement() } }

    /**
     * The news with the time it was worked out at, again every minute, so screens can drop
     * old news (see [Announcement.isCurrent]) without waiting for a change.
     */
    fun observeCurrent(familyId: String): Flow<Pair<List<Announcement>, Instant>> =
        combine(observe(familyId), minuteTicks()) { news, now -> news to now }

    private fun minuteTicks() = flow {
        while (true) {
            emit(Instant.now())
            delay(60_000)
        }
    }

    /** For background workers: one read from the server. */
    suspend fun all(familyId: String): List<Announcement> =
        announcements(familyId).get().await().documents.mapNotNull { it.toAnnouncement() }
}

private fun DocumentSnapshot.toAnnouncement(): Announcement? {
    val kind = runCatching { AnnouncementKind.valueOf(getString(KIND).orEmpty()) }.getOrNull() ?: return null
    return Announcement(getString(MEMBER_ID) ?: return null, kind, Instant.ofEpochMilli(getLong(AT) ?: return null))
}
