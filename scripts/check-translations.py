"""
Check strings.xml files for untranslated strings and output an HTML report.
"""

import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from html import escape
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
REPO_ROOT = SCRIPT_DIR.parent
RESOURCES_DIR = REPO_ROOT / "composeApp/src/commonMain/composeResources"
DOCS_DIR = REPO_ROOT / "docs"

STYLES = """
<style>
  pre {
    white-space: pre-wrap;
    overflow-wrap: break-word;
    max-width: 80ch;
    background: #f8f8f8;
    border: 1px solid #ddd;
    padding: 0.5em 1em;
    font-size: 0.85em;
  }
  .xp { color: #666; }
  .xt { color: #0066bb; }
  .xa { color: #7700bb; }
  .xv { color: #007700; }
  .xc { color: #bb5500; }
  @media (prefers-color-scheme: dark) {
    pre { background: #1e1e1e; border-color: #444; }
    .xp { color: #999; }
    .xt { color: #6699ff; }
    .xa { color: #cc77ff; }
    .xv { color: #66bb66; }
    .xc { color: #ffaa44; }
  }
</style>
"""

def highlight_xml(raw):
    """Return a syntax-highlighted HTML string for a single XML element line."""
    m = re.fullmatch(
        r'<([\w:-]+)((?:\s+[\w:-]+=(?:"[^"]*"|\'[^\']*\'))*)\s*>(.*?)</([\w:-]+)>',
        raw,
        re.DOTALL,
    )
    if not m:
        return escape(raw)

    open_tag, attrs_str, text, close_tag = m.groups()

    def fmt_attrs(s):
        return re.sub(
            r'(\s+)([\w:-]+)(=)("[^"]*")',
            lambda m: (
                m.group(1)
                + f'<span class="xa">{escape(m.group(2))}</span>'
                + '<span class="xp">=</span>'
                + f'<span class="xv">{escape(m.group(4))}</span>'
            ),
            s,
        )

    p = '<span class="xp">'
    t = f'<span class="xt">{escape(open_tag)}</span>'
    return (
        f'{p}&lt;</span>{t}{fmt_attrs(attrs_str)}{p}&gt;</span>'
        f'<span class="xc">{escape(text)}</span>'
        f'{p}&lt;/</span>{t}{p}&gt;</span>'
    )


def parse_strings(xml_path):
    """Return a dict of {name: element} for all <string> elements."""
    tree = ET.parse(xml_path)
    root = tree.getroot()
    strings = {}
    for elem in root.findall("string"):
        name = elem.get("name")
        translatable = elem.get("translatable", "true").lower()
        if name and translatable != "false":
            strings[name] = elem
    return strings

def find_language_dirs():
    """Return list of (lang_code, strings_xml_path) for each values-XX
    directory."""
    langs = []
    for entry in sorted(RESOURCES_DIR.iterdir()):
        if entry.is_dir() and entry.name.startswith("values-"):
            lang_code = entry.name[len("values-"):]
            strings_file = entry / "strings.xml"
            if strings_file.exists():
                langs.append((lang_code, strings_file))
    return langs

def main():
    base_strings_path = RESOURCES_DIR / "values/strings.xml"
    if not base_strings_path.exists():
        print(
            f"Error: base strings.xml not found at {base_strings_path}",
            file=sys.stderr,
        )
        sys.exit(1)

    base_strings = parse_strings(base_strings_path)
    lang_dirs = find_language_dirs()

    if not lang_dirs:
        print("No language directories found.", file=sys.stderr)
        sys.exit(1)

    header = (DOCS_DIR / "header.html").read_text(encoding="utf-8")
    footer = (DOCS_DIR / "footer.html").read_text(encoding="utf-8")

    try:
        branch = subprocess.check_output(
            ["git", "rev-parse", "--abbrev-ref", "HEAD"],
            cwd=REPO_ROOT, text=True, stderr=subprocess.DEVNULL,
        ).strip()
        if branch == "HEAD":
            branch = os.environ.get("CI_COMMIT_BRANCH") or branch
        git_info = f'<p>Branch: <code>{escape(branch)}</code></p>\n'
    except (subprocess.CalledProcessError, FileNotFoundError):
        git_info = ""

    sections = []
    sections.append(STYLES)
    sections.append("<h1>Untranslated Strings</h1>\n")
    sections.append(git_info)

    any_missing = False
    for lang_code, strings_path in lang_dirs:
        lang_strings = parse_strings(strings_path)
        missing = [
            elem
            for name, elem in base_strings.items()
            if name not in lang_strings
        ]

        sections.append(f"<h2>{escape(lang_code)}</h2>\n")

        if missing:
            any_missing = True
            xml_lines = "\n".join(
                highlight_xml(ET.tostring(elem, encoding="unicode").strip())
                for elem in missing
            )
            sections.append(f"<pre>{xml_lines}</pre>\n")
        else:
            sections.append("<p>All strings translated.</p>\n")

    if not any_missing:
        sections.append("<p>All languages are fully translated.</p>\n")

    output = header + "\n" + "".join(sections) + "\n" + footer

    print(output)

if __name__ == "__main__":
    main()
