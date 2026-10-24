package com.choreme.data.remote

import com.choreme.domain.model.Family
import com.choreme.domain.model.JoinRequest
import com.choreme.domain.model.Member
import com.choreme.domain.model.Role
import com.choreme.domain.session.FamilyPointer
import com.choreme.domain.session.Membership
import com.google.firebase.firestore.DocumentSnapshot

/** Collection and field names; must match firebase/firestore.rules. */
object FirestoreSchema {
    const val USERS = "users"
    const val INVITE_CODES = "inviteCodes"
    const val FAMILIES = "families"
    const val MEMBERS = "members"
    const val MEMBER_UIDS = "memberUids"
    const val JOIN_REQUESTS = "joinRequests"

    const val FAMILY_ID = "familyId"
    const val FAMILY_NAME = "familyName"
    const val NAME = "name"
    const val INVITE_CODE = "inviteCode"
    const val CREATED_BY = "createdBy"
    const val CREATED_AT = "createdAt"
    const val ROLE = "role"
    const val COLOR = "color"
    const val UIDS = "uids"
    const val MEMBER_ID = "memberId"
    const val DEVICE_NAME = "deviceName"
}

private fun DocumentSnapshot.role(): Role =
    runCatching { Role.valueOf(getString(FirestoreSchema.ROLE).orEmpty()) }.getOrDefault(Role.KID)

fun DocumentSnapshot.toFamilyPointer(): FamilyPointer? {
    val familyId = getString(FirestoreSchema.FAMILY_ID) ?: return null
    return FamilyPointer(familyId, getString(FirestoreSchema.FAMILY_NAME).orEmpty())
}

fun DocumentSnapshot.toMembership() = Membership(
    memberId = getString(FirestoreSchema.MEMBER_ID) ?: id,
    role = role(),
)

fun DocumentSnapshot.toFamily() = Family(
    id = id,
    name = getString(FirestoreSchema.NAME).orEmpty(),
    inviteCode = getString(FirestoreSchema.INVITE_CODE).orEmpty(),
)

fun DocumentSnapshot.toMember() = Member(
    id = id,
    name = getString(FirestoreSchema.NAME).orEmpty(),
    role = role(),
    color = getLong(FirestoreSchema.COLOR)?.toInt() ?: 0,
    uids = (get(FirestoreSchema.UIDS) as? List<*>)?.filterIsInstance<String>().orEmpty(),
)

fun DocumentSnapshot.toJoinRequest() = JoinRequest(
    uid = id,
    name = getString(FirestoreSchema.NAME).orEmpty(),
    role = role(),
    deviceName = getString(FirestoreSchema.DEVICE_NAME).orEmpty(),
)
