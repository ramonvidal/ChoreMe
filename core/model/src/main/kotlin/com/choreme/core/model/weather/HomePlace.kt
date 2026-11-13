package com.choreme.core.model.weather

import com.choreme.core.model.location.Place
import java.text.Normalizer

private val HOME_WORDS = listOf("home", "casa", "zuhause", "haus", "cafofo", "lar", "heim", "maison")

/**
 * The family's saved place that is home: the one called "Home", "Casa", "Zuhause" and so on. Falls back to the first
 * saved place, so the weather has somewhere to look, and null when the family saved none.
 */
fun homePlace(places: List<Place>): Place? =
    places.firstOrNull { place -> plain(place.name).split(' ').any { it in HOME_WORDS } } ?: places.firstOrNull()

private fun plain(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9 ]"), " ").trim()
