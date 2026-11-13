package com.choreme.feature.home

import com.choreme.core.location.DeviceLocation
import com.choreme.core.model.location.GeoPoint
import com.choreme.core.model.location.Place
import com.choreme.core.model.weather.Moment
import com.choreme.core.model.weather.PlanEntry
import com.choreme.core.model.weather.homePlace
import com.choreme.core.model.weather.momentsOn
import com.choreme.core.weather.WeatherRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * The weather for the family's day, shared by the little house's card and the morning notification: where "here"
 * is, and the trips of a day with the forecast of their whole way (at home and at the place).
 */
internal class DayWeather @Inject constructor(
    private val weather: WeatherRepository,
    private val device: DeviceLocation,
) {
    /** Where it matters: the phone's own (approximate) position, else the family's home on the map, else nowhere. */
    suspend fun home(places: List<Place>): GeoPoint? = device.current()?.point ?: homePlace(places)?.center

    /** The trips of [day] with their weather; trips with no forecast yet are left out. */
    suspend fun moments(day: LocalDate, plans: List<PlanEntry>, places: List<Place>, home: GeoPoint): List<Moment> {
        val hoursByEntry = plans
            .filter { it.isOn(day.dayOfWeek) }
            .associate { entry ->
                val from = day.atTime(entry.start)
                val points = listOfNotNull(home, places.firstOrNull { it.id == entry.placeId }?.center)
                entry.id to weather.hoursFor(points, from, from.plusMinutes(entry.minutes.toLong()))
            }
        return momentsOn(day, plans) { entry, _, _ -> hoursByEntry[entry.id].orEmpty() }
    }
}
