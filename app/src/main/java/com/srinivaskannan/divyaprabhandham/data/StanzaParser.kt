package com.srinivaskannan.divyaprabhandham.data

import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import java.util.Collections

/**
 * Splits a section's flat text into stanzas.
 *
 * The rules are a direct port of the iOS build, and they matter more than
 * they look: the pasuram *numbering* drives bookmark keys, the jump-to-pasuram
 * index, the Divya Desam cross-references and the Margazhi day lookup. Getting
 * a block boundary wrong here silently shifts a bookmark onto a different verse.
 *
 * Structure is always derived from the authoritative Tamil, never from the
 * romanisation, so section keys and ranges are identical across scripts; only
 * the displayed text swaps. Transliteration preserves line count, so Tamil and
 * display lines align 1:1 — and where they somehow don't, we fall back to the
 * Tamil rather than mispairing lines.
 *
 * Unlike iOS, where `stanzas` was a computed property recalculated on every
 * access, results are memoised. The reader asks for them on every
 * recomposition and the corpus-wide pasuram index asks for every section at
 * once; parsing 3,884 verses repeatedly is the difference between a smooth
 * scroll and a stuttering one.
 */
object StanzaParser {

    /** A leading 1–4 digit number introduces a new numbered pasuram. */
    private val numberedLine = Regex("""^(\d{1,4})\s+(.*)$""")

    /** Zero-width non-joiner, which the OCR pipeline leaves in some headings. */
    private const val ZWNJ = '\u200C'

    private val cache =
        Collections.synchronizedMap(HashMap<String, List<Stanza>>())

    fun stanzas(section: BookSection, script: ScriptChoice): List<Stanza> {
        val cacheKey = "${section.id}|${script.key}"
        cache[cacheKey]?.let { return it }
        val parsed = parse(section, script)
        cache[cacheKey] = parsed
        return parsed
    }

    /** Drops every memoised result. Only needed by tests. */
    fun clearCache() = cache.clear()

    private fun parse(section: BookSection, script: ScriptChoice): List<Stanza> {
        val tamilLines = section.content.split("\n")
        val displaySource = section.content(script)
        var displayLines = displaySource.split("\n")
        if (displayLines.size != tamilLines.size) displayLines = tamilLines

        val blocks = mutableListOf<Stanza>()
        var current = mutableListOf<String>()
        var currentNumber: Int? = null

        fun flush() {
            val text = current.joinToString("\n").trim()
            current = mutableListOf()
            if (text.isEmpty()) return
            blocks += Stanza(index = blocks.size, number = currentNumber, text = text)
        }

        for (i in tamilLines.indices) {
            val tamilLine = tamilLines[i].trim()
            val displayLine = displayLines[i].trim()
            val numbered = numberedLine.matchEntire(tamilLine)

            when {
                isAttributionHeading(tamilLine) -> {
                    flush()
                    currentNumber = null
                    blocks += Stanza(
                        index = blocks.size,
                        number = null,
                        text = displayLine.trim { it == '(' || it == ')' },
                        isHeading = true,
                    )
                }

                numbered != null -> {
                    flush()
                    currentNumber = numbered.groupValues[1].toIntOrNull()
                    // Strip the same leading number from the display line.
                    val displayMatch = numberedLine.matchEntire(displayLine)
                    current = mutableListOf(displayMatch?.groupValues?.get(2) ?: displayLine)
                }

                tamilLine.isEmpty() -> {
                    if (current.isNotEmpty()) current += ""
                }

                else -> current += displayLine
            }
        }
        flush()

        // The opening block of a thirumozhi section is not a verse: it is
        // either a decad reference line ("பெரியாழ்வார் திருமொழி 1-1") or a
        // parenthetical theme note. Classify it from the authoritative Tamil.
        val first = blocks.firstOrNull()
        if (first != null && first.number == null && !first.isHeading) {
            val cleaned = section.content.replace(ZWNJ.toString(), "").trim()
            val firstLine = cleaned.split("\n").firstOrNull().orEmpty()
            val isDecadeRef = firstLine.contains("திருமொழி") && firstLine.any { it.isDigit() }
            val isThemeNote =
                cleaned.startsWith("(") && cleaned.endsWith(")") && cleaned.length < 80
            if (isDecadeRef || isThemeNote) {
                blocks[0] = first.copy(isDescription = true)
            }
        }

        return blocks
    }

    /**
     * Attribution lines like "நாதமுனிகள் அருளிச் செய்தது" introduce a taniyan
     * and are rendered as headings rather than verse cards. The length and
     * asterisk guards keep a verse that happens to contain "அருளிச்" from
     * being swallowed as a header.
     */
    private fun isAttributionHeading(raw: String): Boolean {
        var s = raw.replace(ZWNJ.toString(), "").trim()
        s = s.trim { it == '(' || it == ')' }
        if (s.length >= 60 || s.contains("*")) return false
        if (!s.contains("அருளிச்")) return false
        return s.endsWith("செய்தது") || s.endsWith("செய்தவை") || s.endsWith("செய்த")
    }
}
