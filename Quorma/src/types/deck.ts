// I "tipi" descrivono la forma dei dati: così TypeScript ci avvisa se sbagliamo.
export type FactionId = 'umani' | 'artificiali'

export type Suit = { name: string; value: number } // un seme e quante creature ha

export type Deck = {
  id: string
  name: string // es. "Mazzo 01"
  faction: FactionId
  cards: number // carte inserite
  maxCards: number // carte necessarie (32)
  creatures: number
  spells: number // magie / equipaggiamenti
  suits: Suit[]
}
