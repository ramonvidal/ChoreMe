package com.choreme.core.model.weather

import com.choreme.core.model.location.GeoPoint
import com.choreme.core.model.location.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomePlaceTest {
    private fun place(id: String, name: String) = Place(id, name, GeoPoint(52.5, 13.4), 100)

    @Test
    fun `the place called home is home, in any of the family's languages`() {
        assertEquals("b", homePlace(listOf(place("a", "School"), place("b", "Home")))!!.id)
        assertEquals("b", homePlace(listOf(place("a", "Escola"), place("b", "Minha Casa")))!!.id)
        assertEquals("b", homePlace(listOf(place("a", "Büro"), place("b", "Zuhause")))!!.id)
        assertEquals("b", homePlace(listOf(place("a", "Trabalho"), place("b", "Cafofo")))!!.id)
    }

    @Test
    fun `without a home the first place stands in, and without places there is none`() {
        assertEquals("a", homePlace(listOf(place("a", "School"), place("b", "Gym")))!!.id)
        assertNull(homePlace(emptyList()))
    }
}
