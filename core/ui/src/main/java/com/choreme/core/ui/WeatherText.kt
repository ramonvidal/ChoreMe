package com.choreme.core.ui

import android.content.Context
import androidx.annotation.StringRes
import com.choreme.core.model.weather.Advice
import com.choreme.core.model.weather.Concern
import com.choreme.core.model.weather.Extreme
import com.choreme.core.model.weather.Gear
import com.choreme.core.model.weather.Sky
import com.choreme.core.model.weather.SpeedUnit
import com.choreme.core.model.weather.TemperatureUnit
import com.choreme.core.model.weather.WeatherUnits

/** The weather in words, in the phone's language. Shared by the Chores card, the morning note and the widget. */
@get:StringRes
val Sky.label: Int
    get() = when (this) {
        Sky.CLEAR -> R.string.sky_clear
        Sky.PARTLY_CLOUDY -> R.string.sky_partly_cloudy
        Sky.CLOUDY -> R.string.sky_cloudy
        Sky.FOG -> R.string.sky_fog
        Sky.DRIZZLE -> R.string.sky_drizzle
        Sky.RAIN -> R.string.sky_rain
        Sky.SNOW -> R.string.sky_snow
        Sky.STORM -> R.string.sky_storm
    }

@get:StringRes
val Gear.label: Int
    get() = when (this) {
        Gear.UMBRELLA -> R.string.gear_umbrella
        Gear.RAIN_JACKET -> R.string.gear_rain_jacket
        Gear.WARM_JACKET -> R.string.gear_warm_jacket
        Gear.SUN_PROTECTION -> R.string.gear_sun_protection
    }

/** "12°" in the chosen unit. */
fun WeatherUnits.degrees(celsius: Double): String = "${temperature(celsius)}°"

/** "25 km/h", "15 mph" or "7 m/s". */
fun WeatherUnits.windText(kmh: Double): String = "${speed(kmh)} ${when (speed) {
    SpeedUnit.KMH -> "km/h"
    SpeedUnit.MPH -> "mph"
    SpeedUnit.MS -> "m/s"
}}"

/** The temperature unit's letter for settings and small print. */
val TemperatureUnit.symbol: String get() = if (this == TemperatureUnit.CELSIUS) "°C" else "°F"

/** What matters on a trip, in one line: "Rain 70% · umbrella · leave 10 min earlier". Empty when quiet. */
fun Advice.describe(context: Context, units: WeatherUnits): String {
    val parts = mutableListOf<String>()
    headline?.let { parts += concernText(context, it, this, units) }
    gear.forEach { parts += context.getString(it.label).lowercase() }
    if (leaveEarlierMinutes > 0) parts += context.getString(R.string.weather_leave_earlier, leaveEarlierMinutes)
    return parts.joinToString(" · ")
}

private fun concernText(context: Context, concern: Concern, advice: Advice, units: WeatherUnits): String = when (concern) {
    Concern.RAIN -> context.getString(R.string.concern_rain, advice.maxRainProbability)
    Concern.HEAVY_RAIN -> context.getString(R.string.concern_heavy_rain)
    Concern.SNOW -> context.getString(R.string.concern_snow)
    Concern.ICE -> context.getString(R.string.concern_ice)
    Concern.FROST -> context.getString(R.string.concern_frost, units.degrees(advice.minTemperatureC))
    Concern.COLD -> context.getString(R.string.concern_cold, units.degrees(advice.minTemperatureC))
    Concern.HEAT -> context.getString(R.string.concern_heat, units.degrees(advice.maxTemperatureC))
    Concern.WIND -> context.getString(R.string.concern_wind, units.windText(advice.maxGustKmh))
    Concern.STORM -> context.getString(R.string.concern_storm)
    Concern.UV -> context.getString(R.string.concern_uv)
    Concern.FOG -> context.getString(R.string.concern_fog)
}

/** Weather bad enough to warn everyone about. */
@get:StringRes
val Extreme.label: Int
    get() = when (this) {
        Extreme.STORM -> R.string.extreme_storm
        Extreme.HEAT_WAVE -> R.string.extreme_heat
        Extreme.HARD_FROST -> R.string.extreme_frost
        Extreme.GALE -> R.string.extreme_gale
        Extreme.TORRENTIAL_RAIN -> R.string.extreme_rain
    }
