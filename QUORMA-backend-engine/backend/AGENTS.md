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
| Challenges, series and matchmaking | A **series** (`match_series`) is what gets started, and holds the games (`matches`, `game_number`). Every series the API starts is `FACTION_SWAP`, **best of three** (`2026-11-19-best-of-three-faction-swap`): each player uses their own decks, one per faction, chosen when starting or accepting; the two players always have opposite factions and the factions swap every game (seats do not: player A is the one who started it); a win is 1 point, a draw 0.5 each; game 3 is played unless someone has 2 points after game 2 (`SeriesPlanner`); equal points at the end is `DRAW`. Three ways to start (`2026-11-19-three-ways-to-start`): a **room** (`rnd-XXXX-XXXX`, `rooms` table: a player waits online, another joins, the series is played at once and the room becomes an `ACCEPTED` challenge), a **challenge by nickname** (`ch-XXXX-XXXX`, the opponent may be offline and picks their decks at accept), **matchmaking** (a random player with a legal defense deck for both factions and `last_login_at` within 7 days, played at once against their defense decks; no challenge row). Accepting plays the **whole series in one transaction** (`SeriesRunner`): it locks the challenge row, draws a seed per game (`SecureRandom`), calls the engine, stores the recipe of each game and sets the challenge to `ACCEPTED` (there is no `FINISHED` on challenges, `2026-11-19-challenge-accepted-means-played`). An engine error rolls everything back and the challenge stays `PENDING`; a second accept gets 409. The code that starts or accepts a challenge, joins a room or runs matchmaking calls `DeckEligibility.requireCanPlay` and `requireSelectable` (through `DeckSnapshotService`). A new pool snapshot migrates legal decks and flags the others `outdated` (`2026-11-17-deck-migration`). |
| Notifications | `match_notifications` has one row per player of every match (`seen_at IS NULL` = unread). Create both rows when the match is stored; set `seen_at` at once for a player who receives the result in the response, leave it null for the other. Unread matches are delivered at next login: `GET /matches?unread=true`, then `POST /matches/{seriesId}/seen`. `matches` itself is immutable. |
| Matches (replay) | Store the **recipe**, not the log: deck snapshot JSON, seed, ruleset and pool versions, **engine version** and the SHA-256 of the result. The replay is recomputed and verified against the hash. |
| Telemetry | Aggregation tables (global and per user), filled asynchronously from match logs: draw frequency, damage, kills, win rate, **synergy uptime and decisiveness** (`data/balance.json` → `synergy_health`). |

## Database schema

- The schema is **Flyway migrations** in `src/main/resources/db/migration` (`V1` users/ledger, `V2` catalogue, `V3` collections, `V4` decks, `V5` trades, `V6` challenges/matches, `V7` telemetry, `V8` pack openings, `V9` rooms, challenge codes and `users.last_login_at`). Hibernate only validates (`ddl-auto: validate`): a column added to an entity without a migration stops the app at startup.
- **Never edit a migration that was already merged**; add a new `V<n>__name.sql`. Never reuse a version number.
- Pools are per faction. `cards` is the stable identity, `card_definitions` the versioned content inside one `pool_snapshots` row. Owned `card_instances` point to `cards`; stats are resolved through the snapshot a deck or match uses.
- Decks may reuse the same card instance. `deck_cards` repeats `owner_id`, `card_id`, `faction`, `snapshot_id` and `deck_slot`; composite foreign keys keep them consistent, so build a `DeckCard` from the instance and the deck, never by hand. `cards.deck_slot` is a generated column.
- Append-only tables (ledger, snapshots, cards, definitions, ownership history, matches, pack openings) reject UPDATE and DELETE with a trigger. Create a snapshot and all its definitions **in one transaction**; later inserts are rejected.
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
- Challenge by nickname (players only; an admin gets 403, no token 401):
  - `POST /challenges` `{opponentUsername, startingFaction, decks: [{faction, deckId} x2]}`: locks the snapshot of my decks and waits (`matches.challenge-ttl`, 48 h by default, at most 7 days). One `PENDING` challenge per pair of players.
  - `POST /challenges/{id}/accept` `{decks: [...]}`: only the challenged player. Generates the seeds, calls the engine, stores the recipe and the SHA-256 the engine wrote (`result_hash`), plays all the games and returns the series with the log of each game. `ACCEPTED` afterwards.
  - `POST /challenges/{id}/decline` (the opponent) and `POST /challenges/{id}/cancel` (the challenger), `GET /challenges?status=` (the ones I sent and received), `GET /challenges/{id}` (404 for anyone who is not one of the two).
