# game-engine/: agent guide

Pure-Java deterministic match simulator. Input: two decks (JSON), a seed, the ruleset version. Output: a match log (JSON). Same input must give the **same bytes**.

Stack (from the architecture proposal): Java 25, Maven, JUnit. This module is plain Java with **no Spring dependency** (a Maven enforcer rule fails the build otherwise). It is served as a microservice by `engine-service/` (Spring Boot, Docker, HTTP). The simulation core stays single-threaded and deterministic; the server layer only routes requests and does not affect determinism.

## Determinism checklist

- No `System.currentTimeMillis`, `Instant.now`, `Random` without a seed, `SecureRandom`, `UUID.randomUUID`, parallel streams or threads.
- Do not let `HashMap`/`HashSet` iteration order affect results. Use arrays, `List`, or `LinkedHashMap`/sorted keys.
- No `float`/`double`. Multipliers are per-mille integers. Floor every result (`Math.floorDiv` for anything that can be negative).
- PRNG: one named algorithm implemented in this module (not a library default), seeded from the match seed. Shuffle order is fixed: player 1's deck, then player 2's, then any setup reshuffles (ruleset 12.2). Fisher-Yates.
- Match log: stable key order, no timestamps, UTF-8, `\n` newlines. Record ruleset version, pool versions, seed, PRNG name, shuffle algorithm **and the engine version**. Result hash is SHA-256 of the canonical log bytes.
- Never "fix" a result because it looks wrong. Fix the rule or add a test.

## Resolution order (all fixed, never random)

- Turn phases: Valutazione, Dichiarazione, Combattimento (inizio, Ciclo 1, Ciclo 2, fine), Rifornimento, Fine del turno.
- Stack is LIFO. Triggers fired together are pushed in lane order 1 to 5, **Potenze Artificiali first, then Resistenza Umana** (so the Resistenza resolves first).
- Replacement effects (class R) have no stack. Ties: lowest lane first, then Potenze Artificiali, then keywords before suit synergies.
- Refill: lane order 1 to 5, Potenze Artificiali first. Start-of-Refill effects resolve before destroyed creatures are discarded and do not affect destroyed creatures.
- Hand, multiplier and suit counts are fixed at Valutazione. Conditions written inside an effect are checked in real time.
- Modifier order: additive, multiplicative (per-mille), rounding, then "set value".
- Spill = floor((effective ATK − target DEF before the killing blow) × hand multiplier / 1000). Two creatures killing each other in Cycle 1 give no spill; Cycle 2 spill is normal.

## Loading the data

The engine implements the vocabulary in `data/vocab.json`; it does not hard-code abilities.

- Resolve `$param.x` from the card instance, and `$tune.<KEY>.<name>` from the card instance `params` first, then `data/balance.json` (`data/AGENTS.md` §3). Skip keys starting with `_` in `synergies.json`.
- `per_creature` synergies: one copy per creature of that suit, source = that creature. `per_player`: one copy, source = the player.
- An Evolution fires once per card instance per game (`max_triggers` 1).
- Run the same ten checks as `data/validate.py` when loading a pool, or load only data that passed `make validate`.
- Behaviour such as Custode's delayed destruction is data (`E12.then`, `lockout`, condition `C24`). Do not special-case it in code.

## MVP scope

Only the live data ships (decision `2026-11-14-mvp-ability-set`): keywords Scudo, Perforante, Insaziabile and Traboccante, no Evolutions, and the 12 suit synergies. Everything else is in `data/parked/`, which is not bundled in the jar; bring an ability back only when the engine is ready to resolve it (`data/AGENTS.md` §13). The 3-turn demo still uses parked abilities (`2026-11-14-demo-uses-parked-abilities`). No spell or equipment cards exist yet (none are defined in `data/`). A creature may also have an ability written on the card (trigger, effect, target, no keyword) in one of the shapes of those four keywords, with its own numbers (`2026-11-14-inline-abilities`). The engine does not apply suit synergies yet (`2026-11-14-suit-synergies-not-in-engine`). Spells and equipment are accepted only without abilities.

## Entry point

`GameEngine.matchLog(inputJson)` plays one match and returns the canonical MatchLog JSON. `GameEngine.simulate` returns the same match as Java objects (`Simulation`). Input and log follow the JSON Schemas in `src/main/resources/schemas/` (`2026-11-14-match-log-schema`); bad input is a `MatchInputException` whose message starts with the JSON path. Steps: input checks, shuffle, setup (`2026-11-14-match-setup`), turns, log (`2026-11-14-match-log-writer`).

## Tests

- JUnit for effects, edge cases (draws, turn 50, empty lanes, simultaneous deaths) and repeatability.
- **Golden files**: `src/test/resources/golden/<scenario>/input.json` + `expected-log.json`; the output must be byte-identical (`GoldenTest` runs every folder and checks both files against the schemas). Compute expected values by hand and from the ruleset, never from the engine's own output. Scenarios: `all-four-keywords` (the schema example), `mutual-kill-flat-scudo`, `setup-inline-cycle2-insaziabile`. Keep boards under three creatures of a suit until synergies exist.
- A golden file changes only when a rule changes deliberately. Say why in the commit and in `docs/DECISIONS.md`.

## Layout

```
game-engine/src/main/java/quorma/gameengine/
  GameEngine, Simulation, MatchLogWriter, EngineVersion   entry point and the MatchLog
  input/     match input JSON reader
  data/      rule pack loader, Ruleset, rule pack hash
  effects/   ability binder, effect registry, stack, effect resolver
  turn/      setup, turn phases, damage (Strike, Damage)
  hand/      poker hand evaluator
  model/     cards, creatures, players, match state
  log/       events, board snapshots
  rng/       SplitMix64, Fisher-Yates
  json/      canonical JSON writer and SHA-256
game-engine/src/main/resources/schemas/      input and MatchLog JSON Schemas, example log
game-engine/src/test/resources/golden/       golden files
```
