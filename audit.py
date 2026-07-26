#!/usr/bin/env python3
"""
Structural audit for the Android port.

There is no Kotlin compiler in this environment (JRE only, and both the Google
and Maven repositories are blocked), so this stands in for one. It cannot catch
type errors, but it does catch the mistakes that actually happen when writing a
lot of Kotlin without a build: unbalanced delimiters, references to enum cases
that were never declared, imports of classes that do not exist, package
declarations that disagree with the directory, and manifest or resource
references that point at nothing.

Run it after every edit. Exit code is non-zero if anything is wrong.
"""

import os
import re
import sys
import json
from collections import defaultdict

ROOT = os.path.dirname(os.path.abspath(__file__))
APP = os.path.join(ROOT, "app", "src", "main")
SRC = os.path.join(APP, "java")
PKG_ROOT = "com.srinivaskannan.divyaprabhandham"

problems = []
notes = []


def fail(where, msg):
    problems.append(f"{where}: {msg}")


def note(msg):
    notes.append(msg)


def kotlin_files():
    for base, _, names in os.walk(SRC):
        for name in sorted(names):
            if name.endswith(".kt"):
                yield os.path.join(base, name)


def strip_code(text):
    """Remove comments, strings and char literals so delimiters can be counted."""
    out = []
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        # Triple-quoted string
        if text.startswith('"""', i):
            j = text.find('"""', i + 3)
            i = n if j < 0 else j + 3
            continue
        if c == '"':
            i += 1
            while i < n and text[i] != '"':
                if text[i] == "\\":
                    i += 1
                i += 1
            i += 1
            continue
        if c == "'":
            i += 1
            while i < n and text[i] != "'":
                if text[i] == "\\":
                    i += 1
                i += 1
            i += 1
            continue
        if text.startswith("//", i):
            j = text.find("\n", i)
            i = n if j < 0 else j
            continue
        if text.startswith("/*", i):
            depth, i = 1, i + 2
            while i < n and depth:
                if text.startswith("/*", i):
                    depth += 1
                    i += 2
                elif text.startswith("*/", i):
                    depth -= 1
                    i += 2
                else:
                    i += 1
            continue
        out.append(c)
        i += 1
    return "".join(out)


# ---------------------------------------------------------------- delimiters

def check_delimiters(path, code):
    pairs = {")": "(", "]": "[", "}": "{"}
    stack = []
    line = 1
    for ch in code:
        if ch == "\n":
            line += 1
        elif ch in "([{":
            stack.append((ch, line))
        elif ch in ")]}":
            if not stack:
                fail(rel(path), f"unmatched '{ch}' at line {line}")
                return
            open_ch, open_line = stack.pop()
            if open_ch != pairs[ch]:
                fail(rel(path), f"'{open_ch}' at line {open_line} closed by '{ch}' at line {line}")
                return
    for open_ch, open_line in stack:
        fail(rel(path), f"unclosed '{open_ch}' opened at line {open_line}")


def rel(path):
    return os.path.relpath(path, ROOT)


# ------------------------------------------------------------------ packages

def check_package(path, text):
    m = re.search(r"^package\s+([\w.]+)", text, re.M)
    if not m:
        fail(rel(path), "no package declaration")
        return None
    declared = m.group(1)
    expected_dir = os.path.join(SRC, *declared.split("."))
    if os.path.dirname(os.path.abspath(path)) != expected_dir:
        fail(rel(path), f"package '{declared}' does not match its directory")
    return declared


# ------------------------------------------------------------------- symbols

