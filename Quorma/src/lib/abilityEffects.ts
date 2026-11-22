// Frasi per gli effetti "liberi" delle carte: il backend li manda come dati del motore (trigger, effetto, bersaglio,
// durata, condizioni), senza testo. Qui diventano italiano seguendo le definizioni di QUORMA-backend-engine/data/vocab.json.
// Se un codice non è tra quelli conosciuti la frase non si compone: meglio dire che manca che mostrarne una sbagliata.
import type { CardKind } from '../types/deck'

type Params = Record<string, unknown>
export type Expansion = {
  trigger_id?: string
  trigger_params?: Params
  effect_id: string
  effect_params?: Params
  target_id: string
  target_params?: Params
  duration?: string
  conditions?: { id: string; params?: Params }[]
  max_stacks?: number
  max_triggers?: number
  once_per_turn?: boolean
}

/** Il nome di una keyword o di un'Evoluzione concessa, con il suo valore (es. "Scudo 15%"). */
export type GrantName = (id: string, params?: Params) => string | null

type Target = { text: string; plural: boolean }
type Context = { kind: CardKind; grant: GrantName; x: Expansion }

const number = (n: number) => n.toLocaleString('it-IT')
const pct = (permille: number) => `${number(Math.abs(permille) / 10)}%`
const capitalize = (s: string) => s.charAt(0).toUpperCase() + s.slice(1)
const list = (items: string[]) => (items.length < 2 ? items.join('') : `${items.slice(0, -1).join(', ')} e ${items[items.length - 1]}`)
const listOr = (items: string[]) => (items.length < 2 ? items.join('') : `${items.slice(0, -1).join(', ')} o ${items[items.length - 1]}`)

// ---------- Bersagli ----------
const FIXED_TARGETS: Record<string, Target> = {
  S1: { text: 'questa creatura', plural: false },
  S6: { text: 'la creatura avversaria nella stessa corsia', plural: false },
  S7: { text: 'una creatura alleata adiacente', plural: false },
  S9: { text: 'tutte le tue creature', plural: true },
  S10: { text: 'tutte le creature avversarie', plural: true },
  S11: { text: 'tutte le creature di entrambi i giocatori', plural: true },
  S17: { text: 'i tuoi attaccanti', plural: true },
  S20: { text: 'le creature che formano la combinazione della tua mano', plural: true },
  S41: { text: 'le creature alleate adiacenti', plural: true },
  S43: { text: 'il bersaglio', plural: false },
  S44: { text: 'le creature avversarie adiacenti a quel bersaglio', plural: true },
}

const PICKS: Record<string, string> = {
  S33: 'nella corsia più bassa',
  S35: 'con ATK più alto',
  S37: 'con DEF più alta',
  S38: 'con DEF più bassa',
  S39: 'di rango più alto',
  S40: 'di rango più basso',
}

function targetOf({ x, kind }: Context): Target | null {
  const id = x.target_id
  if (id === 'S2') return { text: ['T7', 'T35', 'T39'].includes(x.trigger_id ?? '') ? "l'attaccante" : "la creatura che ha innescato l'effetto", plural: false }
  if (id === 'S27') {
    if (x.trigger_id === 'T29') return { text: 'quella creatura', plural: false }
    return kind === 'EQUIPMENT' ? { text: 'il portatore', plural: false } : null
  }
  const pick = PICKS[id]
  if (pick) {
    const filters = (x.target_params?.filters as string[] | undefined) ?? []
    const enemy = filters.includes('enemy') || x.target_params?.from === 'S10'
    const own = filters.includes('own') || filters.includes('ally')
    if (!enemy && !own) return null
    const among = filters.includes('damaged_this_turn') ? ' tra quelle che hanno subito danno in questo turno' : ''
    return { text: `${enemy ? 'la creatura avversaria' : 'la tua creatura'} ${pick}${among}`, plural: false }
  }
  return FIXED_TARGETS[id] ?? null
}

// ---------- Effetti ----------
const SCALES: Record<string, string> = { non_attackers: ' per ogni tua creatura che non attacca', adjacent_allies: ' per ogni creatura alleata adiacente' }

function statChange(t: Target, permille: number, stat: 'ATK' | 'DEF', scaleBy: unknown) {
  const own = stat === 'ATK' ? (t.plural ? 'del loro ATK base' : 'del suo ATK base') : t.plural ? 'della loro DEF base' : 'della sua DEF base'
  const scale = SCALES[String(scaleBy)] ?? ''
  const verb = permille >= 0 ? (t.plural ? 'guadagnano +' : 'guadagna +') : t.plural ? 'perdono il ' : 'perde il '
  return `${t.text} ${verb}${pct(permille)} ${own}${scale}`
}

