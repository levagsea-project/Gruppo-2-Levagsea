# Known issues and open points



Add an entry here instead of guessing. Remove it when fixed. Date format YYYY-MM-DD.

**ID format:** `YYYY-MM-DD-short-slug`, same as `DECISIONS.md` (no sequential numbers, to avoid collisions between branches). Old `KI-nn` IDs were converted on 2026-11-07.



## Fix before use


## Gaps


### `2026-11-06-faction-swap-open-rules`: Faction-swap series: open rules

Not decided yet: whether `FACTION_SWAP` is the main format; how the faction of game 3 is chosen; when game 3 is needed (a 1-1 split, or also after draws) and when a series ends; how the series outcome follows from the games; and how many games a series may last (after a drawn game 3 the rule is "swap and play again", with no stated cap). The schema stores each game and its factions and leaves these to the backend.


### `2026-11-07-spell-equipment-hooks-no-data`: Spell and equipment hooks do nothing

`CardEffects.onSpell` and `onEquipment` are no-ops (`TODO(spells-equipment)`) because no spell or equipment cards exist. Refill draws, resolves, discards and replaces them as decided (`2026-11-07-refill-spells-equipment-immediate`, `2026-11-08-equipment-discard-after-use`) and `RefillTest` covers that flow with a recording hook. When cards exist: replace the no-op default and test what the effects do.


### `2026-11-14-vincolo-no-themed-ability`: No live ability carries Vincolo's theme

None of the four live keywords has the `adjacency` theme (Sostegno and Vendicativo did, both parked). `make validate` warns that Vincolo has no themed ability at any rank, so pool generation falls back to unthemed abilities for Vincolo cards. Acceptable for the MVP; restore an adjacency ability before pool generation is tuned.

### `2026-11-12-pack-odds`: Pack prices, sizes and Tier weights are placeholders

The three pack types (`standard`, `advanced`, `elite`) in `application.yml`, with their prices and slots per Tier, are placeholders (`2026-11-12-pack-pricing-strategy`, from the team's first proposal). The mapping of rarity names to Tiers (comune, non comune, rara, epica = Tier 1 to 4) is an assumption to confirm. Spells and equipment have no Tier, so their pack slot is a plain uniform draw over all of them. No spell or equipment cards exist in the pool yet, so **packs cannot be opened until some do** (the drawer refuses to sell a pack short); to decide whether to relax that meanwhile. Not decided: spell/equipment rarity, and whether a pack type can be limited in time. Also: `auth.registration-bonus` is 0 by default, so a new player cannot buy a pack without a grant.

### `2026-11-12-card-names-one-language`: Card names are stored in one language

`card_definitions.name` holds a single text, so card names cannot follow the player's language like the rest of the interface (`2026-11-11-error-codes-not-text`). Not designed: a table of names per locale, or label keys per card code. To decide with the card pool, and before the card names are final.