def collect_declarations(files):
    """Map fully-qualified name -> file, for every top-level declaration."""
    declared = {}
    pattern = re.compile(
        r"^(?:@\w+(?:\([^)]*\))?\s*)*"
        r"(?:public\s+|internal\s+|private\s+)?"
        r"(?:abstract\s+|sealed\s+|open\s+|final\s+|data\s+|value\s+|enum\s+|annotation\s+)*"
        r"(class|interface|object|fun|val|var)\s+"
        r"([A-Za-z_]\w*)",
        re.M,
    )
    for path in files:
        text = open(path, encoding="utf-8").read()
        pkg = re.search(r"^package\s+([\w.]+)", text, re.M)
        if not pkg:
            continue
        pkg = pkg.group(1)
        code = strip_code(text)
        for kind, name in pattern.findall(code):
            declared.setdefault(f"{pkg}.{name}", path)
        # Extension properties/functions on a receiver: `val Foo.bar get()`
        for m in re.finditer(r"^(?:val|var|fun)\s+[\w.<>]+\.(\w+)", code, re.M):
            declared.setdefault(f"{pkg}.{m.group(1)}", path)
    return declared


# Package roots that come from outside this project. An import that starts
# with none of these, and is not one of ours, is a mistake.
EXTERNAL_ROOTS = (
    "android.", "androidx.", "kotlin.", "kotlinx.", "java.", "javax.",
    "com.google.", "com.android.", "org.", "dalvik.",
)


def check_internal_imports(files, declared):
    """
    Every import must resolve to something.

    This checks *all* imports, not only ours. An earlier version only looked at
    imports under our own package root, and so sailed straight past a mangled
    `import ReaderPalette` that cost 39 compile errors in one file. A
    single-segment import is never valid in this project: everything lives in a
    package.
    """
    for path in files:
        text = open(path, encoding="utf-8").read()
        for m in re.finditer(r"^import\s+([\w.]+)(?:\s+as\s+\w+)?", text, re.M):
            target = m.group(1)

            if "." not in target:
                fail(rel(path), f"import '{target}' has no package — almost certainly mangled")
                continue

            if target.startswith(EXTERNAL_ROOTS):
                continue

            if not target.startswith(PKG_ROOT + "."):
                fail(rel(path), f"import '{target}' is from no package this project knows")
                continue

            # R is generated by the Android build, not by us.
            if target == PKG_ROOT + ".R" or target.startswith(PKG_ROOT + ".R."):
                continue
            if target in declared:
                continue
            # Nested types (Foo.Bar) and enum members import via their owner.
            owner = target.rsplit(".", 1)[0]
            if owner in declared:
                continue
            fail(rel(path), f"imports '{target}', which is not declared anywhere")


# ------------------------------------------------------------------ Ui table

def check_project_type_usage(files, declared):
    """
    Flags a project type that a file uses but never imported.

    The mangled-import bug this was written for showed up as an unresolved
    reference on the *usage* line, not the import line, so checking from both
    directions is worth the few lines. Only names declared somewhere in this
    project are considered — anything from a library is out of scope here.
    """
    by_simple = defaultdict(set)
    for fqn in declared:
        simple = fqn.rsplit(".", 1)[1]
        if simple[:1].isupper():
            by_simple[simple].add(fqn)

    for path in files:
        text = open(path, encoding="utf-8").read()
        pkg = re.search(r"^package\s+([\w.]+)", text, re.M)
        if not pkg:
            continue
        pkg = pkg.group(1)
        code = strip_code(text)
        imported = set(re.findall(r"^import\s+([\w.]+)", text, re.M))
        # A malformed single-segment import is reported elsewhere; here it just
        # must not crash the run.
        imported_simple = {i.rsplit(".", 1)[-1] for i in imported}
        # A wildcard import makes everything in that package visible.
        wildcards = {i[:-2] for i in re.findall(r"^import\s+([\w.]+\.\*)", text, re.M)}

        used = set(re.findall(r"\b([A-Z]\w+)\b", code))
        for name in sorted(used & by_simple.keys()):
            owners = by_simple[name]
            if name in imported_simple:
                continue
            # Declared in this same package: no import needed.
            if any(o.rsplit(".", 1)[0] == pkg for o in owners):
                continue
            if any(o.rsplit(".", 1)[0] in wildcards for o in owners):
                continue
            # Referenced only as a qualified name, e.g. `theme.ReaderPalette`.
            if re.search(r"[\w.]+\.%s\b" % re.escape(name), code):
                continue
            fail(rel(path), f"uses project type '{name}' but never imports it "
                            f"(declared in {sorted(owners)[0]})")


