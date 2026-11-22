# TODO – frontend Quorma

Cose rimaste da fare per allineare il sito al backend attuale (branch SWD) e completare le sezioni "in arrivo".
Aggiornato al 20 novembre 2026. Fonte del contratto: `/api-docs` del backend in esecuzione.

## Gioco online

- [X] **Stanze** (`POST /rooms`, `GET /rooms`, `GET /rooms/mine`, `POST /rooms/{code}/join`, `POST /rooms/{code}/cancel`)
  - aprire una stanza con due mazzi e la fazione della prima partita; la stanza scade dopo 10 minuti
  - elenco delle stanze aperte da altri, con "Entra" (gioca subito la serie)
  - la propria stanza aperta: niente notifiche push, rileggere `GET /rooms/mine` a intervalli finché è `OPEN`
- [X] **Sfide per nickname** (`POST /challenges`, `GET /challenges`, `GET /challenges/{id}`, `accept`, `decline`, `cancel`)
  - sfidare un giocatore per nome, anche offline; la sfida dura 48 ore
  - sfide ricevute: accetta (con i miei due mazzi) o rifiuta; sfide inviate: ritira
- [X] **Pagina Sfide**: la voce di menu esiste ma è "in arrivo" (`data/navItems.ts`); ci vanno stanze e sfide
- [X] **Modalità della pagina Gioca** (`data/play.ts`): aggiungere Stanza e Sfida come modalità vere;
- [X] **"Braci" sulla voce Sfide** quando ci sono sfide ricevute in attesa (`NavItemData.alert`)
- [ ] Messaggi d'errore nuovi da verificare a schermo: `challenge.*`, `room.*`, `matchmaking.no_opponent`,
      `engine.unavailable` (503, si può riprovare)

## Profilo e storico

- [X] **Replay animato** di una partita (`GET /matches/{seriesId}/games/{n}/replay`): oggi il bottone è "in arrivo";
      il log turno per turno è già letto in `lib/series.ts` (`MatchLog`)
- [ ] **Statistiche del giocatore**: il backend le prevede ma non le ha ancora. Quando arrivano, cambiare solo
      `lib/stats.ts`. Oggi le mani si contano solo sulle partite di cui il browser conosce il log
- [ ] **Nome e immagine del profilo**: salvati solo nel browser (`lib/profileStore.ts`), manca l'API
- [X] Etichetta "Nuova" nello storico per le serie ancora non lette (caso raro: se `POST …/seen` non è riuscito)

## Carte e collezione

- [X] **Pagina Collezione** con `GET /collection` (oggi la voce di menu è "in arrivo")
- [ ] **Illustrazioni delle carte**: file in `public/images/carte/arte/<codice carta>.webp` e codice in `CARD_ART`
      (`src/data/cardArt.ts`); circa 150 carte nel set base, da generare a lotti di 4 con ComfyUI
- [ ] **Cornici per magie ed equipaggiamenti**: oggi solo le creature hanno la cornice per Tier
- [X] **Testo delle abilità** sulle carte: testi ufficiali per keyword ed Evoluzioni (`lib/abilities.ts`), frasi composte dai dati del motore
      per magie, equipaggiamenti ed effetti liberi delle creature (`lib/abilityEffects.ts`). Se il team aggiunge testi ufficiali, vanno usati quelli
- [X] **Duplica mazzo** (`GET /decks/{id}` + `POST /decks`): oggi il bottone è "in arrivo"

## Negozio ed economia

- [X] **Negozio**: elenco bustine (`GET /packs`) e apertura (`POST /packs/open` con header `Idempotency-Key`)
  - errori da gestire: `pack.insufficient_credits` (402), `pack.not_on_sale` (409), `pack.no_pool` (404)
- [ ] **Saldo delle valute** nella barra in alto: manca l'endpoint nel backend (da chiedere al team Software)

## Non ancora possibili (manca il backend)

- [ ] Scambi: la tabella esiste nel database ma non ci sono endpoint; non costruire schermate che li richiedono
- [ ] Classifica (modalità Classificata) e partite contro il computer (Allenamento)
- [ ] News della home e invio del modulo Contatti

## Da controllare a mano

- [X] Mobile, pagina Mazzi: la fila dei mazzi scorre di lato e la pagina non si sposta più (`overflow-x: clip` su `.app`);
      se qualcosa risulta tagliato sul bordo destro in un'altra pagina, era l'elemento che sporgeva
- [X] Editor: pressione lunga sulla carta (telefono) e lente per la carta ingrandita
- [X] Finestra "Mentre eri via" con un account che ha risultati non letti (es. `alice` o `tester2`)
- [X] Pagina Gioca senza scroll a 1366×768 e 1920×1080; posizione dei testi nelle cornici delle carte


<!-- ## da fare importanti
- [X] carte collezione, non hanno gli effetti davanti bensi la lore.
- [X] design collezione diverso dal design originale (barra laterale).
- [X] sistemare pagina mazzi quando si crea un mazzo, pagina troppo piccola
- [X] generare 6 immagini per il profilo.
- [X] aggiungere notifica a collezione siccome si hanno dei pacchetti da aprire.
- [X] migliorare il design dei pacchetti, aggiungendo anche animazioni per esaltarne alcuni
- [ ] migliorare la pagina gioca, design bello ma molto ai, scollegato un po dal design del sito (eventualmente cambiare leggermente design sito)
- [X] migliorare le animazioni della battaglia -->