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

// free = frase composta dai dati, senza un nome ufficiale; short = versione breve per il fronte della carta;
// related = le parole chiave che questa abilità concede o toglie, spiegate nel pop-up
export type AbilityText = { id: string; name: string; text: string; free?: boolean; short?: string; related?: AbilityText[] }

type Params = Record<string, unknown>
type Definition = { name: string; description?: string; expands_to?: Expansion[] }
type Tuning = { params?: Record<string, unknown>; display?: Record<string, unknown> }
type RawAbility = { id?: unknown; params?: Record<string, unknown>; expansions?: Expansion[] }

const DEFINITIONS = { ...keywordData, ...evolutionData } as unknown as Record<string, Definition>
const balance = balanceData as unknown as { keywords: Record<string, Tuning>; evolutions: Record<string, Tuning> }

const RANKS: Record<string, string> = { 1: 'A', 11: 'J', 12: 'Q', 13: 'K' }
const number = (n: number) => n.toLocaleString('it-IT')
const capitalize = (s: string) => s.charAt(0).toUpperCase() + s.slice(1)

// I file del backend scrivono alcuni accenti con l'apostrofo ("puo'"): al giocatore arrivano corretti.
const ACCENTS: Record<string, string> = { puo: 'può', piu: 'più', finche: 'finché', comunita: 'comunità', Conformita: 'Conformità', da: 'dà', e: 'è' }
const withAccents = (s: string) => s.replace(/\b(puo|piu|finche|comunita|Conformita|da|e)'(?=[\s.,;:)]|$)/g, (_, word: string) => ACCENTS[word])

// Abilità con più varianti: la descrizione ufficiale le elenca con i nomi tecnici (on=attack, scope any),
// qui resta solo la frase della variante scritta sulla carta. I valori di default sono quelli di keyword.json.
const VARIANTS: Record<string, (p: Params) => string> = {
  K_ALLONTANA: (p) => `Quando questa creatura ${p.on === 'attacked' ? 'viene attaccata' : 'attacca'}: le creature alleate adiacenti perdono il {pct}% del loro ATK base fino alla fine del turno.`,
  K_ASSIMILA: (p) => {
    const who = p.scope === 'any' ? 'una creatura alleata o avversaria' : p.scope === 'adjacent' ? 'una creatura alleata adiacente' : 'una creatura alleata'
    return `Quando ${who} viene distrutta: questa creatura guadagna +{bonus_pct}% del suo ATK base in modo permanente, fino a {max_stacks} volte.`
  },
  K_CATENA: (p) => `Quando questa creatura ${p.on === 'kill' ? 'uccide una creatura' : 'infligge danno da combattimento'}: le creature avversarie adiacenti al bersaglio subiscono il {pct}% di quel danno. Si ferma alla prima corsia vuota.`,
  K_NON_RISCRIVIBILE: (p) => `L'ATK ${p.scope === 'self_adjacent' ? 'di questa creatura e delle creature alleate adiacenti' : 'delle creature alleate adiacenti'} non può essere ridotto dagli effetti.`,
}

// Le versioni brevi per il fronte della carta: nella casella stanno tre righe, la frase intera è nel pop-up.
type Short = (v: Record<string, string>, p: Params) => string
const SHORT: Record<string, Short> = {
  K_ACCUMULO: (v) => `+${v.pct}% ATK ogni turno (max ${v.max_stacks})`,
  K_ALLONTANA: (v, p) => `${p.on === 'attacked' ? 'se attaccata' : 'se attacca'}, alleati vicini −${v.pct}% ATK`,
  K_APRI_STRADA: (v) => `se uccide, alleati vicini +${v.pct}% ATK`,
  K_ASSIMILA: (v, p) => `+${v.pct}% ATK se cade ${p.scope === 'any' ? 'una creatura' : p.scope === 'adjacent' ? 'un alleato vicino' : 'un alleato'}`,
  K_CAMALEONTE: () => 'vale ogni seme per il colore',
  K_CAMPI_VUOTI: (v) => `+${v.pct}% ATK per chi non attacca`,
  K_CATENA: (v, p) => `${p.on === 'kill' ? 'se uccide, ' : ''}colpisce i vicini al ${v.pct}%`,
  K_CONFINE: () => 'nemici vicini senza bonus',
  K_CONFORMITA: () => 'toglie i bonus al bersaglio',
  K_CONTA_COME: (v) => `conta anche come ${v.rank}`,
  K_CUSTODE: () => 'resiste alla prima distruzione',
  K_ECO: (v) => `rimanda il ${v.pct}% del danno`,
  K_FLESSIBILE: () => 'vale ±1 nelle scale',
  K_GUSCIO: () => 'non può essere curata',
  K_INDURISCE: (v) => `+${v.pct}% DEF se cade un alleato`,
  K_INSAZIABILE: (v) => `+${v.pct}% ATK a ogni uccisione`,
  K_ISOLA: () => 'isola il nemico di fronte',
  K_LASCITO_CURA: (v) => `se distrutta, cura i vicini del ${v.pct}%`,
  K_LASCITO_DIFESA: (v) => `se distrutta, vicini +${v.pct}% DEF`,
  K_LETALE: () => 'ogni danno distrugge',
  K_MURO: (v) => `−${v.reduction_pct}% danno, protegge i vicini`,
  K_NON_RISCRIVIBILE: (_, p) => `ATK ${p.scope === 'self_adjacent' ? 'suo e dei vicini' : 'dei vicini'} non riducibile`,
  K_PERFORANTE: (v) => `ignora il ${v.x}% delle riduzioni`,
  K_PORTA_AVANTI: (v) => `se cade un alleato, cura il ${v.pct}%`,
  K_PRIMO_COLPO: () => 'se uccide nel Ciclo 1, non subisce danno',
  K_PROVOCAZIONE: () => 'attira gli attacchi ai vicini',
  K_RIGENERANTE: (v) => `cura il ${v.pct}% a ogni Rifornimento`,
  K_SALITA: (v) => `+${v.pct}% ATK, −${v.cost_pct}% DEF ogni turno`,
  K_SCUDO: (v, p) => (p.kind === 'flat' ? `−${v.x} danno per colpo` : `−${v.x}% danno subito`),
  K_SCUDO_ADIACENTI: (v) => `vicini −${v.x}% danno`,
  K_SCUDO_COMUNITA: (v) => `−${v.x}% danno per ogni vicino`,
  K_SLANCIO: (v) => `se attacca, vicini +${v.pct}% ATK, lei −${v.cost_pct}% DEF`,
  K_SOSTEGNO: (v) => `se non attacca, attaccanti vicini +${v.x}% ATK`,
  K_TRABOCCANTE: (v) => `spill ${v.multiplier} quando uccide`,
  K_ULTIMO_GESTO: (v) => `se distrutta, ${v.pct}% ATK al nemico`,
  EVO_CAPITANO: (v) => `se cade un nemico, ottiene Sostegno ${v.x}%`,
  EVO_CARNEFICE: () => 'se uccide, ottiene Letale',
  EVO_CRESCITA: (v) => `se sopravvive, +${v.pct}% ATK`,
  EVO_EREDITA: (v) => `se distrutta, +${v.life} vita`,
  EVO_FURIA: (v) => `se ferita, +${v.pct}% ATK nel turno`,
  EVO_INTRECCIO: (v) => `con Vincolo 3, vicini +${v.pct}% ATK`,
  EVO_MATURAZIONE: () => 'a fine turno ottiene Flessibile',
  EVO_MIMETICO: () => 'a fine turno ottiene Camaleonte',
  EVO_PREDATORE: (v) => `se uccide, ottiene Perforante ${v.x}%`,
  EVO_RINFORZO: (v) => `con una sostituzione, +${v.pct}% DEF`,
  EVO_RISONANZA: (v) => `con una tua magia, +${v.pct}% ATK`,
  EVO_SENTINELLA: () => 'se attaccata, ottiene Provocazione',
  EVO_SFONDAMENTO: () => 'con lo spill ottiene Traboccante',
  EVO_SOPRAVVISSUTO: (v) => `se sopravvive, ottiene Scudo ${v.x}%`,
  EVO_TEMPRATO: () => 'se ferita, ottiene Rigenerante',
  EVO_TRINCEA: (v) => `se bersaglio, +${v.pct}% DEF`,
  EVO_VENDETTA: () => 'se cade un alleato, ottiene Primo Colpo',
}

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
  return { values, merged, flat: merged.kind === 'flat' }
}

