# data/: how to use the card-effect data files

> **Quick start.** Run `make validate` from the repo root (or `python3 validate.py .` in this folder). It must report 0 errors. Open design questions are in `balance.json` → `open_decisions`, decisions taken in `../docs/DECISIONS.md`.


`data/` holds the data-driven effect system for the card game (ruleset v1.0-alpha, LEVAGSEA / QUORMA). Read this before editing or loading any JSON here.

All game text is in Italian. IDs, field names and this guide are in English. Every number marked as a placeholder is a playtest value, not a final one.

## 1. File map

| File | Role | Edit when |
|---|---|---|
| `vocab.json` | The vocabulary: constants, resolution classes, durations, conditions, effects, triggers, targets, target selection rules, evolution rules. Defines what can be expressed. | You need a new building block. |
| `keyword.json` | Keywords (Scudo, Letale, …) as expansions into vocab pieces. Structure only, no balance numbers. | You add or change a keyword. |
| `evolutions.json` | Evolutions: one-shot wrappers that give a creature a keyword or an effect when a trigger fires. | You add or change an evolution. |
| `synergies.json` | The 12 suit-tier effects of ruleset 7.10 (3 / 4 / 5 creatures of a suit). | The ruleset's suit synergies change. |
| `balance.json` | All tunable numbers, rank eligibility, stat costs, simulation status, testing protocol, open decisions. | You tune balance. |
| `ranks.json` | The rank ladder (Tier names, roles per rank and faction, naming rules) and `role_affinity`. Lore plus pool-generation hints, no rules. | You change the lore of the ranks. |
| `invalid-combinations.json` | Class rules per trigger, a description of the derived rules, and the few hand-written exceptions. | You find a combination that should be illegal. |

**Golden rule: structure lives in the structure files, numbers live in `balance.json`.** A designer should be able to retune the game by editing only `balance.json`.

## 2. Core concept: an expansion

Every keyword, evolution and synergy expands into one or more **expansions**: a (trigger → effect → target) tuple.

```json
{
  "trigger_id": "T0",
  "trigger_params": {},
  "effect_id": "E28",
  "effect_params": { "mode": "reduction_pct", "pct": "$param.x" },
  "target_id": "S1",
  "target_params": {},
  "duration": "D_Permanente",
  "conditions": [ { "id": "C4", "params": { "suit": "Vincolo", "level": 3 } } ],
  "class": "C"
}
```

- `trigger_id`: key in `vocab.triggers` (`T…`).
- `effect_id`: key in `vocab.effects` (`E…`).
- `target_id`: key in `vocab.targets` (`S…`).
- `duration`: key in `vocab.durations` (`D_…`).
- `conditions`: a list of `{id, params?}` evaluated in AND. An empty list means always true.
- `class`: the resolution class actually used: `C` (continuous), `T` (triggered, uses the stack), `R` (replacement, no stack), `I` (immediate, spells and gear only).
- Optional common parameters on any expansion: `max_triggers`, `once_per_turn`, `max_stacks`, `stack_key` (see `vocab.common_params`).

## 3. Value references

String values in params can point elsewhere. Resolve them before the engine uses them.

| Syntax | Resolves to |
|---|---|
| `$param.<name>` | The value written on the card instance, e.g. card has `{"id":"K_SCUDO","params":{"x":15}}` so `$param.x` = 15. |
| `$tune.<KEY>.<name>` | `balance.json` → `keywords[KEY]` or `evolutions[KEY]`. Looks in the card instance's `params` first (values written at pool creation, e.g. tier scaling), then in `params` of the balance entry, then at its top level (e.g. `max_stacks`). |
| `$tune.synergies.<TIER>.<name>` | `balance.json` → `synergies[TIER][name]`. |

Text placeholders such as `{reduction_pct}` or `{x}` in descriptions are filled from the keyword's card params or from `balance.json` → `display`.

## 4. Cards reference abilities

There is no cards file yet. The intended card shape:

```json
{
  "keywords":   [ { "id": "K_SCUDO", "params": { "x": 15 } } ],
  "evolutions": [ { "id": "EVO_CRESCITA", "params": { "permille": 180 } } ]
}
```

