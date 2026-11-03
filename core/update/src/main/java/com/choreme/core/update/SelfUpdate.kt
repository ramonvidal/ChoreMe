package com.choreme.core.update

/**
 * Whether this build may update itself from the project's release page. The build installed from
 * Google Play must not (Google updates it, and its rules forbid the rest), so `:app` says which,
 * per distribution flavor: `github` yes, `play` no.
 */
class SelfUpdate(val enabled: Boolean)
