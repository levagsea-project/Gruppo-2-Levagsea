// I "tipi" descrivono la forma dei dati: così TypeScript ci avvisa se sbagliamo.
export type FactionId = 'umani' | 'artificiali'

export type Suit = { name: string; value: number } // un seme e quante creature ha

// Un mazzo nella lista (GET /decks). Il backend accetta solo mazzi di 32 carte che rispettano le regole,
// quindi un mazzo salvato è sempre completo: può però diventare "obsoleto" quando cambia il pool di carte.
export type Deck = {
  id: string
  name: string // es. "Mazzo 01"
  faction: FactionId
  cards: number // carte inserite
  maxCards: number // carte necessarie (32)
  defense: boolean // è il mazzo di difesa della sua fazione
  outdated: boolean // non più valido per il pool di carte attuale: non si può usare finché non lo modifichi
}

// ---------- Carte ----------
export type CardKind = 'CREATURE' | 'SPELL' | 'EQUIPMENT'
export type SuitId = 'VINCOLO' | 'FRATTURA' | 'SPIRA' | 'VESTIGIO'

// Una carta del gioco (dati della carta, non della singola copia posseduta).
export type GameCard = {
  cardId: string
  code: string
  faction: FactionId
  kind: CardKind
  name: string
  subfaction: string | null // es. SEKTAC (solo creature)
  suit: SuitId | null // solo creature
  rank: number | null // 1 = Asso … 11 Jack, 12 Queen, 13 King (solo creature)
  atk: number | null
  def: number | null
  slot: string // posto nel mazzo: una creatura per seme e rango, una copia per magia o equipaggiamento
}

// Una copia posseduta di una carta (GET /collection): nel mazzo si mette la copia, non la carta.
export type OwnedCard = GameCard & { instanceId: string }

// Un mazzo con le sue carte (GET /decks/{id}).
export type DeckDetail = Deck & { snapshotId: string; cardList: OwnedCard[] }

// Conteggi del mazzo scelto, calcolati dalle sue carte (GET /decks/{id}).
export type DeckStats = {
  creatures: number
  spells: number // magie / equipaggiamenti
  suits: Suit[]
}

// "Posso giocare?" (GET /decks/play-status): serve un mazzo di difesa valido per entrambe le fazioni.
export type PlayStatus = {
  canPlay: boolean
  missingDefense: FactionId[] // fazioni senza mazzo di difesa
  outdatedDefense: FactionId[] // fazioni con il mazzo di difesa obsoleto
  outdatedDecks: { id: string; name: string; faction: FactionId; defense: boolean }[]
}
