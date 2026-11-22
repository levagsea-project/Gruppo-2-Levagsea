// Ricerca e ordinamento delle carte, uguali in Collezione ed editor.
import type { AbilityText } from './abilities'
import type { GameCard } from '../types/deck'

export type CardSort = 'base' | 'rank-desc' | 'rank-asc' | 'atk' | 'def' | 'ability'

export const SORT_OPTIONS: { value: CardSort; label: string }[] = [
  { value: 'base', label: 'Seme e rango' },
  { value: 'rank-desc', label: 'Rango: dal più alto' },
  { value: 'rank-asc', label: 'Rango: dal più basso' },
  { value: 'atk', label: 'ATK: dal più alto' },
  { value: 'def', label: 'DEF: dalla più alta' },
  { value: 'ability', label: 'Abilità: dalla A alla Z' },
]

// Senza accenti e maiuscole: "comunita" trova "comunità".
const normalize = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()

const abilityWords = (a: AbilityText): string[] => [a.name, a.text, a.short ?? '', ...(a.related ?? []).flatMap(abilityWords)]

/** La carta contiene tutte le parole cercate, nel nome o nelle sue abilità. */
export function matchesCard(card: GameCard, query: string) {
  const words = normalize(query).split(/\s+/).filter(Boolean)
  if (words.length === 0) return true
  const text = normalize([card.name, ...(card.abilities ?? []).flatMap(abilityWords)].join(' '))
  return words.every((w) => text.includes(w))
}

// L'Asso è la carta più alta.
const rankValue = (c: GameCard) => (c.rank === 1 ? 14 : c.rank)
const abilityName = (c: GameCard) => {
  const first = c.abilities?.[0]
  return first ? (first.free ? 'Effetto' : first.name) : null
}

// Le carte senza quel valore (magie ed equipaggiamenti per rango, ATK e DEF) vanno sempre in fondo.
function compareMissingLast<V>(a: V | null, b: V | null, compare: (x: V, y: V) => number) {
  if (a === null) return b === null ? 0 : 1
  if (b === null) return -1
  return compare(a, b)
}

/** Una copia ordinata. A parità di valore resta l'ordine di partenza. */
export function sortCards<T>(items: T[], cardOf: (item: T) => GameCard, sort: CardSort): T[] {
  if (sort === 'base') return items
  const down = (x: number, y: number) => y - x
  const up = (x: number, y: number) => x - y
  const compare = (a: GameCard, b: GameCard) => {
    switch (sort) {
      case 'rank-desc': return compareMissingLast(rankValue(a), rankValue(b), down)
      case 'rank-asc': return compareMissingLast(rankValue(a), rankValue(b), up)
      case 'atk': return compareMissingLast(a.atk, b.atk, down)
      case 'def': return compareMissingLast(a.def, b.def, down)
      case 'ability': return compareMissingLast(abilityName(a), abilityName(b), (x, y) => x.localeCompare(y, 'it'))
    }
  }
  return [...items].sort((a, b) => compare(cardOf(a), cardOf(b)))
}