def check_state_read_from_ui(files):
    """
    Flags mutable fields the UI reads that are not snapshot-backed.

    Compose only re-renders when it observes a change, and it only observes
    state created by mutableStateOf and friends. A plain `var` read from a
    composable registers no subscription at all, so assigning it later schedules
    nothing — the screen silently keeps whatever it drew first.

    This is written from a real failure: the Application published its four
    startup objects into four fields, two of them plain vars. A recomposition
    triggered by one of the snapshot-backed fields ran while the plain ones were
    still null, read them, subscribed to nothing, and the app sat on a blank
    screen forever.
    """
    ui_sources = []
    for path in files:
        rp = rel(path).replace(os.sep, "/")
        if "/ui/" in rp or rp.endswith("MainActivity.kt"):
            ui_sources.append(open(path, encoding="utf-8").read())
    if not ui_sources:
        return

    state_factories = ("mutableStateOf", "mutableStateListOf", "mutableStateMapOf",
                       "mutableIntStateOf", "mutableFloatStateOf", "mutableLongStateOf",
                       "derivedStateOf")

    for path in files:
        rp = rel(path).replace(os.sep, "/")
        if "/ui/" in rp:
            continue
        code = strip_code(open(path, encoding="utf-8").read())
        # Non-private `var` declarations. Everything after the name is taken
        # as the declaration tail, because the snapshot factory can appear
        # either after `=` or after `by`, and an earlier version of this regex
        # tried to split on `=` and so read every `by mutableStateOf(...)`
        # property as unbacked.
        for m in re.finditer(r"^\s{4}(?!private)(?:internal\s+)?var\s+(\w+)\b(.*)$",
                             code, re.M):
            name, tail = m.group(1), m.group(2)
            if any(f in tail for f in state_factories):
                continue
            # Only a problem if the UI actually reads it.
            if not any(re.search(r"\.%s\b" % re.escape(name), src) for src in ui_sources):
                continue
            fail(rel(path), f"'var {name}' is read from the UI but is not "
                            f"snapshot-backed — Compose will not recompose when "
                            f"it changes; wrap it in mutableStateOf")


def check_jvm_setter_clash(files):
    """
    Catches Kotlin/JVM platform declaration clashes.

    A `var foo` whose setter is not public still generates a `setFoo(...)`
    method, so declaring `fun setFoo(value)` in the same class collides with it.
    Properties with an `internal` setter escape only because Kotlin mangles
    internal accessors with a module suffix — which makes this a trap that
    fires for some properties and not others, for reasons invisible at the
    call site.
    """
    for path in files:
        code = strip_code(open(path, encoding="utf-8").read())
        properties = set(re.findall(r"^\s*(?:private\s+|internal\s+)?var\s+(\w+)\s*:", code, re.M))
        functions = set(re.findall(r"^\s*(?:private\s+|internal\s+)?fun\s+(set[A-Z]\w*)\s*\(", code, re.M))
        for prop in sorted(properties):
            generated = "set" + prop[0].upper() + prop[1:]
            if generated in functions:
                fail(rel(path), f"'fun {generated}(...)' clashes with the setter "
                                f"generated for 'var {prop}' — rename it to "
                                f"'update{prop[0].upper() + prop[1:]}'")


