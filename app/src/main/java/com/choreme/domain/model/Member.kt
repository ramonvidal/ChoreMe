package com.choreme.domain.model

enum class Role { PARENT, KID }

data class Member(
    val id: String,
    val name: String,
    val role: Role,
)
