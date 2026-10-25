package com.choreme.core.data

import com.choreme.core.data.remote.FirestoreSchema
import com.choreme.core.data.remote.FirestoreSchema.MEMBER_ID
import com.choreme.core.data.remote.FirestoreSchema.URL
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Lazy
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Each family phone's wake-up address, one document per signed-in device (see firestore.rules). */
@Singleton
class PushEndpointRepository @Inject constructor(
    private val auth: AuthRepository,
    private val firestore: Lazy<FirebaseFirestore>,
) {
    private fun endpoints(familyId: String) = firestore.get()
        .collection(FirestoreSchema.FAMILIES).document(familyId)
        .collection(FirestoreSchema.PUSH_ENDPOINTS)

    /** Records this phone's address; written only when it changes. */
    suspend fun register(familyId: String, memberId: String, url: String) {
        val uid = auth.currentAccount()?.uid ?: return
        val ref = endpoints(familyId).document(uid)
        val current = ref.get().await()
        if (current.getString(URL) == url && current.getString(MEMBER_ID) == memberId) return
        ref.set(mapOf(MEMBER_ID to memberId, URL to url)).await()
    }

    suspend fun unregister(familyId: String) {
        val uid = auth.currentAccount()?.uid ?: return
        endpoints(familyId).document(uid).delete().await()
    }

    /** Addresses of the phones of [memberIds], e.g. the parents. */
    suspend fun of(familyId: String, memberIds: Set<String>): List<String> =
        endpoints(familyId).get().await().documents
            .filter { it.getString(MEMBER_ID) in memberIds }
            .mapNotNull { it.getString(URL) }

    /** Every phone in the family; what someone waiting to join can ping (see firestore.rules). */
    suspend fun all(familyId: String): List<String> =
        endpoints(familyId).get().await().documents.mapNotNull { it.getString(URL) }

    /** Addresses of the family's other members' phones. */
    suspend fun othersThan(familyId: String, memberId: String): List<String> =
        endpoints(familyId).get().await().documents
            .filter { it.getString(MEMBER_ID) != memberId }
            .mapNotNull { it.getString(URL) }
}
