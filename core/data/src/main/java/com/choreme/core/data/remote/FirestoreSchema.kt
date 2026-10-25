package com.choreme.core.data.remote

import com.choreme.core.model.grocery.GroceryCategory
import com.choreme.core.model.grocery.GroceryItem
import com.choreme.core.model.Family
import com.choreme.core.model.JoinRequest
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.session.FamilyPointer
import com.choreme.core.model.session.Membership
import com.google.firebase.firestore.DocumentSnapshot

/** Collection and field names; must match firebase/firestore.rules. */
object FirestoreSchema {
    const val USERS = "users"
    const val INVITE_CODES = "inviteCodes"
    const val FAMILIES = "families"
    const val MEMBERS = "members"
    const val MEMBER_UIDS = "memberUids"
    const val JOIN_REQUESTS = "joinRequests"
    const val GROCERY_ITEMS = "groceryItems"
    const val CHORES = "chores"
    const val CHORE_TEMPLATES = "choreTemplates"
    const val LOCATIONS = "locations"
    const val PLACES = "places"
    const val PLACE_EVENTS = "placeEvents"
    const val PUSH_ENDPOINTS = "pushEndpoints"

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
    const val CATEGORY = "category"
    const val IS_NEEDED = "isNeeded"
    const val IS_STAPLE = "isStaple"
    const val UPDATED_AT = "updatedAt"
    const val UPDATED_BY = "updatedBy"

    // Chores
    const val TITLE = "title"
    const val TEMPLATE = "template"
    const val RECURRENCE_TYPE = "recurrenceType"
    const val EVERY_DAYS = "everyDays"
    const val WEEK_DAYS = "weekDays"
    const val DUE_DATE = "dueDate"
    const val STATUS = "status"
    const val CLAIMED_BY = "claimedBy"
    const val COMPLETED_BY = "completedBy"
    const val APPROVED_BY = "approvedBy"
    const val LAST_DONE_BY = "lastDoneBy"
    const val LAST_DONE_ON = "lastDoneOn"
    const val NOTIFY_WHEN_DONE = "notifyWhenDone"

    // Locations and places
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val ACCURACY = "accuracy"
    const val AT = "at"
    const val RADIUS = "radius"
    const val PLACE_ID = "placeId"
    const val PLACE_NAME = "placeName"
    const val TYPE = "type"
    const val URL = "url"
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

fun DocumentSnapshot.toGroceryItem() = GroceryItem(
    id = id,
    name = getString(FirestoreSchema.NAME).orEmpty(),
    category = runCatching { GroceryCategory.valueOf(getString(FirestoreSchema.CATEGORY).orEmpty()) }
        .getOrDefault(GroceryCategory.OTHER),
    isNeeded = getBoolean(FirestoreSchema.IS_NEEDED) ?: false,
    isStaple = getBoolean(FirestoreSchema.IS_STAPLE) ?: false,
)
