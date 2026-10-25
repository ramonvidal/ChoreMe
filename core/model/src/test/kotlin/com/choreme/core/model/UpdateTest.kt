package com.choreme.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class UpdateTest {
    private val update = AppUpdate(buildNumber = 31, versionName = "0.31", downloadUrl = "https://x/ChoreMe.apk", sizeBytes = 1)
    private val now = Instant.parse("2026-10-25T10:00:00Z")

    @Test
    fun `release tags give the build number`() {
        assertEquals(31, Updates.buildNumber("v0.31"))
        assertNull(Updates.buildNumber("latest"))
        assertNull(Updates.buildNumber("v1.2.3"))
    }

    @Test
    fun `only newer builds are offered, and Later lasts until the next one`() {
        assertTrue(Updates.shouldOffer(update, installedBuild = 29, dismissedBuild = null))
        assertFalse(Updates.shouldOffer(update, installedBuild = 31, dismissedBuild = null))
        assertFalse(Updates.shouldOffer(update, installedBuild = 29, dismissedBuild = 31))
        assertTrue(Updates.shouldOffer(update.copy(buildNumber = 32), installedBuild = 29, dismissedBuild = 31))
    }

    @Test
    fun `checks happen at most once a day`() {
        assertTrue(Updates.isCheckDue(null, now))
        assertFalse(Updates.isCheckDue(now.minus(Duration.ofHours(3)), now))
        assertTrue(Updates.isCheckDue(now.minus(Duration.ofHours(25)), now))
    }
}