- Assumption (placeholder, see `balance.globals.card_ability_slots`): at most `keyword` (currently 3) distinct keywords and exactly one evolution per card. Keywords granted by an evolution don't use a slot. Several keywords on one card resolve in list order; each keeps its own `params`. The `evolutions` array holds at most one entry.
- **There are no copy limits; scarcity is per rank.** A deck has at most one creature per rank and suit, so any single rank appears at most 4 times in a deck. Every keyword and evolution has a `cap_class` in `balance.json`: `capped`, `limited` or `open`.
  - `capped` abilities (swingy ones such as Letale and Doppio Colpo, or ones that scale with copies such as Muro, Provocazione, Flessibile, Camaleonte) have exactly one `allowed_ranks` entry and `max_built_in` 4. Several capped abilities may share a rank (Letale, Doppio Colpo and Carnefice all sit on the Ace): the 4-card limit then applies to them combined, so a deck can have four Letale Aces, four Doppio Colpo Aces or any mix.
  - `limited` abilities are bounded by their own effect, so they may sit on 2 to `globals.limited_max_ranks` (3) ranks, normally within one Tier (Provocazione 7-8, Custode Q-K). `max_built_in` is then 8 or 12. Promote an ability from `capped` to `limited` only when its effect is conditional, one-shot or bounded; `balance.json` → `open_decisions` lists the candidates.
  - `open` abilities have more than 3 allowed ranks (in practice all 13). Each carries an `intrinsic_limit` note explaining why it cannot be spammed (stacking mode, `max_stacks`, mitigation floor, adjacency, once-per-game).
  - An evolution that grants a capped or limited keyword inherits its ranks (Carnefice A, Vendetta J, Sentinella 7-8, Maturazione 3, Mimetico 2). Otherwise the cap could be bypassed.
- Rank does not decide the ability. An Ace may carry any open ability, and each (faction, rank, suit) has several versions (`pool_generation.variants_per_rank_suit`) with different abilities.
- Numeric values of open abilities are scaled by Tier at pool creation (`balance.json` → `tier_scaling`) and written to the card's `params`. The engine never reads Tiers.
- Rank eligibility is checked when the **card pool is created**, not at deckbuilding.

## 5. Keywords vs evolutions

**Keywords** (`keyword.json`) are permanent rules on a creature. None of them is an evolution. They may have per-card parameters, declared in a `params` object, e.g. Scudo (X), Perforante (X), Sostegno (X). Variants that depend on a parameter use the `K_PARAM` condition (Scudo has a `pct` and a `flat` variant).

**Evolutions** (`evolutions.json`) are wrappers:
- The trigger is always `T_EVO`, with the real trigger in `trigger_params.trigger_id` (and its params in `trigger_params.trigger_params`).
- `max_triggers` is always 1: an evolution fires **once per card instance, per game**.
- The effect is either `E14` (grant a keyword, with `keyword_params`) or any other effect of class `T`.
- `name` is optional and used only for UI and logs. The key is the identity.
- Spira synergies count and amplify evolutions: Spira 3 counts triggered evolutions, Spira 4 multiplies their numeric values (including granted keyword parameters), Spira 5 forces the one-time trigger via `E17`.

Repeatable growth is **not** an evolution. Use a normal trigger plus `max_stacks` (see Insaziabile).

## 6. Synergies

`synergies.json` keys are `<SUIT>_<TIER>`, e.g. `VESTIGIO_5`. The `_schema` key is documentation: skip keys starting with `_` when loading.

- `instantiation`: `per_creature` makes one copy per creature of that suit for the controller (source = that creature). `per_player` makes one copy with the player as source.
- `replaces`: the listed synergies stop applying while this one is active (Frattura 5 replaces Frattura 4, Vestigio 5 replaces Vestigio 3).
- Tiers are cumulative: with N creatures of a suit, tiers 3 to N apply. The `C4` condition on each expansion enforces the threshold.
- The suit counts are fixed for the turn at Valutazione. Conditions written inside an effect are checked in real time.

## 7. Validation

