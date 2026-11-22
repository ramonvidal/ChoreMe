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
enum class Occasion {
    HALLOWEEN, CHRISTMAS, NEW_YEAR, EASTER, CARNIVAL, SAO_JOAO, CHILDRENS_DAY,
    /** Lavagem do Bonfim (Salvador): ribbons on the gate. */
    BONFIM,
    /** Festa de Iemanjá, 2 February: flowers and a little boat on the sea. */
    IEMANJA,
    /** 2 de Julho, the Independence of Bahia: blue, white and red bunting. */
    DOIS_DE_JULHO,
    MOTHERS_DAY, FATHERS_DAY,
    /** The start of spring (September, in the south): flowers on the lawn. */
    SPRING,
}

/** The Lavagem do Bonfim: the second Thursday after Epiphany (6 January). */
fun bonfimDay(year: Int): LocalDate {
    var d = LocalDate.of(year, 1, 7)
    while (d.dayOfWeek != java.time.DayOfWeek.THURSDAY) d = d.plusDays(1)
    return d.plusDays(7)
}

/** The [n]th Sunday of [month] in [year] (Mother's Day: 2nd of May; Father's Day in Brazil: 2nd of August). */
fun nthSunday(year: Int, month: Int, n: Int): LocalDate {
    var d = LocalDate.of(year, month, 1)
    while (d.dayOfWeek != java.time.DayOfWeek.SUNDAY) d = d.plusDays(1)
    return d.plusWeeks((n - 1).toLong())
}

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
 * Also Bahia's own: the Lavagem do Bonfim (its Thursday and the two days before), Iemanjá (1 and 2 February) and the
 * 2 de Julho (1 and 2 July); Mother's and Father's Day (their weekend) and the first week of spring (22 to 28 September).
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
        date in bonfimDay(date.year).minusDays(2)..bonfimDay(date.year) -> Occasion.BONFIM
        md in 201..202 -> Occasion.IEMANJA
        md in 701..702 -> Occasion.DOIS_DE_JULHO
        date in nthSunday(date.year, 5, 2).minusDays(2)..nthSunday(date.year, 5, 2) -> Occasion.MOTHERS_DAY
        date in nthSunday(date.year, 8, 2).minusDays(2)..nthSunday(date.year, 8, 2) -> Occasion.FATHERS_DAY
        md in 922..928 -> Occasion.SPRING
        else -> null
    }
}

/** Little events that now and then happen around the house, like the old castaway screensaver. */
enum class Gag(val seconds: Float) {
    BIRD(9f), CAT(12f), KITE(12f), BUTTERFLY(10f), SHOOTING_STAR(2.2f), RAINBOW(8f),
    BATS(8f), GHOST(9f), SLEIGH(11f), BUNNY(10f), FIREWORKS(6f), BALLOONS(12f), CONFETTI(7f),
    HUMMINGBIRD(8f), GECKO(10f), PLANE_BANNER(12f), ICE_CREAM(14f), METEORS(6f), HEARTS(8f),
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
    /** The colours (member colour index) of the people at home, for the little figures; at most a few are drawn. */
    val homeColors: List<Int> = emptyList(),
    /** The colours of the people who said "on my way home": they walk up the path. */
    val arriving: List<Int> = emptyList(),
    /** Laundry is waiting and it's a good moment to dry it: the washing is on the line. */
    val laundryOut: Boolean = false,
    /** A bin chore is due: the bin is out by the path. */
    val trashOut: Boolean = false,
    /** The moon's phase, 0 new, 0.5 full (see [moonPhase]). */
    val moonPhase: Float = 0.5f,
    /** A meteor shower night (Perseids, Geminids). */
    val meteors: Boolean = false,
    /** October to March, the warm half of the year where the family lives (fireflies, ice cream). */
    val warm: Boolean = true,
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
    if (day && scene.sky in DRY) add(Gag.HUMMINGBIRD)
    add(Gag.GECKO)
    if (day && scene.sky !in WET && scene.sky != Sky.FOG) add(Gag.PLANE_BANNER)
    if (day && scene.sky in DRY && scene.warm && scene.hour in 10f..18f) add(Gag.ICE_CREAM)
    if (!day && scene.sky in DRY && scene.meteors) repeat(3) { add(Gag.METEORS) }
    val special = when (scene.occasion) {
        Occasion.HALLOWEEN -> if (!day) listOf(Gag.BATS, Gag.GHOST) else listOf(Gag.BATS)
        Occasion.CHRISTMAS -> listOf(Gag.SLEIGH)
        Occasion.NEW_YEAR -> if (!day) listOf(Gag.FIREWORKS) else emptyList()
        Occasion.EASTER -> if (day) listOf(Gag.BUNNY) else emptyList()
        Occasion.CARNIVAL -> listOf(Gag.CONFETTI, Gag.BALLOONS)
        Occasion.SAO_JOAO -> if (!day) listOf(Gag.FIREWORKS, Gag.BALLOONS) else listOf(Gag.BALLOONS)
        Occasion.CHILDRENS_DAY -> if (day) listOf(Gag.BALLOONS, Gag.KITE) else emptyList()
        Occasion.BONFIM -> listOf(Gag.CONFETTI)
        Occasion.IEMANJA -> if (!day) listOf(Gag.FIREWORKS) else listOf(Gag.BALLOONS)
        Occasion.DOIS_DE_JULHO -> if (!day) listOf(Gag.FIREWORKS) else listOf(Gag.BALLOONS)
        Occasion.MOTHERS_DAY, Occasion.FATHERS_DAY -> listOf(Gag.HEARTS, Gag.BALLOONS)
        Occasion.SPRING -> if (day) listOf(Gag.BUTTERFLY, Gag.HUMMINGBIRD) else emptyList()
        null -> emptyList()
    }
    // Sleigh and ghost only when the sky is dry enough to see them; the rest do not mind.
    special.filter { !(it == Gag.SLEIGH && scene.sky in WET) }.forEach { add(it); add(it) }
}

