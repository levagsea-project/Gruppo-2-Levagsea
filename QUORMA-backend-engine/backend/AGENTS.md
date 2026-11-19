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
| Challenges, series and matchmaking | A **series** (`match_series`) is what gets started, and holds one or more games (`matches`, `game_number`). The start method (challenge or matchmaking) and the format are independent: every combination is valid, and a `SINGLE` series is a series of one game (a trigger rejects game 2, and a second deck per side in a `SINGLE` challenge). Formats: `SINGLE`, and `FACTION_SWAP` (opposite factions, swapped after every game; which factions game 3 uses and when it is needed are not decided, `2026-11-06-faction-swap-open-rules`). Two ways to start one. A **challenge** targets a chosen player, who accepts (picking decks, one per faction in `FACTION_SWAP`; stored in `challenge_decks`) or declines, and expires. **Matchmaking** picks an opponent and plays at once against that player's **defense decks** (`decks.is_defense`, one per faction), even while they are offline; in each game the defender's deck is the one of the **opposite faction** to the attacker's. Player A is always the one who started the series; swapping changes factions, not seats. Every player starts with two pre-made decks (one per faction, `STARTER` cards) and must keep a legal defense deck per faction: a new pool snapshot migrates legal decks and flags the others `outdated`, and a player with an outdated defense deck cannot play until it is fixed (`2026-11-17-deck-migration`). The code that starts or accepts a challenge, or runs matchmaking, must call `DeckEligibility.requireCanPlay` and `requireSelectable`. |
| Notifications | `match_notifications` has one row per player of every match (`seen_at IS NULL` = unread). Create both rows when the match is stored; set `seen_at` at once for a player who receives the result in the response, leave it null for the other. Unread matches are delivered at next login. `matches` itself is immutable. |
| Matches (replay) | Store the **recipe**, not the log: deck snapshot JSON, seed, ruleset and pool versions, **engine version** and the SHA-256 of the result. The replay is recomputed and verified against the hash. |
| Telemetry | Aggregation tables (global and per user), filled asynchronously from match logs: draw frequency, damage, kills, win rate, **synergy uptime and decisiveness** (`data/balance.json` → `synergy_health`). |

## Database schema

