package com.hizkia.homescreen.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationMathTest {
    @Test
    fun `koordinat dibulatkan ke 2 desimal`() {
        assertEquals(-6.21, LocationMath.round2(-6.2149), 0.00001)
        assertEquals(106.83, LocationMath.round2(106.8267), 0.00001)
    }

    @Test
    fun `geser kecil tidak dianggap pindah`() {
        assertFalse(LocationMath.moved(-6.20, 106.80, -6.21, 106.81))
    }

    @Test
    fun `pindah kota dianggap pindah`() {
        assertTrue(LocationMath.moved(-6.20, 106.80, -6.90, 107.60))
    }
}