function discardText(count: number) {
  return count === 1 ? 'scarti la prima carta del tuo mazzo' : `scarti le prime ${count} carte del tuo mazzo`
}

// La frase dell'effetto, senza trigger, condizioni e durata.
function effectText(ctx: Context): string | null {
  const { x, grant } = ctx
  const p = x.effect_params ?? {}
  const v = (s: string, pl: string, t: Target) => (t.plural ? pl : s)
  const n = (key: string) => (typeof p[key] === 'number' ? (p[key] as number) : null)

  // Effetti sul giocatore, sul mazzo o sulla corsia: non hanno una creatura come bersaglio.
  switch (x.effect_id) {
    case 'E8': return n('amount') !== null && x.target_id === 'S31' ? `perdi ${number(n('amount')!)} punti vita` : null
    case 'E9': return n('amount') !== null && x.target_id === 'S31' ? `guadagni ${number(n('amount')!)} punti vita` : null
    case 'E35': return 'peschi la prima carta del tuo mazzo'
    case 'E57': return n('count') !== null && x.target_id === 'S31' ? discardText(n('count')!) : null
    case 'E36': return p.source === 'discard' ? "l'ultima creatura entrata nella tua pila degli scarti torna nella prima corsia vuota" : null
    case 'E47': return p.player === 'avversario' ? 'il piazzamento non avviene' : null
    case 'E49': return p.position === 'top' && x.target_id === 'S47' ? `tra le prime ${x.target_params?.count} carte del tuo mazzo, la creatura di rango più alto passa in cima` : null
    case 'E21': return n('factor') !== null ? `lo spill delle tue creature vale ×${number(n('factor')! / 1000)}` : null
    case 'E22': return n('permille') !== null ? `lo spill tratta la DEF del bersaglio come il ${pct(n('permille')!)} del suo valore` : null
    case 'E64': {
      const inner = p.effect_params as Params | undefined
      if (p.effect_id !== 'E2' || inner?.mode !== 'pct_base_def' || typeof inner.permille !== 'number') return null
      return `la prossima creatura piazzata in questa corsia guadagna +${pct(inner.permille)} della sua DEF base in modo permanente`
    }
  }

  const t = targetOf(ctx)
  if (!t) return null
  const who = t.text
  switch (x.effect_id) {
    case 'E1': return p.mode === 'pct_base_atk' && n('permille') !== null ? statChange(t, n('permille')!, 'ATK', p.scale_by) : null
    case 'E2':
      if (p.mode === 'flat' && n('amount') !== null) return `${who} ${n('amount')! < 0 ? v('perde', 'perdono', t) : v('guadagna', 'guadagnano', t)} ${number(Math.abs(n('amount')!))} DEF`
      return p.mode === 'pct_base_def' && n('permille') !== null ? statChange(t, n('permille')!, 'DEF', p.scale_by) : null
    case 'E3': return n('amount') !== null ? `${who} ${v('guadagna', 'guadagnano', t)} +${number(n('amount')!)} DEF base` : null
    case 'E4': return p.value === 'base' ? `${who} attacca con il suo ATK base` : null
    case 'E6': return p.mode === 'pct_base_def' && n('permille') !== null ? `${who} ${v('recupera', 'recuperano', t)} il ${pct(n('permille')!)} ${t.plural ? 'della loro' : 'della sua'} DEF base` : null
    case 'E7': return n('amount') !== null ? `${who} ${v('subisce', 'subiscono', t)} ${number(n('amount')!)} danni` : null
    case 'E10': return `${who} ${v('viene distrutta', 'vengono distrutte', t)}`
    case 'E11': return n('set_def') !== null ? `${who} sopravvive con DEF ${number(n('set_def')!)}` : null
    case 'E13': return `${who} ${v('viene scartata', 'vengono scartate', t)} senza essere distrutta`
    case 'E14': {
      const name = grant(String(p.keyword), p.keyword_params as Params | undefined)
      return name ? `${who} ${v('ottiene', 'ottengono', t)} ${name}` : null
    }
    case 'E15': {
      const name = grant(String(p.keyword))
      return name ? `${who} ${v('perde', 'perdono', t)} ${name}` : null
    }
    case 'E16': {
      const name = grant(String(p.evolution_id), p.evolution_params as Params | undefined)
      return name ? `${who} ${v('ottiene', 'ottengono', t)} l'Evoluzione ${name}` : null
    }
    case 'E18': return `${who} ${v('non può innescare la sua', 'non possono innescare la loro', t)} Evoluzione`
    case 'E23': return `${who} non infligge danno da combattimento a questa creatura`
    case 'E25': return `${who} non effettua l'attacco`
    case 'E28': return p.mode === 'reduction_pct' && n('pct') !== null ? `${who} ${v('subisce', 'subiscono', t)} il ${number(n('pct')!)}% di danno in meno` : null
    case 'E29': return p.once === true ? `${who} non subisce quel danno` : null
    case 'E32': return p.rank === 'highest_ally' ? `${who} conta come la tua creatura di rango più alto per la mano` : null
    case 'E34': return p.group === 'stesso_giocatore' ? `${who} conta come adiacente a tutte le tue creature` : null
    case 'E51': return n('permille') === 0 ? `${who} ${v('non può', 'non possono', t)} ricevere cure` : null
    case 'E52': return `${who} ${v('non può vedere ridotto il suo', 'non possono vedere ridotto il loro', t)} ATK dagli effetti`
    case 'E54': return n('step') !== null ? `per formare scale, ${who} può contare anche come il rango adiacente (±${n('step')})` : null
    case 'E55': return `${who} conta come qualsiasi seme per colore, scala colore e quattro dello stesso seme`
    case 'E56': return n('pct') !== null ? `${who} ignora il ${number(n('pct')!)}% delle riduzioni di danno del bersaglio` : null
    case 'E60': return `${who} non è più adiacente alle creature alleate`
    case 'E63': return `${who} non conta per la mano di poker`
  }
  return null
}

