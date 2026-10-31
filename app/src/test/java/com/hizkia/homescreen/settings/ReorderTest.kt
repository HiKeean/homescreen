package com.hizkia.homescreen.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ReorderTest {
    private val list = listOf("a", "b", "c")

    @Test
    fun `geser ke bawah`() = assertEquals(listOf("b", "a", "c"), list.moved(0, 1))

    @Test
    fun `geser ke atas`() = assertEquals(listOf("a", "c", "b"), list.moved(2, -1))

    @Test
    fun `batas atas dan bawah tidak mengubah daftar`() {
        assertEquals(list, list.moved(0, -1))
        assertEquals(list, list.moved(2, 1))
    }

    @Test
    fun `indeks tidak valid tidak mengubah daftar`() = assertEquals(list, list.moved(9, 1))
}
