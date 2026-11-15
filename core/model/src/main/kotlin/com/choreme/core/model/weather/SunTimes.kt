package com.choreme.core.model.weather

import java.time.LocalDate
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** When the sun comes up and goes down at a place, as hours of the local day (5.5 is 5:30). */
data class SunTimes(val sunrise: Float, val sunset: Float)

/**
 * Sunrise and sunset on [date] at [latitude]/[longitude], in the clock of a place [utcOffsetHours] from UTC (the NOAA
 * solar calculator, good to a few minutes). Null where the sun does not rise or set that day (near the poles).
 */
fun sunTimes(date: LocalDate, latitude: Double, longitude: Double, utcOffsetHours: Double): SunTimes? {
    val gamma = 2 * Math.PI / 365 * (date.dayOfYear - 1)
    val equationOfTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
    val declination = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) +
        0.000907 * sin(2 * gamma) - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
    val lat = Math.toRadians(latitude)
    val cosHourAngle = cos(Math.toRadians(90.833)) / (cos(lat) * cos(declination)) - tan(lat) * tan(declination)
    if (cosHourAngle > 1 || cosHourAngle < -1) return null
    val hourAngle = Math.toDegrees(acos(cosHourAngle))
    val sunriseUtc = 720 - 4 * (longitude + hourAngle) - equationOfTime
    val sunsetUtc = 720 - 4 * (longitude - hourAngle) - equationOfTime
    return SunTimes(
        sunrise = (sunriseUtc / 60 + utcOffsetHours).toFloat(),
        sunset = (sunsetUtc / 60 + utcOffsetHours).toFloat(),
    )
}