const DURATIONS: Record<string, string> = {
  D_FinoAlProssimoRifornimento: ' fino al prossimo Rifornimento',
  D_FinoAllaFineDelTurno: ' fino alla fine del turno',
  D_FinoAllaFineDelCombattimento: ' fino alla fine del combattimento',
  D_FinoAUccisione: ' finché non distrugge una creatura',
}
const PERMANENT_EFFECTS = new Set(['E1', 'E2', 'E15', 'E18', 'E28'])
// Effetti la cui frase non comincia dal bersaglio: non si possono unire alla precedente.
const EFFECTS_WITHOUT_SUBJECT = new Set(['E8', 'E9', 'E21', 'E22', 'E35', 'E36', 'E47', 'E49', 'E54', 'E57', 'E64'])

function durationText(x: Expansion) {
  // Un effetto statico con una condizione vale solo finché la condizione è vera: non è "permanente" per il giocatore.
  const conditional = (!x.trigger_id || x.trigger_id === 'T0') && !!x.conditions?.length
  if (x.duration === 'D_Permanente') return PERMANENT_EFFECTS.has(x.effect_id) && !conditional ? ' in modo permanente' : ''
  return DURATIONS[x.duration ?? ''] ?? ''
}

// ---------- Quando si attiva ----------
// `event` si legge dopo "Quando" o "La prima volta che"; `moment` è già una frase completa.
function triggerParts(id: string | undefined, params: Params | undefined, kind: CardKind): { event?: string; moment?: string } | null {
  const bearer = kind === 'EQUIPMENT'
  switch (id) {
    case undefined: case 'T0': case 'T4': case 'T23': case 'T34': return {}
    case 'T2':
      if (params?.phase === 'Ciclo 1') return { moment: "All'inizio del Ciclo 1" }
      if (params?.phase === 'Valutazione') return { moment: "All'inizio della Valutazione" }
      return null
    case 'T6': return { event: 'attacca' }
    case 'T7': return { event: 'viene attaccata' }
    case 'T8': return params?.attacker === 'opponent' && params?.target_scope === 'self_or_adjacent' ? { event: 'un avversario dichiara un attacco contro questa creatura o una adiacente' } : null
    case 'T10': return { event: 'viene distrutta' }
    case 'T12': return { event: 'sopravvive a danno da combattimento' }
    case 'T13': return params?.source === 'combattimento' ? { event: 'subisce danno da combattimento' } : null
    case 'T15': return params?.source === 'ally' ? { event: "un'altra creatura alleata innesca un'Evoluzione" } : null
    case 'T19': return { event: 'uccide una creatura' }
    case 'T20': return { event: bearer ? 'il portatore sta per essere distrutto' : 'sta per essere distrutta' }
    case 'T21': return { event: bearer ? 'il portatore sta per subire danno' : 'sta per subire danno' }
    case 'T22': return { event: 'sta per attaccare' }
    case 'T25': return { event: "l'avversario sta per piazzare una creatura in una corsia" }
    case 'T26': return { moment: 'Alla fine di ogni turno' }
    case 'T29': return { event: kind === 'SPELL' ? 'piazzi una creatura sostitutiva' : 'viene piazzata una creatura sostitutiva' }
    case 'T35': return { event: 'una creatura alleata adiacente è bersaglio di un attacco' }
    case 'T39': return { event: 'sta per essere attaccata' }
  }
  return null
}

