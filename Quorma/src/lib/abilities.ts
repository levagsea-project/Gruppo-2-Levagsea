// Testo delle abilità delle creature. Il backend manda solo id e parametri: nomi e descrizioni sono quelli ufficiali
// di QUORMA-backend-engine/data (keyword.json, evolutions.json), i valori di base stanno in balance.json.
// I parametri scritti sulla carta vincono su quelli di base, perché alcuni cambiano con il Tier.
// Gli effetti senza id (magie, equipaggiamenti e alcune creature) arrivano come dati del motore: la frase si compone
// in lib/abilityEffects.ts.
import keywordData from '../../../QUORMA-backend-engine/data/keyword.json'
import evolutionData from '../../../QUORMA-backend-engine/data/evolutions.json'
import balanceData from '../../../QUORMA-backend-engine/data/balance.json'
import type { CardKind } from '../types/deck'
import { composeEffect, type Expansion } from './abilityEffects'

// free = frase composta dai dati, senza un nome ufficiale
export type AbilityText = { id: string; name: string; text: string; free?: boolean }

type Definition = { name: string; description?: string }
type Tuning = { params?: Record<string, unknown>; display?: Record<string, unknown> }
type RawAbility = { id?: unknown; params?: Record<string, unknown>; expansions?: Expansion[] }

const DEFINITIONS = { ...keywordData, ...evolutionData } as unknown as Record<string, Definition>
const balance = balanceData as unknown as { keywords: Record<string, Tuning>; evolutions: Record<string, Tuning> }

const RANKS: Record<string, string> = { 1: 'A', 11: 'J', 12: 'Q', 13: 'K' }
const number = (n: number) => n.toLocaleString('it-IT')

function valuesFor(id: string, params: Record<string, unknown> = {}) {
  const tuning = balance.keywords[id] ?? balance.evolutions[id]
  const merged = { ...tuning?.params, ...params }
  const values: Record<string, string> = {}
  for (const [key, value] of Object.entries(tuning?.display ?? {})) values[key] = String(value)

  const read = (key: string) => (typeof merged[key] === 'number' ? (merged[key] as number) : null)
  const permille = read('permille')
  if (permille !== null) {
    const pct = number(permille / 10)
    for (const key of ['bonus_pct', 'pct', 'heal_pct', 'bonus_pct_per_stack', 'heal_pct_base_def']) values[key] = pct
  }
  const cost = read('cost_permille')
  if (cost !== null) values.cost_pct = number(cost / 10)
  const spill = read('spill_def_permille')
  if (spill !== null) values.spill_def_pct = number(spill / 10)
  const factor = read('factor')
  if (factor !== null) values.multiplier = `×${number(factor / 1000)}`
  const amount = read('amount')
  if (amount !== null) values.life = number(amount)
  for (const key of ['x', 'max_stacks']) {
    const n = read(key)
    if (n !== null) values[key] = number(n)
  }
  if (merged.rank !== undefined) values.rank = RANKS[String(merged.rank)] ?? String(merged.rank)
  return { values, flat: merged.kind === 'flat' }
}

function describe({ id, params }: { id: string; params?: Record<string, unknown> }): AbilityText | null {
  const definition = DEFINITIONS[id]
  if (!definition) return null
  const { values, flat } = valuesFor(id, params)
  let text = definition.description ?? ''

  // Scudo ha due forme nella stessa frase: resta solo quella della carta.
  if (id === 'K_SCUDO') {
    text = flat
      ? 'Scudo (X): il danno subito da questa creatura è ridotto di X per colpo, minimo 1.'
      : text.replace(/\s*\(Scudo flat:[^)]*\)/, '')
  }

  const parametric = /\(X\)/.test(text)
  if (values.x) text = text.replace(/\bX\b/g, values.x)
  text = text.replace(/\{(\w+)\}/g, (all, key: string) => values[key] ?? all)
  if (/\{\w+\}|\bX\b/.test(text)) return null // un valore manca: meglio nessun testo che una frase con i segnaposto

  const name = parametric && id.startsWith('K_') && values.x ? `${definition.name} ${values.x}${flat ? '' : '%'}` : definition.name
  return { id, name, text }
}

/** Il nome di una keyword o Evoluzione concessa da un effetto, con il valore che ha su questa carta. */
function grantedName(id: string, params?: Record<string, unknown>) {
  const definition = DEFINITIONS[id]
  if (!definition) return null
  const tuning = balance.keywords[id] ?? balance.evolutions[id]
  const merged = { ...tuning?.params, ...params }
  if (typeof merged.x === 'number') return `${definition.name} ${number(merged.x)}${merged.kind === 'flat' ? '' : '%'}`
  if (typeof merged.permille === 'number') return `${definition.name} ${number(merged.permille / 10)}%`
  if (typeof merged.amount === 'number') return `${definition.name} ${number(merged.amount)}`
  return definition.name
}

/** Le abilità di una carta con nome e descrizione; quelle che non si sanno descrivere vengono saltate. */
export function abilityTexts(abilities: unknown, kind: CardKind): AbilityText[] {
  if (!Array.isArray(abilities)) return []
  return (abilities as RawAbility[]).flatMap((a, i) => {
    if (Array.isArray(a?.expansions)) {
      const text = composeEffect(a.expansions, kind, grantedName)
      return text ? [{ id: `effetto-${i}`, name: 'Effetto', text, free: true }] : []
    }
    if (typeof a?.id !== 'string') return []
    const described = describe({ id: a.id, params: a.params })
    return described ? [described] : []
  })
}

const FRONT_MAX = 100 // oltre, nella casella della carta non sta: la frase intera è accanto alla carta

/** Il testo breve per il fronte della carta: i nomi delle abilità e le frasi corte. */
export const abilityFront = (abilities: AbilityText[]) =>
  abilities.map((a) => (a.free ? (a.text.length <= FRONT_MAX ? a.text : 'Effetto speciale') : a.name)).join(' · ')
