package com.srinivaskannan.divyaprabhandham.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The syllable scanner, checked the way the iOS one was: fixed cases read
 * off the rules, then a sweep of the whole bundled corpus.
 *
 * The sweep's strongest check is the round trip. Putting the segments back
 * together has to reproduce the line character for character -- that is what
 * guarantees the reader can never drop, duplicate or reorder a letter while
 * marking it, which is the one failure that would actually corrupt a verse.
 */
class TamilProsodyTest {

    private fun scanned(s: String) = TamilProsody.scan(s).joinToString(" ") {
        when (it) {
            is TamilProsody.Segment.Syllable -> it.text + if (it.long) "/L" else "/S"
            is TamilProsody.Segment.Other -> "_"
        }
    }

    @Test
    fun `syllables and weights follow the orthographic rules`() {
        val cases = mapOf(
            // long vowel, then a closing ஒற்று that joins the syllable before it
            "பல்லாண்டு" to "பல்/S லாண்/L டு/S",
            // ஐ is நெடில், and ங் closes the syllable before it
            "மங்கை" to "மங்/S கை/L",
            // inherent அ is குறில்
            "வர" to "வ/S ர/S",
            // a word-final ஒற்று closes the last syllable rather than opening one
            "உன்" to "உன்/S",
            // ZWNJ after a word-final pulli stays inside the syllable
            "உன்‌" to "உன்‌/S",
            // every syllable long
            "நீராட" to "நீ/L ரா/L ட/S",
            // ஆய்தம் closes the syllable it follows
            "அஃது" to "அஃ/S து/S",
        )
        for ((input, expected) in cases) {
            assertEquals(input, expected, scanned(input))
        }
    }

    @Test
    fun `a word-initial cluster is left unmarked rather than guessed at`() {
        // ஸ்ரீ opens on a consonant cluster, which Tamil phonotactics does
        // not allow; it is Sanskrit, and is passed through as Other.
        val segments = TamilProsody.scan("ஸ்ரீ")
        assertTrue(segments.first() is TamilProsody.Segment.Other)
    }

    @Test
    fun `every corpus line scans and round-trips exactly`() {
        val assets = File("src/main/assets")
        val files = assets.listFiles { f -> f.name.startsWith("prabandham") ||
            f.name == "desika_prabandham.json" || f.name == "podhu_thaniyangal.json" }
        assertTrue("corpus assets not found at ${assets.absolutePath}", files != null && files.isNotEmpty())

        var lines = 0
        var syllables = 0
        var long = 0
        for (file in files!!) {
            val works = JSONObject(file.readText()).getJSONArray("works")
            for (w in 0 until works.length()) {
                val sections = works.getJSONObject(w).getJSONArray("sections")
                for (s in 0 until sections.length()) {
                    val content = sections.getJSONObject(s).getString("content")
                    for (line in content.split("\n")) {
                        if (line.isBlank()) continue
                        lines++
                        val segments = TamilProsody.scan(line)
                        val rebuilt = segments.joinToString("") {
                            when (it) {
                                is TamilProsody.Segment.Syllable -> it.text
                                is TamilProsody.Segment.Other -> it.text
                            }
                        }
                        assertEquals("round trip failed on: $line", line, rebuilt)
                        for (seg in segments) {
                            if (seg is TamilProsody.Segment.Syllable) {
                                syllables++
                                if (seg.long) long++
                            }
                        }
                    }
                }
            }
        }
        println("scanned $lines lines, $syllables syllables, $long long")
        // Locks the figures the feature was designed against, so a change in
        // either the scanner or the corpus shows up here rather than on a
        // reader's screen.
        //
        // The syllable total is 209 higher than the 314,102 that
        // tools_prosody_ta.py --corpus reports, and the difference is the
        // reference's summary, not the rules: it drops a word whole once it
        // opens on a Sanskrit cluster, where the scanner passes only the
        // cluster through and still marks the rest (ஸ்ரீமத் -> ஸ்ர unmarked,
        // then ரீ and மத்). 56 such words, averaging under four syllables
        // each. On the rules themselves the two agree everywhere.
        assertEquals(24444, lines)
        assertEquals(314311, syllables)
        assertEquals(87257, long)
    }
}
