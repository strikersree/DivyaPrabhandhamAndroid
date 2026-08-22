"""
Splits Siriya Thirumadal (b3w9s2) into 40 numbered sub-units (2673.1 ..
2673.40), boundaries derived from the printed book pages (Aandavan Ashram
edition, pp. 531-536) and cross-referenced against the stored corpus text.

Split points are found by locating the standalone hyphen token ('-') that
the stored text already uses to mark "sentence continues into the next
unit" (mirroring the book's own '(N)-' notation) -- confirmed present at
every one of the 36 boundary lines that need a mid-line split, each with
exactly one such token. This replaces an earlier manual word-counting pass
that had multiple off-by-one errors, caught by printing every split and
checking it against the established pattern (hyphen always starts the new
unit) before any of this touched the real file.

ONE MANUAL OVERRIDE: line 153 contains an internal poetic enjambment
hyphen belonging to unit 38 itself (unrelated to any sub-unit boundary --
the same kind of thing as line 51's internal hyphen within unit 12, found
during the original cross-reference), *and* the real 38->39 boundary later
in the same line at a different asterisk. Auto-detection would find the
wrong hyphen here, so this one boundary is given explicitly.
"""
import json

PATH = "/home/claude/port/DivyaPrabhandhamAndroid/app/src/main/assets/prabandham_iyarpa.json"

# line_index -> next_unit_number, for every boundary marked by that line's
# own standalone hyphen token.
AUTO_HYPHEN_BOUNDARIES = {
    4: 2, 7: 3, 13: 4, 17: 5, 23: 6, 28: 7, 31: 8, 33: 9, 38: 10, 43: 11,
    45: 12, 52: 13, 65: 14, 68: 15, 73: 16, 77: 17, 80: 18, 83: 19, 86: 20,
    89: 21, 95: 23, 100: 24, 104: 25, 108: 26, 110: 27, 112: 28, 115: 29,
    122: 30, 123: 31, 125: 32, 128: 33, 133: 35, 136: 36, 143: 37, 154: 40,
}

# Explicit (word_idx, next_unit) overrides where auto-hyphen-detection
# doesn't apply -- word_idx is the 0-based index of the LAST word staying
# with the unit that's ending.
MANUAL_BOUNDARIES = {
    153: (6, 39),  # "...கண்ணானை *" | "எண்ணருஞ்சீர்" -- NOT at this line's hyphen
}

# Lines where the boundary is already clean (previous line ends its unit
# with no trailing continuation; this line starts the next one fresh).
CLEAN_STARTS = {93: 22, 131: 34, 149: 38}

JUNK_LINE_INDICES = {41, 126}

# Two stray OCR apostrophes noticed during the final read-through, unrelated
# to the splitting work itself but worth fixing while touching this text.
STRAY_APOSTROPHE_FIXES = {
    "வாமனனாகிய'": "வாமனனாகிய",
    "vaamananaagiya'": "vaamananaagiya",
    "vāmaṉaṉākiya'": "vāmaṉaṉākiya",
    "'ஊராதொழியேன்": "ஊராதொழியேன்",
    "'ooraadhozhiyen": "ooraadhozhiyen",
    "'ūrātoḻiyēṉ": "ūrātoḻiyēṉ",
    "கேளாமே''!": "கேளாமே",
    "kaelaamae''!": "kaelaamae",
    "kēḷāmē''!": "kēḷāmē",
}


def fix_stray_apostrophes(line):
    for bad, good in STRAY_APOSTROPHE_FIXES.items():
        line = line.replace(bad, good)
    return line


def split_line_at_word(line, word_idx):
    words = line.split(' ')
    return ' '.join(words[:word_idx + 1]), ' '.join(words[word_idx + 1:])


def hyphen_word_idx(line):
    words = line.split(' ')
    positions = [i for i, w in enumerate(words) if w == '-']
    assert len(positions) == 1, f"expected exactly one hyphen, found {len(positions)}: {line!r}"
    return positions[0] - 1  # last word BEFORE the hyphen stays with the ending unit


def main():
    d = json.load(open(PATH, encoding='utf-8'))
    w = next(w for w in d['works'] if 'சிறிய திருமடல்' in w['title'])
    s = w['sections'][1]

    for field in ('content', 'content_r', 'content_s'):
        text = s.get(field)
        lines = text.split('\n')
        assert len(lines) == 163, f'{field}: expected 163 lines, got {len(lines)}'

        out = []
        for i, line in enumerate(lines):
            line = fix_stray_apostrophes(line)
            if i in JUNK_LINE_INDICES:
                continue
            if i == 0:
                rest = line.split(' ', 1)[1] if ' ' in line else ''
                out.append(f'2673.1 {rest}')
                continue
            if i in CLEAN_STARTS:
                out.append(f'2673.{CLEAN_STARTS[i]} {line}')
                continue
            if i in MANUAL_BOUNDARIES:
                word_idx, next_unit = MANUAL_BOUNDARIES[i]
                end_part, start_part = split_line_at_word(line, word_idx)
                out.append(end_part)
                out.append(f'2673.{next_unit} {start_part}')
                continue
            if i in AUTO_HYPHEN_BOUNDARIES:
                next_unit = AUTO_HYPHEN_BOUNDARIES[i]
                word_idx = hyphen_word_idx(line)
                end_part, start_part = split_line_at_word(line, word_idx)
                out.append(end_part)
                out.append(f'2673.{next_unit} {start_part}')
                continue
            out.append(line)

        s[field] = '\n'.join(out)

    json.dump(d, open(PATH, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    print('Done. New line count:', len(s['content'].split(chr(10))))


if __name__ == '__main__':
    main()
