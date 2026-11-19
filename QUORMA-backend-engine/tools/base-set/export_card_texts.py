#!/usr/bin/env python3
"""Exports the Italian card text of the base set to data/labels/cards.it.json.

Usage (from the repo root): python3 tools/base-set/export_card_texts.py [<vault folder, default ../QUORMA-vault>]
Reads every card note of the vault (Cards/*.md and Cards/Magie ed Equipaggiamenti/*.md): the frontmatter "id" and
"titolo" give the key and the name, the "Testo di colore" callout gives the flavour. Cross-checks the names against
docs/cards/card-effects.json and the ids against data/cards.json. Writes:

  card.<id>.name, card.<id>.flavour      one pair per card of data/cards.json
  expansion.BASE.name, expansion.BASE.description

Card text is mutable and outside the snapshot hash. Italian is the fallback locale: every key is required here, other
locales (cards.<locale>.json) may be partial. Wiki links [[a|b]] and [[a]] are flattened to plain text.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
VAULT = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT.parent / "QUORMA-vault"

EXPANSION_TEXTS = {
    "expansion.BASE.name": "Set base",
    "expansion.BASE.description": "Le 150 carte con cui comincia QUORMA: 104 creature, 26 magie e 20 equipaggiamenti, "
                                  "divise tra Resistenza Umana e Potenze Artificiali.",
}


def plain(s):
    s = re.sub(r"\[\[[^\]|]+\|([^\]]+)\]\]", r"\1", s)
    return re.sub(r"\[\[([^\]]+)\]\]", r"\1", s).strip()


def block(t, name):
    m = re.search(r"> \[!\w+\] " + re.escape(name) + r"[^\n]*\n((?:>.*\n?)+)", t)
    if not m:
        return ""
    return " ".join(l[2:] if l.startswith("> ") else l[1:] for l in m.group(1).rstrip("\n").split("\n")).strip()


def frontmatter(t, key):
    m = re.search(rf'^{key}:\s*"?(.*?)"?\s*$', t.split("\n---\n", 1)[0], re.M)
    return m.group(1) if m else ""


cards = json.loads((ROOT / "data/cards.json").read_text(encoding="utf-8"))["cards"]
effects = {c["id"]: c["title"] for c in
           json.loads((ROOT / "docs/cards/card-effects.json").read_text(encoding="utf-8"))["cards"]}
wanted = [c["id"] for c in cards]

notes = {}
for folder in (VAULT / "Cards", VAULT / "Cards" / "Magie ed Equipaggiamenti"):
    for p in sorted(folder.glob("*.md")):
        t = p.read_text(encoding="utf-8").replace("\r\n", "\n")
        cid = frontmatter(t, "id")
        if not cid.startswith("BASE-"):
            continue
        if cid in notes:
            sys.exit(f"{p.name}: id {cid} already used by {notes[cid][0]}")
        notes[cid] = (p.name, frontmatter(t, "titolo"), plain(block(t, "Testo di colore")))

problems = []
for cid in wanted:
    if cid not in notes:
        problems.append(f"{cid}: no note in the vault")
        continue
    fname, title, flavour = notes[cid]
    if not title:
        problems.append(f"{cid} ({fname}): empty titolo")
    if not flavour:
        problems.append(f"{cid} ({fname}): no 'Testo di colore'")
    if effects.get(cid) != title:
        problems.append(f"{cid}: name differs, card-effects.json '{effects.get(cid)}' vs vault '{title}'")
for cid in sorted(set(notes) - set(wanted)):
    problems.append(f"{cid} ({notes[cid][0]}): in the vault but not in data/cards.json")
if problems:
    sys.exit("\n".join(problems))

out = {}
for cid in wanted:
    out[f"card.{cid}.name"] = notes[cid][1]
    out[f"card.{cid}.flavour"] = notes[cid][2]
out.update(EXPANSION_TEXTS)
(ROOT / "data/labels/cards.it.json").write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
print(f"{len(wanted)} cards -> data/labels/cards.it.json")