def check_ui_keys(files):
    ui_path = os.path.join(SRC, *PKG_ROOT.split("."), "data", "UiText.kt")
    text = open(ui_path, encoding="utf-8").read()
    body = text[text.index("enum class Ui {"): text.index("object UiText")]
    cases = set(re.findall(r"^\s*([A-Z][A-Z0-9_]*),", body, re.M))
    entries = set(re.findall(r"Ui\.([A-Z][A-Z0-9_]*)\s+to\s+\(", text))

    missing_entry = cases - entries
    orphan_entry = entries - cases
    if missing_entry:
        fail("UiText.kt", f"enum cases with no string: {sorted(missing_entry)}")
    if orphan_entry:
        fail("UiText.kt", f"strings with no enum case: {sorted(orphan_entry)}")

    used = set()
    for path in files:
        text = open(path, encoding="utf-8").read()
        if path.endswith("UiText.kt"):
            continue
        used |= set(re.findall(r"\bUi\.([A-Z][A-Z0-9_]*)\b", text))

    unknown = used - cases
    if unknown:
        fail("Ui references", f"used but never declared: {sorted(unknown)}")

    unused = cases - used
    if unused:
        note(f"{len(unused)} Ui strings are declared but never used: {sorted(unused)}")
    return cases


# ------------------------------------------------------------- Android bits

def check_manifest(files, declared):
    path = os.path.join(APP, "AndroidManifest.xml")
    xml = open(path, encoding="utf-8").read()

    for m in re.finditer(r'android:name="(\.[\w.]+)"', xml):
        cls = PKG_ROOT + m.group(1)
        if cls not in declared:
            fail("AndroidManifest.xml", f"declares '{cls}', which is not defined")

    # Resource references
    for m in re.finditer(r'"@(\w+)/(\w+)"', xml):
        kind, name = m.group(1), m.group(2)
        if kind in ("android", "style"):
            continue
        if not resource_exists(kind, name):
            fail("AndroidManifest.xml", f"references @{kind}/{name}, which does not exist")


def resource_exists(kind, name):
    res = os.path.join(APP, "res")
    # Glance supplies this layout from its own library.
    if name == "glance_default_loading_layout":
        return True
    if kind == "mipmap":
        for base, _, names in os.walk(res):
            if any(n.startswith(name + ".") for n in names):
                return True
        return False
    for base, _, names in os.walk(res):
        folder = os.path.basename(base).split("-")[0]
        if folder == kind:
            for n in names:
                if n.rsplit(".", 1)[0] == name:
                    return True
        # values files declare resources by name
        if folder == "values":
            for n in names:
                if not n.endswith(".xml"):
                    continue
                text = open(os.path.join(base, n), encoding="utf-8").read()
                if re.search(r'name="%s"' % re.escape(name), text):
                    return True
    return False


def check_r_references(files):
    res_names = defaultdict(set)
    res = os.path.join(APP, "res")
    for base, _, names in os.walk(res):
        folder = os.path.basename(base).split("-")[0]
        for n in names:
            stem = n.rsplit(".", 1)[0]
            if folder == "values" and n.endswith(".xml"):
                text = open(os.path.join(base, n), encoding="utf-8").read()
                for m in re.finditer(r'<(string|color|style|array|integer|bool)\s+name="([\w.]+)"', text):
                    res_names[m.group(1)].add(m.group(2))
            else:
                res_names[folder].add(stem)

    for path in files:
        text = open(path, encoding="utf-8").read()
        for m in re.finditer(r"\bR\.(\w+)\.(\w+)\b", text):
            kind, name = m.group(1), m.group(2)
            if name not in res_names.get(kind, set()):
                fail(rel(path), f"references R.{kind}.{name}, which does not exist")


def check_gradle():
    catalog = open(os.path.join(ROOT, "gradle", "libs.versions.toml"), encoding="utf-8").read()
    versions = set(re.findall(r"^(\w[\w-]*)\s*=", catalog.split("[libraries]")[0], re.M))
    libs = set(re.findall(r"^([\w-]+)\s*=\s*\{", catalog.split("[libraries]")[1].split("[plugins]")[0], re.M))
    plugins = set(re.findall(r"^([\w-]+)\s*=\s*\{", catalog.split("[plugins]")[1], re.M))

    # Every version.ref must exist
    for ref in re.findall(r'version\.ref\s*=\s*"([\w-]+)"', catalog):
        if ref not in versions:
            fail("libs.versions.toml", f"version.ref '{ref}' is not declared")

    for gradle in ("build.gradle.kts", os.path.join("app", "build.gradle.kts")):
        text = open(os.path.join(ROOT, gradle), encoding="utf-8").read()
        for m in re.finditer(r"libs\.((?:\w+\.)*\w+)", text):
            alias = m.group(1)
            if alias.startswith("plugins."):
                key = alias[len("plugins."):].replace(".", "-")
                pool = plugins
            else:
                key = alias.replace(".", "-")
                pool = libs
            if key not in pool:
                fail(gradle, f"uses libs alias '{alias}' -> '{key}', which is not in the catalog")


