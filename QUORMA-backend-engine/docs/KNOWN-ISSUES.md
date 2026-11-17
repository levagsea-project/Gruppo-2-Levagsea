# Known issues and open points



Add an entry here instead of guessing. Remove it when fixed. Date format YYYY-MM-DD.

**ID format:** `YYYY-MM-DD-short-slug`, same as `DECISIONS.md` (no sequential numbers, to avoid collisions between branches). Old `KI-nn` IDs were converted on 2026-11-07.



## Fix before use


## Gaps


### `2026-11-17-set-base-java-tuning`: Base set values to tune in the Java simulation

The base set's structure is fixed in `docs/cards/card-effects.json` (decisions `2026-11-17-set-base-*`), but the Python smoke test (`tools/balance-sim/report-set-base-1.md`) left these values out of band. Retune them in the Java simulation once the effects are implemented, not before: the Python numbers predate the structural fixes.

- **Mono-suit builds** win 67-74% (Resistenza) and 59-62% (Potenze): 13 of 22 creatures of one suit is 59% of a Resistenza deck, so four of a suit comes up in 15-23% of turns. Levers: the Quattro dello stesso seme multiplier, the suit tiers, or a per-suit deck limit (a ruleset change to 3.1 and to deck validation).
- **High-card and random builds** win 38-43%, under the 42% floor of `data/AGENTS.md` section 9; no support card lifts them past 52%. Levers: the Carta alta multiplier, Campi vuoti, Urlo della vittoria.
- **Hand spread:** straight, flush, full house and four of a kind are each under 1% of turns; 82% are high card or pair.
- **Aces:** Ilemo +9.8 (both abilities pull: without Carnefice 59.6%, without Assimila 63.6%), Berenice -12.1, Ines -8.1 (Rinforzo and Indurisce worth about 0), Errata -7.7 (Il registro precede +0.8 for 8%), Elio -4.1.
- **Others:** Tabita +8.5 (recheck with the one-substitution rule), Pannello della Reliquia / Muro +8.9 (no stalls), Campi vuoti (recheck at 60), Presagio (recheck with the scope and once-per-turn), Posto tenuto and the E36 spells (new semantics, never simulated), Rinforzo (fires in 85% of games), Calce -5.4, Nives -4.5, Lacuna -4.9, Zaira's Sigillo aperto (rarely matters), Flessibile (almost inert).


### `2026-11-06-faction-swap-open-rules`: Faction-swap series: open rules

Not decided yet: whether `FACTION_SWAP` is the main format; how the faction of game 3 is chosen; when game 3 is needed (a 1-1 split, or also after draws) and when a series ends; how the series outcome follows from the games; and how many games a series may last (after a drawn game 3 the rule is "swap and play again", with no stated cap). The schema stores each game and its factions and leaves these to the backend.


### `2026-11-17-no-spell-equipment-data`: No spell or equipment cards are defined

The engine plays spells and equipment with effects written on the card (`2026-11-17-spells-equipment`), but `data/` defines none: there is no `spells.json`, `balance.json` has no `spells` / `equipment` sections, pool generation does not make them, and `validate.py` has no pass over them (its `check_expansion` is ready, `2026-11-17-class-i-no-trigger`). Packs cannot be opened until some exist (`2026-11-12-pack-odds`). To decide: which effects they have (the engine runs ATK, DEF and heal on a side or on one creature), their rarity and cost, and what a keyword-granting equipment is (effect E14 is parked).

### `2026-11-14-vincolo-no-themed-ability`: No live ability carries Vincolo's theme

None of the four live keywords has the `adjacency` theme (Sostegno and Vendicativo did, both parked). `make validate` warns that Vincolo has no themed ability at any rank, so pool generation falls back to unthemed abilities for Vincolo cards. Acceptable for the MVP; restore an adjacency ability before pool generation is tuned.

### `2026-11-12-pack-odds`: Pack prices, sizes and Tier weights are placeholders

The three pack types (`standard`, `advanced`, `elite`) in `application.yml`, with their prices and slots per Tier, are placeholders (`2026-11-12-pack-pricing-strategy`, from the team's first proposal). The mapping of rarity names to Tiers (comune, non comune, rara, epica = Tier 1 to 4) is an assumption to confirm. Spells and equipment have no Tier, so their pack slot is a plain uniform draw over all of them. No spell or equipment cards exist in the pool yet, so **packs cannot be opened until some do** (the drawer refuses to sell a pack short); to decide whether to relax that meanwhile. Not decided: spell/equipment rarity, and whether a pack type can be limited in time. Also: `auth.registration-bonus` is 0 by default, so a new player cannot buy a pack without a grant.

### `2026-11-12-card-names-one-language`: Card names are stored in one language

`card_definitions.name` holds a single text, so card names cannot follow the player's language like the rest of the interface (`2026-11-11-error-codes-not-text`). Not designed: a table of names per locale, or label keys per card code. To decide with the card pool, and before the card names are final.