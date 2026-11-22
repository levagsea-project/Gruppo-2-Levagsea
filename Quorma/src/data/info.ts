// Testi delle pagine "Informazioni": Chi siamo, FAQ, Glossario, Contatti, Privacy, Cookie, Termini.
// Tutti i testi stanno qui: per correggerli non serve toccare i componenti.
//
// Prima di una pubblicazione vera mancano: titolare del trattamento, un canale di contatto e i nomi reali della squadra.
// Finché non esistono, i testi lo dicono invece di mostrare dati inventati.
import type { Page } from './navItems'

// ---------- Formato dei testi ----------
// Un paragrafo può finire con un collegamento a un'altra pagina del sito.
export type Block = { p: string; link?: { label: string; page: Page } } | { ul: string[] }
export type Section = { id: string; title: string; blocks: Block[] }

// Id dell'elemento di una sezione nella pagina: l'indice laterale ("In questa pagina") lo usa per scorrere fin lì.
export const sectionAnchor = (id: string) => `info-${id}`

// Chi offre il sito: finché non c'è un titolare indicato, resta scritto che cos'è il progetto.
const PROJECT = 'QUORMA è un progetto di gruppo realizzato durante un corso ITS'
const NO_OWNER = 'Il titolare del trattamento non è stato ancora indicato'
const NO_CHANNEL = 'Il sito non ha un canale attivo per ricevere richieste'

// ======================================================================
// CHI SIAMO
// ======================================================================

// La squadra. NOMI PROVVISORI E GENERICI: sostituirli con quelli reali prima della pubblicazione.
export const team: { name: string; role: string }[] = [
  { name: 'Marco Rossi', role: 'Web developer' },
  { name: 'Giulia Bianchi', role: 'Web developer' },
  { name: 'Luca Ferrari', role: 'Web developer' },
  { name: 'Sara Romano', role: 'Software developer' },
  { name: 'Andrea Colombo', role: 'Software developer' },
  { name: 'Elena Ricci', role: 'Software developer' },
  { name: 'Davide Marino', role: 'Software developer' },
  { name: 'Chiara Greco', role: 'Digital marketing' },
  { name: 'Matteo Conti', role: 'Digital marketing' },
]

export const aboutSections: Section[] = [
  {
    id: 'studio',
    title: 'Lo studio',
    blocks: [
      {
        p: 'LEVAGSEA è lo studio che ha creato QUORMA, dal mondo narrativo fino all’ultima riga del motore di gioco. Lavoriamo in tre gruppi affiancati: i web developer curano il sito e l’interfaccia che usi per giocare, i software developer i dati delle carte, il motore che risolve le partite e i server che custodiscono account, collezioni e mazzi, e il digital marketing racconta il gioco e lo fa conoscere ai giocatori.',
      },
      {
        p: 'Ogni scelta passa da due documenti condivisi: il regolamento, che stabilisce come si gioca, e il dossier narrativo, che racconta il mondo, le fazioni e le loro dottrine. Quando i due non coincidono ci fermiamo e decidiamo insieme, invece di correggere uno dei due di nascosto.',
      },
    ],
  },
  {
    id: 'gioco',
    title: 'Il gioco',
    blocks: [
      {
        p: 'QUORMA è un gioco di carte collezionabili digitale ambientato in un futuro post-apocalittico: intelligenze artificiali che si proclamano perfette hanno quasi annientato l’umanità, ma il loro ordine si è incrinato. La Resistenza Umana e le Potenze Artificiali si contendono quello che resta, divise in otto sottofazioni e quattro semi.',
      },
      {
        p: 'Le partite sono completamente deterministiche: durante lo scontro nessuno prende decisioni, e a parità di mazzi e di seme casuale la partita va sempre allo stesso modo. La strategia sta tutta nel mazzo da 32 carte: ogni turno le creature nelle cinque corsie formano una mano di poker, che decide chi attacca e quanto danno trabocca sull’avversario.',
      },
    ],
  },
  {
    id: 'squadra',
    title: 'La squadra',
    blocks: [
      {
        p: 'QUORMA è il lavoro di una squadra piccola, in cui ognuno segue una parte del gioco dall’idea alla consegna.',
      },
    ],
  },
  {
    id: 'valori',
    title: 'Cosa ci guida',
    blocks: [
      { p: 'Quattro principi orientano ogni schermata e ogni regola che scriviamo:' },
      {
        ul: [
          'Il mazzo è la partita: tutto ciò che aiuta a capire, costruire e confrontare i mazzi viene prima del resto.',
          'Le regole si capiscono: regolamento, numeri e vantaggi devono essere sempre raggiungibili e leggibili.',
          'Niente dati finti: mostriamo solo ciò che esiste davvero.',
          'Desktop e telefono alla pari: ogni schermata deve funzionare bene in entrambi.',
        ],
      },
      {
        p: 'I numeri tra parentesi quadre che trovi nel regolamento sono valori di prova: li fissiamo con il playtesting, non a tavolino.',
      },
    ],
  },
  {
    id: 'lavora',
    title: 'Lavora con noi',
    blocks: [
      {
        p: 'QUORMA nasce come lavoro di gruppo di un corso ITS: al momento non cerchiamo collaboratori e non abbiamo un canale per ricevere candidature.',
      },
    ],
  },
]

