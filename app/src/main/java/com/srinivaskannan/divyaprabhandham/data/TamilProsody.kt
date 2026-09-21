package com.srinivaskannan.divyaprabhandham.data

/**
 * Splits Tamil into syllables and says whether each is long or short, for
 * the reciter's syllable marks.
 *
 * A Tamil syllable is one vowel-bearing letter plus any ஒற்று (pulli'd
 * consonants) that close it, and its weight is decided entirely by the
 * vowel: குறில் (அ இ உ எ ஒ and their signs) is short, நெடில் (ஆ ஈ ஊ ஏ ஐ ஓ ஔ)
 * is long. Both rules are orthographic, so this is exact rather than a best
 * guess — checked over the corpus, 109,703 of 109,759 Tamil words scan
 * cleanly and the 56 that don't are Sanskrit written with grantha clusters
 * (ஸ்ரீ, க்ருஷ்ண-, ப்ர-), which open on a consonant cluster Tamil
 * phonotactics does not allow. Those are left unmarked rather than guessed
 * at.
 *
 * A direct port of the iOS build's `TamilProsody.swift`, rule for rule, and
 * of the Python reference `tools_prosody_ta.py` it was checked against.
 * Works on code points, not on Kotlin Chars or grapheme clusters: cluster
 * boundaries are not guaranteed to fall where syllables do, and the rules
 * below are exact at the code-point level.
 *
 * Deliberately not attempted: நேர்/நிரை அசை and சீர் feet. அசை is decidable
 * inside a word but a real சீர் straddles word boundaries, and 23% of
 * non-final words in the corpus end in a short open syllable that a சீர்
 * would pair forward — so a word-level scan would be confidently wrong on
 * over half the lines. That layer needs the metre of each work.
 */
object TamilProsody {

    /** One piece of a scanned line. */
    sealed interface Segment {
        /** A syllable, and whether its vowel is நெடில். */
        data class Syllable(val text: String, val long: Boolean) : Segment
        /**
         * Anything that is not Tamil — spaces, the * and ** reading marks,
         * verse numbers, punctuation — passed through untouched.
         */
        data class Other(val text: String) : Segment
    }

    private const val PULLI = 0x0BCD
    private const val AYTHAM = 0x0B83
    private const val ZWNJ = 0x200C

    private val SHORT_VOWELS = setOf(0x0B85, 0x0B87, 0x0B89, 0x0B8E, 0x0B92)          // அ இ உ எ ஒ
    private val LONG_VOWELS =
        setOf(0x0B86, 0x0B88, 0x0B8A, 0x0B8F, 0x0B90, 0x0B93, 0x0B94)                 // ஆ ஈ ஊ ஏ ஐ ஓ ஔ
    private val SHORT_SIGNS = setOf(0x0BBF, 0x0BC1, 0x0BC6, 0x0BCA)                    // ி ு ெ ொ
    private val LONG_SIGNS =
        setOf(0x0BBE, 0x0BC0, 0x0BC2, 0x0BC7, 0x0BC8, 0x0BCB, 0x0BCC)                  // ா ீ ூ ே ை ோ ௌ

    private fun isConsonant(c: Int) = c in 0x0B95..0x0BB9
    private fun isVowel(c: Int) = c in SHORT_VOWELS || c in LONG_VOWELS
    private fun isSign(c: Int) = c in SHORT_SIGNS || c in LONG_SIGNS

    /** Scan a line into syllables and the non-Tamil text between them. */
    fun scan(text: String): List<Segment> {
        val segments = mutableListOf<Segment>()
        val pending = StringBuilder()
        val current = StringBuilder()
        var currentLong = false
        var open = false

        fun flushSyllable() {
            if (open) {
                segments += Segment.Syllable(current.toString(), currentLong)
                current.setLength(0); open = false; currentLong = false
            }
        }
        fun flushPending() {
            if (pending.isNotEmpty()) {
                segments += Segment.Other(pending.toString()); pending.setLength(0)
            }
        }
        fun begin(c: Int, long: Boolean) {
            flushSyllable(); flushPending()
            current.appendCodePoint(c); currentLong = long; open = true
        }

        val points = text.codePoints().toArray()
        var i = 0
        while (i < points.size) {
            val c = points[i]
            when {
                isVowel(c) -> {
                    begin(c, c in LONG_VOWELS); i++
                }
                isConsonant(c) -> {
                    val next = if (i + 1 < points.size) points[i + 1] else -1
                    if (next == PULLI) {
                        // A closing ஒற்று belongs to the syllable already
                        // open. With none open it is a word-initial cluster —
                        // Sanskrit, not Tamil — so it passes through unmarked.
                        val sink = if (open) current else pending
                        sink.appendCodePoint(c); sink.appendCodePoint(PULLI)
                        i += 2
                    } else if (next >= 0 && isSign(next)) {
                        begin(c, next in LONG_SIGNS)
                        current.appendCodePoint(next)
                        i += 2
                    } else {
                        begin(c, false)          // inherent அ
                        i++
                    }
                }
                c == AYTHAM || c == ZWNJ -> {
                    // ZWNJ is invisible and written after a word-final pulli,
                    // so it belongs inside the syllable it closes.
                    (if (open) current else pending).appendCodePoint(c); i++
                }
                else -> {
                    flushSyllable(); pending.appendCodePoint(c); i++
                }
            }
        }
        flushSyllable(); flushPending()
        return segments
    }
}
