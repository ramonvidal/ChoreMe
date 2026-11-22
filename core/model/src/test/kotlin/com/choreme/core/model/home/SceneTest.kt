package com.choreme.core.model.home

import com.choreme.core.model.location.GeoPoint
import com.choreme.core.model.location.MemberLocation
import com.choreme.core.model.location.Place
import com.choreme.core.model.weather.Sky
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

class SceneTest {
    @Test
    fun `easter follows the church calendar`() {
        assertEquals(LocalDate.of(2026, 4, 5), easterSunday(2026))
        assertEquals(LocalDate.of(2027, 3, 28), easterSunday(2027))
        assertEquals(LocalDate.of(2025, 4, 20), easterSunday(2025))
    }

    @Test
    fun `the house dresses up for the occasions`() {
        assertEquals(Occasion.HALLOWEEN, occasionOn(LocalDate.of(2026, 10, 31)))
        assertEquals(Occasion.CHILDRENS_DAY, occasionOn(LocalDate.of(2026, 10, 12)))
        assertEquals(Occasion.CHRISTMAS, occasionOn(LocalDate.of(2026, 12, 24)))
        assertEquals(Occasion.NEW_YEAR, occasionOn(LocalDate.of(2026, 12, 31)))
        assertEquals(Occasion.NEW_YEAR, occasionOn(LocalDate.of(2027, 1, 1)))
        assertEquals(Occasion.EASTER, occasionOn(LocalDate.of(2026, 4, 3)))
        // Carnival 2027: Tuesday 9 February, from the Friday before to Ash Wednesday.
        assertEquals(Occasion.CARNIVAL, occasionOn(LocalDate.of(2027, 2, 5)))
        assertEquals(Occasion.CARNIVAL, occasionOn(LocalDate.of(2027, 2, 10)))
        assertNull(occasionOn(LocalDate.of(2027, 2, 12)))
        assertEquals(Occasion.SAO_JOAO, occasionOn(LocalDate.of(2026, 6, 24)))
        assertNull(occasionOn(LocalDate.of(2026, 9, 15)))
        assertNull(occasionOn(LocalDate.of(2026, 11, 14)))
    }

    @Test
    fun `ordinary gags fit the weather and the hour`() {
        val sunny = SceneInput(hour = 12f, sky = Sky.CLEAR, windKmh = 12f)
        assertTrue(Gag.BIRD in eligibleGags(sunny) && Gag.KITE in eligibleGags(sunny) && Gag.BUTTERFLY in eligibleGags(sunny))
        val rainy = SceneInput(hour = 12f, sky = Sky.RAIN)
        // In the rain only the gecko comes out, on the wall under the eaves.
        assertEquals(listOf(Gag.GECKO), eligibleGags(rainy))
        val night = SceneInput(hour = 23f, sky = Sky.CLEAR)
        assertTrue(Gag.SHOOTING_STAR in eligibleGags(night))
        assertFalse(Gag.BIRD in eligibleGags(night))
        assertFalse(Gag.KITE in eligibleGags(SceneInput(hour = 12f, sky = Sky.CLEAR, windKmh = 2f)))
    }

    @Test
    fun `an occasion adds its own gags, twice as likely`() {
        val halloween = SceneInput(hour = 22f, sky = Sky.CLEAR, occasion = Occasion.HALLOWEEN)
        val gags = eligibleGags(halloween)
        assertEquals(2, gags.count { it == Gag.GHOST })
        assertEquals(2, gags.count { it == Gag.BATS })
        assertTrue(Gag.FIREWORKS in eligibleGags(SceneInput(hour = 23f, occasion = Occasion.NEW_YEAR)))
        assertFalse(Gag.FIREWORKS in eligibleGags(SceneInput(hour = 12f, occasion = Occasion.NEW_YEAR)))
        assertFalse(Gag.SLEIGH in eligibleGags(SceneInput(hour = 12f, sky = Sky.STORM, occasion = Occasion.CHRISTMAS)))
        assertTrue(Gag.BUNNY in eligibleGags(SceneInput(hour = 12f, occasion = Occasion.EASTER)))
    }

    @Test
    fun `a gag is picked only when one fits`() {
        assertEquals(Gag.GECKO, pickGag(SceneInput(hour = 12f, sky = Sky.RAIN), Random(1)))
        assertTrue(pickGag(SceneInput(hour = 12f, sky = Sky.CLEAR), Random(1)) != null)
    }

    @Test
    fun `a rainbow comes when the rain stops and the sun is out`() {
        assertTrue(rainbowAfter(Sky.RAIN, SceneInput(hour = 15f, sky = Sky.PARTLY_CLOUDY)))
        assertFalse(rainbowAfter(Sky.RAIN, SceneInput(hour = 22f, sky = Sky.CLEAR)))
        assertFalse(rainbowAfter(Sky.CLOUDY, SceneInput(hour = 15f, sky = Sky.CLEAR)))
        assertFalse(rainbowAfter(Sky.RAIN, SceneInput(hour = 15f, sky = Sky.RAIN)))
    }

