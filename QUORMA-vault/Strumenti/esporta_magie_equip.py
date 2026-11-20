"""Esporta le Magie e gli Equipaggiamenti del vault in due CSV (uno per fazione) da importare in Excel / Google Sheets.

Uso:  python esporta_magie_equip.py [<cartella di uscita, default Downloads>]
Gemello di esporta_excel.py: legge Cards/Magie ed Equipaggiamenti/*.md e scrive "Magie ed Equipaggiamenti - AI (dal vault).csv"
e "... - Resistenza (dal vault).csv" (UTF-8 con BOM, perché Excel riconosca gli accenti). Una riga per carta, ordinata per tipo e ID.
"""
import csv, re, sys
from pathlib import Path

VAULT = Path(__file__).resolve().parent.parent
DST = Path(sys.argv[1]) if len(sys.argv) > 1 else Path.home() / "Downloads"
COLS = ["ID", "Tipo", "Titolo carta", "Flavour text", "Lore interna", "Illustrazione", "Autore", "Stato"]

def plain(s):
    s = re.sub(r"\[\[[^\]|]+\|([^\]]+)\]\]", r"\1", s)
    return re.sub(r"\[\[([^\]]+)\]\]", r"\1", s).strip()

def block(t, name):
    m = re.search(r"> \[!\w+\] " + re.escape(name) + r"[^\n]*\n((?:>.*\n?)+)", t)
    return " ".join(l[2:] if l.startswith("> ") else l[1:] for l in m.group(1).rstrip("\n").split("\n")).strip() if m else ""

rows = []
for p in (VAULT / "Cards" / "Magie ed Equipaggiamenti").glob("*.md"):
    t = p.read_text(encoding="utf-8")
    fm = t.split("\n---\n", 1)[0]
    get = lambda k: (re.search(rf'^{k}: "?([^"\n]*)"?$', fm, re.M) or [None, ""])[1]
    autori = re.search(r"^autore:\n((?:  - .*\n?)+)", fm + "\n", re.M)
    rows.append({
        "ID": get("id"), "Fazione": get("fazione_excel"), "Tipo": get("tipo_carta"),
        "Titolo carta": get("titolo"),
        "Flavour text": plain(block(t, "Testo di colore")),
        "Lore interna": plain(block(t, "Lore interna")),
        "Illustrazione": plain((re.search(r"\*\*Versione condensata:\*\* (.*)", t) or [None, ""])[1]),
        "Autore": ", ".join(x.strip()[2:] for x in autori.group(1).strip().split("\n")) if autori else "",
        "Stato": get("stato"),
    })
rows.sort(key=lambda r: (r["Tipo"], r["ID"]))
for fz in ("AI", "Resistenza"):
    sub = [{c: r[c] for c in COLS} for r in rows if r["Fazione"] == fz]
    out = DST / f"Magie ed Equipaggiamenti - {fz} (dal vault).csv"
    with open(out, "w", encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(f, fieldnames=COLS); w.writeheader(); w.writerows(sub)
    print(f"{fz}: {len(sub)} carte -> {out}")
