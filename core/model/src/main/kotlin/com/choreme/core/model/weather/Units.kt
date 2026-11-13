package com.choreme.core.model.weather

import kotlin.math.roundToInt

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }
enum class SpeedUnit { KMH, MPH, MS }
enum class RainUnit { MM, INCH }

/** How this phone's owner likes numbers: Celsius and km/h until they say otherwise. */
data class WeatherUnits(
    val temperature: TemperatureUnit = TemperatureUnit.CELSIUS,
    val speed: SpeedUnit = SpeedUnit.KMH,
    val rain: RainUnit = RainUnit.MM,
) {
    /** A temperature as a whole number in the chosen unit. */
    fun temperature(celsius: Double): Int = when (temperature) {
        TemperatureUnit.CELSIUS -> celsius.roundToInt()
        TemperatureUnit.FAHRENHEIT -> (celsius * 9 / 5 + 32).roundToInt()
    }

    /** A wind speed as a whole number in the chosen unit. */
    fun speed(kmh: Double): Int = when (speed) {
        SpeedUnit.KMH -> kmh.roundToInt()
        SpeedUnit.MPH -> (kmh / KM_PER_MILE).roundToInt()
        SpeedUnit.MS -> (kmh / 3.6).roundToInt()
    }

    /** Rain in the chosen unit, to one decimal for mm and two for inches. */
    fun rain(mm: Double): Double = when (rain) {
        RainUnit.MM -> (mm * 10).roundToInt() / 10.0
        RainUnit.INCH -> (mm / MM_PER_INCH * 100).roundToInt() / 100.0
    }

    private companion object {
        const val KM_PER_MILE = 1.609344
        const val MM_PER_INCH = 25.4
    }
}
