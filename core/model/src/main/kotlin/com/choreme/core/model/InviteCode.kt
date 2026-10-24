package com.choreme.core.model

import kotlin.random.Random

object InviteCode {
    const val LENGTH = 6

    // No 0/O, 1/I/L: codes get read aloud and typed on phones.
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    fun generate(random: Random = Random.Default): String =
        String(CharArray(LENGTH) { ALPHABET[random.nextInt(ALPHABET.length)] })

    /** Accepts what people actually type: lowercase, spaces, dashes. */
    fun normalize(input: String): String =
        input.uppercase().filter { it.isLetterOrDigit() }

    fun isValid(code: String): Boolean =
        code.length == LENGTH && code.all { it in ALPHABET }
}
