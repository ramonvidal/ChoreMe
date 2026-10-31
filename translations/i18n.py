#!/usr/bin/env python3
"""The Baianês texts, as one table.

    python3 translations/i18n.py export   # every English text + its Baianês, to baianes.csv
    python3 translations/i18n.py apply    # baianes.csv back into each module's values-pt-rBR

Columns: module, key, quantity (for counted texts like "%d points": one/other), english,
baianes. Edit only the baianes column. Keep %1$s, %2$d, %d as they are: the app fills them
in (a name, a number). An empty baianes cell falls back to English in the app.
"""
import csv
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TABLE = ROOT / "translations" / "baianes.csv"
LOCALE = "values-pt-rBR"


def modules():
    for english in sorted(ROOT.glob("**/src/main/res/values/strings.xml")):
        if "build" in english.parts or "node_modules" in english.parts:
            continue
        yield english.parent.parent.parent.parent.parent.relative_to(ROOT).as_posix(), english


def unescape(text):
    return (text or "").replace("\\'", "'").replace('\\"', '"').replace("\\n", "\n")


def escape(text):
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = text.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    return "\\" + text if text[:1] in "@?" else text


def entries(path):
    """(key, quantity, text) for each string; quantity is '' except for plurals."""
    if not path.exists():
        return []
    out = []
    for node in ET.parse(path).getroot():
        if node.get("translatable") == "false":
            continue
        if node.tag == "string":
            out.append((node.get("name"), "", unescape(node.text)))
        elif node.tag == "plurals":
            for item in node:
                out.append((node.get("name"), item.get("quantity"), unescape(item.text)))
    return out


def export():
    rows = []
    for module, english in modules():
        local = {(k, q): t for k, q, t in entries(english.parent.parent / LOCALE / "strings.xml")}
        for key, quantity, text in entries(english):
            rows.append([module, key, quantity, text, local.get((key, quantity), "")])
    with TABLE.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["module", "key", "quantity", "english", "baianes"])
        writer.writerows(rows)
    print(f"{len(rows)} texts in {TABLE.relative_to(ROOT)}")


def apply():
    table = {}
    with TABLE.open(newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            if row["baianes"].strip():
                table[(row["module"], row["key"], row["quantity"])] = row["baianes"]
    missing = 0
    for module, english in modules():
        lines = ['<?xml version="1.0" encoding="utf-8"?>',
                 "<!-- Baianês. Made from translations/baianes.csv by translations/i18n.py: edit the table, not this file. -->",
                 "<resources>"]
        plurals = {}
        for key, quantity, _ in entries(english):
            text = table.get((module, key, quantity))
            if text is None:
                missing += 1
                continue
            if quantity:
                plurals.setdefault(key, []).append((quantity, text))
            else:
                lines.append(f'    <string name="{key}">{escape(text)}</string>')
        for key, items in plurals.items():
            lines.append(f'    <plurals name="{key}">')
            lines += [f'        <item quantity="{q}">{escape(t)}</item>' for q, t in items]
            lines.append("    </plurals>")
        lines.append("</resources>")
        out = english.parent.parent / LOCALE / "strings.xml"
        out.parent.mkdir(exist_ok=True)
        out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {LOCALE} for every module; {missing} texts still in English only")


if __name__ == "__main__":
    {"export": export, "apply": apply}[sys.argv[1] if len(sys.argv) > 1 else "export"]()
