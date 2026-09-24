package com.srinivaskannan.divyaprabhandham.data

import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The essences follow the reader's script.
 *
 * Decoded from the real bundled asset rather than a fixture, so a
 * regenerated corpus that dropped the ta_te/ta_ml/ta_de fields would fail
 * here instead of quietly showing English to a Telugu reader again.
 */
class EssenceScriptTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun essences(name: String): Map<String, Essence> =
        json.decodeFromString(File("src/main/assets/$name.json").readText())

    private fun inBlock(s: String, lo: Int, hi: Int) =
        s.codePoints().anyMatch { it in lo..hi }

    @Test
    fun `every essence carries all three Indic scripts`() {
        for (name in listOf("essences", "decad_essences")) {
            val all = essences(name)
            assertTrue("$name is empty", all.isNotEmpty())
            for ((id, e) in all) {
                assertEquals("$name/$id tamil", e.ta, e.text(ScriptChoice.TAMIL))
                assertEquals("$name/$id readable", e.en, e.text(ScriptChoice.READABLE))
                assertEquals("$name/$id scholarly", e.en, e.text(ScriptChoice.SCHOLARLY))
                assertTrue("$name/$id telugu", inBlock(e.text(ScriptChoice.TELUGU), 0x0C00, 0x0C7F))
                assertTrue("$name/$id malayalam", inBlock(e.text(ScriptChoice.MALAYALAM), 0x0D00, 0x0D7F))
                assertTrue("$name/$id devanagari", inBlock(e.text(ScriptChoice.DEVANAGARI), 0x0900, 0x097F))
            }
            println("$name: ${all.size} essences, all six scripts present")
        }
    }
}