// "$tune.EVO_PREDATORE.x" o "$param.x": il valore scritto sulla carta o quello di base.
function resolveValue(value: unknown, merged: Params) {
  const key = typeof value === 'string' ? /^\$(?:param|tune\.[^.]+)\.(\w+)$/.exec(value)?.[1] : undefined
  return key ? merged[key] : value
}

/** Le parole chiave ed Evoluzioni che degli effetti concedono o tolgono, con i valori di questa carta. */
function relatedFrom(expansions: Expansion[] = [], merged: Params = {}, seen = new Set<string>()): AbilityText[] {
  return expansions.flatMap((x) => {
    const p = x.effect_params ?? {}
    const evolution = x.effect_id === 'E16'
    const id = evolution ? p.evolution_id : x.effect_id === 'E14' || x.effect_id === 'E15' ? p.keyword : null
    if (typeof id !== 'string' || seen.has(id)) return []
    seen.add(id)
    const raw = (evolution ? p.evolution_params : p.keyword_params) as Params | undefined
    const params = Object.fromEntries(Object.entries(raw ?? {}).map(([key, value]) => [key, resolveValue(value, merged)]))
    const described = describe({ id, params }, seen)
    return described ? [described, ...(described.related ?? [])] : []
  })
}

function describe({ id, params }: { id: string; params?: Record<string, unknown> }, seen = new Set<string>([id])): AbilityText | null {
  const definition = DEFINITIONS[id]
  if (!definition) return null
  const { values, merged, flat } = valuesFor(id, params)
  let text = VARIANTS[id]?.(merged) ?? definition.description ?? ''

  // Scudo ha due forme nella stessa frase: resta solo quella della carta.
  if (id === 'K_SCUDO') {
    text = flat
      ? 'Scudo (X): il danno subito da questa creatura è ridotto di X per colpo, minimo 1.'
      : text.replace(/\s*\(Scudo flat:[^)]*\)/, '')
  }

  const parametric = /\(X\)/.test(text)
  if (values.x) {
    // "ottiene Scudo (X)" diventa "ottiene Scudo 10%"; "Scudo (X): il danno…" perde il prefisso, il nome è già il titolo
    if (id.startsWith('EVO_')) text = text.replace(/\s*\(X\)/g, ` ${values.x}%`)
    else text = capitalize(text.replace(/^[^:]*\(X\):\s*/, ''))
    text = text.replace(/\bX\b/g, values.x)
  }
  text = text.replace(/\{(\w+)\}/g, (all, key: string) => values[key] ?? all)
  if (/\{\w+\}|\bX\b/.test(text)) return null // un valore manca: meglio nessun testo che una frase con i segnaposto

  const baseName = withAccents(definition.name)
  const name = parametric && id.startsWith('K_') && values.x ? `${baseName} ${values.x}${flat ? '' : '%'}` : baseName
  const related = relatedFrom(definition.expands_to, merged, seen)
  return { id, name, text: withAccents(text), short: SHORT[id]?.(values, merged), related }
}

