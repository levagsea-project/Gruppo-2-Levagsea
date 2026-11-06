# Known issues and open points



Add an entry here instead of guessing. Remove it when fixed. Date format YYYY-MM-DD.



## Fix before use


## Gaps


### KI-07 The ability validator exists only in Python

`data/validate.py` implements the ten checks. The proposal says the engine validates ability coherence at deck validation. Either port the checks to Java (and keep `validate.py` as a CI gate), or call the engine only for deck-level rules.






## Resolved (only list those with future impact)


### KI-08 Spira 3 counts opponent Evolutions  [RESOLVED 2026-11-04]

"Per ogni Evoluzione innescata in gioco" counts **only your own** Evolutions, not the opponent's. Confirmed as intended; removed from open gaps.