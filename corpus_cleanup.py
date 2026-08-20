"""
Corpus content cleanup for items 1, 3, 5 (glued footnote-digit, trailing
decad-numbers, page-break footer junk). Verified against real data via
extensive dry-run inspection before being applied here.

CRITICAL: junk-line indices for item 5 are always determined from the
Tamil `content` field, then the SAME indices are removed from content_r/
content_s regardless of their own text at that position -- this keeps all
three fields' line counts aligned, per this project's established rule
that structure is always derived from the authoritative Tamil.
"""
import json, re, glob

ASSETS = "/home/claude/port/DivyaPrabhandhamAndroid/app/src/main/assets"

# ---------- Item 3: trailing decad-position numbers ----------
# Extended to accept a Tamil-zero-glyph OCR misread mixed with ascii digits,
# e.g. "(6௦)" for "(60)", found during verification.
TRAILING_PAREN = re.compile(r'\s*\([௦0-9]+\)')
BARE_TRAILING_ZERO = re.compile(r'\s+௦$')  # one confirmed isolated case

# ---------- Item 1: bare "1" footnote-marker glued onto real verse text,
# immediately after a valid leading pasuram number. Always literally "1". ----------
NUMBERED = re.compile(r'^([0-9]{1,4})(\s+)(.*)$')
GLUED_ONE = re.compile(r'^1(?=[^\s0-9])')

# ---------- Item 5: whole-line page-break footer junk ----------
FRAGMENTS = {'மப','மீடர்','பயப்','கண்றாமகாய்','றாக','றாம்','மம','வபனயறர்',
             'மொயின்','வணக','சமா','வரிப்','கறக்க','கறக்','கற்க','கணறாமலீய்',
             'கணறாஹகாட','காமக்','கணறாமகீரர்','பப','வன','மயா','பர்வம்',
             'விபர','ளி','குறம்','இதுவழி','ம்'}

def is_whole_line_junk(line):
    t = line.strip().replace('\u200c', '')
    if not t or len(t) > 40:
        return False
    if '௦' not in t and '0' not in t:
        return False
    tokens = re.findall(r'[\u0B80-\u0BFF]+|[0-9]+|[^\s]', t)
    for tok in tokens:
        if re.fullmatch(r'[0-9]+', tok): continue
        if re.fullmatch(r'[௦0-9]+', tok): continue
        if re.fullmatch(r'[.\-,\]\)ஃ:]', tok): continue
        if tok in FRAGMENTS: continue
        return False
    return True

# Four confirmed one-off stragglers (verified individually, exact literal
# text) that the general whole-line check doesn't reach.
JUNK_LINE_LITERALS = {
    '31 கணறாஹகாட.௦௦ங 9 மம விபர',
    'மம 1வபனயறர்\u200c 36 கற்க. ௦௦',
    'சமா வரிப்\u200c 1] கற்க. ௦௦',
}

def clean_inline(line):
    """Items 1 and 3: substring-level fixes that don't change line count."""
    line = TRAILING_PAREN.sub('', line)
    line = BARE_TRAILING_ZERO.sub('', line)
    m = NUMBERED.match(line)
    if m:
        num, sep, rest = m.groups()
        line = f'{num}{sep}{GLUED_ONE.sub("", rest)}'
    return line

def junk_line_indices(tamil_content):
    """Item 5: line indices to delete entirely, determined from Tamil only."""
    lines = tamil_content.split('\n')
    idx = set()
    for i, line in enumerate(lines):
        stripped = line.strip()
        if stripped in JUNK_LINE_LITERALS or is_whole_line_junk(line):
            idx.add(i)
    return idx

def process_section(section):
    tamil = section.get('content')
    if not isinstance(tamil, str):
        return False
    drop = junk_line_indices(tamil)
    changed = False
    for field in ('content', 'content_r', 'content_s'):
        old = section.get(field)
        if not isinstance(old, str):
            continue
        lines = old.split('\n')
        # Guard: only drop by index if this field's line count still
        # matches the Tamil field's (established project invariant); if a
        # field has drifted out of alignment, skip item 5 for it rather
        # than risk misaligning further, and report loudly.
        tamil_lines = tamil.split('\n')
        if len(lines) != len(tamil_lines):
            print(f'  WARNING: line-count mismatch, skipping whole-line drop for this field')
            kept = lines
        else:
            kept = [l for i, l in enumerate(lines) if i not in drop]
        new_lines = [clean_inline(l) for l in kept]
        new = '\n'.join(new_lines)
        if new != old:
            section[field] = new
            changed = True
    return changed

def main():
    changed_files = []
    files = ['prabandham.json','prabandham_irandam.json','prabandham_iyarpa.json',
             'prabandham_thiruvaimozhi.json','desika_prabandham.json']
    for fn in files:
        path = f'{ASSETS}/{fn}'
        d = json.load(open(path, encoding='utf-8'))
        touched = False
        for w in d.get('works', []):
            for s in w.get('sections', []):
                if process_section(s):
                    touched = True
        if touched:
            json.dump(d, open(path, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
            changed_files.append(fn)
    print('Changed files:', changed_files)

if __name__ == '__main__':
    main()