function conditionText(c: { id: string; params?: Params }): string | null {
  if (c.id === 'C1') return 'se attacca'
  if (c.id === 'C26' && c.params?.max === 0) return 'se non ha creature alleate adiacenti'
  if (c.id === 'C9') {
    const hands = c.params?.hand_type
    const names = Array.isArray(hands) ? (hands as string[]) : typeof hands === 'string' ? [hands] : []
    return names.length ? `se la tua mano è ${listOr(names)}` : null
  }
  return null
}

function whenText(x: Expansion, kind: CardKind): string | null {
  const evolution = x.trigger_id === 'T_EVO'
  const id = evolution ? (x.trigger_params?.trigger_id as string | undefined) : x.trigger_id
  const params = evolution ? (x.trigger_params?.trigger_params as Params | undefined) : x.trigger_params
  const parts = triggerParts(id, params, kind)
  if (!parts) return null
  const once = !evolution && x.max_triggers === 1
  let when = parts.moment ?? (parts.event ? `${once ? 'La prima volta che' : 'Quando'} ${parts.event}` : '')

  const conditions = (x.conditions ?? []).map(conditionText)
  if (conditions.some((c) => c === null)) return null
  if (conditions.length) {
    const text = (conditions as string[]).join(' e ')
    when = when ? `${when}, ${text}` : capitalize(text)
  }
  return evolution ? `Evoluzione — ${when}` : when
}

function suffix(x: Expansion, whenHasOnce: boolean) {
  const perTurn = x.once_per_turn && !x.max_triggers ? ', una volta per turno' : ''
  if (x.max_stacks) return `${perTurn}, fino a ${x.max_stacks} volte`
  if (x.max_triggers === 1 && !whenHasOnce && x.trigger_id !== 'T_EVO') return ', una sola volta'
  return perTurn
}

/** La descrizione di un effetto libero, oppure null se contiene un codice che non sappiamo descrivere. */
export function composeEffect(expansions: Expansion[], kind: CardKind, grant: GrantName): string | null {
  const groups: { when: string; clauses: string[] }[] = []
  const removed = new Map<string, string[]>() // più keyword tolte agli stessi bersagli diventano un solo elenco
  let lastSubject: string | null = null

  for (const x of expansions) {
    const when = whenText(x, kind)
    if (when === null) return null
    const body = effectText({ kind, grant, x })
    if (body === null) return null
    const once = when.startsWith('La prima volta')
    const clause = `${body}${durationText(x)}${suffix(x, once)}`

    const previous = groups[groups.length - 1]
    if (x.effect_id === 'E15') {
      const key = `${when}|${x.target_id}|${x.duration}`
      const names = removed.get(key)
      const name = grant(String(x.effect_params?.keyword))
      if (names && name && previous?.when === when) {
        names.push(name)
        const t = targetOf({ kind, grant, x })!
        previous.clauses[previous.clauses.length - 1] = `${t.text} ${t.plural ? 'perdono' : 'perde'} ${list(names)}${durationText(x)}`
        continue
      }
      if (name) removed.set(key, [name])
    } else {
      removed.clear()
    }

    // Stesso soggetto della frase precedente: "il portatore ottiene X e perde Y", senza ripeterlo.
    const subject = EFFECTS_WITHOUT_SUBJECT.has(x.effect_id) ? null : (targetOf({ kind, grant, x })?.text ?? null)
    if (previous?.when === when) {
      if (subject && subject === lastSubject && clause.startsWith(`${subject} `) && x.effect_id !== 'E15') {
        previous.clauses[previous.clauses.length - 1] += ` e ${clause.slice(subject.length + 1)}`
      } else {
        previous.clauses.push(clause)
      }
    } else {
      groups.push({ when, clauses: [clause] })
    }
    lastSubject = subject
  }

  return groups
    .map(({ when, clauses }) => {
      const text = clauses.join('; ')
      return `${when ? `${when}: ${text}` : capitalize(text)}.`
    })
    .join(' ')
}
