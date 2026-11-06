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
| Challenges, series and matchmaking | A **series** (`match_series`) is what gets started, and holds one or more games (`matches`, `game_number`). The start method (challenge or matchmaking) and the format are independent: every combination is valid, and a `SINGLE` series is a series of one game (a trigger rejects game 2, and a second deck per side in a `SINGLE` challenge). Formats: `SINGLE`, and `FACTION_SWAP` (opposite factions, swapped after every game; which factions game 3 uses and when it is needed are not decided, KI-11). Two ways to start one. A **challenge** targets a chosen player, who accepts (picking decks, one per faction in `FACTION_SWAP`; stored in `challenge_decks`) or declines, and expires. **Matchmaking** picks an opponent and plays at once against that player's **defense decks** (`decks.is_defense`, one per faction), even while they are offline; in each game the defender's deck is the one of the **opposite faction** to the attacker's. Player A is always the one who started the series; swapping changes factions, not seats. Every player starts with two pre-made decks (one per faction, `STARTER` cards) and must keep at least one legal deck (see KI-10). |
| Notifications | `match_notifications` has one row per player of every match (`seen_at IS NULL` = unread). Create both rows when the match is stored; set `seen_at` at once for a player who receives the result in the response, leave it null for the other. Unread matches are delivered at next login. `matches` itself is immutable. |
| Matches (replay) | Store the **recipe**, not the log: deck snapshot JSON, seed, ruleset and pool versions, **engine version** and the SHA-256 of the result. The replay is recomputed and verified against the hash. |
| Telemetry | Aggregation tables (global and per user), filled asynchronously from match logs: draw frequency, damage, kills, win rate, **synergy uptime and decisiveness** (`data/balance.json` → `synergy_health`). |

## Database schema

- The schema is **Flyway migrations** in `src/main/resources/db/migration` (`V1` users/ledger, `V2` catalogue, `V3` collections, `V4` decks, `V5` trades, `V6` challenges/matches, `V7` telemetry). Hibernate only validates (`ddl-auto: validate`): a column added to an entity without a migration stops the app at startup.
- **Never edit a migration that was already merged**; add a new `V<n>__name.sql`. Never reuse a version number.
- Pools are per faction. `cards` is the stable identity, `card_definitions` the versioned content inside one `pool_snapshots` row. Owned `card_instances` point to `cards`; stats are resolved through the snapshot a deck or match uses.
- Decks may reuse the same card instance. `deck_cards` repeats `owner_id`, `card_id`, `faction`, `snapshot_id` and `deck_slot`; composite foreign keys keep them consistent, so build a `DeckCard` from the instance and the deck, never by hand. `cards.deck_slot` is a generated column.
- Append-only tables (ledger, snapshots, cards, definitions, ownership history, matches) reject UPDATE and DELETE with a trigger. Create a snapshot and all its definitions **in one transaction**; later inserts are rejected.
- `created_at`-style columns are filled by the database (`@Generated`), because the snapshot sealing trigger compares with `now()`.
- Entities use plain UUID foreign-key fields, not JPA associations. JSON columns (`abilities`, `effect`, deck snapshots) are mapped as `String`: the backend stores and hashes them but never interprets them.

## Lombok

Lombok is used on **JPA entities only**, for `@Getter`, `@Setter` and `@NoArgsConstructor(access = PROTECTED)`.

- Never use `@Data`, `@EqualsAndHashCode`, `@ToString` or `@AllArgsConstructor` on entities: they touch every field, including database-generated ones.
- Put `@Setter` on individual fields, never on the class. Immutable rows (ledger, snapshots, matches) have no setters on purpose.
- Keep the explicit constructors: they take only the fields the caller may set.
- Annotation processing is declared in `pom.xml` (`annotationProcessorPaths`) because the project does not use the Spring Boot parent. If an IDE shows missing getters, enable annotation processing and the Lombok plugin.

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
