# PROPOSTA TECNICA: ARCHITETTURA SOFTWARE
Progetto: Gioco di Carte Collezionabili Digitale (QUORMA)

## Contesto e Coerenza con il Brief

Vincoli Tecnologici: Backend in Java + Spring Boot; database relazionale PostgreSQL; frontend integrato tramite API REST.

Vincoli di Gioco: Il motore di simulazione è completamente separato dall'interfaccia. Le partite sono deterministiche e non prevedono scelte da parte del giocatore durante lo svolgimento (Regola d'Oro 0).

Architettura Generale: Il sistema adotta un'approccio a Microservizi Disaccoppiati (orchestrati via Docker), progettato per garantire modularità, scalabilità e una netta separazione delle responsabilità.

## Architettura di Sistema e Stack Tecnologico
L'ecosistema si articola in tre componenti principali, ospitati in container Docker indipendenti:

Backend API (Spring Boot): Il cuore gestionale del gioco. Gestisce autenticazione, economia (crediti e pacchetti), collezioni, mazzi, scambi, storicizzazione delle sfide e il motore di aggregazione delle statistiche (Telemetria). È l'unico componente autorizzato a interagire con il database.

Game Engine Service (Java 25 + Spring Boot 4.1.x): un microservizio isolato in un container Docker, con server HTTP su porta configurata. Riceve in input i mazzi e un seed numerico, esegue la simulazione deterministica e restituisce un output JSON strutturato.

Infrastruttura: Docker Compose per l'orchestrazione locale.

## Struttura dei Dati (Database Schema)
Il database PostgreSQL è progettato per garantire l'integrità transazionale dei dati correnti, mantenendo al contempo l'immutabilità dello storico delle partite. Lo schema è suddiviso in domini logici:

Utenti ed Economia: Anagrafica con saldo crediti. Utilizza il locking ottimistico (gestione delle versioni) per prevenire race condition durante acquisti simultanei. È presente un registro immutabile delle transazioni (audit) per tracciare ogni movimento di crediti.

Catalogo e Versionamento: Le carte non vengono mai modificate in-place. Vengono create "fotografie" immutabili del pool (Snapshot) con hash crittografico. Questo assicura che una partita giocata oggi possa essere rigiocata identica in futuro, anche a seguito di bilanciamenti delle carte.

Collezioni: Ogni singola carta digitale posseduta da un utente è registrata come un'istanza unica (non un semplice contatore). Questo approccio è vitale per tracciare l'origine della carta e garantire che gli scambi non generino duplicazioni o perdite.

Mazzi: Intestazione del mazzo e associazione con le singole carte possedute, con vincoli strutturali che impediscono inserimenti non validi (es. più di una carta per rango/seme).

Scambi: Gestione delle proposte. L'accettazione esegue una transazione atomica (ACID) che scambia la proprietà delle carte in modo sicuro.

Partite e Risultati (Replay Architecture): Invece di salvare il log completo della partita (troppo pesante), il database salva la "Ricetta" della partita: snapshot JSON dei mazzi, seed numerico, versioni del pool utilizzate, versione del motore (engine version) e l'hash SHA-256 del risultato. Il replay viene ricalcolato al volo e verificato tramite hash.

Telemetria e Statistiche: Tabelle di aggregazione (globali e per utente) popolate asincronamente. Tracciano dati come frequenza di pescata, danni inflitti, uccisioni e win-rate, utili sia per i giocatori (deckbuilding) che per gli sviluppatori (bilanciamento).

## Il Motore di Simulazione (Game Engine Service)
Il motore è progettato per essere un componente matematico puro, testabile e assolutamente deterministico.

Input e Isolamento: Riceve due mazzi (JSON), un seed (numero intero) e la versione del regolamento. Non utilizza orologi di sistema, non fa uso di multithreading e non accede a risorse esterne.

Processo: Esegue un mescolamento deterministico (Fisher-Yates con PRNG). Simula il ciclo di gioco (Valutazione Mano di Poker, Dichiarazione, Combattimento, Rifornimento) risolvendo la pila degli effetti (LIFO) e le sostituzioni.

Output (Match Log): Genera un JSON (registro degli eventi) che descrive turno per turno ogni azione, danno e attivazione. Questo JSON è la fonte primaria sia per il rendering del replay da parte del frontend, sia per l'elaborazione delle statistiche nel backend.

## Controlli sul Mazzo e Flussi Operativi (API)
La logica applicativa risiede interamente nel backend, che agisce come unico punto di verità per l'integrità del gioco.

Validazione dei Mazzi: Il frontend fornisce solo un feedback visivo (UX). Il Backend esegue i controlli strutturali (32 carte totali, limiti di fazione 20-23 per Resistenza / 24-27 per AI, limiti di copia). Successivamente, invia il mazzo all'Engine Service per validare la coerenza logica delle abilità.

Operazioni API REST: Tutte le richieste passano per API sicure (JWT). Le operazioni critiche (acquisto pacchetti, accettazione scambi) sono protette da transazioni atomiche. Il flusso di sfida prevede: POST /challenges (blocca lo snapshot del mazzo), POST /challenges/{id}/accept (genera il seed, chiama l'Engine, salva la ricetta), e GET /matches/{id}/replay (restituisce il log ricostruito).

## Test Automatici e Qualità
La suite di test garantisce l'affidabilità del sistema e la ripetibilità del motore:

Test JUnit (Engine): Coprono gli effetti, i casi limite (pareggi, fine partita al turno 50) e la ripetibilità. Quest'ultima è assicurata da "Golden Files": a parità di seed e mazzi, l'output JSON deve essere identico byte per byte.

Test JUnit (Telemetria): Verificano che l'estrattore delle statistiche elabori correttamente i log senza errori di concorrenza.

Test di Integrazione: Verificano l'interazione tra Backend ed Engine Service e la stabilità delle transazioni sul database.

## Sintesi Progettuale (Slide per il Cliente)
Tecnologie: Java 25 + Spring Boot 4.1.x (Backend + Engine), PostgreSQL 16 (Database), Docker (Infrastruttura).

Responsabilità (Team Software): Sviluppo del database, API REST, Game Engine deterministico, sistema di telemetria e test JUnit. Il confine con il team Web è definito dal contratto Swagger.

Rischi e Mitigazioni:

Complessità del motore: MVP focalizzato su un sottoinsieme di keyword per il SAL, con architettura estensibile.

Determinismo/Replay: Utilizzo di hash crittografici (SHA-256) e test di ripetibilità.

Tempi ristretti: Focus sul "Vertical Slice" (flusso completo: acquisto → mazzo → sfida → replay).

Tempi (Roadmap verso il SAL del 23/11): Infrastruttura e Core Engine (Sett. 1); Parser effetti e Integrazione (Sett. 2); Sfide, Replay, Telemetria e Test E2E (Sett. 3).

Costi Stimati (Solo perimetro Software): Valore di mercato dello sviluppo ~20.000 € (costi dimezzati se il team è interno). Costi operativi AWS stimati in ~100 €/mese per i primi 5.000 utenti. Con 50.000 utenti, il costo stimato sale a ~250 €/mese.

## Documentazione e Deliverable
README.md: Istruzioni dettagliate per l'avvio dell'ambiente Docker e l'esecuzione dei test.

Swagger/OpenAPI: Documentazione delle API REST, punto di contatto formale per il team Web.

JSDoc: Documentazione del codice frontend (a cura del team Web).