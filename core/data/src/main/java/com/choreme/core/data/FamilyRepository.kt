package com.choreme.core.data

import android.os.Build
import com.choreme.core.data.remote.FirestoreSchema
import com.choreme.core.data.remote.FirestoreSchema.COLOR
import com.choreme.core.data.remote.FirestoreSchema.CREATED_AT
import com.choreme.core.data.remote.FirestoreSchema.CREATED_BY
import com.choreme.core.data.remote.FirestoreSchema.DEVICE_NAME
import com.choreme.core.data.remote.FirestoreSchema.FAMILY_ID
import com.choreme.core.data.remote.FirestoreSchema.FAMILY_NAME
import com.choreme.core.data.remote.FirestoreSchema.INVITE_CODE
import com.choreme.core.data.remote.FirestoreSchema.MEMBER_ID
import com.choreme.core.data.remote.FirestoreSchema.NAME
import com.choreme.core.data.remote.FirestoreSchema.ROLE
import com.choreme.core.data.remote.FirestoreSchema.UIDS
import com.choreme.core.data.remote.documentsFlow
import com.choreme.core.data.remote.map
import com.choreme.core.data.remote.remoteFlow
import com.choreme.core.data.remote.toFamily
import com.choreme.core.data.remote.toFamilyPointer
import com.choreme.core.data.remote.toJoinRequest
import com.choreme.core.data.remote.toMember
import com.choreme.core.data.remote.toMembership
import com.choreme.core.model.DomainError
import com.choreme.core.model.DomainException
import com.choreme.core.model.Family
import com.choreme.core.model.InviteCode
import com.choreme.core.model.JoinRequest
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.session.FamilyPointer
import com.choreme.core.model.session.Remote
import com.choreme.core.model.session.SessionState
import com.choreme.core.model.session.resolveSession
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Families, their members and join requests. The data model is described in PLAN.md. */
@Singleton
class FamilyRepository @Inject constructor(
    private val auth: AuthRepository,
    private val firestore: Lazy<FirebaseFirestore>,
) {
    private val db get() = firestore.get()
    private val families get() = db.collection(FirestoreSchema.FAMILIES)
    private fun family(familyId: String) = families.document(familyId)
    private fun members(familyId: String) = family(familyId).collection(FirestoreSchema.MEMBERS)
    private fun memberUids(familyId: String) = family(familyId).collection(FirestoreSchema.MEMBER_UIDS)
    private fun joinRequests(familyId: String) = family(familyId).collection(FirestoreSchema.JOIN_REQUESTS)
    private fun userPointer(uid: String) = db.collection(FirestoreSchema.USERS).document(uid)

    private fun requireUid(): String = checkNotNull(auth.currentAccount()) { "Not signed in" }.uid

    val session: Flow<SessionState> = auth.account.flatMapLatest { account ->
        if (account == null) return@flatMapLatest flowOf(SessionState.SignedOut)
        userPointer(account.uid).remoteFlow()
            .map { it.toPointer() }
            .distinctUntilChanged()
            .flatMapLatest { pointer ->
                if (pointer !is Remote.Present) {
                    flowOf(resolveSession(account, pointer, Remote.Missing, Remote.Missing))
                } else {
                    val familyId = pointer.value.familyId
                    combine(
                        memberUids(familyId).document(account.uid).remoteFlow().map { r -> r.map { it.toMembership() } },
                        joinRequests(familyId).document(account.uid).remoteFlow().map { r -> r.map { } },
                    ) { membership, request -> resolveSession(account, pointer, membership, request) }
                }
            }
    }.distinctUntilChanged()

    private fun Remote<DocumentSnapshot>.toPointer(): Remote<FamilyPointer> = when (this) {
        Remote.Unknown -> Remote.Unknown
        Remote.Missing -> Remote.Missing
        is Remote.Present -> value.toFamilyPointer()?.let { Remote.Present(it) } ?: Remote.Missing
    }

    fun observeFamily(familyId: String): Flow<Family?> =
        family(familyId).remoteFlow().map { (it as? Remote.Present)?.value?.toFamily() }.distinctUntilChanged()

    /** Parents first, then by name. */
    fun observeMembers(familyId: String): Flow<List<Member>> =
        members(familyId).documentsFlow().map { docs ->
            docs.map { it.toMember() }.sortedWith(compareBy({ it.role != Role.PARENT }, { it.name.lowercase() }))
        }

    fun observeJoinRequests(familyId: String): Flow<List<JoinRequest>> =
        joinRequests(familyId).documentsFlow().map { docs -> docs.map { it.toJoinRequest() } }

    suspend fun createFamily(familyName: String, yourName: String) {
        val uid = requireUid()
        val familyRef = families.document()
        repeat(CODE_ATTEMPTS) { attempt ->
            val code = InviteCode.generate()
            val batch = db.batch()
            batch.set(familyRef, mapOf(NAME to familyName, INVITE_CODE to code, CREATED_BY to uid, CREATED_AT to FieldValue.serverTimestamp()))
            batch.set(db.collection(FirestoreSchema.INVITE_CODES).document(code), mapOf(FAMILY_ID to familyRef.id, FAMILY_NAME to familyName))
            batch.set(memberUids(familyRef.id).document(uid), mapOf(MEMBER_ID to uid, ROLE to Role.PARENT.name))
            batch.set(members(familyRef.id).document(uid), memberData(yourName, Role.PARENT, color = 0, uids = listOf(uid)))
            batch.set(userPointer(uid), mapOf(FAMILY_ID to familyRef.id, FAMILY_NAME to familyName))
            try {
                batch.commit().await()
                return
            } catch (e: FirebaseFirestoreException) {
                // An existing invite code can't be overwritten, so a clash shows up as permission denied.
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED || attempt == CODE_ATTEMPTS - 1) throw e
            }
        }
    }

    suspend fun requestToJoin(code: String, yourName: String, role: Role) {
        val uid = requireUid()
        val invite = try {
            db.collection(FirestoreSchema.INVITE_CODES).document(code).get(Source.SERVER).await()
        } catch (e: FirebaseFirestoreException) {
            throw DomainException(DomainError.OFFLINE)
        }
        if (!invite.exists()) throw DomainException(DomainError.INVITE_CODE_NOT_FOUND)
        val familyId = invite.getString(FAMILY_ID) ?: throw DomainException(DomainError.INVITE_CODE_NOT_FOUND)

        val batch = db.batch()
        batch.set(
            joinRequests(familyId).document(uid),
            mapOf(NAME to yourName, ROLE to role.name, DEVICE_NAME to deviceName(), CREATED_AT to FieldValue.serverTimestamp()),
        )
        batch.set(userPointer(uid), mapOf(FAMILY_ID to familyId, FAMILY_NAME to invite.getString(FAMILY_NAME).orEmpty()))
        batch.commit().await()
    }

    suspend fun cancelJoinRequest(familyId: String) {
        val uid = requireUid()
        val batch = db.batch()
        batch.delete(joinRequests(familyId).document(uid))
        batch.delete(userPointer(uid))
        batch.commit().await()
    }

    /**
     * Lets someone in. A kid is either linked to an existing kid profile ([linkToKidId])
     * or gets a new profile; a parent always gets their own member entry.
     */
    suspend fun approve(familyId: String, request: JoinRequest, linkToKidId: String?, color: Int) {
        val batch = db.batch()
        val memberId = when {
            request.role == Role.PARENT -> request.uid.also {
                batch.set(members(familyId).document(it), memberData(request.name, Role.PARENT, color, listOf(request.uid)))
            }
            linkToKidId != null -> linkToKidId.also {
                batch.update(members(familyId).document(it), UIDS, FieldValue.arrayUnion(request.uid))
            }
            else -> members(familyId).document().id.also {
                batch.set(members(familyId).document(it), memberData(request.name, Role.KID, color, listOf(request.uid)))
            }
        }
        batch.set(memberUids(familyId).document(request.uid), mapOf(MEMBER_ID to memberId, ROLE to request.role.name))
        batch.delete(joinRequests(familyId).document(request.uid))
        batch.commit().await()
    }

    suspend fun decline(familyId: String, request: JoinRequest) {
        joinRequests(familyId).document(request.uid).delete().await()
    }

    /** For kids without a device of their own; parents mark their chores done for them. */
    suspend fun addKidProfile(familyId: String, name: String, color: Int) {
        members(familyId).document().set(memberData(name, Role.KID, color, emptyList())).await()
    }

    /** Removes the member, signs all their devices out of the family and deletes their shared location. */
    suspend fun removeMember(familyId: String, member: Member) {
        val batch = db.batch()
        member.uids.forEach {
            batch.delete(memberUids(familyId).document(it))
            batch.delete(family(familyId).collection(FirestoreSchema.PUSH_ENDPOINTS).document(it))
        }
        batch.delete(members(familyId).document(member.id))
        batch.delete(family(familyId).collection(FirestoreSchema.LOCATIONS).document(member.id))
        batch.commit().await()
    }

    private fun memberData(name: String, role: Role, color: Int, uids: List<String>) =
        mapOf(NAME to name, ROLE to role.name, COLOR to color, UIDS to uids)

    private fun deviceName(): String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    private companion object {
        const val CODE_ATTEMPTS = 3
    }
}