/** What a tap on the picture sets off: a bird or the hummingbird by day, the cat or the gecko otherwise. */
fun tapGags(scene: SceneInput): List<Gag> = buildList {
    if (scene.isDay && scene.sky !in WET) { add(Gag.BIRD); add(Gag.HUMMINGBIRD) }
    if (scene.sky !in WET && scene.sky != Sky.SNOW) add(Gag.CAT)
    add(Gag.GECKO)
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

/** The moon's phase on [date]: 0 is new, 0.5 full, back to 1 (counted from the new moon of 6 January 2000). */
fun moonPhase(at: Instant): Float {
    val days = Duration.between(REFERENCE_NEW_MOON, at).seconds / 86_400.0
    val phase = (days % SYNODIC_MONTH) / SYNODIC_MONTH
    return (if (phase < 0) phase + 1 else phase).toFloat()
}

private val REFERENCE_NEW_MOON: Instant = Instant.parse("2000-01-06T18:14:00Z")
private const val SYNODIC_MONTH = 29.530588853

/** The nights of the big meteor showers: the Perseids (11 to 13 August) and the Geminids (12 to 14 December). */
fun meteorShower(date: LocalDate): Boolean {
    val md = date.monthValue * 100 + date.dayOfMonth
    return md in 811..813 || md in 1212..1214
}

/** October to March: the warm half of the year in the south (Bahia). */
fun warmSeason(date: LocalDate): Boolean = date.monthValue >= 10 || date.monthValue <= 3

/** The washing is out on the line: laundry is waiting, by day, and the sky is dry. */
fun laundryOut(laundryDue: Boolean, sky: Sky, isDay: Boolean): Boolean = laundryDue && isDay && sky in DRY

/** A bin chore (rubbish, organic, recycling) is due by [today]: the bin is out. */
fun trashOut(chores: List<com.choreme.core.model.chore.Chore>, today: LocalDate): Boolean = chores.any {
    it.isActive && !it.dueDate.isAfter(today) && it.template in BINS
}

private val BINS = setOf(
    com.choreme.core.model.chore.ChoreTemplate.TRASH,
    com.choreme.core.model.chore.ChoreTemplate.ORGANIC_WASTE,
    com.choreme.core.model.chore.ChoreTemplate.RECYCLING,
)

/**
 * What the house does when chores get done: confetti when one more was finished today, and fireworks when that made
 * the day's list empty. [doneBefore] and [doneNow] count today's finishes; null when nothing new happened.
 */
fun celebrationFor(doneBefore: Int, doneNow: Int, leftNow: Int): Gag? = when {
    doneNow <= doneBefore -> null
    leftNow == 0 -> Gag.FIREWORKS
    else -> Gag.CONFETTI
}

/** The member ids among [locations] who are at [home] now (see [peopleAtHome]). */
fun membersAtHome(locations: List<MemberLocation>, home: Place?, now: Instant): List<String> {
    if (home == null) return emptyList()
    return locations.filter { loc ->
        Duration.between(loc.at, now) <= FRESH && currentPlace(null, loc.point, loc.accuracyMeters, listOf(home))?.id == home.id
    }.map { it.memberId }
}
