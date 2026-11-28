# Balance changelog

Qui vive la storia di ogni modifica di bilanciamento di QUORMA: cosa è cambiato, perché, cosa è stato scartato e come
verificare. Un file per versione.

**Per lo stato finale di tutto in un solo file: [RIEPILOGO.md](RIEPILOGO.md).**

## Come si versiona

`vMAGGIORE.MINORE.PATCH`, indipendente dalla versione del motore e del regolamento (`1.0-alpha`).

| Parte | Quando sale |
|---|---|
| MAGGIORE | cambia una regola strutturale del gioco (turno, mani, limiti di mazzo) a gioco già in vendita |
| MINORE | un passaggio di bilanciamento: più numeri o abilità cambiati insieme, con un'unica simulazione di controllo |
| PATCH | una correzione piccola a un passaggio precedente (un valore, un errore nei dati) |

Prima del rilascio 1.0 tutto resta in `v0.x`. Ogni versione dichiara l'hash delle regole (`ruleset_hash`) dei dati che
descrive: due dati con hash diverso **non** vanno confrontati né mescolati nello stesso database di statistiche.

Ogni file di versione ha sempre queste sezioni: Sintesi, Modifiche (con valore prima e dopo), Decisioni e motivi, Cosa NON
è stato fatto, Interpretazioni da confermare, Modifiche al motore e ai test, Come verificare, Compatibilità.

Il registro breve del repository resta `QUORMA-backend-engine/docs/rules/CHANGELOG.md` e le decisioni stanno in
`QUORMA-backend-engine/docs/DECISIONS.md`. Qui c'è il testo completo.

## Versioni

| Versione | Data | Hash regole | Contenuto |
|---|---|---|---|
| [v0.2.7](v0.2.7.md) | 2026-11-28 | `61f60f2c…9e24` | Ines: ATK 3000, DEF 7050, Rinforzo 350, Indurisce 500. Pannello: ATK 2100, DEF 8400 |
| [v0.2.6](v0.2.6.md) | 2026-11-28 | `61f60f2c…9e24` | Ines: ATK 2750, DEF 7300, Indurisce 450. Pannello: ATK 2400, DEF 9600, Muro 20% |
| [v0.2.5](v0.2.5.md) | 2026-11-28 | `3f9b7a67…50e2` | Frattura più debole: tier 3 +800 ATK, tier 4 ×1,10 con DEF al 90%, tier 5 con DEF al 70% (sostituisce i valori di v0.2.4) |
| [v0.2.4](v0.2.4.md) | 2026-11-28 | `477c5aa0…8a7d` | Frattura: tier 3 +1000 ATK, tier 4 ×1,15 con DEF al 90%, tier 5 con DEF al 60% |
| [v0.2.3](v0.2.3.md) | 2026-11-28 | `584f6e55…cdc1` | Quattro dello stesso seme: moltiplicatore da ×1,35 a ×1,28 (effetto nullo nelle simulazioni) |
| [v0.2.2](v0.2.2.md) | 2026-11-27 | `cd49b93b…599f` | Letale: quota di DEF nello spill da 28% a 42% (correzione dell'errore di v0.2.0) |
| [v0.2.1](v0.2.1.md) | 2026-11-27 | `0120e0c8…f239` | Buff di Spira: tier 3 da +200 a +350 ATK, tier 4 da ×1,2 a ×1,3 |
| [v0.2.0](v0.2.0.md) | 2026-11-27 | `96088176…193a` | Prima passata dopo lo studio statistico: Quattro dello stesso seme con 3 attaccanti, moltiplicatori di Coppia, Doppia coppia e Tris, otto abilità ritoccate, Confine ridefinito |
| v0.1.0 | fino al 2026-11-26 | `24d88983…fcc1` | Stato di partenza dello studio statistico (il documento `STATS-BALANCE.md` è stato rimosso: la sintesi è in [RIEPILOGO.md](RIEPILOGO.md)). Non ha un file: è lo stato del repository al commit `309a2ed` |
