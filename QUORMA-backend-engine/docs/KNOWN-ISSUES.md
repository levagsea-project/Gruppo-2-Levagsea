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


### `2026-11-07-four-of-a-suit-kicker`: Four of a suit uses the fifth creature as a kicker (ruleset wording differs)

Ruleset 7.5 says a Four of a suit is compared "per i ranghi delle sue quattro creature". The engine contract (`HandEvaluatorTest`) also uses the fifth creature as a last tie-break value when there is one, as every other hand with a kicker does. Decided with the Task 1 owner on 2026-11-07. Until ruleset 7.5 is updated, this is the wording: *"Un Quattro dello stesso seme viene confrontato per i ranghi delle sue quattro creature, dal più alto al più basso, poi per il rango della quinta creatura, se c'è."* The fifth creature still does not attack (7.8). A missing fifth creature counts as 0 (`2026-11-07-missing-kicker-is-zero`). Tests: `HandEvaluatorTest.fourOfASuitWithFiveCreaturesUsesTheOddOneAsKicker`, `HandCompareTest.fourOfASuitComparesItsFourRanksThenTheKicker`.


### `2026-11-07-camaleonte-suit-choice`: Which suit Camaleonte takes in a hand

`2026-11-01-camaleonte-suit` says Camaleonte counts as any suit for Colore and Quattro dello stesso seme (not for synergies). Not decided: which suit the caller passes to the evaluator when several choices exist (for example a Camaleonte next to three Vincolo and two Spira). Proposal: the choice that gives the best hand, and on a tie the lowest `Suit` ordinal, so it stays deterministic. The evaluator only sees the effective suit in `HandCard`, so the rule belongs to whoever builds the hand input, not to `HandEvaluator`. No test yet.


## Resolved (only list those with future impact)


### `2026-11-04-spira3-opponent-evolutions`: Spira 3 counts opponent Evolutions  [RESOLVED 2026-11-04]

"Per ogni Evoluzione innescata in gioco" counts **only your own** Evolutions, not the opponent's. Confirmed as intended; removed from open gaps.