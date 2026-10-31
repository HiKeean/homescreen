package com.hizkia.homescreen.i18n

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Memastikan Inggris (default), Indonesia, dan Jerman punya teks yang sama persis kuncinya, dengan placeholder yang sama. */
class StringsParityTest {
    private fun res(folder: String) = File("src/main/res/$folder/strings.xml").also { assertTrue("tidak ada: ${it.path}", it.exists()) }.readText()

    private val nameRegex = Regex("""<(string|plurals) name="([^"]+)"""")
    private val placeholder = Regex("""%\d+\$[sd]|%d|%s""")

    private fun keys(xml: String) = nameRegex.findAll(xml).map { "${it.groupValues[1]}:${it.groupValues[2]}" }.toSet()

    /** kunci -> himpunan placeholder (mis. %1$d) dari seluruh isi elemennya. */
    private fun placeholders(xml: String): Map<String, List<String>> {
        val element = Regex("""<(string|plurals) name="([^"]+)"[^>]*>(.*?)</\1>""", RegexOption.DOT_MATCHES_ALL)
        return element.findAll(xml).associate { m ->
            val body = m.groupValues[3]
            // untuk plurals, kumpulkan placeholder unik dari semua <item>
            m.groupValues[2] to placeholder.findAll(body).map { it.value }.toSet().sorted()
        }
    }

    @Test
    fun `kunci sama di tiga bahasa`() {
        val en = keys(res("values"))
        assertEquals("id vs en", en, keys(res("values-id")))
        assertEquals("de vs en", en, keys(res("values-de")))
    }

    @Test
    fun `placeholder sama di tiga bahasa`() {
        val en = placeholders(res("values"))
        for (folder in listOf("values-id", "values-de")) {
            val other = placeholders(res(folder))
            for ((key, ph) in en) {
                // %d dan %1$d setara untuk plurals satu argumen; bandingkan hanya bila keduanya berformat posisi
                val normalizedEn = ph.map { it.replace("%d", "%1\$d") }.toSet()
                val normalizedOther = other[key].orEmpty().map { it.replace("%d", "%1\$d") }.toSet()
                assertEquals("$folder / $key", normalizedEn, normalizedOther)
            }
        }
    }
}
