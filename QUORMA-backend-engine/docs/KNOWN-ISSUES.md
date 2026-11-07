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


## Resolved (only list those with future impact)


### `2026-11-04-spira3-opponent-evolutions`: Spira 3 counts opponent Evolutions  [RESOLVED 2026-11-04]

"Per ogni Evoluzione innescata in gioco" counts **only your own** Evolutions, not the opponent's. Confirmed as intended; removed from open gaps.