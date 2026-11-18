"""Esporta le carte del vault nei due fogli Excel (AI, Resistenza) come CSV da importare in Google Sheets.

Uso:  python esporta_excel.py <cartella dei CSV del foglio> [<cartella di uscita>]
Legge l'ordine degli slot e le sottofazioni da "Carte - AI.csv" e "Carte - Resistenza.csv" (scaricati dal foglio),
riempie ogni slot con la carta del vault che lo occupa e scrive "Carte - AI (dal vault).csv" e "Carte - Resistenza (dal vault).csv".
Il vault è la fonte: le righe già compilate nel foglio vengono sovrascritte con il testo del vault.
"""
import csv, re, sys
from pathlib import Path

VAULT = Path(__file__).resolve().parent.parent
SRC = Path(sys.argv[1]) if len(sys.argv) > 1 else Path.home() / "Downloads"
DST = Path(sys.argv[2]) if len(sys.argv) > 2 else SRC
OVERRIDE = {("Resistenza", "J", "Vincolo"): "PORTANOME"}  # decisione 2026-11-14: J · Vincolo diventa Portanome
COLS = ["Rank", "Seme", "Sottofazione", "Titolo carta", "Flavour text", "Lore interna", "Illustrazione", "Autore"]

def plain(s):
    s = re.sub(r"\[\[[^\]|]+\|([^\]]+)\]\]", r"\1", s)
    return re.sub(r"\[\[([^\]]+)\]\]", r"\1", s).strip()

def block(t, name):
    m = re.search(r"> \[!\w+\] " + re.escape(name) + r"[^\n]*\n((?:>.*\n?)+)", t)
    return " ".join(l[2:] if l.startswith("> ") else l[1:] for l in m.group(1).rstrip("\n").split("\n")).strip() if m else ""

cards = {}
for p in (VAULT / "Cards").glob("*.md"):
    t = p.read_text(encoding="utf-8")
    fm = t.split("\n---\n", 1)[0]
    get = lambda k: (re.search(rf'^{k}: "?([^"\n]*)"?$', fm, re.M) or [None, ""])[1]
    autori = re.search(r"^autore:\n((?:  - .*\n?)+)", fm + "\n", re.M)
    cards[(get("fazione_excel"), get("rango"), get("seme"))] = {
        "Titolo carta": get("titolo"),
        "Flavour text": plain(block(t, "Testo di colore")),
        "Lore interna": plain(block(t, "Lore interna")),
        "Illustrazione": plain((re.search(r"\*\*Versione condensata:\*\* (.*)", t) or [None, ""])[1]),
        "Autore": ", ".join(x.strip()[2:] for x in autori.group(1).strip().split("\n")) if autori else "",
    }

used = set()
for sheet in ("AI", "Resistenza"):
    rows = list(csv.DictReader(open(SRC / f"Carte - {sheet}.csv", encoding="utf-8")))
    out = []
    for r in rows:
        key = (sheet, r["Rank"].replace(".0", ""), r["Seme"])
        row = {c: r.get(c, "") for c in COLS}
        row["Rank"] = key[1]
        row["Sottofazione"] = OVERRIDE.get(key, r["Sottofazione"])
        if key in cards:
            row.update(cards[key]); used.add(key)
        out.append(row)
    with open(DST / f"Carte - {sheet} (dal vault).csv", "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=COLS); w.writeheader(); w.writerows(out)
    print(f"{sheet}: {sum(1 for r in out if r['Titolo carta'])} carte su {len(out)} slot")
missing = set(cards) - used
if missing: print("Carte senza slot nel foglio:", sorted(missing))