def check_assets():
    """Every division/resource the code expects must actually be bundled."""
    assets = os.path.join(APP, "assets")
    present = {n[:-5] for n in os.listdir(assets) if n.endswith(".json")}
    divisions_kt = open(
        os.path.join(SRC, *PKG_ROOT.split("."), "data", "Divisions.kt"), encoding="utf-8"
    ).read()
    for res in re.findall(r'resource\s*=\s*"(\w+)"', divisions_kt):
        if res not in present:
            fail("assets", f"Divisions.kt expects {res}.json, which is not bundled")
    for extra in ("essences", "decad_essences", "divyadesams", "recitations", "azhwars"):
        if extra not in present:
            fail("assets", f"{extra}.json is referenced by the repository but not bundled")
    # And the JSON must actually parse.
    for name in sorted(present):
        try:
            json.load(open(os.path.join(assets, name + ".json"), encoding="utf-8"))
        except Exception as exc:
            fail("assets", f"{name}.json does not parse: {exc}")


def check_composable_previews(files):
    """@Composable functions must be capitalised, or Compose tooling complains."""
    for path in files:
        text = open(path, encoding="utf-8").read()
        for m in re.finditer(r"@Composable\s*(?:@\w+(?:\([^)]*\))?\s*)*"
                             r"(?:private\s+|internal\s+|public\s+)?fun\s+([a-z]\w*)"
                             r"\s*\([^)]*\)\s*(:)?", text, re.S):
            name, returns = m.group(1), m.group(2)
            # A composable that returns a value is conventionally lowercase.
            if returns or name.startswith("remember") or name.startswith("current"):
                continue
            note(f"{rel(path)}: @Composable '{name}' is lowercase; "
                 "capitalise it unless it returns a value")


def main():
    files = list(kotlin_files())
    if not files:
        fail("project", "no Kotlin sources found")
        report()
        return

    for path in files:
        text = open(path, encoding="utf-8").read()
        check_package(path, text)
        check_delimiters(path, strip_code(text))

    declared = collect_declarations(files)

    # Each check is isolated: one blowing up must not hide what the others
    # already found. Learned the hard way — a check crashed on exactly the
    # malformed import it existed to catch, and the run printed nothing at all.
    checks = [
        ("imports", lambda: check_internal_imports(files, declared)),
        ("type usage", lambda: check_project_type_usage(files, declared)),
        ("UI state backing", lambda: check_state_read_from_ui(files)),
        ("JVM setter clash", lambda: check_jvm_setter_clash(files)),
        ("Ui table", lambda: check_ui_keys(files)),
        ("manifest", lambda: check_manifest(files, declared)),
        ("resources", lambda: check_r_references(files)),
        ("gradle", check_gradle),
        ("assets", check_assets),
        ("composables", lambda: check_composable_previews(files)),
    ]
    for name, check in checks:
        try:
            check()
        except Exception as exc:
            fail("audit", f"the '{name}' check itself crashed: {exc!r}")

    print(f"audited {len(files)} Kotlin files, {len(declared)} top-level declarations")
    report()


def report():
    if notes:
        print("\nNotes (not failures):")
        for n in notes:
            print(f"  - {n}")
    if problems:
        print(f"\n{len(problems)} PROBLEM(S):")
        for p in problems:
            print(f"  ! {p}")
        sys.exit(1)
    print("\nNo structural problems found.")


if __name__ == "__main__":
    main()
