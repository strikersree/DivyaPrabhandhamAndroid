package com.srinivaskannan.divyaprabhandham.data

import com.srinivaskannan.divyaprabhandham.prefs.BuiltInCollections
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The two curated collections, Nithyanusanthanam and Kovil Thiruvaaymozhi, checked against the
 * bundled corpus itself: every key must name a pasuram that is really at the start of a line in
 * that section, and the keys must cover exactly the works and decads the collection is meant to hold.
 */
class BuiltInCollectionsTest {

    /** section id -> the pasuram numbers that open a line in its Tamil content. */
    private val numbersBySection: Map<String, Set<String>> by lazy {
        val out = HashMap<String, Set<String>>()
        val number = Regex("^([0-9]+(?:\\.[0-9]+)?) ")
        for (name in listOf("prabandham", "prabandham_irandam", "prabandham_iyarpa", "prabandham_thiruvaimozhi")) {
            val book: JsonObject = Json.parseToJsonElement(File("src/main/assets/$name.json").readText()).jsonObject
            for (work in book.getValue("works").jsonArray) for (section in work.jsonObject.getValue("sections").jsonArray) {
                val s = section.jsonObject
                out[s.getValue("id").jsonPrimitive.content] = s.getValue("content").jsonPrimitive.content
                    .lines().mapNotNull { number.find(it)?.groupValues?.get(1) }.toSet()
            }
        }
        out
    }

    private fun assertResolves(label: String, keys: List<String>) {
        assertEquals("$label has duplicate keys", keys.size, keys.toSet().size)
        for (key in keys) {
            val (section, number) = key.split("#")
            val numbers = numbersBySection[section] ?: error("$label: no section $section ($key)")
            assertTrue("$label: $key is not a pasuram of $section", number in numbers)
        }
    }

    @Test
    fun `nithyanusanthanam resolves and holds 104 pasurams in order`() {
        val keys = BuiltInCollections.nithyanusanthanamKeys
        assertResolves("nithyanusanthanam", keys)
        assertEquals(104, keys.size)
        // Work by work: Thiruppallaandu, Thiruppalliyezhuchi, Thiruppaavai, Poochoodal (2-7),
        // Kaappidal (2-8), Senniyongu (5-4), Amalanaadhipiraan, Kanninun Siruthaambu.
        val runs = keys.map { it.substringBefore("#") }.fold(mutableListOf<Pair<String, Int>>()) { acc, id ->
            if (acc.lastOrNull()?.first == id) acc[acc.lastIndex] = id to acc.last().second + 1 else acc += id to 1
            acc
        }
        assertEquals(
            listOf("w1s2" to 12, "w8s2" to 10, "w3s2" to 30, "w2s16" to 10, "w2s17" to 10, "w2s43" to 11,
                "w9s2" to 10, "w10s2" to 11),
            runs,
        )
    }

    @Test
    fun `kovil thiruvaaymozhi resolves and holds ten whole decads`() {
        val keys = BuiltInCollections.kovilThiruvaaymozhiKeys
        assertResolves("kovil", keys)
        assertEquals(110, keys.size)
        val runs = keys.map { it.substringBefore("#") }.groupingBy { it }.eachCount()
        // 1-1, 1-2, 2-10, 3-3, 4-1, 4-10, 5-5, 7-2, 8-10, 10-10 -- section b4w1s(n+1) is decad n.
        assertEquals(
            listOf("b4w1s2", "b4w1s3", "b4w1s21", "b4w1s24", "b4w1s32", "b4w1s41", "b4w1s46", "b4w1s63",
                "b4w1s81", "b4w1s101"),
            runs.keys.toList(),
        )
        assertTrue(runs.values.all { it == 11 })
    }

    @Test
    fun `both collections are named in every script`() {
        val blocks = mapOf(
            ScriptChoice.TELUGU to 0x0C00..0x0C7F, ScriptChoice.MALAYALAM to 0x0D00..0x0D7F,
            ScriptChoice.DEVANAGARI to 0x0900..0x097F, ScriptChoice.KANNADA to 0x0C80..0x0CFF,
            ScriptChoice.TAMIL to 0x0B80..0x0BFF,
        )
        for (id in listOf(BuiltInCollections.NITHYANUSANTHANAM_ID, BuiltInCollections.KOVIL_THIRUVAAYMOZHI_ID)) {
            for (script in ScriptChoice.entries) {
                val name = BuiltInCollections.displayName(id, script) ?: error("$id has no name")
                assertTrue("$id/$script is blank", name.isNotBlank())
                blocks[script]?.let { range ->
                    assertTrue("$id/$script '$name' has a letter outside its script",
                        name.all { it == ' ' || it.code in range })
                }
            }
        }
    }
}
