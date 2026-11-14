package com.choreme.feature.home

import com.choreme.core.model.weather.AirLevel
import com.choreme.core.model.weather.AirNote
import com.choreme.core.model.weather.PollenLevel

/** What to say about the air, as string ids: nothing on an ordinary day. */
internal fun AirNote.lines(): List<Int> = listOfNotNull(
    when (air) {
        AirLevel.POOR -> R.string.weather_air_poor
        AirLevel.VERY_POOR -> R.string.weather_air_very_poor
        else -> null
    },
    when (pollen) {
        PollenLevel.MODERATE -> R.string.weather_pollen_moderate
        PollenLevel.HIGH -> R.string.weather_pollen_high
        else -> null
    },
)
