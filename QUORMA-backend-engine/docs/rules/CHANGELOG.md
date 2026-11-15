# Ruleset changelog

How versions work: `docs/rules/VERSIONING.md`. One line per change that touches the rulebook, `data/` or engine behaviour, added in the same pull request. Design decisions keep their full text in `docs/DECISIONS.md`; this file is the short list, newest first.

Format: `YYYY-MM-DD | area | what changed | reference`. Areas: `rules` (rulebook text), `data` (`data/*.json`), `engine` (behaviour of `game-engine`), `erratum` (wording only, no outcome changes).

## Unreleased (1.0-alpha, in development)

Anything may change until 1.0 is released. Changes before this log started are in `docs/DECISIONS.md` (see the `2026-11-01` to `2026-11-07` entries).

| Date | Area | Change | Reference |
|---|---|---|---|
| 2026-11-14 | rules | Rulebook text aligned with decisions already in force: 4.4 to 4.6 (when the stack resolves, effects triggered while it resolves go on top, an effect on a destroyed target does nothing), 9.5 (damage is reduced by the target's damage reductions) and 9.6 (spill uses the damage dealt). No outcome changes | `2026-11-14-stack-resolves-when-quiet`, `2026-11-14-spill-after-mitigation` |
| 2026-11-14 | engine | The engine writes the canonical MatchLog JSON (`GameEngine.matchLog`) with `ruleset_hash`, `engine_version` and `result_hash`, and refuses an input whose `ruleset_hash` differs from its rule pack. The rule pack hash now also covers `subfactions.json` | `2026-11-14-match-log-writer`, `2026-11-14-match-log-schema` |
| 2026-11-14 | engine | `GameEngine.simulate` plays a whole match from the input JSON: input checks, shuffle (A then B), setup with spells and equipment set aside and shuffled back (A then B, only if any) | `2026-11-14-match-setup` |
| 2026-11-14 | data | `balance.json` `globals.starting_life` = 12000 (ruleset 5.2, value unchanged) | `2026-11-14-match-setup` |
| 2026-11-14 | engine | Effect stack and Insaziabile: a kill triggers the killer's T19 abilities, resolved LIFO once all Cycle 1 blows have landed or right after the Cycle 2 attack (Potenze Artificiali pushed first); Insaziabile adds 15% of base ATK, at most 3 times; an effect whose creature is destroyed fizzles. | `2026-11-14-stack-resolves-when-quiet`, `2026-11-14-mvp-ability-set` |
| 2026-11-14 | engine | Scudo, Perforante and Traboccante change combat: Scudo reduces the damage taken (percentage reductions multiply, capped at 30%; flat leaves at least 1), Perforante ignores part of the target's reductions, Traboccante multiplies the spill multiplier on a kill. Spill comes from the damage dealt. A card with an ability the engine cannot play is refused instead of ignored | `2026-11-14-spill-after-mitigation`, `2026-11-14-traboccante-multiplies`, `2026-11-14-mvp-ability-set` |
| 2026-11-07 | rules | Added the development status note and sections 12.4 to 12.6 (versions, immutability after release, replayability) to the rulebook | `VERSIONING.md` (proposed) |
| 2026-11-07 | rules | 7.5: a missing kicker counts as 0; Ace is high in comparisons except in A-2-3-4-5; suits and lanes never break ties; the hand of a board with no creatures has no attackers and multiplier 0 (7.3) | `2026-11-07-missing-kicker-is-zero` |
| 2026-11-07 | rules | 7.5: a Four of a suit is also compared by its fifth creature (kicker), which does not attack | `2026-11-07-four-of-a-suit-kicker` (KNOWN-ISSUES) |
| 2026-11-07 | data | Hand spill multipliers moved to `balance.json` → `hand_multipliers_permille` (12 hands plus `NONE` = 0), values unchanged from 7.8 | `2026-11-07-hand-multipliers-in-balance` |
| 2026-11-07 | data | New `data/subfactions.json` (the 8 subfactions, faction, cycle, strong and weak against) and `balance.json` → `subfaction_advantage_permille` (strong 1100, weak 900). Values unchanged from ruleset 2.4 and 9.1; the lore dossier agrees on all 16 relations | `2026-11-07-subfaction-matrix-in-data` |
| 2026-11-07 | erratum | Ruleset 2.4: the second cycle is called "Ciclo dell'Identità" (was "Ciclo del Sé"), as in the lore. No outcome changes | `2026-11-07-subfaction-matrix-in-data` |
| 2026-11-07 | engine | `Ruleset.advantagePermille(attacker, defender)` and `RulesetLoader` reads `subfactions.json` (new required file) | `docs/plans/game-loop.md` |
| 2026-11-07 | rules | 3.2.6: a destroyed creature is flagged, stays in its lane until Refill and cannot be targeted unless an effect says so | `2026-11-07-destroyed-creatures-flagged` |
| 2026-11-07 | rules | 8.1: no targetable enemy creature means no target: the attacker does not attack and nothing hits the player | `2026-11-07-no-target-no-attack` |
| 2026-11-07 | rules | 9.2: Cycle 1 is simultaneous, a creature destroyed in it still deals its damage (clarification of the existing wording) | `2026-11-07-cycle-2-order` |
| 2026-11-07 | rules | 9.3: Cycle 2 attacks are sequential, ordered by the attacker's lane with both players interleaved; ties by faction then player A; a destroyed attacker does not attack; Cycle 2 spill is normal | `2026-11-07-cycle-2-order` |
| 2026-11-07 | rules | 4.3 and 10.3 (and 9.3): with the same faction on both sides, player A goes first | `2026-11-07-resolution-order-faction-then-side` |
| 2026-11-07 | engine | Convention: the top of a deck is index 0 | `2026-11-07-deck-top-is-index-0` |
| 2026-11-07 | engine | Every card kind has the same `abilities` list (`AbilityRef` by id, or `InlineEffect`); no code reads it yet | `2026-11-07-cards-share-abilities` |
| 2026-11-07 | engine | Task 2 contract: packages `model`, `turn`, `log` (match state, targeting, damage, turn phases, events); the turn logic is not implemented yet | `docs/plans/game-loop.md` |
| 2026-11-07 | engine | `quorma.gameengine.hand` contract (`Suit`, `HandType`, `HandCard`, `HandResult`, `HandEvaluator`); `evaluate` and `compare` not implemented yet | `docs/plans/hand-evaluator.md` |
| 2026-11-14 | data | Live abilities reduced to Scudo, Perforante, Insaziabile and Traboccante (no Evolutions); the rest of the keywords, all 16 Evolutions, the unused vocab entries, their class and combination rules, role affinities and open decisions moved unchanged to `data/parked/`, which is not bundled in the jar. Numbers and IDs unchanged | `2026-11-14-mvp-ability-set` |
| 2026-11-14 | rules | 9.6: spill uses the damage actually dealt (after the target's damage reductions such as Scudo), not the effective ATK. Rulebook text pending | `2026-11-14-spill-after-mitigation` |
| 2026-11-14 | rules | 4: the stack resolves once nothing more is added (after all Cycle 1 blows, each Cycle 2 attack, each card drawn); effects triggered during resolution go on top; an effect on a destroyed target does nothing. Rulebook text pending | `2026-11-14-stack-resolves-when-quiet` |
| 2026-11-14 | erratum | `keyword.json` Traboccante: the hand multiplier "viene moltiplicato per" ×1.2 (was "è ×1.2"), as `E21` already did. No outcome changes | `2026-11-14-traboccante-multiplies` |
| 2026-11-14 | data | Traboccante stacks with Frattura 4 and 5 (factors multiply); removed `stacking: max_with` from `keyword.json` (the `max_with` stacking mode moved to `data/parked/vocab.json`: only the parked Rigenerante uses it), description and `intrinsic_limit` updated, new open decision to simulate it. Numbers unchanged | `2026-11-14-traboccante-stacks-with-frattura` |
| 2026-11-14 | engine | New JSON Schemas for the match input and the MatchLog in `game-engine/src/main/resources/schemas/`, with an example log | `2026-11-14-match-log-schema` |
| 2026-11-14 | data | `labels/*.json`: hand-type keys renamed to the engine code (`game.hand_type.high_card`, was `carta_alta`), new `game.hand_type.none`; texts unchanged. Match input uses codes for faction and subfaction (`RESISTENZA_UMANA`, `ADATTATI`) | `2026-11-14-hand-type-label-keys` |

## Released versions

None yet.
