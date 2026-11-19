# Known issues and open points



Add an entry here instead of guessing. Remove it when fixed. Date format YYYY-MM-DD.

**ID format:** `YYYY-MM-DD-short-slug`, same as `DECISIONS.md` (no sequential numbers, to avoid collisions between branches). Old `KI-nn` IDs were converted on 2026-11-07.



## Fix before use


## Gaps

### `2026-11-17-base-set-engine-readings`: How the engine reads the base set where the data leaves room

The engine resolves the whole base set (`2026-11-17-base-set-engine`). Where the vocabulary or the card text left a choice, it does the following; confirm or change each one.

- **Temporary DEF bonuses** (E2 with a duration): when they end, DEF goes back down by the bonus, never below 1 for a creature still in play. E59 takes DEF bonuses away the same way.
- **E61** (Confine): positive ATK modifiers are ignored while it applies, and a positive E2 aimed at the creature does nothing; DEF bonuses it already had stay.
- **E59** (Conformita') removes positive ATK modifiers, DEF bonuses and the keywords and Evolutions granted by triggered effects or spells; what an equipment or a static effect grants comes back at once.
- **Triggers from a destroyed creature**: T10 (its own destruction), T13 (Eco still reflects the damage that destroyed it), T19 (a killer destroyed in the same exchange) and T38 fire; the others need a living source. T36 does not fire on the destroyed creature itself.
- **Kills by an effect** trigger T19 on the effect's source creature, even when it destroys an ally (Furia cieca, Ordine pagato), and end its `D_FinoAUccisione` effects.
- **Order after blows**: destructions (T10, T19, T36, T37) resolve first, then damage (T13, T38, T11), then survivors (T12), each a separate stack.
- **T6/T7** ("quando attacca", "quando e' attaccata") fire before the blow; in Cycle 1 all of them resolve before the simultaneous blows are worked out. T22 and T39 replacements come first.
- **E35** (La risposta prima della lettera) draws the top card: a creature goes to the lowest empty lane (with none it stays on the deck), a spell resolves, an equipment waits for the next creature.
- **E47** (Assegnazione sospesa) keeps every empty lane of the affected player empty for one Refill pass, then it is spent; the deck is not touched.
- **Spells' static effects**: aimed at a selection (S33-S40) they go to the creature chosen when the spell resolves; aimed at a group (S9, S10, S11, S20) they reach whoever is in it at each moment. A T4 selection (Voce fuori conteggio, Chi conta) is chosen at each Evaluate.
- **E30 and E32 with C9**: the condition is read on the hand that results; the substitution is used only if it gives the best hand (it is optional).
- **A DEF malus that brings DEF to 0** (Slancio, Salita, Prelievo, equipment costs) destroys the creature by effect: T20 replacements apply, then T10, T36 and T37.
- **Card params**: a card may write any number of its ability's balance entry (for example `max_stacks` of Assimilazione), as `$tune` already read them from the card first.
- **K_CONTA_COME** is `open` on every rank: `limited` with every rank is refused by the validator; the set uses it on ranks 5, 7, 10 and K.


### `2026-11-17-set-base-java-tuning`: Base set values to tune in the Java simulation

The base set's structure is fixed in `docs/cards/card-effects.json` (decisions `2026-11-17-set-base-*`), but the Python smoke test (`tools/balance-sim/report-set-base-1.md`) left these values out of band. Retune them in the Java simulation once the effects are implemented, not before: the Python numbers predate the structural fixes. The effects are now implemented (`2026-11-17-base-set-engine`); `BaseSetTest` plays 300 matches between random decks of the set, a starting point for that simulation.

- **Mono-suit builds** win 67-74% (Resistenza) and 59-62% (Potenze): 13 of 22 creatures of one suit is 59% of a Resistenza deck, so four of a suit comes up in 15-23% of turns. Levers: the Quattro dello stesso seme multiplier, the suit tiers, or a per-suit deck limit (a ruleset change to 3.1 and to deck validation).
- **High-card and random builds** win 38-43%, under the 42% floor of `data/AGENTS.md` section 9; no support card lifts them past 52%. Levers: the Carta alta multiplier, Campi vuoti, Urlo della vittoria.
- **Hand spread:** straight, flush, full house and four of a kind are each under 1% of turns; 82% are high card or pair.
- **Aces:** Ilemo +9.8 (both abilities pull: without Carnefice 59.6%, without Assimila 63.6%), Berenice -12.1, Ines -8.1 (Rinforzo and Indurisce worth about 0), Errata -7.7 (Il registro precede +0.8 for 8%), Elio -4.1.
- **Others:** Tabita +8.5 (recheck with the one-substitution rule), Pannello della Reliquia / Muro +8.9 (no stalls), Campi vuoti (recheck at 60), Presagio (recheck with the scope and once-per-turn), Posto tenuto and the E36 spells (new semantics, never simulated), Rinforzo (fires in 85% of games), Calce -5.4, Nives -4.5, Lacuna -4.9, Zaira's Sigillo aperto (rarely matters), Flessibile (almost inert).


### `2026-11-06-faction-swap-open-rules`: Faction-swap series: open rules

Not decided yet: whether `FACTION_SWAP` is the main format; how the faction of game 3 is chosen; when game 3 is needed (a 1-1 split, or also after draws) and when a series ends; how the series outcome follows from the games; and how many games a series may last (after a drawn game 3 the rule is "swap and play again", with no stated cap). The schema stores each game and its factions and leaves these to the backend.


### `2026-11-17-no-spell-equipment-data`: Spells and equipment have no balance data

The 46 spells and equipment of the base set are defined (`data/sets/BASE.json`, checked by `validate.py` check 12 and by the engine's card check) and the engine plays all their effects (`2026-11-17-base-set-engine`). What `data/` still lacks: there is no `spells.json` and `balance.json` has no `spells` / `equipment` sections, so pool generation does not make them and nothing prices or limits their power (no stat cost, no cap class), and the five-build matrix of `data/AGENTS.md` section 9 does not cover them. To decide: their rarity and cost, where those numbers live (`balance.json`), and a validator pass over them.

### `2026-11-12-pack-odds`: Pack prices, sizes and Tier weights are placeholders

The three pack types (`standard`, `advanced`, `elite`) are `pack_types` of the base set file (`data/sets/BASE.json`), with prices and slots per Tier that are placeholders (`2026-11-12-pack-pricing-strategy`, from the team's first proposal); an admin changes them through `PUT /admin/catalogue/pack-types/{code}`. The mapping of rarity names to Tiers (comune, non comune, rara, epica = Tier 1 to 4) is an assumption to confirm. Spells and equipment have no Tier, so their pack slot is a plain uniform draw over all the spells and equipment of the pack's set. Packs can be opened now that the base set has spells and equipment (the engine checks at every publish that the pool of a set can fill each of its pack types). Not decided: spell and equipment rarity; how adding cards to a set changes the odds inside the packs of that set (the check covers fillable slots, not balance); and whether a pack type can be limited in time (a set has a status and dates, a pack type does not). The name of a pack type is not data yet: the frontend labels `standard`, `advanced` and `elite` only. Also: `auth.registration-bonus` is 0 by default, so a new player cannot buy a pack without a grant.

### `2026-11-19-starter-deck-not-validated`: The starter decks are a first rule, not a validated design

Every new player gets one starter deck per faction, chosen by a fixed rule in `StarterDeckService`: the lowest creatures first (rank 2 upwards, the Ace last, one per suit and rank) up to the fewest creatures the faction's deck rules allow (20 for Resistenza Umana, 24 for Potenze Artificiali), then spells and equipment alternately by card code up to 32 cards. `DeckValidator` accepts them, so they are legal, but nothing more has been checked: they have not been simulated or playtested, so it is not known whether they win or lose against each other, against a deck built from packs, or against the defense decks of other players. The choice ignores the suits' synergies (a deck of four suits at ranks 2 to 6 reaches few of them), abilities and the subfaction matrix, and the support cards are the first ones by code, not the best ones. Every player gets the same decks. To decide: whether the starter decks are authored by the design team (a list in the set file, which also lets them be tested like any build, with the five-build matrix of `data/AGENTS.md` section 9) instead of derived by a rule, and what strength a new player's first deck should have. Until then the rule only guarantees a legal, playable deck so that a new player can play at once.

### `2026-11-19-vanilla-card-parity`: May a card have no ability? The engine and validate.py disagree

The engine's card check (`CardPoolChecker`, used at every publish) accepts a creature whose `abilities` list is empty, and the engine plays it (many engine tests use such creatures). `data/validate.py` check 12 rejects the same card (`abilities must be a non-empty list`), so a vanilla creature in a set file of the repo fails CI, while the same card sent through the admin API or a batch import passes the engine's check and is published. `CardCheckParityTest` records this as the one known difference between the two (`game-engine/src/test/resources/parity/cards.json`) and fails if it moves. Every card of the base set has at least one ability, so nothing breaks today. To decide: whether a card with no ability is allowed. If it is, drop the rule from `validate.py`; if not, add it to `CardPoolChecker` (a new check code such as `card.no_ability`) and change the parity case to a plain rejection. The rule must also be said in the ruleset or `data/AGENTS.md`, which today does not mention it.

### `2026-11-19-admin-card-tools`: There is an admin API for the catalogue, and no admin tool

`/admin/catalogue` can do everything (a draft, its preview and publish, batch import and export, drift, direct edits of texts, set status and membership, and pack types), but there is no screen for it. Next step: an admin card editor (a card, its texts in every language, its sets, with the engine's preview) and admin set tools (create a set, change its status and dates, move cards in and out, edit its pack types). The problem codes of a draft report (`draft.*`, and the engine's `card.*`, `pool.*`, `pack.*`) are data for the tool to translate and have no labels yet, and a pack type's name is not data yet. Only one draft can be open at a time, so two admins cannot work in parallel.

### `2026-11-19-ability-text`: A card shows its abilities by name only

There is no player-facing text for abilities (`data/labels` has none; `ability.<ID>.text` does not exist). Proposal to review: text for every part of an effect. A keyword or evolution has a name and a glossary text with `{x}` placeholders filled from the card's own params (shown on hover or in a glossary); an ability written on the card is described from its parts (a label per trigger, effect, target, condition and duration of the vocabulary, composed into a sentence). The frontend renders it from the labels and the card's `abilities`, so the API still sends no text. Italian grammar (gender, number, articles) makes naive composition fragile, so each target label should carry its article forms. `validate.py` would then require a label for every vocabulary id that a live card uses. Not started: it is new player-facing wording and needs review.
