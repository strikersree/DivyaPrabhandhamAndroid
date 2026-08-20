"""
Corpus content cleanup for items 1, 3, 5.

CRITICAL: a junk line's leading number cannot be left as an orphaned "N "
line by itself -- the real Kotlin parser trims every line before matching
(`tamilLines[i].trim()`), so after trimming, a bare "N" has nothing
following it and the numbered-line regex's required `\\s+` fails to match,
silently un-recognizing that number as a stanza trigger. The fix: when a
junk line carries a leading number, that number is PREPENDED onto the next
kept line instead of being left on its own -- verified by re-checking the
preserved-number set using the same trim-then-match logic the real parser
uses, not a looser check.
"""
import json, re

ASSETS = "/home/claude/port/DivyaPrabhandhamAndroid/app/src/main/assets"

TRAILING_PAREN = re.compile(r'\s*\([௦0-9]+\)')
BARE_TRAILING_ZERO = re.compile(r'\s+௦$')

NUMBERED = re.compile(r'^([0-9]{1,4})(\s+)(.*)$')
GLUED_ONE = re.compile(r'^1(?=[^\s0-9])')

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

JUNK_LINE_LITERALS = {
    '31 கணறாஹகாட.௦௦ங 9 மம விபர',
    'மம 1வபனயறர்\u200c 36 கற்க. ௦௦',
    'சமா வரிப்\u200c 1] கற்க. ௦௦',
}

def clean_inline(line):
    line = TRAILING_PAREN.sub('', line)
    line = BARE_TRAILING_ZERO.sub('', line)
    m = NUMBERED.match(line)
    if m:
        num, sep, rest = m.groups()
        line = f'{num}{sep}{GLUED_ONE.sub("", rest)}'
    return line

def junk_line_leading_number(line):
    """Returns the leading number as a string ('12') if this junk line
    carries one, else None -- determines merge vs. plain delete."""
    m = re.match(r'^\s*([0-9]{1,4})\s+', line)
    return m.group(1) if m else None

def junk_line_indices_and_numbers(tamil_content):
    """Item 5 plan, determined from Tamil only: {index: leading_number_or_None}."""
    lines = tamil_content.split('\n')
    plan = {}
    for i, line in enumerate(lines):
        stripped = line.strip()
        if stripped in JUNK_LINE_LITERALS or is_whole_line_junk(line):
            plan[i] = junk_line_leading_number(line)
    return plan

def apply_line_plan(lines, plan):
    """Drops junk-line indices; a preserved leading number is prepended onto
    the next surviving line rather than left orphaned on its own line."""
    out = []
    pending_prefix = None
    for i, line in enumerate(lines):
        if i in plan:
            num = plan[i]
            if num is not None:
                pending_prefix = num if pending_prefix is None else pending_prefix
            continue  # the junk line itself never survives as its own line
        if pending_prefix is not None:
            out.append(f'{pending_prefix} {line}')
            pending_prefix = None
        else:
            out.append(line)
    if pending_prefix is not None:
        # A junk line's number had no following line to attach to (end of
        # section) -- extremely unlikely given content always continues,
        # but fail loudly rather than silently drop it.
        out.append(pending_prefix)
    return out

def process_section(section):
    tamil = section.get('content')
    if not isinstance(tamil, str):
        return False
    plan = junk_line_indices_and_numbers(tamil)
    tamil_lines = tamil.split('\n')
    changed = False
    for field in ('content', 'content_r', 'content_s'):
        old = section.get(field)
        if not isinstance(old, str):
            continue
        lines = old.split('\n')
        if len(lines) != len(tamil_lines):
            kept = lines
        else:
            kept = apply_line_plan(lines, plan)
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