- The schema is **Flyway migrations** in `src/main/resources/db/migration` (`V1` users/ledger, `V2` catalogue, `V3` collections, `V4` decks, `V5` trades, `V6` challenges/matches, `V7` telemetry, `V8` pack openings). Hibernate only validates (`ddl-auto: validate`): a column added to an entity without a migration stops the app at startup.
- **Migrations are edited in place until the first database exists** (`docs/plans/cards-into-game.md`: V2 and V8 were reshaped for card text, sets and pack types). From the first deployed database on, never edit a migration: add a new `V<n>__name.sql`. Never reuse a version number.
- Pools are per faction. `cards` is the stable identity, `card_definitions` the versioned content inside one `pool_snapshots` row: **only what changes a match** (kind, stats, abilities; the card's faction, subfaction, suit and rank are on `cards`). It has no name: names and flavour are `card_texts(card_id, locale, name, flavour)`, mutable and outside the snapshot hash, with Italian required (a deferred trigger refuses a card without its `it` row) and the fallback of every other locale (`CardTextService`). Owned `card_instances` point to `cards`; stats are resolved through the snapshot a deck or match uses.
- Everything commercial is mutable and outside the hash: `card_sets` (BOZZA, IN_VENDITA, CHIUSA) with `card_set_texts`, the bridge `card_set_cards` (reprints allowed; leaving a card out of the bridge takes it out of the packs, never out of the game), and `pack_types` (one set each, price, slots per Tier). `active_snapshots(faction, snapshot_id)` says which snapshot is active; the newest by `created_at` is not necessarily the active one.
- Publishing goes through one pipeline: `catalogue_drafts` (one open at a time), `draft_bases` (the active snapshot each faction had when the draft opened), `draft_items`, and one append-only `snapshot_publications` row per faction and publish (who, when, source seed/api/batch, based_on, reactivated).
- Decks may reuse the same card instance. `deck_cards` repeats `owner_id`, `card_id`, `faction`, `snapshot_id` and `deck_slot`; composite foreign keys keep them consistent, so build a `DeckCard` from the instance and the deck, never by hand. `cards.deck_slot` is a generated column.
- Append-only tables (ledger, snapshots, cards, definitions, ownership history, matches, pack openings, snapshot publications) reject UPDATE and DELETE with a trigger. Create a snapshot and all its definitions **in one transaction**; later inserts are rejected.
- `created_at`-style columns are filled by the database (`@Generated`), because the snapshot sealing trigger compares with `now()`.
- Entities use plain UUID foreign-key fields, not JPA associations. JSON columns (`abilities`, deck snapshots) are mapped as `String`: the backend stores and hashes them but never interprets them.

## Lombok

Lombok is used on **JPA entities only**, for `@Getter`, `@Setter` and `@NoArgsConstructor(access = PROTECTED)`.

- Never use `@Data`, `@EqualsAndHashCode`, `@ToString` or `@AllArgsConstructor` on entities: they touch every field, including database-generated ones.
- Put `@Setter` on individual fields, never on the class. Immutable rows (ledger, snapshots, matches) have no setters on purpose.
- Keep the explicit constructors: they take only the fields the caller may set.
- Annotation processing is declared in `pom.xml` (`annotationProcessorPaths`) because the project does not use the Spring Boot parent. If an IDE shows missing getters, enable annotation processing and the Lombok plugin.

## Deck validation

Structural checks live here (the frontend only gives UX feedback): exactly 32 cards; Resistenza Umana 20-23 creatures + 9-12 spells/equipment; Potenze Artificiali 24-27 creatures + 5-8; one pool per deck (single main faction); at most one creature per rank and suit; one copy per spell/equipment. Logical coherence of abilities is checked against the same rules as `data/validate.py`.

Pool rules: rank eligibility of abilities (`allowed_ranks`, `cap_class`) is enforced when the **card pool is created**, not when decks are built.

## API (from the proposal)

All requests use JWT. Critical operations (buying packs, accepting trades) are atomic transactions.

- `GET /packs`: the pack types on sale.
- `POST /packs/open`: locks the user row (`SELECT ... FOR UPDATE`), charges credits, creates the card instances. Needs an `Idempotency-Key` header (a UUID): the same key returns the same pack and charges once.
- `GET /collection`: the owned card instances.
- `POST /challenges`: locks the deck snapshot.
- `POST /challenges/{id}/accept`: generates the seed, calls the engine, stores the recipe.
- `GET /matches/{id}/replay`: returns the rebuilt log.
- `POST /v1/simulate`: **admins only**, a development entry point that forwards a match input to the engine-service and returns its match log. Errors: 400 `engine.input_invalid`, 503 `engine.unavailable` (the engine's own message is only logged).

Keep the OpenAPI document current: it is the formal contact point with the Web team. Treat changes to it as breaking changes.

## Errors and languages

The API never returns text for the player. Every error has a `code`, a key of `data/labels/<locale>.json` (Italian and English), and the frontend translates it (`2026-11-11-error-codes-not-text`). Shape (RFC 9457 `ProblemDetail`): `{status, detail, code}`; `detail` repeats the code. A 400 from bean validation has `code: "validation.failed"` and `fieldErrors: { field: { code, params } }`, where `params` fills the `{min}`, `{max}`, `{value}` placeholders of the label.

- Codes are constants in `common/ErrorCodes`, one class for every domain (label keys are one shared namespace), grouped by domain. Use them as the `message` of constraint annotations and as the second argument of the domain exception (`AuthException`, `PackException`). Never write English text there.
- A new code needs a key in **both** `it.json` and `en.json`, in the same commit. `data/validate.py` checks the two files against each other, not against `ErrorCodes`.
- Labels for game text (factions, suits, phases, hand types, card rarities and kinds) and for pack types (`pack.type.<code>`) are in the same files; the backend does not read them. A new pack type in `application.yml` needs its `pack.type.<code>` label.
- Identifiers sent to the client are language-neutral constants, never translated text: `Faction` and `Subfaction` constant names (the same text the database stores), enum names for kind, origin and rarity (`2026-11-12-faction-ids-neutral`).

## Tests

Unit tests exist for the engine, and for the engine client in the backend (`engine` package: `HttpGameEngineTest`, `EngineExceptionHandlerTest`; decision `2026-11-16-backend-engine-client-tests`). The rest of the backend has no automated tests for now. Whatever is added later, a trade or purchase must still be proven atomic with no duplication.

## Do not

- Do not simulate or re-derive match results in the backend.
- Do not read `data/*.json` for rules logic. The engine owns that; the backend only passes versions and snapshots.
- Do not update ledger or snapshot rows.
