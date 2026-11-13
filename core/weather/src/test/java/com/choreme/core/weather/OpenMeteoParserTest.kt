package com.choreme.core.weather

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.location.GeoPoint
import com.choreme.core.model.weather.Sky
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class OpenMeteoParserTest {
    private val json = """
        {"latitude":52.52,"longitude":13.41,"timezone":"Europe/Berlin","hourly":{
          "time":["2026-11-16T07:00","2026-11-16T08:00","2026-11-16T09:00"],
          "temperature_2m":[4.5,5.0,null],
          "apparent_temperature":[2.0,3.1,4.0],
          "precipitation_probability":[10,70,90],
          "precipitation":[0.0,1.2,2.5],
          "wind_speed_10m":[12.0,15.5,20.0],
          "wind_gusts_10m":[25.0,30.0,40.0],
          "relative_humidity_2m":[80,85,90],
          "uv_index":[0.0,0.5,1.0],
          "weather_code":[3,63,65],
          "is_day":[0,1,1]}}
    """.trimIndent()

    @Test
    fun `reads the hours and their numbers`() {
        val forecast = OpenMeteoParser.parse(json, Instant.EPOCH)
        // The third hour has no temperature, so it is left out rather than guessed.
        assertEquals(2, forecast.hours.size)
        val eight = forecast.hours[1]
        assertEquals(LocalDateTime.of(2026, 11, 16, 8, 0), eight.time)
        assertEquals(5.0, eight.temperatureC, 0.0)
        assertEquals(3.1, eight.feelsLikeC!!, 0.0)
        assertEquals(70, eight.rainProbability)
        assertEquals(1.2, eight.rainMm, 0.0)
        assertEquals(30.0, eight.gustKmh, 0.0)
        assertEquals(Sky.RAIN, eight.sky)
        assertTrue(eight.isDay)
        assertTrue(!forecast.hours[0].isDay)
    }

    @Test
    fun `missing fields get calm defaults`() {
        val minimal = """{"hourly":{"time":["2026-11-16T07:00"],"temperature_2m":[9.0]}}"""
        val hour = OpenMeteoParser.parse(minimal, Instant.EPOCH).hours.single()
        assertEquals(0, hour.rainProbability)
        assertEquals(0.0, hour.rainMm, 0.0)
        assertEquals(Sky.CLEAR, hour.sky)
        assertNull(hour.feelsLikeC)
    }

    @Test
    fun `coordinates are rounded to about a kilometre`() {
        val coarse = WeatherRepository.coarse(GeoPoint(52.523456, 13.405678))
        assertEquals(52.52, coarse.latitude, 0.0)
        assertEquals(13.41, coarse.longitude, 0.0)
        assertEquals("52.52,13.41", WeatherRepository.key(GeoPoint(52.5249, 13.4051)))
    }
}
