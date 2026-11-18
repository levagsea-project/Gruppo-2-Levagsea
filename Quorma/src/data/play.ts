// Modalità di gioco della pagina Gioca. paths = tratti dell'icona (viewBox 24×24).
export type PlayMode = { id: string; title: string; desc: string; meta: string; hint: string; paths: string[] }

export const playModes: PlayMode[] = [
  {
    id: 'libera',
    title: 'Partita libera',
    desc: 'Affronta altri giocatori senza classifica.',
    meta: 'Online · Senza classifica',
    hint: 'Inizia per cercare un avversario online.',
    paths: ['M12 2 22 12 12 22 2 12z', 'M8.5 8.5l7 7', 'M15.5 8.5l-7 7'],
  },
  {
    id: 'classificata',
    title: 'Classificata',
    desc: 'Sfida altri giocatori e sali in classifica.',
    meta: 'Online · Con classifica',
    hint: 'Inizia per cercare un avversario in classificata.',
    paths: ['M12 2.5 19.5 5.5v6c0 4.6-3.2 8.3-7.5 10-4.3-1.7-7.5-5.4-7.5-10v-6z', 'M12 7.5l1.2 2.8 2.8 1.2-2.8 1.2L12 15.5l-1.2-2.8-2.8-1.2 2.8-1.2z'],
  },
  {
    id: 'allenamento',
    title: 'Allenamento',
    desc: 'Prova il tuo mazzo contro il computer.',
    meta: 'Contro il computer',
    hint: 'Inizia una partita contro il computer.',
    paths: ['M2.5 12 12 5l9.5 7-9.5 7z', 'M12 9.5 14.5 12 12 14.5 9.5 12z', 'M12 1.5v2M12 20.5v2'],
  },
]
