# docs/: agent guide

## Layout

| Path | Content | Status |
|---|---|---|
| `rules/` | Ruleset (Regolamento v1.0-alpha) as Markdown | **Authoritative source.** Contains the Vincolo 4 correction of 2026-11-01. The PDF in `pdf/` predates it. |
| `lore/` | LEVAGSEA lore dossier v2.0 as Markdown | Put the original source here when available. |
| `architecture/` | Architecture proposal (`Proposta Architettura.md`) | Same. |
| `demo/` | The 3-turn gameplay demo | Has known errors, see `KNOWN-ISSUES.md` before using it as a test. |
| `pdf/` | External PDF exports for humans to share | **Read-only.** Never edit. Not an export copy of a `.md` in this folder. |
| `DECISIONS.md` | Design decisions taken, with status | Committed only (rejected kept). |
| `KNOWN-ISSUES.md` | Contradictions, gaps, items to fix | Add before guessing. |

Agents read Markdown far better than PDFs. Prefer `rules/`, `lore/`, `architecture/`, and never read `pdf/` unless nothing else exists.

## Conventions for PDF export copies

- A PDF or TXT file in the **same folder as a `.md` source** (for example `Proposta Architettura.pdf` next to `Proposta Architettura.md`, or `regolamento-v1.0-alpha.pdf` next to `regolamento-v1.0-alpha.md`) is an **export copy for humans to share and read**, generated from the `.md` and may go stale or lose formatting.
- **Agents must always use the `.md` file.** Ignore same-folder PDF and TXT export copies for any agent work. This rule applies across `docs/`.
- `pdf/` holds **external PDF exports** (for example `demo-gameplay-3-turni.pdf`, `lore-levagsea-v2.0.pdf`) that are not export copies of a `.md` in the same folder. These are for humans to read; agents do not use them as source material and never edit them.

## Authority

- Ruleset: authoritative for turn structure, hands, spill, tiers, deck limits. Numbers in `[brackets]` are placeholders.
- Lore: authoritative for names, factions, doctrines. The doctrine cycles give *narrative reasons* for matchups, but do not add abilities by themselves (the dossier says so explicitly).
- Architecture: a proposal. Open points are in `DECISIONS.md`.

## Writing rules

- Italian for anything players will read, English for anything developers will read.
- Keep the Italian faction and subfaction names exactly as written.
- Do not assign AI subfactions to planetary sovereigns or alliances: the dossier lists this as open.
- When you change a rule in `data/`, add the wording change to the ruleset section in `KNOWN-ISSUES.md` until the ruleset itself is updated.

## Glossary

| Italian | English |
|---|---|
| Valutazione | Evaluate (hand + suit counts) |
| Dichiarazione | Declare (targets) |
| Combattimento / Ciclo 1 / Ciclo 2 | Combat / Cycle 1 / Cycle 2 |
| Rifornimento | Refill |
| Fine del turno | End of turn |
| corsia | lane (5 per player) |
| mano (di poker) | hand (poker hand) |
| traboccamento / spill | overflow damage to the player after a kill |
| seme | suit (Vincolo, Frattura, Spira, Vestigio) |
| rango | rank (2-10, Jack, Queen, King, Asso) |
| Tier | Tier 1 = 2-6, Tier 2 = 7-10, Tier 3 = J/Q/K, Tier 4 = Ace |
| Resistenza Umana | Human Resistance |
| Potenze Artificiali | Artificial Powers |
| sottofazione | subfaction |
| magia / equipaggiamento | spell / equipment |
| pila | stack (LIFO) |
| Evoluzione | Evolution (one-shot ability that fires once per card per game) |
