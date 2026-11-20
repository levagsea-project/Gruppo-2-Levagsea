import type { FactionId } from '../types/deck'

// Le due fazioni. I testi della schermata "Nuovo mazzo" vengono da qui.
export type Faction = {
  id: FactionId
  name: string
  cover: string // illustrazione della fazione (1024×427): copertine dei mazzi, Nuovo mazzo, home su telefono
  coverFocus: string // dove sta il soggetto, per il ritaglio (background-position / object-position)
  cards: string
  creatures: string
  spells: string
  subfactions: { name: string; short: string; color: string }[]
}

export const factions: Record<FactionId, Faction> = {
  umani: {
    id: 'umani',
    name: 'Resistenza Umana',
    cover: '/images/sfondi/fazione-umana.webp',
    coverFocus: '50% 70%', // il falò e i sopravvissuti con le torce
    cards: '32',
    creatures: '20–23',
    spells: '9–12',
    subfactions: [
      { name: 'Gli Adattati', short: 'LO', color: '#d9694a' },
      { name: 'Gli Spezzasigilli', short: 'AF', color: '#9b6ae0' },
      { name: 'I Portanome', short: 'PK', color: '#d8b94a' },
      { name: 'I Segnaterra', short: 'SG', color: '#7cc04a' },
    ],
  },
  artificiali: {
    id: 'artificiali',
    name: 'Potenze Artificiali',
    cover: '/images/sfondi/fazione-ia.webp',
    coverFocus: '80% 30%', // il costrutto più vicino, con gli occhi ciano
    cards: '32',
    creatures: '24–27',
    spells: '5–8',
    subfactions: [
      { name: 'SEKTAC', short: 'SK', color: '#4a7be0' },
      { name: 'MEROZYN', short: 'MZ', color: '#d95aa8' },
      { name: 'EFFRIK', short: 'EF', color: '#e0933a' },
      { name: 'VLMOR', short: 'VL', color: '#3fb89a' },
    ],
  },
}