// ======================================================================
// FAQ
// ======================================================================

export type FaqItem = { topic: string; q: string; a: string; link?: { label: string; page: Page } }

export const faqTopics = ['Account', 'Partite', 'Mazzi', 'Negozio']

export const faq: FaqItem[] = [
  // Account
  {
    topic: 'Account',
    q: 'Come creo un account?',
    a: 'Dalla pagina iniziale scegli Registrati e inserisci il nome del giocatore (da 3 a 32 caratteri: lettere, cifre e i simboli _ . -), un indirizzo email e una password da 8 a 72 caratteri. Appena l’account è creato entri direttamente nel gioco.',
  },
  {
    topic: 'Account',
    q: 'Cosa fa “Ricordami su questo dispositivo”?',
    a: 'Se la spunti, resti collegato anche dopo aver chiuso il browser, fino alla scadenza della sessione (al massimo 24 ore). Se non la spunti, l’accesso termina quando chiudi la scheda. Su un computer condiviso è meglio lasciarla vuota.',
  },
  {
    topic: 'Account',
    q: 'Ho dimenticato la password: cosa faccio?',
    a: 'Il sito non ha una funzione per recuperare la password e nemmeno un canale di assistenza attivo: senza password non puoi rientrare nell’account. Conservala in un posto sicuro.',
  },
  // Partite
  {
    topic: 'Partite',
    q: 'Devo prendere decisioni durante la partita?',
    a: 'No. In QUORMA la partita si svolge da sola ed è completamente deterministica: a parità di mazzi e di seme casuale va sempre allo stesso modo. Le tue scelte contano prima, quando costruisci il mazzo.',
  },
  {
    topic: 'Partite',
    q: 'Come si vince una partita?',
    a: 'Vince chi porta a 0 i punti vita dell’avversario oppure lo lascia senza creature in gioco. Se dopo 50 turni nessuno ha vinto, vince il giocatore con più vita; in caso di parità conta la mano di poker migliore dell’ultimo turno.',
  },
  {
    topic: 'Partite',
    q: 'A cosa servono le mani di poker?',
    a: 'A ogni turno le creature nelle tue cinque corsie formano una mano di poker con i loro ranghi e semi. La mano decide quali creature attaccano e il moltiplicatore del danno che trabocca sul giocatore avversario quando una creatura viene distrutta: più alta è la mano, più forte è il colpo.',
    link: { label: 'Leggi le regole', page: 'regole' },
  },
  // Mazzi
  {
    topic: 'Mazzi',
    q: 'Quante carte servono per un mazzo?',
    a: 'Esattamente 32, tutte della stessa fazione. La Resistenza Umana usa da 20 a 23 creature e da 9 a 12 magie ed equipaggiamenti; le Potenze Artificiali da 24 a 27 creature e da 5 a 8 magie ed equipaggiamenti. Ogni combinazione di rango e seme può comparire una sola volta, e ogni magia o equipaggiamento in una sola copia.',
  },
  {
    topic: 'Mazzi',
    q: 'Cos’è il mazzo di difesa?',
    a: 'Per giocare ti serve un mazzo di difesa valido per ciascuna delle due fazioni: puoi sceglierlo tra i tuoi mazzi. Se il pool di carte cambia e un mazzo diventa obsoleto, va modificato prima di poterlo usare di nuovo; la Home ti avvisa quando manca qualcosa.',
    link: { label: 'Vai ai mazzi', page: 'mazzi' },
  },
  // Negozio
  {
    topic: 'Negozio',
    q: 'Cosa sono Valuta 1 e Valuta 2?',
    a: 'Sono le due valute del gioco: servono per ottenere nuove carte. Si guadagnano giocando e non hanno alcun valore al di fuori di QUORMA.',
  },
  {
    topic: 'Negozio',
    q: 'Cosa contiene una busta?',
    a: 'Una busta contiene carte casuali di una fazione a tua scelta: creature di diverse rarità (Comune, Non comune, Rara, Epica) e alcune magie o equipaggiamenti. I tipi di busta si differenziano per prezzo e per quante carte rare contengono; prezzi e contenuti sono ancora in fase di bilanciamento.',
  },
  {
    topic: 'Negozio',
    q: 'Posso acquistare con denaro reale?',
    a: 'No. Le valute e le buste si ottengono solo giocando: al momento QUORMA non accetta pagamenti in denaro.',
  },
]

