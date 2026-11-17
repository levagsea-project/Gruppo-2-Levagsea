# Known issues and open points



Add an entry here instead of guessing. Remove it when fixed. Date format YYYY-MM-DD.

**ID format:** `YYYY-MM-DD-short-slug`, same as `DECISIONS.md` (no sequential numbers, to avoid collisions between branches). Old `KI-nn` IDs were converted on 2026-11-07.



## Fix before use


### `2026-11-14-suit-synergies-not-in-engine`: The engine does not apply suit synergies

Ruleset 7.10 gives a synergy to a suit with 3, 4 or 5 creatures, and `synergies.json` keeps all twelve tiers live (`2026-11-14-mvp-ability-set`), but the engine has no code for them: Evaluate counts no suits and no synergy effect is ever applied. A match with three or more creatures of one suit therefore differs from the rulebook, silently. The golden files avoid such boards. Before real decks are simulated: implement the synergies on the live vocabulary (Vincolo, Frattura and Vestigio need new effects in `EffectRegistry`; Spira is inert anyway, `2026-11-14-spira-inert-without-evolutions`), or refuse to play a board that would trigger one.


## Gaps


### `2026-11-06-validator-python-only`: The ability validator exists only in Python

`data/validate.py` implements the ten checks. The proposal says the engine validates ability coherence at deck validation. Either port the checks to Java (and keep `validate.py` as a CI gate), or call the engine only for deck-level rules.


### `2026-11-06-faction-swap-open-rules`: Faction-swap series: open rules

Not decided yet: whether `FACTION_SWAP` is the main format; how the faction of game 3 is chosen; when game 3 is needed (a 1-1 split, or also after draws) and when a series ends; how the series outcome follows from the games; and how many games a series may last (after a drawn game 3 the rule is "swap and play again", with no stated cap). The schema stores each game and its factions and leaves these to the backend.


### `2026-11-06-ruleset-change-illegal-deck`: A ruleset change can make a player's last legal deck illegal

Players must always have at least one legal deck per faction (`2026-11-06-starter-decks`). Decks are pinned to a pool snapshot (`decks.snapshot_id`), so a new snapshot does not touch existing decks; the problem appears when decks are moved to a new snapshot, or when an old one is retired. Not decided. Options: keep the old snapshot playable until the player migrates; mark a deck invalid and grant a fresh starter deck when no legal deck remains; or reject the new snapshot if it would leave players without one. Also open: where the starter deck templates are stored (no table yet).


### `2026-11-07-spell-equipment-hooks-no-data`: Spell and equipment hooks do nothing

`CardEffects.onSpell` and `onEquipment` are no-ops (`TODO(spells-equipment)`) because no spell or equipment cards exist. Refill draws, resolves, discards and replaces them as decided (`2026-11-07-refill-spells-equipment-immediate`, `2026-11-08-equipment-discard-after-use`) and `RefillTest` covers that flow with a recording hook. When cards exist: replace the no-op default and test what the effects do.


### `2026-11-14-spira-inert-without-evolutions`: Spira synergies do nothing while every Evolution is parked

Spira 3 counts triggered Evolutions, Spira 4 multiplies their values (`E19`) and Spira 5 forces them (`E17`). With no live Evolution (`2026-11-14-mvp-ability-set`) all three tiers are inert, so Spira decks only get hand value. The synergies stay live because they are ruleset 7.10. Restore at least one Evolution before Spira is balanced or tested.


### `2026-11-14-vincolo-no-themed-ability`: No live ability carries Vincolo's theme

None of the four live keywords has the `adjacency` theme (Sostegno and Vendicativo did, both parked). `make validate` warns that Vincolo has no themed ability at any rank, so pool generation falls back to unthemed abilities for Vincolo cards. Acceptable for the MVP; restore an adjacency ability before pool generation is tuned.

### `2026-11-12-pack-odds`: Pack prices, sizes and Tier weights are placeholders

The three pack types (`standard`, `advanced`, `elite`) in `application.yml`, with their prices and slots per Tier, are placeholders (`2026-11-12-pack-pricing-strategy`, from the team's first proposal). The mapping of rarity names to Tiers (comune, non comune, rara, epica = Tier 1 to 4) is an assumption to confirm. Spells and equipment have no Tier, so their pack slot is a plain uniform draw over all of them. No spell or equipment cards exist in the pool yet, so **packs cannot be opened until some do** (the drawer refuses to sell a pack short); to decide whether to relax that meanwhile. Not decided: spell/equipment rarity, and whether a pack type can be limited in time. Also: `auth.registration-bonus` is 0 by default, so a new player cannot buy a pack without a grant.

### `2026-11-12-card-names-one-language`: Card names are stored in one language

`card_definitions.name` holds a single text, so card names cannot follow the player's language like the rest of the interface (`2026-11-11-error-codes-not-text`). Not designed: a table of names per locale, or label keys per card code. To decide with the card pool, and before the card names are final.

## Resolved (only list those with future impact)


### `2026-11-04-spira3-opponent-evolutions`: Spira 3 counts opponent Evolutions  [RESOLVED 2026-11-04]

"Per ogni Evoluzione innescata in gioco" counts **only your own** Evolutions, not the opponent's. Confirmed as intended; removed from open gaps.