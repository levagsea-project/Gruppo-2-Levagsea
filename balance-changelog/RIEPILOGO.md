# Riepilogo del bilanciamento: stato finale (v0.2.7, 2026-11-28)

Questo file riunisce in un solo posto lo stato finale di tutte le versioni (`v0.2.0` … `v0.2.7`). Per il ragionamento passo per passo di ogni
versione, i motivi e le interpretazioni si leggono i file `vX.Y.Z.md` nella stessa cartella. Qui c'è solo il risultato.

Hash delle regole finale: `61f60f2c8d9f37d3554fba67fda54686f2464a9b5014d60d7bb940b1925f9e24`.
Nessun valore è stato rifatto passare dal modello dei costi delle carte (vedi sezione 5).

## 1. Cosa è stato fatto

Studio statistico su 60.000 partite miste e 30.000 casuali (stessi seed e stessi mazzi a ogni versione, motore sempre ricostruito con le regole
nuove), poi otto passaggi di bilanciamento guidati dall'autore. Il punto di partenza (v0.1.0, hash `24d88983…`) è lo stato del repository al
commit `309a2ed`.

| Versione | Hash regole | Contenuto in una riga |
|---|---|---|
| v0.1.0 | `24d88983…fcc1` | stato di partenza |
| [v0.2.0](v0.2.0.md) | `96088176…193a` | Quattro dello stesso seme a 3 attaccanti, Coppia/Doppia coppia/Tris, otto abilità, Confine |
| [v0.2.1](v0.2.1.md) | `0120e0c8…f239` | buff Spira 3 e 4 |
| [v0.2.2](v0.2.2.md) | `cd49b93b…599f` | Letale 42% (corregge l'errore di v0.2.0) |
| [v0.2.3](v0.2.3.md) | `584f6e55…cdc1` | Quattro dello stesso seme ×1,28 |
| [v0.2.4](v0.2.4.md) | `477c5aa0…8a7d` | nerf Frattura (prima passata) |
| [v0.2.5](v0.2.5.md) | `3f9b7a67…50e2` | nerf Frattura (seconda passata, sostituisce v0.2.4) |
| [v0.2.6](v0.2.6.md) | `61f60f2c…9e24` | Ines buff, Pannello nerf, Muro 20% |
| [v0.2.7](v0.2.7.md) | `61f60f2c…9e24` | Ines e Pannello (seconda passata, solo valori di carta) |

## 2. Valori finali (prima → dopo)

### Mani di poker (`data/balance.json`, `hand_multipliers_permille`)

| Mano | Prima | Finale | Note |
|---|---|---|---|
| Coppia | 1100 | **1150** | |
| Doppia coppia | 1300 | **1340** | |
| Quattro dello stesso seme | 1350, 4 attaccanti | **1280, 3 attaccanti** | i 3 più forti; a parità di rango la casella più bassa |
| Tris | 1400 | **1500** | |
| Tutte le altre | invariate | invariate | Carta alta 1000, Scala 1600, Colore 1700, Full 1800, Poker 2200 |

L'ordine di forza delle mani non è cambiato, ma il moltiplicatore di Quattro dello stesso seme (1280) ora è **sotto** quello della Doppia coppia
(1340): conta solo per lo spill, il confronto tra mani resta quello dell'ordine.

### Sinergie di seme (`data/balance.json`, `synergies`)

| Sinergia | Prima | Finale |
|---|---|---|
| Spira 3 | +200 ATK per Evoluzione innescata | **+350** |
| Spira 4 | ×1,2 ai valori delle Evoluzioni | **×1,3** |
| Spira 5 | invariata | invariata |
| Frattura 3 | +1200 ATK | **+800** |
| Frattura 4 | spill ×1,2, DEF bersaglio all'80% | **×1,10, 90%** |
| Frattura 5 | spill ×1,2, DEF bersaglio al 50% | **×1,2 (invariato), 70%** |

Una quota di DEF più alta nello spill dà **meno** spill.

### Abilità

| Abilità | Prima | Finale | Dove |
|---|---|---|---|
| Letale | spill con DEF al 35% | **42%** | `balance.json` `K_LETALE.spill_def_permille` 350 → 420 |
| Muro | 30% di danno in meno | **20%** | `balance.json` `K_MURO.permille` 800 (modalità `permille`, prima `pct` 30) |
| Confine | corsia di fronte + 2 adiacenti | **solo le 2 corsie adiacenti** | motore, bersaglio S45 |
| Campi vuoti | 96 permille | **80** | 3 carte: Postilla dei Campi Vuoti, Tacet, Tobia |
| Eco | 480 | **360** | 2 carte: Ascoltatori del Canale 9, Replica |
| Catena | 360 | **300** | 2 carte: Dissenso, Tabita |
| Accumulo | 48 | **67** | 1 carta: Anagrafe in Transito |
| Indurisce | 300 | **500** | solo Ines |
| Rinforzo | 300 | **350** | solo Ines |

Il valore di Letale 28% di v0.2.0 era un errore di verso: abbassare la quota di DEF **aumenta** lo spill. È stato corretto in v0.2.2.

### Carte

| Carta | Campo | Prima | Finale |
|---|---|---|---|
| Ines `BASE-RES-A-SPI-ADA` | ATK / DEF | 2550 / 6350 (rapporto 2,5) | **3000 / 7050** (rapporto 2,35) |
| Pannello `BASE-RES-2-VES-SPE` | ATK / DEF | 2350 / 9400 (rapporto 4) | **2100 / 8400** (rapporto 4) |

## 3. Risultati finali contro lo stato di partenza

Simulazione di v0.2.7, motore con hash `61f60f2c…`. Errore tipico sulle percentuali di un gruppo di mazzi circa 0,2 pp; per carta circa 0,45 pp.

| Misura | v0.1.0 | **v0.2.7** | Banda obiettivo |
|---|---|---|---|
| Mazzi per seme | 58,5% | **57,0%** | 44–56% |
| Mazzi per combinazioni | 43,0% | **44,8%** | 44–56% |
| Mazzi casuali | 35,6% | **35,9%** | 44–56% |
| FRATTURA / VINCOLO / VESTIGIO / SPIRA | 65,1 / 59,6 / 57,5 / 51,5 | **59,1 / 57,9 / 56,4 / 54,4** | |
| Distanza tra il seme più forte e il più debole | 13,6 pp | **4,7 pp** | |
| Divario tra fazioni (misti) | 2,0 pp | **1,4 pp** | |
| Turni medi misti / casuali | 8,9 / 11,3 | **9,4 / 11,5** | 7–10 |
| Quattro dello stesso seme, turni dei mazzi per seme | 20,4% | circa 21,9% | |

Carte (effetto in pp rispetto alla media della fazione):

| Carta | v0.1.0 | v0.2.7 |
|---|---|---|
| ARACNO «Anarco» | +3,7 | **+1,3** |
| Tobia, Solo sul Crinale | +2,5 | +1,8 circa |
| Tacet, Voce Unica | +2,5 | +0,9 circa |
| Pannello della Reliquia | +1,8 | **+1,8** |
| Ines «Oltre la Sutura» | −3,8 | **−1,8** |
| Più deboli ora | | Berenice −2,6, Seduta straordinaria −2,4, Elio −2,4 |
| Più forti ora | | SEK-54 «Battente» +2,9, Teodora +2,5, Etichetta +2,0 |

## 4. Cosa resta fuori banda

1. **Mazzi per seme** al 57,0% (1 pp sopra). "Quattro dello stesso seme" compare ancora in circa un turno su cinque dei loro turni: né il moltiplicatore (1350 → 1280)
   né il numero di attaccanti (4 → 3) hanno cambiato quella frequenza. L'autore ha deciso di **non** introdurre un limite di carte per seme nel mazzo.
2. **Mazzi casuali** al 36% e partite di 11,5 turni (obiettivo 7–10).
3. **Carte ancora lontane dalla media**: Berenice, Seduta straordinaria, Elio (deboli), Battente, Teodora (forti).

## 5. Cosa NON è stato ricalcolato

- **I costi delle abilità e i punti statistica** delle carte toccate. Il modello dei costi dice `stat_points = ATK + DEF/2`: Ines passa da 5708 a **6525** (+14%), Pannello
  da 7040 a **6300** (−10%). Ines è fuori dal budget del suo Tier. I campi `stat_points` in `docs/cards/card-effects.json` sono aggiornati ai valori nuovi.
- Eco 600, Catena 300/240 e Indurisce 160/240 (le altre carte con le stesse abilità) non sono state toccate, quindi la scala per Tier di quelle abilità non è più proporzionale.

## 6. Come si misura (per rifare i test)

- **Mazzi**: 32 carte, 50% per seme, 40% per combinazioni, 10% casuali (strumento `json-translator`, modalità "Casuale con logica", fuori da questo repository);
  fazioni alternate tra i lati (non c'è effetto del lato: 50,1%).
- **Seed**: partite miste da 1.000.000, casuali da 2.000.000; mazzi identici a ogni versione, quindi il confronto è a coppie.
- **Effetto per carta**: regressione del risultato sulle carte presenti nel mazzo, con le strategie dei due mazzi come controlli. Un'abilità si legge come media delle sue carte.
- **Avvertenze**: i dati di versioni con `ruleset_hash` diverso non si mescolano nello stesso database; le differenze sotto 0,2 pp (gruppi) o 0,45 pp (carta singola) sono rumore.

## 7. Struttura dati e codice cambiati

| Area | Cambiamento |
|---|---|
| `HandEvaluator` | Quattro dello stesso seme restituisce 3 attaccanti (i più forti, a parità la casella più bassa) |
| `EnemyNeighboursTarget` (S45) | solo le corsie a distanza 1 (prima ≤ 1). Usato solo da Confine |
| `DamageReductionEffect` (E28) | nuova modalità `permille` (fattore in per mille: 800 = −20%); il record `Reduction` ha un quarto campo, `fine` |
| `Damage`, `Strike` | la mitigazione si calcola in per mille (`mitigationFromPermille`); le riduzioni in percentuale intera si comportano come prima |
| `Modifier`, `match-log.schema.json` | nuovo tipo di modificatore `damage_reduction_permille`. I lettori con un elenco chiuso di tipi vanno aggiornati |
| `data/balance.json` | moltiplicatori delle mani, `K_LETALE`, `K_MURO` (ora `permille`), sinergie `SPIRA_3/4`, `FRATTURA_3/4/5` |
| `data/keyword.json`, `data/vocab.json`, `data/synergies.json` | testi di Confine, Muro, S45, Frattura, Spira allineati ai valori |
| `docs/cards/card-effects.json` → `data/sets/BASE.json` | valori delle carte (rigenerato con `tools/base-set/`) |
| Regolamento 7.8, 7.9, 7.10 | tabelle dei moltiplicatori e delle sinergie aggiornate |
| Test e golden | valori attesi ricalcolati a mano; i 3 golden, l'esempio di log e il golden di `engine-service` cambiano solo per `ruleset_hash` e `result_hash` |

Compatibilità: i log di partita giocati con regole precedenti non sono riproducibili (l'hash è cambiato); nessun cambiamento al formato dell'input di una partita.

## 8. Idee valutate e non applicate

- **Limite di carte per seme nel mazzo**: scartato dall'autore.
- **Generazione dei mazzi iniziali** (1 Asso, 3–4 figure, Tier 2 e Tier 1 in rapporto 1:2, magie/equipaggiamenti a rapporto casuale 35–65%, un seme al massimo al 35–40% con
  rigenerazione fino a 4 volte). Simulata su 20.000 partite: 50,5% contro se stessa, solo il 13% dei mazzi rigenerato, ma 32% di vittorie contro un mazzo per seme e
  42% contro uno per combinazioni; regalare un Asso e 3–4 figure toglie valore ai pacchetti. Non implementata; il mazzo iniziale oggi resta quello della regola fissa in `StarterDeckService`.
- Starter come scelta tra archetipi, buchi voluti nello starter, uno slot dei pacchetti guidato sui buchi del mazzo, più versioni della stessa carta: proposte, non implementate.

## 9. Errori trovati durante il lavoro

- **Letale 28%** (v0.2.0): era un buff, non un nerf. Corretto con 42% in v0.2.2.
- **"Rinforzo 48/1000"**: era Accumulo. Rinforzo era stato lasciato com'era in v0.2.0 e modificato solo in v0.2.7, su richiesta.
- **Tabella di 7.2 dello studio**: i valori non coincidevano con `balance.json` perché le carte portano valori già scalati per Tier.