- Rooms: `POST /rooms` (open one, `matches.room-ttl`, 10 minutes), `GET /rooms` (open rooms of others), `POST /rooms/{code}/join` `{decks}` (plays at once, returns the series), `POST /rooms/{code}/cancel`.
- `POST /matchmaking` `{startingFaction, decks}`: a random opponent, played at once, returns the series.
- History and delivery: `GET /matches?unread=&page=&size=` (my finished series, newest first, games without the log), `GET /matches/{seriesId}`, `POST /matches/{seriesId}/seen`. Only the two players see a series (404 for anyone else). The log is **not stored**: the turn by turn of a game comes from `GET /matches/{id}/replay`, which rebuilds it and checks the hash (issue #23).
- `POST /v1/simulate`: **admins only**, a development entry point that forwards a match input to the engine-service and returns its match log. Errors: 400 `engine.input_invalid`, 503 `engine.unavailable` (the engine's own message is only logged).

Keep the OpenAPI document current: it is the formal contact point with the Web team. Treat changes to it as breaking changes.

## Errors and languages

The API never returns text for the player. Every error has a `code`, a key of `data/labels/<locale>.json` (Italian and English), and the frontend translates it (`2026-11-11-error-codes-not-text`). Shape (RFC 9457 `ProblemDetail`): `{status, detail, code}`; `detail` repeats the code. A 400 from bean validation has `code: "validation.failed"` and `fieldErrors: { field: { code, params } }`, where `params` fills the `{min}`, `{max}`, `{value}` placeholders of the label.

- Codes are constants in `common/ErrorCodes`, one class for every domain (label keys are one shared namespace), grouped by domain. Use them as the `message` of constraint annotations and as the second argument of the domain exception (`AuthException`, `PackException`). Never write English text there.
- A new code needs a key in **both** `it.json` and `en.json`, in the same commit. `data/validate.py` checks the two files against each other, not against `ErrorCodes`.
- Labels for game text (factions, suits, phases, hand types, card rarities and kinds) and for pack types (`pack.type.<code>`) are in the same files; the backend does not read them. A new pack type in `application.yml` needs its `pack.type.<code>` label.
- Identifiers sent to the client are language-neutral constants, never translated text: `Faction` and `Subfaction` constant names (the same text the database stores), enum names for kind, origin and rarity (`2026-11-12-faction-ids-neutral`).

## Tests

- Unit tests with mocks: the engine client (`HttpGameEngineTest`, `EngineExceptionHandlerTest`, decision `2026-11-16-backend-engine-client-tests`), the series rules (`SeriesPlannerTest`: scoring, third game, draws), the series loop with a fake engine (`SeriesRunnerTest`: turn limit, engine faults, seeds, notifications), and the services (`ChallengeServiceTest`, `RoomServiceTest`, `MatchmakingServiceTest`, `MatchServiceTest`).
- `ChallengeFlowDockerTest` runs the whole flow on a **real PostgreSQL** with Testcontainers (all migrations, triggers, jsonb, row locks) and a fake engine: the recipe stored, six concurrent accepts that play one series, an engine fault in the middle of a series, draws, rooms joined at the same time, matchmaking and the authorization of the endpoints over HTTP. It needs Docker running; without Docker the class is skipped (JUnit reports it as skipped), and the GitHub workflow runs it.
- Run from the repository root with `mvn -pl backend -am test` (JDK 25). The rest of the backend (decks, packs) has no automated tests yet. Whatever is added later, a trade or purchase must still be proven atomic with no duplication.

## Do not

- Do not simulate or re-derive match results in the backend.
- Do not read `data/*.json` for rules logic. The engine owns that; the backend only passes versions and snapshots.
- Do not update ledger or snapshot rows.
