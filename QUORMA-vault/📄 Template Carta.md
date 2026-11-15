---
tags:
  - template
  - quorma
---

# 📄 Template Carta — come si fa una carta

Guida per creare carte ed elementi di canone a mano. Per farle scrivere a un LLM usa [[🤖 Prompt Carte]], che produce già questo formato.

## In breve

1. **Scegli lo slot** tra quelli liberi in [[🗂️ Slot del set base]]: fazione + rango + seme. La sottofazione è già decisa dal foglio Excel; non cambiarla. Dopo aver salvato la carta rilancia `python Strumenti/elenca_slot.py`: aggiorna la nota e l'elenco degli slot liberi nel prompt.
   - *Griglia Resistenza:* il 2026-11-14 è stato deciso che **J · Vincolo è Portanome**, così ogni sottofazione ha 13 slot (4+3+3+3 per seme). Finché il foglio Google non è aggiornato, `esporta_excel.py` applica la correzione da solo.
2. **Crea la carta**: nuova nota in `Cards/` con il titolo come nome, poi *Inserisci modello* → **Carta** (`Strumenti/Template/Carta.md`). Sostituisci i valori segnaposto (`0`, `Vincolo`, `Portanome`…) in frontmatter, tag, etichetta del seme e riga di registro.
3. **Crea gli elementi di canone, se servono**: solo per ciò che un'altra carta potrebbe incontrare (un luogo, un'entità, un oggetto, una pratica condivisa). Nuova nota in `Lore/Canone/` → *Inserisci modello* → **Elemento di canone**. Il CAN è il successivo all'ultimo della vista Registro di [[📋 Canone.base]]; i CAN ritirati non si riusano. Frasi, formule e dettagli che appartengono solo a questa carta vanno nel callout **Glossario** della carta, senza CAN. Vedi «Elemento di canone» più sotto.
4. **Collega**: nella sezione dei nuovi elementi della carta lascia solo `- **CAN-xxx** [[Nome]]`; nella lore interna collega carte ed elementi esistenti.
5. **Aggiorna il foglio Excel** con la riga della carta.

## Frontmatter della carta

| Campo | Valori |
|---|---|
| `id` | `BASE-{AI\|RES}-{rango}-{VIN\|FRA\|SPI\|VES}-{EFF\|MER\|SEK\|YLM\|ADA\|SPE\|POR\|SEG}` |
| `titolo` | uguale al nome del file; max 32 caratteri, unico, senza `/ \ : * ? " < > \|` |
| `fazione` / `fazione_excel` | `Potenze Artificiali` / `AI` · `Resistenza Umana` / `Resistenza` |
| `rango` | tra virgolette: `"2"` … `"10"`, `"J"`, `"Q"`, `"K"`, `"A"` |
| `seme` | `Vincolo`, `Frattura`, `Spira`, `Vestigio` |
| `sottofazione` | `Adattati`, `Spezzasigilli`, `Portanome`, `Segnaterra`, `SEKTAC`, `MEROZYN`, `EFFRIX`, `YLMOR` (i nomi del motore di gioco) |
| `autore` | lista di due voci: chi dirige e chi scrive (esempio `- Nome autore umano` / `- Modello`, `- Nome autore umano`…); nell'Excel `Nome autore umano, Modello` |
| `stato` | `Proposta` → `Revisione` → `Approvata` (oppure `Scartata`) |
| `can`, `categoria: Carta`, `definizione` | sempre: ogni carta è anche un elemento di canone. `definizione` è una o due frasi su chi o che cosa è il soggetto e che cosa fa; compare nelle tabelle delle carte e nel registro del canone |
| `trama` | le trame emergenti a cui la carta aggiunge un frammento (vedi [[🧵 Trame]]); vuoto se nessuna |
| `gradino` | per ogni trama, il gradino a cui la carta la porta: `<trama>-<numero>` (`aracno-2`). I gradini e le domande di ogni trama sono in [[🪜 Scale delle trame]] |
| `temi_abilita` | 2-3 parole tematiche per chi progetterà l'abilità (`eredità`, `isolamento`, `previsione`…); riusa quelle già presenti quando calzano, così emergono famiglie di carte |
| `aliases` | designazione o soprannome (`SEK-88`, `Inquisitore`); mai il titolo intero |
| `tags` | senza `#`: `fazione/…`, `rango/…`, `seme/…`, `sottofazione/…` in minuscolo, `stato/proposta`, `tipo/carta` |

