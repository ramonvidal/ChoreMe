package com.choreme.core.model.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class WeatherTest {
    private val monday = LocalDate.of(2026, 11, 16)
    private fun hour(
        h: Int,
        temp: Double = 12.0,
        rain: Int = 0,
        mm: Double = 0.0,
        wind: Double = 10.0,
        gust: Double = 15.0,
        humidity: Int = 60,
        uv: Double = 1.0,
        code: Int = 0,
        day: Boolean = true,
    ) = WeatherHour(monday.atTime(h, 0), temp, null, rain, mm, wind, gust, humidity, uv, code, day)

    // -- sky and units

    @Test
    fun `weather codes become a sky`() {
        assertEquals(Sky.CLEAR, skyOf(0))
        assertEquals(Sky.PARTLY_CLOUDY, skyOf(2))
        assertEquals(Sky.CLOUDY, skyOf(3))
        assertEquals(Sky.FOG, skyOf(45))
        assertEquals(Sky.DRIZZLE, skyOf(53))
        assertEquals(Sky.RAIN, skyOf(63))
        assertEquals(Sky.RAIN, skyOf(81))
        assertEquals(Sky.SNOW, skyOf(73))
        assertEquals(Sky.STORM, skyOf(95))
    }

    @Test
    fun `units default to celsius and km per hour`() {
        val units = WeatherUnits()
        assertEquals(12, units.temperature(11.6))
        assertEquals(30, units.speed(30.2))
        assertEquals(1.5, units.rain(1.46), 0.0001)
    }

    @Test
    fun `units convert when asked`() {
        val units = WeatherUnits(TemperatureUnit.FAHRENHEIT, SpeedUnit.MPH, RainUnit.INCH)
        assertEquals(32, units.temperature(0.0))
        assertEquals(68, units.temperature(20.0))
        assertEquals(62, units.speed(100.0))
        assertEquals(0.39, units.rain(10.0), 0.0001)
        assertEquals(10, WeatherUnits(speed = SpeedUnit.MS).speed(36.0))
    }

    // -- forecast windows

    @Test
    fun `a window takes the hour it starts in up to the hour it ends in`() {
        val forecast = Forecast((6..12).map { hour(it) }, Instant.EPOCH)
        val window = forecast.between(monday.atTime(7, 40), monday.atTime(8, 20))
        assertEquals(listOf(7, 8), window.map { it.time.hour })
        assertEquals(9, forecast.at(monday.atTime(9, 30))?.time?.hour)
        assertNull(forecast.at(monday.atTime(20, 0)))
    }

    // -- advice

    @Test
    fun `a dry mild trip is quiet`() {
        val advice = adviceFor(listOf(hour(8), hour(9)), TripMode.CAR)
        assertTrue(advice.isQuiet)
        assertTrue(advice.gear.isEmpty())
    }

    @Test
    fun `rain on foot means an umbrella, on a bike a rain jacket`() {
        val wet = listOf(hour(8, rain = 70, mm = 1.2, code = 63))
        val walk = adviceFor(wet, TripMode.WALK)
        assertTrue(Concern.RAIN in walk.concerns)
        assertEquals(listOf(Gear.UMBRELLA), walk.gear)
        assertEquals(Concern.RAIN, walk.headline)
        assertEquals(listOf(Gear.RAIN_JACKET), adviceFor(wet, TripMode.BIKE).gear)
    }

    @Test
    fun `a small chance of drizzle is not worth a word`() {
        assertTrue(adviceFor(listOf(hour(8, rain = 30, mm = 0.1)), TripMode.WALK).isQuiet)
    }

    @Test
    fun `heavy rain by car leaves earlier`() {
        val advice = adviceFor(listOf(hour(8, rain = 90, mm = 6.0, code = 65)), TripMode.CAR)
        assertTrue(Concern.HEAVY_RAIN in advice.concerns)
        assertEquals(10, advice.leaveEarlierMinutes)
        assertEquals(Concern.HEAVY_RAIN, advice.headline)
    }

    @Test
    fun `snow and ice make the way slow for cars and buses only`() {
        val snow = listOf(hour(7, temp = -2.0, rain = 80, mm = 1.0, code = 73))
        assertEquals(15, adviceFor(snow, TripMode.CAR).leaveEarlierMinutes)
        assertEquals(0, adviceFor(snow, TripMode.BIKE).leaveEarlierMinutes)
        assertTrue(Concern.SNOW in adviceFor(snow, TripMode.WALK).concerns)
    }

    @Test
    fun `freezing and wet is ice`() {
        val advice = adviceFor(listOf(hour(7, temp = -1.0, rain = 60, mm = 0.6, code = 61)), TripMode.BIKE)
        assertTrue(Concern.ICE in advice.concerns)
        assertTrue(Concern.FROST in advice.concerns)
        assertEquals(Concern.ICE, advice.headline)
    }

    @Test
    fun `cold is judged colder for people outdoors`() {
        val cool = listOf(hour(8, temp = 7.0))
        assertTrue(Concern.COLD in adviceFor(cool, TripMode.BIKE).concerns)
        assertTrue(Concern.COLD !in adviceFor(cool, TripMode.CAR).concerns)
        assertEquals(listOf(Gear.WARM_JACKET), adviceFor(cool, TripMode.BIKE).gear)
    }

    @Test
    fun `wind matters most on a bike`() {
        val windy = listOf(hour(8, wind = 25.0, gust = 40.0))
        assertTrue(Concern.WIND in adviceFor(windy, TripMode.BIKE).concerns)
        assertTrue(Concern.WIND !in adviceFor(windy, TripMode.CAR).concerns)
    }

    @Test
    fun `strong sun and heat want sun protection`() {
        val hot = listOf(hour(13, temp = 33.0, uv = 8.0))
        val advice = adviceFor(hot, TripMode.WALK)
        assertTrue(Concern.HEAT in advice.concerns && Concern.UV in advice.concerns)
        assertEquals(listOf(Gear.SUN_PROTECTION), advice.gear)
    }

    @Test
    fun `at most two things to bring`() {
        val awful = listOf(hour(8, temp = 2.0, rain = 90, mm = 2.0, uv = 7.0, code = 63))
        assertTrue(adviceFor(awful, TripMode.WALK).gear.size <= 2)
    }

    @Test
    fun `the whole trip counts not only the start`() {
        // Dry at 8:00, pouring at 9:00: a trip from 8:30 to 9:15 gets wet.
        val hours = listOf(hour(8), hour(9, rain = 90, mm = 3.0, code = 63))
        assertTrue(Concern.RAIN in adviceFor(hours, TripMode.WALK).concerns)
        assertTrue(adviceFor(hours.take(1), TripMode.WALK).isQuiet)
    }

    @Test
    fun `no forecast no advice`() {
        assertTrue(adviceFor(emptyList(), TripMode.BIKE).isQuiet)
    }

    // -- plan

    private val work = PlanEntry("w", "ramon", "Work", setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), LocalTime.of(8, 0), 30, TripMode.BIKE, "work")
    private val gym = PlanEntry("g", "ramon", "Gym", setOf(DayOfWeek.MONDAY), LocalTime.of(18, 30), 90, TripMode.WALK, "gym")
    private val saturday = PlanEntry("s", "ramon", "Market", setOf(DayOfWeek.SATURDAY), LocalTime.of(10, 0))

    @Test
    fun `entries of a day come in the order of the day`() {
        assertEquals(listOf("w", "g"), entriesOn(monday, listOf(gym, saturday, work)).map { it.id })
        assertEquals(listOf("s"), entriesOn(monday.plusDays(5), listOf(gym, saturday, work)).map { it.id })
    }

    @Test
    fun `moments carry their advice and skip entries without a forecast`() {
        val moments = momentsOn(monday, listOf(work, gym)) { entry, _, _ ->
            if (entry.id == "w") listOf(hour(8, rain = 80, mm = 1.5, code = 63)) else emptyList()
        }
        assertEquals(1, moments.size)
        assertEquals(monday.atTime(8, 30), moments[0].end)
        assertEquals(Concern.RAIN, moments[0].advice.headline)
    }

    @Test
    fun `a quiet day has nothing worth saying`() {
        val moments = momentsOn(monday, listOf(work)) { _, _, _ -> listOf(hour(8)) }
        val summary = summarize(moments)
        assertTrue(summary.isQuiet)
        assertFalse(summarize(momentsOn(monday, listOf(work)) { _, _, _ -> listOf(hour(8, temp = 1.0)) }).isQuiet)
    }

    // -- extremes and drying

    @Test
    fun `extremes are rare and clear`() {
        assertTrue(extremesIn(listOf(hour(12), hour(13))).isEmpty())
        assertEquals(setOf(Extreme.STORM), extremesIn(listOf(hour(15, code = 96))))
        assertEquals(setOf(Extreme.HEAT_WAVE), extremesIn(listOf(hour(15, temp = 36.0))))
        assertEquals(setOf(Extreme.GALE), extremesIn(listOf(hour(15, gust = 90.0))))
        assertEquals(setOf(Extreme.TORRENTIAL_RAIN), extremesIn(listOf(hour(15, mm = 9.0))))
        assertEquals(setOf(Extreme.HARD_FROST), extremesIn(listOf(hour(6, temp = -12.0))))
    }

    @Test
    fun `a dry breezy mild day is good for drying washing`() {
        val good = (9..17).map { hour(it, temp = 16.0, humidity = 55, wind = 12.0) }
        assertEquals(DryingQuality.GOOD, dryingQuality(good))
        assertEquals(DryingQuality.POOR, dryingQuality(good + hour(12, rain = 80, mm = 2.0)))
        assertEquals(DryingQuality.OK, dryingQuality((9..17).map { hour(it, temp = 10.0, humidity = 80, wind = 3.0) }))
        assertEquals(DryingQuality.POOR, dryingQuality(emptyList()))
    }
}
