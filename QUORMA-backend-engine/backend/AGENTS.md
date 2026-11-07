# backend/: agent guide

Spring Boot API, the game's management layer and **the only component allowed to touch the database**. From the architecture proposal: Java 25, Spring Boot 4.1.x, PostgreSQL 16, JWT, Docker Compose, Swagger/OpenAPI as the contract with the Web team. Match results always come from the engine.

## Domains

| Domain | Rules |
|---|---|
| Users and economy | Credit balance with optimistic locking (`@Version`). Immutable transaction ledger: every credit movement is a row, never an update. |
| Catalogue and versioning | Cards are never edited in place. Immutable **pool snapshots** with a cryptographic hash. A match played today must replay identically later. |
| Collections | Every owned card is a **unique instance** row, not a counter. This guarantees traceable origin and trades that cannot duplicate or lose cards. |
| Decks | Header + owned card instances. Constraints in the database: at most one creature per rank and suit, one copy of each spell/equipment. |
| Trades | A proposal; accepting runs one atomic ACID transaction that swaps ownership. |
| Matches (replay) | Store the **recipe**, not the log: deck snapshot JSON, seed, ruleset and pool versions, **engine version** and the SHA-256 of the result. The replay is recomputed and verified against the hash. |
| Telemetry | Aggregation tables (global and per user), filled asynchronously from match logs: draw frequency, damage, kills, win rate, **synergy uptime and decisiveness** (`data/balance.json` → `synergy_health`). |

## Deck validation

Structural checks live here (the frontend only gives UX feedback): exactly 32 cards; Resistenza Umana 20-23 creatures + 9-12 spells/equipment; Potenze Artificiali 24-27 creatures + 5-8; one pool per deck (single main faction); at most one creature per rank and suit; one copy per spell/equipment. Logical coherence of abilities is checked against the same rules as `data/validate.py` (KI-07).

Pool rules: rank eligibility of abilities (`allowed_ranks`, `cap_class`) is enforced when the **card pool is created**, not when decks are built.

## API (from the proposal)

All requests use JWT. Critical operations (buying packs, accepting trades) are atomic transactions.

- `POST /challenges`: locks the deck snapshot.
- `POST /challenges/{id}/accept`: generates the seed, calls the engine, stores the recipe.
- `GET /matches/{id}/replay`: returns the rebuilt log.

Keep the OpenAPI document current: it is the formal contact point with the Web team. Treat changes to it as breaking changes.

## Tests

**No automated tests in the backend for now** (project decision: unit tests exist for the engine only), so the pom has no test dependencies. If this changes, log it in `docs/DECISIONS.md`. Whatever is added later, a trade or purchase must still be proven atomic with no duplication.

## Do not

- Do not simulate or re-derive match results in the backend.
- Do not read `data/*.json` for rules logic. The engine owns that; the backend only passes versions and snapshots.
- Do not update ledger or snapshot rows.