// ======================================================================
// GLOSSARIO
// ======================================================================

// rules = sezione del regolamento dove il termine è spiegato (vuoto se non c'è)
export const glossary: { term: string; rules?: string; text: string }[] = [
  { term: 'Asso', rules: 'Carte', text: 'Il rango più alto, di Tier 4. Nelle scale conta sia alto sia basso: A-2-3-4-5 è la scala più bassa, 10-J-Q-K-A la più alta.' },
  { term: 'Attaccante', rules: 'Fase di Valutazione', text: 'Una creatura scelta dalla mano di poker per attaccare in quel turno. Le creature non scelte restano in corsia ma non attaccano.' },
  { term: 'Busta', text: 'Una confezione di carte casuali di una fazione, con creature di diverse rarità e alcune magie o equipaggiamenti. Si ottiene con le valute del gioco.' },
  { term: 'Carta alta', rules: 'Fase di Valutazione', text: 'La mano più debole: attacca solo la creatura di rango più alto e il danno che trabocca non viene moltiplicato.' },
  { term: 'Corsia', rules: 'Setup', text: 'Ognuno dei cinque spazi in cui un giocatore schiera le creature, numerati da 1 a 5 da sinistra a destra. Una creatura attacca di norma la corsia opposta.' },
  { term: 'DEF base', rules: 'Fase di Valutazione', text: 'La Difesa stampata sulla carta. I danni la riducono per il resto della partita; le cure possono riportarla al massimo fino alla DEF base.' },
  { term: 'Evoluzione', rules: 'Carte', text: 'Un effetto che dà a una creatura, di qualunque seme, un potere in più quando si verifica la sua condizione di innesco.' },
  { term: 'Fazione', rules: 'Fazioni', text: 'Resistenza Umana o Potenze Artificiali. Ogni fazione ha il proprio pool di carte e un mazzo usa le carte di una sola fazione.' },
  { term: 'Five of a kind', rules: 'Fase di Valutazione', text: 'Cinque creature dello stesso rango: la mano migliore. È possibile solo con effetti che fanno contare una creatura come un altro rango.' },
  { term: 'Mano di poker', rules: 'Fase di Valutazione', text: 'La combinazione che le tue creature in gioco formano a ogni turno con ranghi e semi. Decide gli attaccanti e il moltiplicatore del danno che trabocca.' },
  { term: 'Pila', rules: 'Pila', text: 'L’elenco degli effetti in attesa di risolversi. L’ultimo effetto aggiunto è il primo a risolversi.' },
  { term: 'Rango', rules: 'Carte', text: 'Il valore di una creatura, dall’Asso al King. Determina il Tier della carta e le mani di poker che può formare.' },
  { term: 'Rifornimento', rules: 'Fase di Rifornimento', text: 'La fase in cui le creature distrutte vanno negli scarti e le corsie vuote si riempiono pescando dal mazzo. Le magie e gli equipaggiamenti pescati si applicano subito.' },
  { term: 'Seme', rules: 'Fazioni', text: 'Vincolo, Frattura, Spira o Vestigio. I semi sono condivisi dalle due fazioni e con tre o più creature dello stesso seme si attiva una sinergia.' },
  { term: 'Sinergia', rules: 'Fase di Valutazione', text: 'Un bonus che si attiva quando hai 3, 4 o 5 creature dello stesso seme in gioco. I livelli si sommano: con 5 creature valgono anche quelli da 3 e da 4.' },
  { term: 'Sottofazione', rules: 'Fazioni', text: 'Uno dei quattro gruppi in cui è divisa ogni fazione. Ogni sottofazione è forte contro un’altra e debole contro un’altra ancora.' },
  { term: 'Spill', rules: 'Fase di Combattimento', text: 'Il danno da traboccamento: quando un attacco distrugge una creatura, il danno in eccesso colpisce il giocatore avversario, moltiplicato per il valore della mano.' },
  { term: 'Tier', rules: 'Carte', text: 'La rarità di una creatura, data dal rango: Tier 1 dal 2 al 6, Tier 2 dal 7 al 10, Tier 3 Jack, Queen e King, Tier 4 l’Asso.' },
  { term: 'Vantaggio', rules: 'Fase di Combattimento', text: 'Il modificatore tra sottofazioni: l’attacco vale ×1.1 contro una sottofazione debole e ×0.9 contro una forte.' },
]

