package com.choreme.core.weather

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AirParserTest {
    @Test
    fun `reads the index and the strongest pollen`() {
        val json = """{"hourly":{"time":["2026-04-10T08:00","2026-04-10T09:00"],"us_aqi":[42,null],
            "birch_pollen":[10.0,null],"grass_pollen":[55.5,null],"alder_pollen":[null,null]}}"""
        val air = AirParser.parse(json, Instant.EPOCH)
        assertEquals(42, air.hours[0].usAqi)
        assertEquals(55.5, air.hours[0].pollen!!, 0.0)
        assertNull(air.hours[1].usAqi)
        assertNull(air.hours[1].pollen)
    }

    @Test
    fun `a place without pollen data still has its air`() {
        val air = AirParser.parse("""{"hourly":{"time":["2026-04-10T08:00"],"us_aqi":[80]}}""", Instant.EPOCH)
        assertEquals(80, air.hours.single().usAqi)
        assertNull(air.hours.single().pollen)
    }
}