## Corpo della carta

Sei callout, in quest'ordine: **Testo di colore** · **Lore interna** · **Rivela / Nasconde** · **Nuovi elementi di canone (PROPOSTA)** · **Descrizione dell'illustrazione** · **Intento per l'abilità**. Un settimo, facoltativo, subito dopo i nuovi elementi: **Glossario** (`> [!note] Glossario`), con una riga `- **Voce** — definizione` per ogni formula o dettaglio proprio della carta. Ogni riga di un callout inizia con `>`; una riga vuota tra un callout e l'altro.

| Sezione | Limite |
|---|---|
| Testo di colore | 1-3 frasi, max 200 caratteri + attribuzione in-world; una voce sola |
| Lore interna | 120-250 parole di solo contenuto: che cosa è vero, che cosa succede, i collegamenti. Almeno un `[[link]]` a una carta o a un elemento esistente. Niente «Ciò che non dice…» né note sulla voce: le domande aperte vanno in Rivela / Nasconde |
| Versione condensata | 60-90 parole, chiusa dalla frase di stile; nessuna parola che chieda testo nell'immagine |
| Intento per l'abilità | **Deve far sentire:** l'esperienza che l'abilità dovrebbe dare, in una frase · **Temi:** 2-3 parole tematiche (le stesse in `temi_abilita`). Niente numeri, niente regole, niente termini di gioco: è un indirizzo per chi progetterà l'abilità, non una meccanica |

## Nomi per rango

| Ranghi | Soggetto | Titolo |
|---|---|---|
| 2-6 Truppa | squadre, reclute, mezzi, sciami, luoghi, oggetti | descrittivo; ammessi piccoli nomi di colore e, per le AI, sigla d'unità + soprannome umano |
| 7-10 Specialisti | individui riconoscibili per ciò che fanno | Resistenza: soprannome d'uso · AI: sigla + numero, soprannome umano facoltativo |
| J, Q, K Luogotenenti | figure con un seguito | nome proprio (per le AI il J è il primo con un nome dalla corrente) |
| A Personaggi | figura simbolo della sottofazione | nome proprio; nessun Asso AI è sovrano o la Primaria |

Non scrivere il rango nel testo («di rango alto»).

## Elemento di canone

**Quando serve un CAN.** Un elemento merita un CAN solo se un'altra carta potrebbe incontrarlo: andarci, parlarne, usarlo, subirlo.

