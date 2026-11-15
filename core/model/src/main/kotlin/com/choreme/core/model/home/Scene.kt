package com.choreme.core.model.home

import com.choreme.core.model.location.MemberLocation
import com.choreme.core.model.location.Place
import com.choreme.core.model.location.currentPlace
import com.choreme.core.model.weather.Sky
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

/** A special time of the year the little house dresses up for. */
enum class Occasion { HALLOWEEN, CHRISTMAS, NEW_YEAR, EASTER, CARNIVAL, SAO_JOAO, CHILDRENS_DAY }

/** Easter Sunday of [year] (the Western date; Meeus/Jones/Butcher). */
fun easterSunday(year: Int): LocalDate {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day = (h + l - 7 * m + 114) % 31 + 1
    return LocalDate.of(year, month, day)
}

/**
 * The occasion [date] belongs to, or null on an ordinary day. Halloween from 25 October, the Children's Day on 12
 * October (the week before), Christmas from 8 December, New Year from the 26th to 2 January, Easter the week before the
 * Sunday, Carnival from the Friday before Ash Wednesday, and São João (Festa Junina, big in Bahia) from 13 to 29 June.
 */
fun occasionOn(date: LocalDate): Occasion? {
    val md = date.monthValue * 100 + date.dayOfMonth
    val easter = easterSunday(date.year)
    val carnivalTuesday = easter.minusDays(47)
    return when {
        date in carnivalTuesday.minusDays(4)..carnivalTuesday.plusDays(1) -> Occasion.CARNIVAL
        date in easter.minusDays(7)..easter -> Occasion.EASTER
        md in 1006..1012 -> Occasion.CHILDRENS_DAY
        md in 1025..1031 -> Occasion.HALLOWEEN
        md in 1208..1225 -> Occasion.CHRISTMAS
        md >= 1226 || md <= 102 -> Occasion.NEW_YEAR
        md in 613..629 -> Occasion.SAO_JOAO
        else -> null
    }
}

/** Little events that now and then happen around the house, like the old castaway screensaver. */
enum class Gag(val seconds: Float) {
    BIRD(9f), CAT(12f), KITE(12f), BUTTERFLY(10f), SHOOTING_STAR(2.2f), RAINBOW(8f),
    BATS(8f), GHOST(9f), SLEIGH(11f), BUNNY(10f), FIREWORKS(6f), BALLOONS(12f), CONFETTI(7f),
}

/** What the picture needs to know to draw the house: the hour, the weather, who is home and what to celebrate. */
data class SceneInput(
    /** 0 to 24. */
    val hour: Float = 12f,
    val sky: Sky = Sky.CLEAR,
    val windKmh: Float = 5f,
    /** How many family members are at home now (the windows light up for them). */
    val peopleHome: Int = 0,
    /** An urgent chore is waiting: the mailbox flag goes up. */
    val urgent: Boolean = false,
    val occasion: Occasion? = null,
    /** When the sun comes up and goes down where the house is, as hours of the day. */
    val sunrise: Float = 6f,
    val sunset: Float = 18.5f,
) {
    val isDay: Boolean get() = hour >= sunrise && hour <= sunset
    val wind: Float get() = (windKmh / 20f).coerceIn(0.1f, 1.6f)
}

private val DRY = setOf(Sky.CLEAR, Sky.PARTLY_CLOUDY)
private val WET = setOf(Sky.RAIN, Sky.DRIZZLE, Sky.STORM)

/**
 * The gags that fit this moment: birds and the cat by day and in decent weather, a kite when it is breezy, a shooting
 * star on clear nights, and the occasion's own ones when there is one (listed twice, so they come up more often).
 */
fun eligibleGags(scene: SceneInput): List<Gag> = buildList {
    val day = scene.isDay
    if (day && scene.sky !in WET) add(Gag.BIRD)
    if (scene.sky !in WET && scene.sky != Sky.SNOW) add(Gag.CAT)
    if (day && scene.sky in DRY && scene.windKmh >= 8f) add(Gag.KITE)
    if (!day && scene.sky in DRY) add(Gag.SHOOTING_STAR)
    if (day && scene.sky == Sky.CLEAR) add(Gag.BUTTERFLY)
    val special = when (scene.occasion) {
        Occasion.HALLOWEEN -> if (!day) listOf(Gag.BATS, Gag.GHOST) else listOf(Gag.BATS)
        Occasion.CHRISTMAS -> listOf(Gag.SLEIGH)
        Occasion.NEW_YEAR -> if (!day) listOf(Gag.FIREWORKS) else emptyList()
        Occasion.EASTER -> if (day) listOf(Gag.BUNNY) else emptyList()
        Occasion.CARNIVAL -> listOf(Gag.CONFETTI, Gag.BALLOONS)
        Occasion.SAO_JOAO -> if (!day) listOf(Gag.FIREWORKS, Gag.BALLOONS) else listOf(Gag.BALLOONS)
        Occasion.CHILDRENS_DAY -> if (day) listOf(Gag.BALLOONS, Gag.KITE) else emptyList()
        null -> emptyList()
    }
    // Sleigh and ghost only when the sky is dry enough to see them; the rest do not mind.
    special.filter { !(it == Gag.SLEIGH && scene.sky in WET) }.forEach { add(it); add(it) }
}

/** One of the eligible gags, or null when none fits. */
fun pickGag(scene: SceneInput, random: Random): Gag? = eligibleGags(scene).takeIf { it.isNotEmpty() }?.random(random)

/** A rainbow shows when rain stops and the sun comes out, by day. */
fun rainbowAfter(before: Sky, now: SceneInput): Boolean = before in WET && now.sky in DRY && now.isDay

private val FRESH: Duration = Duration.ofHours(6)

/**
 * How many people are at [home] now: the ones whose last shared position is recent and inside the place. Only people
 * who share their location can be counted.
 */
fun peopleAtHome(locations: List<MemberLocation>, home: Place?, now: Instant): Int {
    if (home == null) return 0
    return locations.count { loc ->
        Duration.between(loc.at, now) <= FRESH && currentPlace(null, loc.point, loc.accuracyMeters, listOf(home))?.id == home.id
    }
}
