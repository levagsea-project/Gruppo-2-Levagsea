"""Controlla il registro del canone e i collegamenti del vault.

Uso:  python Strumenti/controlla_canone.py [--orfani]

Segnala sempre:
  - link rotti, e link che puntano a un alias (Obsidian non li risolve)
  - link a «#Glossario» verso note che non hanno la sezione
  - CAN duplicati, categorie non ammesse
Con --orfani elenca anche gli elementi di canone che nessuna nota collega, a parte la carta che li ha introdotti:
sono i primi da riusare quando si scrive una carta nuova.
Avvisa, senza contarlo come errore, delle carte nuove (data uguale o successiva a DATA_CARTE_NUOVE) con più di due
elementi nuovi oltre alla carta stessa: le carte scritte prima possono superare il limite e non vengono segnalate.
Esce con codice 1 se trova errori (avvisi e orfani non sono errori).
"""
import re, sys
from collections import defaultdict
from pathlib import Path

VAULT = Path(__file__).resolve().parent.parent
CATEGORIE = {"Mondo", "Luogo", "Oggetto", "Entità", "Pratica", "Seme", "Sottofazione", "Carta"}
MAX_NUOVI = 2
DATA_CARTE_NUOVE = "2026-11-17"   # le carte con data precedente sono scritte prima della regola
IGNORA = {"📄 Template Carta.md", "Prompt Carte.md", "Prompt Magie ed Equipaggiamenti.md", "Carta.md",
          "Magia.md", "Equipaggiamento.md", "Elemento di canone.md"}

def campo(testo, chiave):
    m = re.search(rf'^{chiave}: *"?(.*?)"?\s*$', testo, re.M)
    return m.group(1) if m else ""

note = {p: p.read_text(encoding="utf-8") for p in VAULT.rglob("*.md")
        if not {".trash", ".obsidian"} & set(p.parts)}
nomi = {p.stem for p in note} | {p.name for p in VAULT.rglob("*") if p.is_file() and ".trash" not in p.parts}
alias = set()
for t in note.values():
    m = re.search(r"^aliases:\n((?:  - .*\n)+)", t, re.M)
    if m:
        alias |= {a.strip().strip('"') for a in re.findall(r"  - (.*)", m.group(1))}

errori, avvisi, usi = [], [], defaultdict(set)
for p, t in note.items():
    if p.name in IGNORA:
        continue
    senza_elenco = re.sub(r"> \[!warning\][^\n]*\n(?:>.*\n?)*", "", t)   # l'elenco dei nuovi elementi non conta come uso
    for bersaglio, ancora in re.findall(r"\[\[([^\]|#]*)(#[^\]|]*)?", t):
        if not bersaglio:
            continue
        if bersaglio not in nomi:
            tipo = "link a un alias" if bersaglio in alias else "link rotto"
            errori.append(f"{tipo}: [[{bersaglio}]] in {p.relative_to(VAULT)}")
        elif ancora == "#Glossario":
            dest = next(q for q in note if q.stem == bersaglio)
            if "## Glossario" not in note[dest]:
                errori.append(f"glossario mancante: [[{bersaglio}#Glossario]] in {p.relative_to(VAULT)}")
    for bersaglio in re.findall(r"\[\[([^\]|#]+)", senza_elenco):
        usi[bersaglio].add(p.stem)

canone = {p: t for p, t in note.items() if campo(t, "can") and "Strumenti" not in p.parts}
visti = defaultdict(list)
for p, t in canone.items():
    visti[campo(t, "can")].append(p.stem)
    cat = campo(t, "categoria")
    if cat not in CATEGORIE:
        errori.append(f"categoria non ammessa «{cat}»: {p.relative_to(VAULT)}")
for can, chi in visti.items():
    if len(chi) > 1:
        errori.append(f"{can} duplicato: {', '.join(sorted(chi))}")

for p, t in note.items():
    if "Cards" in p.parts:
        m = re.search(r"> \[!warning\][^\n]*\n((?:>.*\n?)*)", t)
        carte = {q.stem for q in note if "Cards" in q.parts}   # una carta citata nell'elenco non è un elemento nuovo
        nuovi = [n for n in re.findall(r"\*\*CAN-\d+\*\* \[\[([^\]|#]+)", m.group(1)) if n not in carte] if m else []
        if len(nuovi) > MAX_NUOVI and campo(t, "data") >= DATA_CARTE_NUOVE:
            avvisi.append(f"{len(nuovi)} elementi nuovi (massimo {MAX_NUOVI} per le carte nuove): {p.stem}")

orfani = []
for p, t in canone.items():
    if "Canone" not in p.parts:
        continue
    fonte = re.sub(r"[\[\]]", "", campo(t, "fonte"))
    if not usi[p.stem] - {fonte, p.stem}:
        orfani.append((campo(t, "categoria"), p.stem, fonte))

print(f"Canone: {sum(1 for p in canone if 'Canone' in p.parts)} elementi, {sum(1 for p in note if 'Cards' in p.parts)} carte.")
print(f"Elementi mai riusati fuori dalla carta che li ha introdotti: {len(orfani)}")
if "--orfani" in sys.argv:
    for cat, nome, fonte in sorted(orfani):
        print(f"  {cat:8} {nome}  (da {fonte})")
if avvisi:
    print(f"\n{len(avvisi)} avvisi:")
    for a in sorted(avvisi):
        print("  " + a)
if errori:
    print(f"\n{len(errori)} errori:")
    for e in sorted(errori):
        print("  " + e)
    sys.exit(1)
print("Nessun errore.")