// Le 21 lettere dell'alfabeto italiano, per la griglia del glossario.
export const glossaryLetters = 'ABCDEFGHILMNOPQRSTUVZ'.split('')

// ======================================================================
// CONTATTI
// ======================================================================

export const contactTabs = ['Account', 'Pagamenti', 'Segnala un problema', 'Altro']

// ======================================================================
// PAGINE LEGALI (Privacy, Cookie, Termini)
// ======================================================================

export const LAST_UPDATE = 'ULTIMO AGGIORNAMENTO · 22 NOVEMBRE 2026'

const privacy: Section[] = [
  {
    id: 'intro',
    title: 'Introduzione',
    blocks: [
      { p: `Questa informativa spiega quali dati personali tratta QUORMA, perché li tratta e quali diritti hai. ${PROJECT}. ${NO_OWNER}.` },
      { p: 'Trattiamo solo i dati che servono a farti giocare: non vendiamo i tuoi dati e non li usiamo per pubblicità profilata.' },
    ],
  },
  {
    id: 'dati',
    title: 'Dati che raccogliamo',
    blocks: [
      { p: 'Quando crei un account e giochi raccogliamo:' },
      {
        ul: [
          'Nome del giocatore e indirizzo email, che inserisci alla registrazione.',
          'La password, che conserviamo solo in forma cifrata (hash): nessuno, nemmeno noi, può leggerla.',
          'I dati di gioco legati al tuo account: collezione di carte, mazzi, buste aperte, movimenti delle valute e risultati delle partite.',
        ],
      },
      { p: 'Non raccogliamo dati di pagamento, perché al momento il gioco non accetta pagamenti in denaro.' },
    ],
  },
  {
    id: 'uso',
    title: 'Come usiamo i dati',
    blocks: [
      { p: 'Usiamo i tuoi dati per creare e gestire l’account, farti accedere, salvare collezione e mazzi e far funzionare le partite. La base giuridica è l’esecuzione del contratto di servizio che accetti registrandoti.' },
      { p: 'Possiamo usare dati di gioco aggregati e non riconducibili a te per bilanciare le carte e migliorare il gioco. I tuoi dati sono conservati nel database del servizio.' },
    ],
  },
  {
    id: 'diritti',
    title: 'I tuoi diritti',
    blocks: [
      { p: 'In base al Regolamento europeo sulla protezione dei dati (GDPR) hai il diritto di:' },
      {
        ul: [
          'accedere ai tuoi dati e riceverne una copia;',
          'correggere i dati inesatti;',
          'cancellare il tuo account e i dati collegati;',
          'limitare il trattamento o opporti ad esso;',
          'ricevere i tuoi dati in un formato leggibile da un computer (portabilità).',
        ],
      },
      { p: 'Hai anche il diritto di presentare reclamo al Garante per la protezione dei dati personali (garanteprivacy.it).' },
    ],
  },
  {
    id: 'durata',
    title: 'Conservazione e sicurezza',
    blocks: [
      { p: 'Conserviamo i dati dell’account finché l’account resta attivo. Il sito non ha una funzione per cancellare l’account e i tempi di cancellazione non sono stati ancora definiti.' },
      { p: 'Le password non vengono mai salvate in chiaro. Per usare QUORMA devi avere almeno 14 anni, l’età minima prevista in Italia per acconsentire da soli al trattamento dei dati nei servizi online.' },
    ],
  },
  {
    id: 'contatti',
    title: 'Contatti',
    blocks: [
      { p: `${NO_CHANNEL} sulla privacy o per esercitare questi diritti. ${NO_OWNER}.` },
    ],
  },
]

