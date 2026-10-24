package com.choreme.core.model.session

import com.choreme.core.model.Role

/** Where the signed-in person stands with respect to a family; drives the top-level screen. */
sealed interface SessionState {
    data object Loading : SessionState

    /** Built without app/google-services.json, so there's nothing to sync with. */
    data object FirebaseNotConfigured : SessionState

    data object SignedOut : SessionState

    data class NoFamily(
        val isKid: Boolean,
        val suggestedName: String,
        /** They had asked to join (or were a member) and that's no longer the case. */
        val requestDeclined: Boolean,
    ) : SessionState

    data class AwaitingApproval(val familyId: String, val familyName: String) : SessionState

    data class InFamily(val familyId: String, val memberId: String, val role: Role) : SessionState
}

data class Account(val uid: String, val isAnonymous: Boolean, val displayName: String?)

data class FamilyPointer(val familyId: String, val familyName: String)

data class Membership(val memberId: String, val role: Role)

/** A remote document as seen so far: [Unknown] until the server has confirmed it. */
sealed interface Remote<out T> {
    data object Unknown : Remote<Nothing>
    data object Missing : Remote<Nothing>
    data class Present<T>(val value: T) : Remote<T>
}

fun resolveSession(
    account: Account?,
    pointer: Remote<FamilyPointer>,
    membership: Remote<Membership>,
    joinRequest: Remote<Unit>,
): SessionState {
    if (account == null) return SessionState.SignedOut
    val noFamily = { declined: Boolean ->
        SessionState.NoFamily(
            isKid = account.isAnonymous,
            suggestedName = account.displayName?.substringBefore(' ').orEmpty(),
            requestDeclined = declined,
        )
    }
    return when (pointer) {
        Remote.Unknown -> SessionState.Loading
        Remote.Missing -> noFamily(false)
        is Remote.Present -> when {
            membership is Remote.Present ->
                SessionState.InFamily(pointer.value.familyId, membership.value.memberId, membership.value.role)
            membership == Remote.Unknown || joinRequest == Remote.Unknown -> SessionState.Loading
            joinRequest is Remote.Present ->
                SessionState.AwaitingApproval(pointer.value.familyId, pointer.value.familyName)
            else -> noFamily(true)
        }
    }
}
