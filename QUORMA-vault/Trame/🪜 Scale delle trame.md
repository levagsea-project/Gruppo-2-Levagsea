---
tags:
  - hub
  - trame
  - riservato-autori
stato: Bozza
---

# 🪜 Scale delle trame

> [!warning] Riservato agli autori
> Queste note contengono le **risposte di lavoro** alle trame. Non vanno esportate e non vanno incollate nel prompt delle carte: al prompt vanno solo i vincoli in fondo a ogni scala.

[[🧵 Trame]] dice quali trame esistono e quali carte le costruiscono. Le scale dicono dove va ciascuna trama, a che gradino è arrivata e che cosa manca.

## Le scale

| Scala | Gradino raggiunto | Nota |
|---|---|---|
| [[Scala ARACNO]] | 3 | condivide il [[Fatto del settore 4]] |
| [[Scala Tecnico]] | 2 | condivide il [[Fatto del settore 4]] |
| [[Scala Undici]] | 3 | condivide il [[Fatto del settore 4]] |
| [[Scala Ritorno]] | 3 | |
| [[Scala Spartizione]] | 3 | è la trama della rivalità tra Potenze |
| [[Scala Eresie]] | 1 | otto agganci, nessun gradino successivo |
| [[Scala Pioggia nera]] | 3 | |
| [[Scala Comunità controllate]] | 3 | |
| [[Scala Pellegrinaggio]] | 3 | |
| [[Scala Primo Ingegnere]] | 3 | si ferma qui per scelta |

Nessuna trama ha ancora raggiunto il gradino 4.

## Che cosa vede il giocatore

Solo **titolo, testo di colore, illustrazione e abilità**. La lore interna non è esposta. Una trama esiste per lui soltanto se si ricostruisce da quei quattro elementi.

## I cinque gradini

| Gradino | Che cosa fa | Dove sta |
|---|---|---|
| 1 · Aggancio | Mostra che la cosa esiste e dà un'**ancora** (un nome, un numero, un luogo) | ranghi 2-6 |
| 2 · Complicazione | Lo stesso fatto visto da un'altra voce, con un dettaglio in più | ranghi 2-10 |
| 3 · Contraddizione | Due testi di colore che non possono essere veri insieme | ranghi 7-10 |
| 4 · Risposta parziale | Chiude **una** domanda aperta dai gradini 1-3 | J, Q, K |
| 5 · Risposta | Chiude la trama, o la consegna a un'espansione | Assi, espansione |

## Il campo `gradino`

Ogni carta in una trama dichiara nel frontmatter a che gradino la porta: `gradino: [aracno-2, tecnico-1]`, un valore per ogni voce di `trama`. Quando una carta cambia gradino, si aggiorna anche la tabella della scala.

## Gli stati delle domande

Ogni scala elenca le sue domande, ciascuna con uno stato.

| Stato | Significato | Regola per le carte nuove |
|---|---|---|
| **Aperta** | Nessuno ha deciso | Una carta può proporre qualcosa |
| **Orientata** | Esiste una risposta di lavoro | Non contraddirla; non serve rivelarla |
| **Chiusa** | Una carta l'ha mostrata | La scala cita la carta; è canone |
| **Vuota** | Non avrà mai risposta, per scelta | Nessuna carta può rispondere |

Due regole tengono in piedi il sistema:

1. **Vince la carta.** Se una carta approvata contraddice una risposta «Orientata», si corregge la scala.
2. **Le risposte restano fuori dal prompt.** Un modello che conosce il segreto tende a rivelarlo. Al prompt vanno solo i vincoli («non dire chi è a bordo»), mai la risposta.

Quando una carta chiude una domanda: stato **Chiusa**, colonna «Chiusa da» con il link alla carta.

## Regole di scrittura per le trame

1. **Il bilancio è della trama, non della carta.** Una carta può aprire una domanda nuova solo se ne fa avanzare o ne chiude una esistente.
2. **Le ancore stanno nel testo di colore o nell'attribuzione**, non solo nella lore interna. Un'ancora che compare in una sola carta, per il giocatore non è un'ancora.
3. **La contraddizione deve essere visibile**: stessa ancora, due voci, due versioni.
4. **L'illustrazione porta indizi** (una scritta, una mano, una luce): si decidono nella scala.
5. **Le risposte stanno in alto.** I ranghi comuni agganciano; le figure rispondono.
6. **Nessuna trama nuova** finché una di queste non ha raggiunto il gradino 4.
7. **Nessuna carta nuova nel settore 4** che non salga almeno al gradino 4.

## Il quadro d'insieme

Tre idee di lavoro tengono insieme quasi tutte le scale. Sono **Orientate**: valgono finché una carta non le smentisce.

1. **Le Potenze si contendono le voci.** Il peso di una Potenza nel quorum dipende da quante identità contate ha sotto di sé; la soglia è 67 su cento. I superstiti sono stati spartiti come voci. Per questo registri, anagrafi ed elenchi contano più delle armi.
2. **Ogni Potenza sbaglia a modo suo.** Le macchine sulla Terra discendono da Potenze diverse, e ogni discendenza ha il suo errore. Le quattro correnti sono mestieri, e attraversano le discendenze. Le eresie sono la firma delle Potenze rivali.
3. **Ciò che non è stato contato è l'unica cosa che l'ordine non può toccare**, e l'unica che lo inceppa.

## Che cosa serve dai 44 slot rimasti

| Priorità | Che cosa | Dove | Scala |
|---|---|---|---|
| 1 | La rivalità tra Potenze, nominata | figura AI | [[Scala Spartizione]] |
| 2 | Chi riconosce una macchina dal suo errore | Resistenza, rango medio | [[Scala Eresie]] |
| 3 | Il proprietario di ARACNO | figura EFFRIX o SEKTAC | [[Scala ARACNO]] |
| 4 | La parola del Coro in una richiesta di rientro | figura MEROZYN | [[Scala Ritorno]] |
| 5 | Una voce che resiste dentro Riva Lenta | Resistenza | [[Scala Comunità controllate]] |
| 6 | Chi è salito sulla torre | Resistenza | [[Scala Pioggia nera]] |
| 7 | Il sito di sutura visto da vicino | Adattati | [[Scala Pellegrinaggio]] |

## Carte senza trama

Nove carte non appartengono a nessuna trama. Prima di aprirne di nuove, vedere se entrano in una esistente.

| Carta | Proposta |
|---|---|
| [[Presidio dei Non Registrati]] | `tecnico` o `comunita-controllate`: l'ordine dei registri rifatto dagli umani |
| [[SEK-40 «Liturgia»]] | `eresie`: le versioni che si contraddicono sono versioni di discendenze diverse |
| [[Sciame di Malta]] | `pellegrinaggio`: materia compatibile e corpi |
| [[YLM-74 «Segno»]] | `ritorno`, oppure resta isolata |
| [[Sentinella dell'Ultimo Cippo]] | `pellegrinaggio`, oppure resta isolata |
| [[«Selce», Incisora del Relè]] | `spartizione`: la lampada che si accende può essere la portante dall'alto |
| [[Squadra dei Cippi Neri]] | resta isolata, ma «YLMOR non tocca i pali» chiede una risposta |
| [[Reliquiario del Sigillo Rotto]] | resta isolata |
| [[Decifratrice «Carta Bianca»]] | resta isolata |

---

*Bozza: 2026-11-15*
