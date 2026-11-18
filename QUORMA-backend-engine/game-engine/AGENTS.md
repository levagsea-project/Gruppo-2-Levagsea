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
- `per_creature` synergies: one copy per creature of that suit, source = that creature. `per_player`: one copy, source = the player. Evaluate binds the active `per_creature` tiers onto the creatures (`SynergyTiers`); the effects are run by the code of their shape (`Board` for static ones), never by synergy-specific code.
- An Evolution fires once per card instance per game (`max_triggers` 1).
- Run the same ten checks as `data/validate.py` when loading a pool, or load only data that passed `make validate`.
- Behaviour such as Custode's delayed destruction is data (`E12.then`, `lockout`, condition `C24`). Do not special-case it in code.

## MVP scope

Only the live data ships (decision `2026-11-14-mvp-ability-set`): what `data/*.json` holds is what the engine resolves, and `data/` is the one place that says which abilities those are. Everything else is in `data/parked/`, which is not bundled in the jar; bring an ability back only when the engine is ready to resolve it (`data/AGENTS.md` §13). A creature may also have an ability written on the card (trigger, effect, target, no keyword) with its own numbers (`2026-11-14-inline-abilities`), an Evolution included (`T_EVO`). A spell or equipment has its effects written on the card as expansions: class I with no trigger for what happens when it is drawn, and lasting ones with a trigger (`2026-11-17-spells-equipment`, `2026-11-17-base-set-engine`). `EffectRegistry` checks each piece of an expansion (trigger, effect per class and mode, target, duration, conditions); the whole base set uses only implemented pieces.

## Effects

What an effect does to a creature lives in one class per vocabulary ID in `effects/` (`AtkEffect` for E1, `HealEffect` for E6, ...), behind the `Effect` interface, and `Game` only looks it up in `Effects` and calls it (`2026-11-18-effect-classes`). An effect class holds the resolution of the ID with all its modes, its allowed resolution classes and param rules (`classes()`, `modes()`, `validate()`), which `EffectRegistry` applies when a card is bound. To add or change an effect: edit its class, register it in `Effects`, add a test next to `EffectClassesTest` (use `FakeContext`). If it needs something from the turn that `EffectContext` does not offer, add one method there and implement it in `Game`; never pass `Game` or `Board` to an effect. Registration order is fixed (a `LinkedHashMap`), never iterate a `HashMap` of effects. Effects on a player (E8, E9), static (class C) and replacement (class R) ones are still resolved by `Game`, `Board`, `Strike` and `TurnResolver`; the S… targets of `Board` are not classes yet. Conditions (C…) follow the same pattern: one `Condition` class per ID in `effects/` (`Conditions` looks them up), reading the table only through `ConditionContext` (implemented by `Board`), with its param checks in `validate` (`2026-11-19-condition-classes`).

## Entry point

`GameEngine.matchLog(inputJson)` plays one match and returns the canonical MatchLog JSON. `GameEngine.simulate` returns the same match as Java objects (`Simulation`). Input and log follow the JSON Schemas in `src/main/resources/schemas/` (`2026-11-14-match-log-schema`); bad input is a `MatchInputException` whose message starts with the JSON path. Steps: input checks, shuffle, setup (`2026-11-14-match-setup`), turns, log (`2026-11-14-match-log-writer`).

## Tests

- JUnit for effects, edge cases (draws, turn 50, empty lanes, simultaneous deaths) and repeatability.
- **Golden files**: `src/test/resources/golden/<scenario>/input.json` + `expected-log.json`; the output must be byte-identical (`GoldenTest` runs every folder and checks both files against the schemas). Compute expected values by hand and from the ruleset, never from the engine's own output. Scenarios: `all-four-keywords` (the schema example), `mutual-kill-flat-scudo`, `setup-inline-cycle2-insaziabile`. Keep boards under three creatures of a suit unless the scenario is about synergies (`2026-11-17-synergies-engine`).
- **Base set**: `BaseSetTest` binds every card of `src/test/resources/base-set/cards.json` (generated from `docs/cards/card-effects.json` by `tools/base-set/export_cards.py`; rerun it when that file changes) and plays 300 matches between random decks of the set (schema, determinism); `BaseSetEffectsTest` checks the new abilities on boards worked out by hand.
- A golden file changes only when a rule changes deliberately. Say why in the commit and in `docs/DECISIONS.md`.

## Layout

```
game-engine/src/main/java/quorma/gameengine/
  GameEngine, Simulation, MatchLogWriter, EngineVersion   entry point and the MatchLog
  input/     match input JSON reader
  data/      rule pack loader, Ruleset, rule pack hash
  effects/   ability binder, effect registry, stack, one Effect class per effect ID (`Effects` looks them up),
             `EffectContext` (what an effect may ask of the turn), `PerMille` math
  turn/      setup, turn phases (TurnResolver), one turn's effects (Game: wires the pieces below, implements the stack
             hooks and EffectContext, spells, equipment, draws and placements), triggers (TriggerDispatcher: which
             abilities listen and in what order), targets of an effect (TargetSelector), damage and destruction by
             effects with the T20/T21 replacements (Destruction), the table as effects see it: static effects, reach,
             conditions, adjacency, selections (Board), hand effects (Hands), damage (Strike, Damage), suit synergy
             tiers (SynergyTiers)
  hand/      poker hand evaluator
  model/     cards, creatures, players, match state
  log/       events, board snapshots
  rng/       SplitMix64, Fisher-Yates
  json/      canonical JSON writer and SHA-256
game-engine/src/main/resources/schemas/      input and MatchLog JSON Schemas, example log
game-engine/src/test/resources/golden/       golden files
```
