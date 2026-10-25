package com.choreme.core.model

/** Failures the person can do something about. :core:ui turns each into a message. */
enum class DomainError {
    OFFLINE,
    NAME_REQUIRED,
    FAMILY_NAME_REQUIRED,
    INVITE_CODE_INVALID,
    INVITE_CODE_NOT_FOUND,
    LAST_PARENT,
    RECENT_SIGN_IN_REQUIRED,
}

class DomainException(val error: DomainError) : Exception(error.name)
