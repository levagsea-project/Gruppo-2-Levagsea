"""Elenca gli slot del set base: quali sono occupati da una carta del vault e quali sono liberi.

Uso:  python Strumenti/elenca_slot.py

Legge la griglia da Strumenti/slot_set_base.csv (fazione, rango, seme, sottofazione: 104 slot, dal foglio Excel)
e le carte da Cards/. Riscrive:
  - la nota «🗂️ Slot del set base.md», con le due griglie e l'elenco degli slot liberi
  - la sezione «## SLOT LIBERI» di «Strumenti/Prompt Carte.md»
Va rilanciato dopo ogni carta nuova. Segnala le carte la cui sottofazione non corrisponde alla griglia.
"""
import csv, re, sys
from datetime import date
from pathlib import Path

VAULT = Path(__file__).resolve().parent.parent
RANGHI = ["2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"]
SEMI = ["Vincolo", "Frattura", "Spira", "Vestigio"]
FAZIONI = {"AI": "Potenze Artificiali", "Resistenza": "Resistenza Umana"}

griglia = {}
with open(VAULT / "Strumenti" / "slot_set_base.csv", encoding="utf-8", newline="") as f:
    for r in csv.DictReader(f):
        griglia[(r["fazione"], r["rango"], r["seme"])] = r["sottofazione"]

carte, errori = {}, []
for p in sorted((VAULT / "Cards").glob("*.md")):
    fm = p.read_text(encoding="utf-8").split("\n---\n", 1)[0]
    get = lambda k: (re.search(rf'^{k}: "?([^"\n]*)"?$', fm, re.M) or [None, ""])[1]
    chiave = (get("fazione_excel"), get("rango"), get("seme"))
    if chiave not in griglia:
        errori.append(f"slot inesistente {chiave}: {p.stem}")
    elif griglia[chiave].lower() != get("sottofazione").lower():
        errori.append(f"sottofazione diversa dalla griglia ({get('sottofazione')} invece di {griglia[chiave]}): {p.stem}")
    elif chiave in carte:
        errori.append(f"slot occupato due volte {chiave}: {carte[chiave]} e {p.stem}")
    else:
        carte[chiave] = p.stem

liberi = [k for k in griglia if k not in carte]
ordine = lambda k: (list(FAZIONI).index(k[0]), RANGHI.index(k[1]), SEMI.index(k[2]))

def per_sottofazione(faz):
    gruppi = {}
    for k in sorted(liberi, key=ordine):
        if k[0] == faz:
            gruppi.setdefault(griglia[k], []).append(f"{k[1]} {k[2]}")
    return gruppi

# ---------- nota
oggi = date.today().isoformat()
n = ["---", "tags:", "  - hub", "  - slot", "---", "", "# 🗂️ Slot del set base", "",
     f"Il set base ha {len(griglia)} slot: 2 fazioni × 13 ranghi × 4 semi. La sottofazione di ogni slot è fissata dal foglio Excel.",
     "Questa nota è generata da `Strumenti/elenca_slot.py`: non modificarla a mano, rilancia lo script dopo ogni carta nuova.", "",
     "| | Occupati | Liberi |", "|---|---|---|"]
for faz in FAZIONI:
    occ = sum(1 for k in carte if k[0] == faz)
    n.append(f"| {FAZIONI[faz]} | {occ} | {52 - occ} |")
n.append(f"| **Totale** | **{len(carte)}** | **{len(liberi)}** |")
for faz in FAZIONI:
    n += ["", f"## {FAZIONI[faz]}", "", "| Rango | " + " | ".join(SEMI) + " |", "|---|" + "---|" * len(SEMI)]
    for r in RANGHI:
        celle = []
        for s in SEMI:
            k = (faz, r, s)
            celle.append(f"{griglia[k]} · [[{carte[k]}]]" if k in carte else f"{griglia[k]} · **libero**")
        n.append(f"| **{r}** | " + " | ".join(celle) + " |")
    n += ["", f"### Slot liberi · {FAZIONI[faz]}", ""]
    for sf, slot in per_sottofazione(faz).items():
        n.append(f"- **{sf}** ({len(slot)}): " + " · ".join(slot))
n += ["", "---", "", f"*Generata: {oggi}*", ""]
(VAULT / "🗂️ Slot del set base.md").write_text("\n".join(n), encoding="utf-8", newline="\n")

# ---------- sezione del prompt
prompt = VAULT / "Strumenti" / "Prompt Carte.md"
t = prompt.read_text(encoding="utf-8")
sez = [f"## SLOT LIBERI (aggiornato: {oggi})",
       f"Slot ancora senza carta: {len(liberi)} su {len(griglia)}. Ogni voce è «rango seme»; la sottofazione è fissa. Se l'input non indica uno slot, scegline uno da qui e dichiaralo. Non usare uno slot che non compare in questo elenco: è già occupato."]
for faz in FAZIONI:
    sez.append(f"{FAZIONI[faz].upper()}")
    for sf, slot in per_sottofazione(faz).items():
        sez.append(f"- {sf}: " + ", ".join(slot))
blocco = "\n".join(sez) + "\n\n"
if "## SLOT LIBERI" in t:
    t = re.sub(r"## SLOT LIBERI.*?\n(?=## )", lambda m: blocco, t, count=1, flags=re.S)
else:
    assert "## REGOLE DI GIOCO UTILI" in t, "sezione di riferimento non trovata nel prompt"
    t = t.replace("## REGOLE DI GIOCO UTILI", blocco + "## REGOLE DI GIOCO UTILI", 1)
prompt.write_text(t, encoding="utf-8", newline="\n")

print(f"Slot: {len(griglia)} · occupati {len(carte)} · liberi {len(liberi)}")
if errori:
    print(f"\n{len(errori)} errori:")
    for e in errori:
        print("  " + e)
    sys.exit(1)
