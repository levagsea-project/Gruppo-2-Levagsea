# Known issues and open points



Add an entry here instead of guessing. Remove it when fixed. Date format YYYY-MM-DD.

**ID format:** `YYYY-MM-DD-short-slug`, same as `DECISIONS.md` (no sequential numbers, to avoid collisions between branches). Old `KI-nn` IDs were converted on 2026-11-07.



## Fix before use


## Gaps


### `2026-11-06-validator-python-only`: The ability validator exists only in Python

`data/validate.py` implements the ten checks. The proposal says the engine validates ability coherence at deck validation. Either port the checks to Java (and keep `validate.py` as a CI gate), or call the engine only for deck-level rules.


### `2026-11-06-faction-swap-open-rules`: Faction-swap series: open rules

Not decided yet: whether `FACTION_SWAP` is the main format; how the faction of game 3 is chosen; when game 3 is needed (a 1-1 split, or also after draws) and when a series ends; how the series outcome follows from the games; and how many games a series may last (after a drawn game 3 the rule is "swap and play again", with no stated cap). The schema stores each game and its factions and leaves these to the backend.


### `2026-11-06-ruleset-change-illegal-deck`: A ruleset change can make a player's last legal deck illegal

Players must always have at least one legal deck per faction (`2026-11-06-starter-decks`). Decks are pinned to a pool snapshot (`decks.snapshot_id`), so a new snapshot does not touch existing decks; the problem appears when decks are moved to a new snapshot, or when an old one is retired. Not decided. Options: keep the old snapshot playable until the player migrates; mark a deck invalid and grant a fresh starter deck when no legal deck remains; or reject the new snapshot if it would leave players without one. Also open: where the starter deck templates are stored (no table yet).


### `2026-11-07-camaleonte-suit-choice`: Which suit Camaleonte takes in a hand

`2026-11-01-camaleonte-suit` says Camaleonte counts as any suit for Colore and Quattro dello stesso seme (not for synergies). Not decided: which suit the caller passes to the evaluator when several choices exist (for example a Camaleonte next to three Vincolo and two Spira). Proposal: the choice that gives the best hand, and on a tie the lowest `Suit` ordinal, so it stays deterministic. The evaluator only sees the effective suit in `HandCard`, so the rule belongs to whoever builds the hand input, not to `HandEvaluator`. No test yet.


### `2026-11-07-class-i-trigger`: Which trigger a spell or equipment expansion carries

Spells and equipment have no trigger (`vocab.card_contexts`), and their effects are class `I` (immediate). But an `ExpansionDef` has a `triggerId`, and the loader requires it. Not decided what a spell's or equipment's expansion puts there: a dedicated "when drawn" trigger, `null`, or a loader exception for class `I`. To decide when the spell and equipment data are written, together with the validator and the loader. See `2026-11-07-cards-share-abilities`.


### `2026-11-07-drawn-card-for-spells`: What `drawn_card` means for a spell

`vocab.card_contexts` gives spells the context `drawn_card`, which target `S28` ("Carta pescata") uses. It does not say which card that is. Probably the spell itself, the card whose draw triggered the effect. To confirm with whoever defines the spell data before spells are written.


### `2026-11-07-cycle-2-ties-with-effects`: Same-lane Cycle 2 attacks and redirects

Without effects, two attackers of different players on the same lane number cannot both be in Cycle 2 (each has an enemy in the opposite lane, so both attack in Cycle 1). With redirect effects (9.4) it can happen. The tie order is set (`2026-11-07-resolution-order-faction-then-side`), but the redirect rules (which cycle a redirected attack uses, what happens to a redirect to a destroyed creature) belong to the effects work and are not yet implemented.


### `2026-11-07-spell-equipment-hooks-no-data`: Spell and equipment hooks do nothing

`CardEffects.onSpell` and `onEquipment` are no-ops (`TODO(spells-equipment)`) because no spell or equipment cards exist. Refill draws, resolves and replaces them as decided (`2026-11-07-refill-spells-equipment-immediate`), but nothing is tested yet. When cards exist: write those tests and replace the no-op default.


## Resolved (only list those with future impact)


### `2026-11-04-spira3-opponent-evolutions`: Spira 3 counts opponent Evolutions  [RESOLVED 2026-11-04]

"Per ogni Evoluzione innescata in gioco" counts **only your own** Evolutions, not the opponent's. Confirmed as intended; removed from open gaps.