`validate.py` in this folder implements these checks (run `python3 validate.py .`; exit code 1 on any error). It loads all the data files (`ranks.json` is optional) and, for every expansion in `keyword.json`, `evolutions.json` and `synergies.json`, checks:

1. **IDs exist** in the vocabulary (trigger, effect, target, duration, condition).
2. **Class**: the expansion `class` is allowed by `invalid-combinations.class_rules.trigger_allowed_effect_classes[trigger]` and is listed in `vocab.effects[effect].class`. For `T_EVO`, the inner trigger must itself be class `T`.
3. **Target kind**: some value of `effects[effect].target_kind` appears in `targets[target].kind` (`card` is also satisfied by `creature`).
4. **Trigger context**: if `targets[target].requires_context` is set, it must appear in `triggers[trigger].provides` (for `T_EVO`, use the inner trigger's `provides`).
5. **Duration**: `effects[effect].duration_mode` is `instant_only` (only `D_Istantanea`), `sustained_only` (anything except `D_Istantanea`) or `any`.
6. **Class `I` only effects** (`E48`, `E49`, `E57`) never appear in creature abilities. `E50` is deprecated (empty class list) and cannot be used anywhere.
7. **`combination_rules`**: a rule matches when all its non-null fields match (`null` means any, a list means any of). Rules with `allowed_triggers` are whitelists: the effect is legal only with those triggers. A match means the expansion is invalid.
8. **Keyword grants**: an `E14` must name an existing keyword, include its required params, and not include unknown ones.
9. **References**: every `$tune.*` resolves in `balance.json`, every `balance_ref` exists, every description placeholder can be filled.
10. **No keyword uses `T_EVO`.**

The structure files are meant to pass all ten checks with zero errors. Run the validator after every change. It also checks the cap model and `ranks.json`, checks that the expected suit and role match of the draw meets `pool_generation.fit_targets` (60% and 40%; currently at least 75% and 69%), and prints theme-coverage warnings (section 10); warnings never fail the run.

## 8. How to add things

**New keyword**
1. Reuse existing vocab where possible; otherwise add vocab first (below).
2. Add the entry to `keyword.json` with `name`, `class`, `balance_ref`, optional `params`, `description`, `expands_to`.
3. Add the matching entry to `balance.json` → `keywords` with `allowed_ranks`, `max_built_in`, `stat_cost_pct`, `family`, `favours`, `params`, `display`, `sim`.
4. Validate.

**New evolution**
1. Add to `evolutions.json` using `T_EVO`, with the real trigger inside `trigger_params`.
2. Add to `balance.json` → `evolutions`.
3. Validate.

**New vocab piece**
- New effect: give it `class` (list), `target_kind`, `duration_mode`, `params`, and a precise description. Use the next free ID (currently `E58`). Never renumber or reuse IDs.
- New trigger: give it `class`, `uses_stack`, `provides`, and add it to `class_rules` (currently next is `T38`).
- New target: give it `kind` and `requires_context` (use `null` if none).
- Replacement effects (`R`) must have a trigger that can express the moment ("sta per…"). If none exists, add one.

**New illegal combination:** add to `invalid-combinations.json` → `combination_rules` only if it cannot be derived from the metadata (loops, cascades, timing).

## 9. Balance workflow

Targets (from ruleset Appendix D): every build between 44% and 56% average win rate (high card at 42% or better), 7 to 10 turns per match, no single effect worth more than about a 5% stat edge.

1. Prefer **percentage** values to flat ones (flat effects hurt low-ATK builds). Percentages are per-mille or percent as named by the param.
2. Control power with `allowed_ranks` first, then `stat_cost_pct`, then numeric params, then rework.
3. Run the five-build matrix (stats only, pair-heavy, straight-window, one-suit, high card) with and without the ability, per main faction, then cross-faction with the subfaction matrix on.
4. Record the result in the entry's `sim` block (`status`, `ruleset_version`, `date`, results). `status: "untested"` means the numbers are guesses.
5. Mitigation is multiplicative and floored at `globals.mitigation_floor_permille` (700 = at most 30% total).
6. Read `balance.json` → `open_decisions` before changing related abilities. These are unresolved design questions.

## 10. Suit themes and pool generation

Suits are narrative principles, not locked ability lists (lore v2.0, section 06). Thematic fit comes from two layers: the synergy tiers (table-level engine) and the card pool (fuel).

- `balance.json` → `suit_themes` maps each suit to theme tags: Vincolo `adjacency`, Frattura `breakthrough`, Spira `adaptation` + `growth`, Vestigio `persistence`.
- Every keyword and evolution in `balance.json` carries a `themes` list. Tags are metadata for pool generation and lint only; the engine ignores them.
- `balance.json` → `pool_generation` (placeholders): a draw procedure picks each ability at card-pool creation. With probability `themed_draw_pct` the candidates are limited to abilities sharing a theme with the card's suit, then with probability `role_draw_pct` to the Tier's `role_affinity` (see `selection_procedure`; each step is skipped when it would leave no candidates). `evolution_slot_fill_pct` fills the Evolution slot more often on Spira cards. `evolution` is a mechanism, not a theme tag: Spira's pull on Evolutions lives in the slot fill, so the suit theme can still tell Evolutions apart; `theme_match_stat_cost_pct` taxes abilities that match their suit's themes, because they stack with the synergy.
- A suit's tiers must still pay off in a deck that only splashes the suit. Tier 4 of Vincolo was rewritten for this reason: it required a non-attacking Vincolo, which is inert in the hands built around Vincolo (Colore, Quattro dello stesso seme).
- `balance.json` → `synergy_health` defines telemetry thresholds. A tier that is rarely active or rarely decisive is thematically dead.
- The validator warns when a suit has ranks with no eligible themed ability. Generation then falls back to unthemed abilities; fix the gaps by adding abilities, not by loosening tags.

## 11. Rank ladder (lore)

`ranks.json` gives each rank a role: 2 to 6 are anonymous troops (gregario, squadra, mezzo, reparto, unità attrezzata), 7 to 10 are specialists and veterans, J/Q/K are named lieutenants and the Ace is the faction's main character, its big gun. The ladder is lore: it adds no rules, and Tiers keep the meaning given in ruleset 3.2.2.

- `role_affinity` lists the abilities each Tier favours at pool creation (`pool_generation.role_draw_pct`). It is a bias, not a limit.
- Each faction's Aces face the other faction's Aces in the narrative only. In play the target depends on the lane.
- Names and characters in `ranks.json` are proposals (`status`) until the narrative team approves them.

## 12. Conventions and gotchas

- **Math is integer or fixed-point.** Multipliers are in per-mille (1200 = ×1.2) and results are floored. Modifier order: additive first, then multiplicative, then rounding, then "set value" effects last (Golden Rule 2).
- **Determinism.** Every resolution order is fixed (lane order, Potenze Artificiali before Resistenza Umana, never random). Stack is LIFO.
- **Replacement ties (class R):** lowest lane first, then Potenze Artificiali first, then keywords before suit synergies.
- **Evolutions and Spira 5:** Spira 5 forces an evolution only if its single trigger is not yet spent.
- **Card text overrides the ruleset** (Golden Rule 1), so the engine must not "correct" an ability that contradicts the base rules.
- **Do not hide logic in the engine.** Behaviour like Custode's delayed destruction is expressed as data (`E12.then`, `E12.lockout`, condition `C24`).
- **Names:** use the Italian faction and subfaction names exactly as in the ruleset (e.g. SEKTAC, MEROZYN, EFFRIX, YLMOR, Adattati, Spezzasigilli, Portanome, Segnaterra).
- **No spell or equipment cards exist yet.** There is no `spells.json` and `balance.json` has no `spells` / `equipment` sections. The vocabulary (class `I`, `vocab.card_contexts`) stays so they can be added later; when they are, add the card file, the balance entries and the validator checks together.
- **No effect may depend on player input or information.** Players decide nothing during a match, so an effect that only reveals information or needs a choice does nothing. `E50` (reveal top card) was removed for this reason and replaced by `E57` (discard the top cards of a deck). Never reuse the `E50` ID.
