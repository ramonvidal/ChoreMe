package com.choreme.core.settings

import android.content.Context
import com.choreme.core.model.weather.RainUnit
import com.choreme.core.model.weather.SpeedUnit
import com.choreme.core.model.weather.TemperatureUnit
import com.choreme.core.model.weather.WeatherUnits
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** How this phone shows the weather: Celsius, km/h and millimetres until its owner says otherwise. */
@Singleton
class WeatherSettings @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("weather_units", Context.MODE_PRIVATE)
    private val _units = MutableStateFlow(read())
    val units: StateFlow<WeatherUnits> = _units.asStateFlow()

    fun setTemperature(unit: TemperatureUnit) = save(_units.value.copy(temperature = unit))
    fun setSpeed(unit: SpeedUnit) = save(_units.value.copy(speed = unit))
    fun setRain(unit: RainUnit) = save(_units.value.copy(rain = unit))

    private fun save(value: WeatherUnits) {
        prefs.edit()
            .putString(KEY_TEMPERATURE, value.temperature.name)
            .putString(KEY_SPEED, value.speed.name)
            .putString(KEY_RAIN, value.rain.name)
            .apply()
        _units.value = value
    }

    private fun read() = WeatherUnits(
        temperature = TemperatureUnit.entries.firstOrNull { it.name == prefs.getString(KEY_TEMPERATURE, null) } ?: TemperatureUnit.CELSIUS,
        speed = SpeedUnit.entries.firstOrNull { it.name == prefs.getString(KEY_SPEED, null) } ?: SpeedUnit.KMH,
        rain = RainUnit.entries.firstOrNull { it.name == prefs.getString(KEY_RAIN, null) } ?: RainUnit.MM,
    )

    private companion object {
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_SPEED = "speed"
        const val KEY_RAIN = "rain"
    }
}
