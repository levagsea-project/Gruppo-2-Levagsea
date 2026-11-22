// Modalità della pagina Gioca: i tre modi di avviare una serie che il backend offre. paths = tratti dell'icona (viewBox 24×24).
export type PlayModeId = 'libera' | 'stanza' | 'sfida'
export type PlayMode = { id: PlayModeId; title: string; desc: string; meta: string; action: string; paths: string[] }

export const playModes: PlayMode[] = [
  {
    id: 'libera',
    title: 'Partita libera',
    desc: 'Affronta un giocatore attivo negli ultimi 7 giorni, contro i suoi mazzi di difesa.',
    meta: 'Si gioca subito',
    action: 'Inizia partita',
    paths: ['M12 2 22 12 12 22 2 12z', 'M8.5 8.5l7 7', 'M15.5 8.5l-7 7'],
  },
  {
    id: 'stanza',
    title: 'Stanza',
    desc: 'Apri una stanza e aspetta un avversario, oppure entra in quella di un altro giocatore.',
    meta: 'Online · Aperta 10 minuti',
    action: 'Apri stanza',
    paths: ['M5 21V10a7 7 0 0 1 14 0v11', 'M3 21h18', 'M14.5 13.5v1.5'],
  },
  {
    id: 'sfida',
    title: 'Sfida',
    desc: 'Sfida un giocatore per nome, anche se è offline: la serie parte quando accetta.',
    meta: 'Per nome · Valida 48 ore',
    action: 'Invia sfida',
    paths: ['M6 21V3', 'M6 4h12l-3 4.5 3 4.5H6'],
  },
]