const cookie: Section[] = [
  {
    id: 'intro',
    title: 'Introduzione',
    blocks: [
      { p: `Questa informativa spiega quali strumenti di memorizzazione usa il sito di QUORMA sul tuo dispositivo. ${PROJECT}. ${NO_OWNER}.` },
      { p: 'In breve: il sito non usa cookie di profilazione né cookie pubblicitari, e non ti mostra un banner perché non ce n’è bisogno.' },
    ],
  },
  {
    id: 'archivio',
    title: 'Cookie e archiviazione locale',
    blocks: [
      { p: 'Il sito non imposta cookie. Usa l’archiviazione del browser (localStorage o sessionStorage), dove salva solo:' },
      {
        ul: [
          'il codice di accesso della tua sessione, che scade al massimo dopo 24 ore, e il nome del giocatore;',
          'il nome e l’immagine scelti in Modifica profilo;',
          'le impostazioni, la velocità della battaglia e le ultime scelte della pagina Gioca;',
          'i dettagli delle partite giocate, le buste da rivelare e l’ultimo saldo visto.',
        ],
      },
      { p: 'Se spunti “Ricordami su questo dispositivo” il codice di accesso resta salvato anche dopo la chiusura del browser; altrimenti viene cancellato quando chiudi la scheda. Quando esci dal gioco lo cancelliamo subito. Sono dati tecnici e funzionali, usati solo per far funzionare il sito.' },
    ],
  },
  {
    id: 'terze-parti',
    title: 'Servizi di terze parti',
    blocks: [
      { p: 'Per mostrare i caratteri tipografici il sito li scarica da Google Fonts. Quando una pagina si apre, il tuo browser contatta i server di Google, che ricevono il tuo indirizzo IP. Google Fonts non imposta cookie; per i dettagli puoi consultare l’informativa privacy di Google.' },
      { p: 'Non usiamo strumenti di analisi del traffico, pulsanti dei social network o pubblicità di terze parti.' },
    ],
  },
  {
    id: 'modifiche',
    title: 'Modifiche a questa informativa',
    blocks: [
      { p: 'Se in futuro introdurremo cookie o altri strumenti che richiedono il tuo consenso, aggiorneremo questa pagina e te lo chiederemo prima di usarli. La data dell’ultimo aggiornamento è indicata in alto.' },
    ],
  },
  {
    id: 'gestione',
    title: 'Gestione dei dati salvati',
    blocks: [
      { p: 'Uscendo dal gioco cancelli il codice di accesso. Tutti gli altri dati salvati si cancellano dalle impostazioni del browser, alla voce dedicata ai dati dei siti; dopo dovrai accedere di nuovo.' },
    ],
  },
]

