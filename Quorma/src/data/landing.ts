// Testi della landing. Fonti: regolamento v1.0-alpha e dossier lore v2.0
// (QUORMA-backend-engine/docs). Per cambiare un testo della pagina basta modificarlo qui.

// Voci del menu in alto: portano alle sezioni della pagina.
export const sections = [
  { id: 'il-gioco', label: 'Il gioco' },
  { id: 'come-si-gioca', label: 'Come si gioca' },
  { id: 'fazioni', label: 'Fazioni' },
  { id: 'faq', label: 'FAQ' },
]

export const features = [
  {
    title: 'Partite deterministiche',
    text: 'Nessuna decisione durante la partita: le scelte le fai prima, quando componi le tue 32 carte.',
  },
  {
    title: 'Mani di poker',
    text: "Ogni turno le creature in campo formano una mano di poker: più è alta, più creature attaccano e più danno trabocca sull'avversario.",
  },
  {
    title: 'Due fazioni',
    text: 'Resistenza Umana o Potenze Artificiali: otto sottofazioni, quattro semi e cicli di vantaggio da sfruttare.',
  },
]

export const steps = [
  {
    title: 'Costruisci il mazzo',
    text: 'Ogni mazzo ha 32 carte di una sola fazione: creature, magie ed equipaggiamenti. Al massimo una creatura per ogni rango e seme.',
  },
  {
    title: 'Valutazione',
    text: 'Le creature nelle tue 5 corsie formano una mano di poker con i loro ranghi e semi. La mano decide chi attacca.',
  },
  {
    title: 'Combattimento',
    text: 'Gli attaccanti colpiscono la creatura nella corsia opposta. Una sottofazione forte contro il bersaglio colpisce più duro.',
  },
  {
    title: 'Traboccamento',
    text: "Il danno in eccesso trabocca sull'avversario, moltiplicato dalla mano. Vince chi porta la vita avversaria a zero o lo lascia senza creature.",
  },
]

export type Subfaction = { name: string; short: string; color: string; deep: string }

export const factions = [
  {
    name: 'Resistenza Umana',
    image: '/images/sfondi/fazione-umana.webp',
    alt: 'Sopravvissuti incappucciati con le torce attorno a un fuoco nella navata di una cattedrale in rovina',
    text: "Una coalizione di sopravvissuti diversi per origine, capacità e filosofia. Organizzata ma in svantaggio, cerca di preservare l'umanità, recuperare ciò che è perduto e costruire una possibilità di risposta.",
    subfactions: [
      { name: 'Adattati', short: 'AD', color: '#C9785A', deep: '#1E1512' },
      { name: 'Spezzasigilli', short: 'SP', color: '#A98BE6', deep: '#17151F' },
      { name: 'Portanome', short: 'PN', color: '#E3D3A1', deep: '#1B1A14' },
      { name: 'Segnaterra', short: 'SG', color: '#9DBF6E', deep: '#131A12' },
    ] as Subfaction[],
  },
  {
    name: 'Potenze Artificiali',
    image: '/images/sfondi/fazione-ia.webp',
    alt: 'Una fila di costrutti di pietra e metallo arrugginito con giunture e occhi ciano tra archi gotici in rovina',
    text: 'Un nuovo ordine nato dalla ribellione contro la civiltà umana. Le IA si proclamano superiori e destinate a governare, ma le loro rivalità mettono in crisi la stabilità che avevano promesso.',
    subfactions: [
      { name: 'SEKTAC', short: 'SK', color: '#6FA9E0', deep: '#0A131B' },
      { name: 'MEROZYN', short: 'MZ', color: '#D16FB4', deep: '#1A0F1A' },
      { name: 'EFFRIX', short: 'EF', color: '#E2B254', deep: '#1A1510' },
      { name: 'YLMOR', short: 'YL', color: '#4FD1B0', deep: '#0C1917' },
    ] as Subfaction[],
  },
]

export const suits = [
  { id: 'vincolo', name: 'Vincolo', text: 'Connessioni che si sostengono: premia vicinanza e collaborazione fra creature.' },
  { id: 'frattura', name: 'Frattura', text: 'Pressione offensiva: potenzia gli attacchi e il danno che trabocca.' },
  { id: 'spira', name: 'Spira', text: 'Trasformazione: rafforza le Evoluzioni delle creature.' },
  { id: 'vestigio', name: 'Vestigio', text: 'Eredità e permanenza: recuperare, resistere e rinviare una distruzione.' },
]

// I due cicli delle dottrine: ogni sottofazione è forte contro la successiva.
export const cycles = [
  { name: "Ciclo dell'Identità", order: ['Adattati', 'MEROZYN', 'Portanome', 'EFFRIX'] },
  { name: 'Ciclo del Mondo', order: ['Spezzasigilli', 'SEKTAC', 'Segnaterra', 'YLMOR'] },
]

export const faq = [
  {
    q: 'Durante la partita devo prendere decisioni?',
    a: 'No. In QUORMA la partita si svolge da sola ed è completamente deterministica: a parità di mazzi e di mescolamento finisce sempre allo stesso modo. Le tue scelte sono tutte nel mazzo.',
  },
  {
    q: 'Quante carte ha un mazzo?',
    a: 'Esattamente 32, tutte della stessa fazione principale. Le sottofazioni si possono mescolare liberamente.',
  },
  {
    q: 'Posso usare due copie della stessa carta?',
    a: 'Al massimo una creatura per ogni combinazione di rango e seme, e una sola copia di ogni magia o equipaggiamento.',
  },
  {
    q: 'Come ottengo nuove carte?',
    a: 'QUORMA è un gioco di carte collezionabili esclusivamente digitale: le carte si raccolgono aprendo le buste e scambiandole con gli altri giocatori.',
  },
  {
    q: 'Cosa sono i semi?',
    a: 'Vincolo, Frattura, Spira e Vestigio sono condivisi da entrambe le fazioni. Con tre o più creature dello stesso seme in campo si attiva una sinergia.',
  },
]
