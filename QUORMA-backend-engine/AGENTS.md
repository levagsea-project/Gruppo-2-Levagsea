# QUORMA: agent guide (root)

Digital, deterministic collectible card game. Two factions (Resistenza Umana, Potenze Artificiali), eight subfactions, four suits. Players make **no decisions during a match**: same decks + same seed = same match (Regola d'Oro 0).

This repo covers the Software team's perimeter (data, engine, backend) plus the project docs. The frontend belongs to the Web team and talks to the backend only through the Swagger/OpenAPI contract.

## Repo map

| Folder | What | Read first |
|---|---|---|
| `data/` | The card-effect system: vocabulary, keywords, evolutions, suit synergies, balance numbers, rank ladder, validator | `data/AGENTS.md` |
| `game-engine/` | Pure Java 25 deterministic simulation core. **No Spring** (enforced in its pom). Tested with plain JUnit | `game-engine/AGENTS.md` |
| `engine-service/` | Spring Boot 4.1.x HTTP wrapper around `game-engine`, runs in a Docker container (port 8081) | `engine-service/AGENTS.md` |
| `backend/` | Spring Boot API, PostgreSQL, economy, decks, trades, replays, telemetry | `backend/AGENTS.md` |
| `docs/` | Ruleset, lore, architecture proposal, demo, decisions, known issues | `docs/AGENTS.md` |

Working in a subfolder? Its `AGENTS.md` adds to this file. `CLAUDE.md` files only import the matching `AGENTS.md` (`@AGENTS.md`) so Claude Code and other agents read the same text.

## Source of truth

1. **Rules and numbers:** `docs/rules/` (ruleset v1.0-alpha) for turn structure, hands, combat and suit synergies. `data/` is the machine-readable implementation of the abilities and synergies. The two must agree. If they differ, log it in `docs/KNOWN-ISSUES.md` and ask; never silently change either one.
2. **Names and lore:** `docs/lore/`. `data/ranks.json` is a lore *proposal* until the narrative team approves it.
3. **Architecture:** `docs/architecture/` is a proposal. Decisions taken since are in `docs/DECISIONS.md`.
4. Every number in square brackets or marked placeholder is a playtest value, not a final one.

## Non-negotiables

- **Determinism.** No clocks, no unseeded randomness, no unordered iteration affecting results, no floating point. Details in `game-engine/AGENTS.md`.
- **Integer math.** Multipliers are per-mille (1200 = ×1.2). Results are floored. Modifier order: additive, then multiplicative, then rounding, then "set value" effects (Regola d'Oro 2).
- **Structure vs numbers.** Structure lives in `data/*.json` (except `balance.json`); numbers live in `data/balance.json`. A designer must be able to retune the game by editing only `balance.json`.
- **No hidden logic.** Behaviour that can be data (for example Custode's delayed destruction) is data, not engine code.
- **Never renumber or reuse vocabulary IDs** (E…, T…, S…, C…).
- **Card text overrides the ruleset** (Regola d'Oro 1). The engine must not "correct" an ability that contradicts the base rules.

## Line endings

Every text file uses **LF**, on every OS, including Windows checkouts. `.gitattributes` (repo root) enforces it (`* text=auto eol=lf`) and `.editorconfig` tells editors. Do not set per-file CRLF. If Git ever shows a whole file as changed after a pull, run `git add --renormalize .` and check `git diff --cached --stat` before committing.

## Language

Game text, card names, lore and faction names are **Italian**, spelled exactly as in the ruleset (SEKTAC, MEROZYN, EFFRIX, YLMOR, Adattati, Spezzasigilli, Portanome, Segnaterra). Code, identifiers, JSON keys, commit messages and agent guides are **English**.

## Commands

```
make validate     # validates all data files; must show 0 errors before any commit touching data/
make engine-test  # game-engine unit tests (needs Maven + JDK 25)
docker compose up --build   # postgres + engine-service + backend
```

Maven is multi-module from the root `pom.xml` (`game-engine`, `engine-service`, `backend`).

## Workflow

1. Read the `AGENTS.md` of the folder you work in.
2. Check `docs/KNOWN-ISSUES.md` and `data/balance.json` → `open_decisions` before touching related mechanics.
3. If the rules are ambiguous, **do not guess**: add an entry to `docs/KNOWN-ISSUES.md` and ask.
4. After a design change, add an entry to `docs/DECISIONS.md` and run `make validate`.
5. Do not edit `docs/pdf/`.

## Do not invent canon

AI subfactions are *currents*, not planetary sovereigns. No AI card may be presented as a sovereign or as the Primaria. Proper names for J/Q/K/Ace cards are not approved yet (`data/ranks.json` → `status`).

## Known gaps

See `docs/KNOWN-ISSUES.md`.