const termini: Section[] = [
  {
    id: 'intro',
    title: 'Introduzione',
    blocks: [
      { p: `Questi termini regolano l’uso di QUORMA, il gioco di carte collezionabili digitale. ${PROJECT}. Creando un account e usando il gioco li accetti; se non sei d’accordo, non usare il servizio.` },
    ],
  },
  {
    id: 'account',
    title: 'Account',
    blocks: [
      {
        ul: [
          'Per giocare devi avere almeno 14 anni e creare un account con dati veri.',
          'L’account è personale: non puoi cederlo, venderlo o condividerlo.',
          'Sei responsabile della riservatezza della tua password e di ciò che avviene con il tuo account.',
          'Il nome del giocatore non deve essere offensivo, ingannevole o violare diritti di altri: possiamo chiederti di cambiarlo.',
        ],
      },
    ],
  },
  {
    id: 'uso',
    title: 'Uso del servizio',
    blocks: [
      { p: 'Ti chiediamo di giocare in modo corretto. In particolare non è permesso:' },
      {
        ul: [
          'usare programmi, script o automatismi per giocare, accumulare valute o aprire buste;',
          'sfruttare errori del gioco invece di segnalarceli;',
          'tentare di accedere ad account o sistemi che non ti appartengono;',
          'disturbare il servizio o gli altri giocatori.',
        ],
      },
      { p: 'In caso di violazione possiamo sospendere o chiudere l’account, dopo averti avvisato quando possibile.' },
    ],
  },
  {
    id: 'contenuti',
    title: 'Carte e valute virtuali',
    blocks: [
      { p: 'Carte, buste e valute di QUORMA sono contenuti virtuali: ti diamo il diritto di usarli all’interno del gioco, ma non ne diventi proprietario. Non hanno valore in denaro, non sono rimborsabili e non possono essere venduti o scambiati fuori dal gioco.' },
      { p: 'Al momento valute e buste si ottengono solo giocando. Se in futuro saranno disponibili acquisti con denaro reale, le relative condizioni verranno aggiunte a questi termini prima dell’attivazione.' },
    ],
  },
  {
    id: 'modifiche',
    title: 'Modifiche al gioco e al servizio',
    blocks: [
      { p: 'QUORMA è in continuo sviluppo. Regole, carte, valori numerici e prezzi possono cambiare per bilanciare il gioco; ogni versione del regolamento è registrata, così una partita giocata con una versione resta riproducibile.' },
      { p: 'Possiamo aggiornare questi termini: per le modifiche importanti ti avviseremo prima che entrino in vigore. Il servizio è offerto così com’è e potrebbe essere temporaneamente non disponibile per manutenzione.' },
    ],
  },
  {
    id: 'contatti',
    title: 'Contatti',
    blocks: [
      { p: `${NO_CHANNEL} su questi termini. Questi termini sono regolati dalla legge italiana.` },
    ],
  },
]

export const legalPages: Record<
  'privacy' | 'cookie' | 'termini',
  { page: Page; title: string; subtitle: string; sections: Section[] }
> = {
  termini: { page: 'termini', title: 'TERMINI', subtitle: 'TERMINI E CONDIZIONI DI SERVIZIO', sections: termini },
  privacy: { page: 'privacy', title: 'PRIVACY', subtitle: 'INFORMATIVA SULLA PRIVACY', sections: privacy },
  cookie: { page: 'cookie', title: 'COOKIE', subtitle: 'INFORMATIVA SUI COOKIE', sections: cookie },
}
