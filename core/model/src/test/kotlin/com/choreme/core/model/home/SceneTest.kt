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
        assertEquals(emptyList<Gag>(), eligibleGags(rainy))
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
        assertNull(pickGag(SceneInput(hour = 12f, sky = Sky.RAIN), Random(1)))
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
}