    @Test
    fun `people at home are the ones recently seen inside the place`() {
        val now = Instant.parse("2026-11-16T20:00:00Z")
        val home = Place("home", "Home", GeoPoint(52.5, 13.4), 100)
        val inside = MemberLocation("a", GeoPoint(52.5001, 13.4001), 10f, now.minusSeconds(600))
        val outside = MemberLocation("b", GeoPoint(52.6, 13.5), 10f, now.minusSeconds(600))
        val stale = MemberLocation("c", GeoPoint(52.5, 13.4), 10f, now.minusSeconds(8 * 3600))
        assertEquals(1, peopleAtHome(listOf(inside, outside, stale), home, now))
        assertEquals(0, peopleAtHome(listOf(inside), null, now))
    }

    @Test
    fun `wind scales between calm and a gale`() {
        assertEquals(0.1f, SceneInput(windKmh = 0f).wind, 0f)
        assertEquals(1.6f, SceneInput(windKmh = 90f).wind, 0f)
    }

    @Test
    fun `Bahia's own dates and the family days`() {
        assertEquals(LocalDate.of(2026, 1, 15), bonfimDay(2026))
        assertEquals(LocalDate.of(2025, 1, 16), bonfimDay(2025))
        assertEquals(Occasion.BONFIM, occasionOn(LocalDate.of(2026, 1, 15)))
        assertEquals(Occasion.IEMANJA, occasionOn(LocalDate.of(2026, 2, 2)))
        assertEquals(Occasion.DOIS_DE_JULHO, occasionOn(LocalDate.of(2026, 7, 2)))
        assertEquals(LocalDate.of(2026, 5, 10), nthSunday(2026, 5, 2))
        assertEquals(Occasion.MOTHERS_DAY, occasionOn(LocalDate.of(2026, 5, 9)))
        assertEquals(Occasion.FATHERS_DAY, occasionOn(LocalDate.of(2026, 8, 9)))
        assertEquals(Occasion.SPRING, occasionOn(LocalDate.of(2026, 9, 23)))
        assertEquals(null, occasionOn(LocalDate.of(2026, 3, 18)))
    }

    @Test
    fun `the moon's phase follows the real sky`() {
        // Full moons on 15 December 2024 and 24 November 2026; new moon on 30 December 2024.
        assertEquals(0.5f, moonPhase(java.time.Instant.parse("2024-12-15T12:00:00Z")), 0.04f)
        assertEquals(0.5f, moonPhase(java.time.Instant.parse("2026-11-24T12:00:00Z")), 0.04f)
        val newMoon = moonPhase(java.time.Instant.parse("2024-12-30T12:00:00Z"))
        assertTrue(newMoon < 0.04f || newMoon > 0.96f)
    }

    @Test
    fun `meteors, the warm season, the washing and the bin`() {
        assertTrue(meteorShower(LocalDate.of(2026, 8, 12)) && meteorShower(LocalDate.of(2026, 12, 13)))
        assertFalse(meteorShower(LocalDate.of(2026, 8, 20)))
        assertTrue(warmSeason(LocalDate.of(2026, 1, 10)) && !warmSeason(LocalDate.of(2026, 7, 10)))
        assertTrue(laundryOut(laundryDue = true, sky = Sky.CLEAR, isDay = true))
        assertFalse(laundryOut(laundryDue = true, sky = Sky.RAIN, isDay = true))
        assertFalse(laundryOut(laundryDue = true, sky = Sky.CLEAR, isDay = false))
        val today = LocalDate.of(2026, 11, 22)
        val bins = com.choreme.core.model.chore.Chore(id = "b", title = "Bins", template = com.choreme.core.model.chore.ChoreTemplate.TRASH, dueDate = today)
        assertTrue(trashOut(listOf(bins), today))
        assertFalse(trashOut(listOf(bins.copy(dueDate = today.plusDays(1))), today))
    }

    @Test
    fun `finishing chores is celebrated, the last one of the day with fireworks`() {
        assertEquals(Gag.CONFETTI, celebrationFor(doneBefore = 1, doneNow = 2, leftNow = 3))
        assertEquals(Gag.FIREWORKS, celebrationFor(doneBefore = 2, doneNow = 3, leftNow = 0))
        assertEquals(null, celebrationFor(doneBefore = 2, doneNow = 2, leftNow = 0))
    }

    @Test
    fun `new gags show at their moment, and a meteor shower comes up often`() {
        val clearDay = SceneInput(hour = 12f, sky = Sky.CLEAR, warm = true)
        assertTrue(Gag.HUMMINGBIRD in eligibleGags(clearDay) && Gag.ICE_CREAM in eligibleGags(clearDay) && Gag.PLANE_BANNER in eligibleGags(clearDay))
        assertFalse(Gag.ICE_CREAM in eligibleGags(clearDay.copy(warm = false)))
        val meteorNight = SceneInput(hour = 23f, sky = Sky.CLEAR, meteors = true)
        assertEquals(3, eligibleGags(meteorNight).count { it == Gag.METEORS })
        assertTrue(Gag.HEARTS in eligibleGags(SceneInput(occasion = Occasion.MOTHERS_DAY)))
        assertTrue(tapGags(clearDay).isNotEmpty())
    }
}
