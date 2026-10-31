package com.hizkia.homescreen.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class RareAppsTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 1_000 * day

    private fun app(pkg: String, daysAgo: Int, mb: Long, opened: Boolean = true) =
        AppUsage(pkg, now - daysAgo * day, opened, mb * 1024 * 1024)

    private fun pkgs(list: List<AppUsage>) = list.map { it.pkg }

    @Test
    fun `hanya app yang tidak dibuka minimal 30 hari`() {
        val all = listOf(app("baru", 29, 10), app("pas", 30, 10), app("lama", 90, 10))
        assertEquals(listOf("lama", "pas"), pkgs(RareApps.rank(all, now, RareSort.OLDEST)))
    }

    @Test
    fun `urutan terlama dulu`() {
        val all = listOf(app("a", 40, 1), app("b", 100, 1), app("c", 60, 1))
        assertEquals(listOf("b", "c", "a"), pkgs(RareApps.rank(all, now, RareSort.OLDEST)))
    }

    @Test
    fun `urutan terbesar dulu`() {
        val all = listOf(app("a", 40, 50), app("b", 100, 5), app("c", 60, 500))
        assertEquals(listOf("c", "a", "b"), pkgs(RareApps.rank(all, now, RareSort.LARGEST)))
    }

    @Test
    fun `total ukuran dijumlahkan`() {
        assertEquals(300L * 1024 * 1024, RareApps.totalBytes(listOf(app("a", 40, 100), app("b", 50, 200))))
    }

    @Test
    fun `format ukuran`() {
        val id = java.util.Locale.forLanguageTag("id-ID")
        assertEquals("412 MB", RareApps.formatSize(412L * 1024 * 1024, id))
        assertEquals("1,5 GB", RareApps.formatSize((1.5 * 1024 * 1024 * 1024).toLong(), id))
        assertEquals("1.5 GB", RareApps.formatSize((1.5 * 1024 * 1024 * 1024).toLong(), java.util.Locale.ENGLISH))
        assertEquals("< 1 MB", RareApps.formatSize(100_000, id))
    }

    @Test
    fun `hari tidak pernah negatif`() {
        assertEquals(0, RareApps.daysSince(now + day, now))
    }
}
