package com.choreme.core.settings

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.AlertKind
import com.choreme.core.model.QuietHours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalTime

/** Which alerts this phone shows and its quiet hours survive the app being closed (they're read back from storage). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class NotificationSettingsTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `everything is on and nothing is quiet at first`() {
        val settings = NotificationSettings(context)
        assertTrue(AlertKind.entries.all { settings.isEnabled(it) })
        assertNull(settings.settings.value.quietHours)
    }

    @Test
    fun `turned-off alerts and quiet hours are kept`() {
        NotificationSettings(context).apply {
            setEnabled(AlertKind.MESSAGES, false)
            setQuietHours(QuietHours(LocalTime.of(21, 30), LocalTime.of(6, 45)))
        }
        val again = NotificationSettings(context)
        assertFalse(again.isEnabled(AlertKind.MESSAGES))
        assertTrue(again.isEnabled(AlertKind.ARRIVALS))
        assertEquals(QuietHours(LocalTime.of(21, 30), LocalTime.of(6, 45)), again.settings.value.quietHours)

        again.setEnabled(AlertKind.MESSAGES, true)
        again.setQuietHours(null)
        val third = NotificationSettings(context)
        assertTrue(third.isEnabled(AlertKind.MESSAGES))
        assertNull(third.settings.value.quietHours)
    }
}
