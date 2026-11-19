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
- `EffectRegistry` runs, for every expansion a card writes, the checks `data/validate.py` runs on the data files (effect, target, trigger and duration fit together, a creature target is found in the expansion's class) and refuses what the engine would crash on or ignore. Load only data that passed `make validate`.
- Behaviour such as Custode's delayed destruction is data (`E12.then`, `lockout`, condition `C24`). Do not special-case it in code.

## MVP scope

Only the live data ships (decision `2026-11-14-mvp-ability-set`): what `data/*.json` holds is what the engine resolves, and `data/` is the one place that says which abilities those are. Everything else is in `data/parked/`, which is not bundled in the jar; bring an ability back only when the engine is ready to resolve it (`data/AGENTS.md` §13). A creature may also have an ability written on the card (trigger, effect, target, no keyword) with its own numbers (`2026-11-14-inline-abilities`), an Evolution included (`T_EVO`). A spell or equipment has its effects written on the card as expansions: class I with no trigger for what happens when it is drawn, and lasting ones with a trigger (`2026-11-17-spells-equipment`, `2026-11-17-base-set-engine`). `EffectRegistry` checks each piece of an expansion (trigger, effect per class and mode, target, duration, conditions); the whole base set uses only implemented pieces.

## Effects

What an effect does to a creature lives in one class per vocabulary ID in `effects/` (`AtkEffect` for E1, `HealEffect` for E6, ...), behind the `Effect` interface, and `Game` only looks it up in `Effects` and calls it (`2026-11-18-effect-classes`). An effect class holds the resolution of the ID with all its modes, its allowed resolution classes and param rules (`classes()`, `modes()`, `validate()`), which `EffectRegistry` applies when a card is bound. To add or change an effect: edit its class, register it in `Effects`, add a test next to `EffectClassesTest` (use `FakeContext`). If it needs something from the turn that `EffectContext` does not offer, add one method there and implement it in `Game`; never pass `Game` or `Board` to an effect. Registration order is fixed (a `LinkedHashMap`), never iterate a `HashMap` of effects. Effects that act on a player, the deck or a lane (E8, E9, E35, E36, E49, E57, E64) are `PlayerEffect` classes (`PlayerEffects`, `PlayerEffectContext`), dispatched by effect ID. Static (class C), replacement (class R) and a few triggered ones (E11, E12, E17, E19, E21-E26, E29, E30, E32, E34, E47, E51, E52, E54-E56, E60, E61, E63) are only an `EffectDefinition`: an ID, its resolution classes and its params, with a pointer to where the turn code reads it. They are read at a hook (`Board`, `Destruction`, `Strike`, `Combat`, `Declaration`, `Refill`, `Hands`) and the modifier order lives in `Strike`, so they have no `apply`. An effect that is also read as a static one keeps that part in its own class: `AtkEffect` (E1: `amount` and `scale_by` of a static copy), `DamageReductionEffect` (E28: `reduction`), the `AbilityLayerEffect`s E14, E15 and E16 (`layer`), and `DefEffect` says it is applied once on an equipment. The turn code (`Board`, `Strike`, `Game`) only finds the effect and calls it. Every effect an ability casts, on the stack or from a spell or an equipment, leaves a line in the MatchLog with its `effect_id` (`effect_resolved`, `card_effect_resolved`); a `PlayerEffect` returns that line and `Game` logs it before the events the effect appended. Passive effects (static, hand, replacement) leave none: their use shows in `modifiers`, `creature_saved`, `attack_skipped` or `refill_prevented` (`2026-11-19-log-deck-effects`). Every effect class has an `ID` constant: read an effect by it, not by a string literal. `EffectDefinitions` lists all 45, and `EffectRegistry` has no table of its own. Conditions (C…) and targets (S…) follow the same pattern: one `Condition` / `Target` class per ID in `effects/` (`Conditions`, `Targets` look them up), reading the table only through `ConditionContext` / `TargetView` (implemented by `Board`), with their param checks in the class (`2026-11-19-condition-classes`, `2026-11-19-target-classes`). A target has one method per way its ID is used (a static effect reaching creatures, a triggered effect choosing when it resolves, a spell or equipment aiming, a player's ability, a hand effect); the ranked ones (S33, S35, S37, S38, S39, S40) only say their order. Targets that are not creatures (S4 lane, S30/S31 players, S32 discard pile, S47 deck) are classes with only an ID and a param check: `Game` acts on them by ID (`TargetIds`).

## Entry point

`GameEngine.matchLog(inputJson)` plays one match and returns the canonical MatchLog JSON. `GameEngine.simulate` returns the same match as Java objects (`Simulation`). Input and log follow the JSON Schemas in `src/main/resources/schemas/` (`2026-11-14-match-log-schema`); bad input is a `MatchInputException` whose message starts with the JSON path. Steps: input checks, shuffle, setup (`2026-11-14-match-setup`), turns, log (`2026-11-14-match-log-writer`).

**Replays.** A replay is the recipe of a match played again, and it is only valid on the engine and rule pack that played it. The MatchLog header names both (`engine_version`, `ruleset_version`, `ruleset_hash`); a match input may carry `engine_version` and `ruleset_hash` (both optional: a new match need not), and the engine refuses an input whose values are not its own, with a message that says so (`input.engine_version: this engine is X, the match was played by Y`). An input built from a log's header plays to the same bytes (`ReplayRecipeTest`).

`GameEngine.cardCheck(requestJson)` checks a candidate card pool of one faction (`cards/CardPoolChecker`, report `CardCheckReport`, codes `CardCheckCodes`): the engine is the single authority on what a card may be. Per card (all problems reported, not only the first): the card schema, the faction, no ability twice, at most `balance.globals.card_ability_slots` keywords and evolutions, the creature's rank within the `allowed_ranks` of each ability it carries (this is what bounds `max_built_in`, which is always 4 per allowed rank because a deck has one creature per rank and suit: there is no separate cap check), and that every ability binds. Per pool: not empty, no id twice, and each pack pool fillable (every Tier with slots has a creature, support slots have a spell or an equipment). Balance is not checked. `data/validate.py` stays the CI check of the data files and must agree with it (a parity test is planned). The rank-to-Tier mapping (ruleset 3.2.2) is in `CardPoolChecker.tierOf`; the backend's `Tier.ofRank` and `validate.py`'s `tier_of` repeat it.

## Tests

- JUnit for effects, edge cases (draws, turn 50, empty lanes, simultaneous deaths) and repeatability.
- **Golden files**: `src/test/resources/golden/<scenario>/input.json` + `expected-log.json`; the output must be byte-identical (`GoldenTest` runs every folder and checks both files against the schemas). Compute expected values by hand and from the ruleset, never from the engine's own output. Scenarios: `all-four-keywords` (the schema example), `mutual-kill-flat-scudo`, `setup-inline-cycle2-insaziabile`. Keep boards under three creatures of a suit unless the scenario is about synergies (`2026-11-17-synergies-engine`).
- **Card pool check**: `CardPoolCheckerTest` runs the whole base set through it (with the three shop pack pools) and one case per code, plus broken requests. `CardCheckParityTest` keeps it and `data/validate.py` check 12 honest: the same good and deliberately bad cards (`src/test/resources/parity/cards.json`, plus a sample of the real base set) go to both and the verdict must be the same; a known difference is written in the fixture with its reason and asserted as it is. It runs `validate.py` with the Python it finds (`python3` or `python`) and is skipped without one. Add a case when a rule is added to either side.
- **Base set**: `BaseSetTest` binds every card of `data/sets/BASE.json` (generated from `docs/cards/card-effects.json` by `tools/base-set/export_cards.py`; rerun it when that file changes) and plays 300 matches between random decks of the set (schema, determinism); `BaseSetEffectsTest` checks the new abilities on boards worked out by hand.
- A golden file changes only when a rule changes deliberately. Say why in the commit and in `docs/DECISIONS.md`.

## Layout

```
game-engine/src/main/java/quorma/gameengine/
  GameEngine, Simulation, MatchLogWriter, EngineVersion   entry point and the MatchLog
  input/     match input JSON reader (`MatchInputReader.readCard` reads one card for the pool check)
  cards/     the card pool check: CardPoolChecker, CardCheckReport, CardCheckCodes
  data/      rule pack loader, Ruleset, rule pack hash
  effects/   ability binder, effect registry, stack, one Effect / PlayerEffect / Condition / Target class per vocabulary ID
             (`Effects`, `PlayerEffects`, `Conditions`, `Targets` look them up), `EffectContext`, `ConditionContext` and `TargetView` (what
             they may ask of the turn), `PerMille` math
  turn/      setup, turn phases (TurnResolver runs the turn and exposes each phase; Declaration, Combat, Refill and
             TurnEnd hold them, Evaluate and T5 stay in it), one turn's effects (Game: wires the pieces below, implements the stack
             hooks and EffectContext, spells, equipment, draws and placements), triggers (TriggerDispatcher: which
             abilities listen and in what order), damage and destruction by
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
