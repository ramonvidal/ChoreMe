package com.choreme.data

import androidx.annotation.StringRes

/** A failure with a message worth showing as-is, rather than a generic "something went wrong". */
class UserFacingException(@StringRes val messageRes: Int) : Exception()
