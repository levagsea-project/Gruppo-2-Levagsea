# Ruleset changelog

How versions work: `docs/rules/VERSIONING.md`. One line per change that touches the rulebook, `data/` or engine behaviour, added in the same pull request. Design decisions keep their full text in `docs/DECISIONS.md`; this file is the short list, newest first.

Format: `YYYY-MM-DD | area | what changed | reference`. Areas: `rules` (rulebook text), `data` (`data/*.json`), `engine` (behaviour of `game-engine`), `erratum` (wording only, no outcome changes).

## Unreleased (1.0-alpha, in development)

Anything may change until 1.0 is released. Changes before this log started are in `docs/DECISIONS.md` (see the `2026-11-01` to `2026-11-07` entries).

| Date | Area | Change | Reference |
|---|---|---|---|
| 2026-11-07 | rules | Added the development status note and sections 12.4 to 12.6 (versions, immutability after release, replayability) to the rulebook | `VERSIONING.md` (proposed) |
| 2026-11-07 | rules | 7.5: a missing kicker counts as 0; Ace is high in comparisons except in A-2-3-4-5; suits and lanes never break ties; the hand of a board with no creatures has no attackers and multiplier 0 (7.3) | `2026-11-07-missing-kicker-is-zero` |
| 2026-11-07 | rules | 7.5: a Four of a suit is also compared by its fifth creature (kicker), which does not attack | `2026-11-07-four-of-a-suit-kicker` (KNOWN-ISSUES) |
| 2026-11-07 | data | Hand spill multipliers moved to `balance.json` → `hand_multipliers_permille` (12 hands plus `NONE` = 0), values unchanged from 7.8 | `2026-11-07-hand-multipliers-in-balance` |
| 2026-11-07 | data | New `data/subfactions.json` (the 8 subfactions, faction, cycle, strong and weak against) and `balance.json` → `subfaction_advantage_permille` (strong 1100, weak 900). Values unchanged from ruleset 2.4 and 9.1; the lore dossier agrees on all 16 relations | `2026-11-07-subfaction-matrix-in-data` |
| 2026-11-07 | erratum | Ruleset 2.4: the second cycle is called "Ciclo dell'Identità" (was "Ciclo del Sé"), as in the lore. No outcome changes | `2026-11-07-subfaction-matrix-in-data` |
| 2026-11-07 | engine | `Ruleset.advantagePermille(attacker, defender)` and `RulesetLoader` reads `subfactions.json` (new required file) | `docs/plans/game-loop.md` |
| 2026-11-07 | engine | `quorma.gameengine.hand` contract (`Suit`, `HandType`, `HandCard`, `HandResult`, `HandEvaluator`); `evaluate` and `compare` not implemented yet | `docs/plans/hand-evaluator.md` |

## Released versions

None yet.
