# Design decisions



Commit log of taken decisions. Status: **applied** (in `data/` and `docs/rules/`), **rejected** (kept so the choice is never re-suggested).



| ID | Date | Decision | Status |
|---|---|---|---|
| D-01 | 2026-11-01 | **Camaleonte does not count as another suit for synergies** (already in vocab `E55`); only for Colore and Quattro dello stesso seme. Text mirrored in `keyword.json`. | applied |
| D-02 | 2026-11-01 | **Suit themes**: Vincolo adjacency, Frattura breakthrough, Spira adaptation + growth, Vestigio persistence. `evolution` is a mechanism, not a theme; Spira's pull on Evolutions is `evolution_slot_fill_pct`. | applied |
| D-03 | 2026-11-01 | **Ability caps by class**: `capped` = 1 rank (max 4 per deck), `limited` = 2-3 ranks, `open` = any rank. Busted abilities are capped; capped abilities may share a rank, so the 4-card cap is combined. Rank does not decide the ability: an Ace may carry any open ability. | applied |
| D-04 | 2026-11-01 | **An Evolution that grants a capped/limited keyword inherits its ranks.** Closed a leak (Mimetico on rank 3 granted Camaleonte, rank 2). | applied |
| D-05 | 2026-11-01 | **Engine deployment**: standalone Java 25 + Spring Boot 4.1.x, packaged as a Docker container and served on a configured HTTP port as a separate microservice. | applied |
| D-06 | 2026-11-01 | **`data/validate.py` is the reference validator** (ten checks from `data/AGENTS.md` §7, cap model, `ranks.json`, fit targets). | applied |
| D-07 | 2026-11-04 | **Database engine**: changed the backend's relational database from MySQL 8.4.x to PostgreSQL 16 (JDBC connection, Hibernate default dialect). | applied |
| D-10 | 2026-11-05 | **Repricing against a working stat scale** (ATK about 4000, DEF 1.5-2.5 x ATK, life 12000, about 9 turns). Scudo x 15 -> 25 with cost 3 (per-x 0.12), Sostegno cost 5 -> 9, Insaziabile 10% -> 15% per stack with cost 4, Muro bonus 10% -> 30%, Custode cost 10 -> 15. Rigenerante unchanged (already fair at this scale). Numbers come from a simplified simulation, not from real cards. | applied |
| D-11 | 2026-11-05 | **Combined deck cap on swingy abilities (max 4 cards per deck).** Rejected: abilities are limited by which ranks may carry them, never by a deck-level count. Rank access also makes high-Tier cards wanted without making them stronger, and keeps low-Tier decks competitive. | rejected |
| D-12 | 2026-11-05 | **Frattura reworked**: tier 3 +[1200] ATK (was 300, rescaled by D-14); tier 4 keeps spill x[1.2] and also treats the target's DEF as [80]% for spill; tier 5 adds spill x[1.2] to its 50% DEF rule so it is a strict upgrade over tier 4. Ruleset 7.10.6 updated to match. | applied |
| D-13 | 2026-11-05 | **Rank concentration is not limited.** Decks built on few ranks hit pairs and better much more often and win clearly more; this is accepted by design. | applied |
| D-14 | 2026-11-05 | **Ruleset clarifications and placeholder rescale.** Spira 3 counts only your own Evolutions (new `scale_by` value `own_evolutions_triggered`); spill has a minimum of 0; the turn-50 tie-break uses the poker hand of the last Evaluation; Vestigio 3 bonus goes to the replacement of any suit. Flat synergy placeholders doubled for the working stat scale (ATK about 4000): Vincolo [600]/[400]/[1000], Frattura 3 [1200], Spira 3 [200] per Evolution, Vestigio 3 [1000] and 4 [1400]; Scudo flat range [400, 1600]. One-suit decks gain about +2 to +2.5 points each in simulation. Spells and equipment stay in the ruleset for later. | applied |
