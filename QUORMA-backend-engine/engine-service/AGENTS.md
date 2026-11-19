# engine-service/: agent guide

Spring Boot 4.1.x HTTP microservice (Docker container, port 8081) that exposes `game-engine` to the outside world. It only routes requests: no game rules, no simulation logic, nothing that can affect determinism lives here. Rules belong in `game-engine/`.

- Depends on `game-engine` (never the other way round).
- Build context for its Dockerfile is the **repo root**: `docker compose build engine`.
- `GET /engine/info` returns status and engine version.
- `POST /v1/simulate` takes a match input (JSON) and returns the canonical MatchLog JSON, byte for byte what `GameEngine.matchLog` writes. An input the engine refuses (`MatchInputException`) is a 400 `ProblemDetail`; any other exception is a bug and stays a 500. Not exposed to the outside: only the backend calls it.
- `POST /v1/cards/check` checks a candidate card pool of one faction against the running engine's rules (`GameEngine.cardCheck`, `CardPoolChecker`). Request: `{schema_version: 1, faction, cards: [card as a deck lists it, no `sets`], pools?: [{name, card_ids, tier_slots: {TIER_1..TIER_4: n}, support_slots}]}`, the `pools` being the pack pools whose slots must be fillable. Always 200 with a report `{schema_version, ok, engine_version, ruleset_version, ruleset_hash, card_errors: [{card_id, code, path, message}], pool_errors: [{pool, code, subject, message}]}`: a wrong card is the answer, not a failed request. A broken request (bad JSON, unknown key, bad faction or Tier, too many cards) is a 400 like `/v1/simulate`. `code` is a stable identifier (`card.rank_not_allowed`, `pool.empty`, `pack.no_creature_for_tier`, ...: `CardCheckCodes`); `message` is English for logs only. Not exposed to the outside: only the backend calls it, and it is not part of the frontend contract.