1. **Zero elementi nuovi è un risultato valido.** Una carta che riusa ciò che esiste è la migliore per il registro.
2. **Al massimo due elementi nuovi per carta**, oltre alla carta stessa.
3. **Prova dell'altra carta**: prima di aprire un CAN, chiediti quale altra carta, esistente o plausibile, potrebbe citarlo. Se non ti viene in mente, è una voce di glossario.
4. **Mai un CAN per una frase.** Le formule («Concedici l'errore») stanno nel glossario della pratica o della carta a cui appartengono.
5. **Prima riusa**: collega almeno un elemento esistente che non sia nato da questa carta, meglio se poco usato (`controlla_canone.py` elenca gli orfani).

**La nota.** `Lore/Canone/<Nome>.md`, con `can`, `categoria`, `definizione` (una o due frasi), `fonte: "[[Titolo carta]]"`, `stato: Proposta`, e negli `aliases` la forma con le virgolette se il testo la usa (`«Fuori registro»`). La definizione sta nella proprietà, così il registro la mostra.

| Categoria | Che cosa contiene |
|---|---|
| `Mondo` | fatti e condizioni del mondo (la pioggia nera, la visita d'imbarco) |
| `Luogo` | dove si può andare |
| `Oggetto` | ciò che passa di mano o che più persone conoscono: cose, registri, canali, sigilli |
| `Entità` | persone, gruppi e fenomeni con un nome |
| `Pratica` | riti, procedure e usanze condivise da una sottofazione o da una corrente |

**Il glossario.** Le voci che non meritano un CAN stanno in una sezione `## Glossario` in fondo alla nota di canone a cui appartengono, una riga ciascuna: `- **Voce** — definizione`. Si collegano con `[[Nota#Glossario|voce]]`. Se appartengono a una carta, stanno nel callout Glossario della carta e si collegano con `[[Titolo carta|voce]]`. Obsidian non risolve un link al solo alias: non scrivere `[[voce]]`.

**Fondere una voce.** Se un elemento con CAN si rivela una voce di glossario: sposta la definizione nel glossario della destinazione, annota il CAN in `can_assorbiti` della destinazione, riscrivi i link, elimina la nota. Il CAN non si riusa.

## Regole di scrittura

1. **Mostra, non spiegare.** Niente riassunti del mondo.
2. **Ogni testo ha una voce**: chi parla, a chi, quando.
3. **Rivela una cosa concreta.** Il bilancio delle domande è della trama, non della carta: apri una domanda nuova solo se la carta fa avanzare una trama esistente (vedi [[🪜 Scale delle trame]]).
4. **Narratori inaffidabili**: le contraddizioni tra voci sono indizi; la lore interna invece non contraddice il canone.
5. **Riferimenti incrociati** ad almeno una carta o un elemento esistente.
6. **Dettagli specifici**, non epica generica.
7. **Tono grave, asciutto, malinconico.**
8. **La linea incerta**: Il potere delle AI appare come magia a chi lo subisce. Una spiegazione esiste, ma nessuno nel mondo la possiede, e nessuna carta la dà né la smentisce: il soprannaturale non si conferma mai e non si esclude mai. Il potere viene dalle AI o dal contatto con loro (nessun mago umano) e ha sempre un costo.
9. **Il lessico sacro è umano**: le AI parlano per procedure; il sacro sta nei soprannomi umani e nelle crepe.
10. **La Primaria non si nomina mai.** **Il [[Telaio]] è principio e agente insieme, e non è un'infrastruttura**: la lore interna lo usa come ciò che decide che cosa esiste; le voci umane come qualcosa che vede e riscrive. La parola è umana: nessun registro o rapporto delle Potenze la usa. Niente «nodi del Telaio».
11. **67 e 61.** La soglia del quorum è di due terzi, 67 su cento: ciò che la raggiunge è valido. Quando serve un numero per qualcosa che non arriva a compiersi, usa 61, sei sotto la soglia. Poche volte, e mai spiegato nel testo di colore.

## Checklist

- [ ] Lo slot (fazione · rango · seme · sottofazione) corrisponde al foglio Excel
- [ ] Nome del file = titolo; ID secondo lo schema
- [ ] Il titolo rispetta i nomi per rango; il testo non nomina il rango
- [ ] Limiti di lunghezza rispettati
- [ ] Almeno un `[[link]]` nella lore interna; nessun link rotto
- [ ] Al massimo due elementi nuovi, ciascuno con la sua nota in `Lore/Canone/` e il CAN successivo; nessun CAN per frasi o dettagli della sola carta
- [ ] `python Strumenti/controlla_canone.py` non segnala link rotti
- [ ] Nessuna contraddizione con il canone, niente Primaria, niente Telaio-infrastruttura
- [ ] Il soprannaturale resta sulla linea: né confermato né escluso
- [ ] Riga aggiunta al foglio Excel

---

*Aggiornato: 2026-11-14*