/** Il nome di una keyword o Evoluzione concessa da un effetto, con il valore che ha su questa carta. */
function grantedName(id: string, params?: Record<string, unknown>) {
  const definition = DEFINITIONS[id]
  if (!definition) return null
  const tuning = balance.keywords[id] ?? balance.evolutions[id]
  const merged = { ...tuning?.params, ...params }
  const name = withAccents(definition.name)
  if (typeof merged.x === 'number') return `${name} ${number(merged.x)}${merged.kind === 'flat' ? '' : '%'}`
  if (typeof merged.permille === 'number') return `${name} ${number(merged.permille / 10)}%`
  if (typeof merged.amount === 'number') return `${name} ${number(merged.amount)}`
  return name
}

/** Le abilità di una carta con nome e descrizione; quelle che non si sanno descrivere vengono saltate. */
export function abilityTexts(abilities: unknown, kind: CardKind): AbilityText[] {
  if (!Array.isArray(abilities)) return []
  return (abilities as RawAbility[]).flatMap((a, i) => {
    if (Array.isArray(a?.expansions)) {
      const text = composeEffect(a.expansions, kind, grantedName)
      return text ? [{ id: `effetto-${i}`, name: 'Effetto', text, free: true, related: relatedFrom(a.expansions) }] : []
    }
    if (typeof a?.id !== 'string') return []
    const described = describe({ id: a.id, params: a.params })
    return described ? [described] : []
  })
}

const FRONT_MAX = 100 // oltre, nella casella della carta non sta: la frase intera è accanto alla carta

export type FrontPart = { ability: AbilityText; label: string; detail: string }

/** Le voci del fronte della carta: `label` è la parola premibile (vuota se la frase è già intera), `detail` il testo breve. */
export const frontParts = (abilities: AbilityText[]): FrontPart[] =>
  abilities.map((ability) => {
    if (ability.free) return ability.text.length <= FRONT_MAX ? { ability, label: '', detail: ability.text } : { ability, label: 'Effetto speciale', detail: '' }
    return { ability, label: ability.name, detail: ability.short ?? '' }
  })

/** Il testo breve per il fronte della carta, quando la carta non è premibile. */
export const abilityFront = (abilities: AbilityText[]) =>
  frontParts(abilities).map((p) => [p.label, p.detail].filter(Boolean).join(': ')).join(' · ')
