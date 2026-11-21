// Dal log del motore alla battaglia da animare. Non calcola nessuna regola: legge gli eventi già accaduti e li racconta
// in "battute" (schieramento, mani, un colpo alla volta, effetti, rifornimento), ognuna con una frase e lo stato del campo.
// Alla fine di ogni turno il campo viene riallineato a quello scritto nel log, che è la fonte di verità.
import { handName, handOrder, type Result } from '../data/profile'
import type { FactionId, GameCard, SuitId } from '../types/deck'
import { NO_NAMES, type BattleNames } from './battleNames'
import { SUIT_NAMES, rankLabel, toFaction } from './cards'
import { otherSeat, resultFor, type MatchLog, type Seat } from './series'

// ---------- Le parti del log che servono qui (schema match-log del motore) ----------
type CreatureRef = { side: Seat; lane: number; card_id: string }
type LaneState = { card_id: string; atk: number; def: number; destroyed: boolean }
type PlayerBoard = { life: number; deck_count: number; discard_count: number; lanes: (LaneState | null)[] }
export type Board = Record<Seat, PlayerBoard>
type StatChange = { stat: 'atk' | 'def' | 'life'; before: number; after: number }
type Modifier = { ability_id: string; kind: string; value: number }
type LogEvent = {
  type: string
  attacker?: CreatureRef
  target?: CreatureRef | { side: Seat; lane?: number }
  creature?: CreatureRef
  source?: CreatureRef
  from?: CreatureRef
  to?: CreatureRef
  side?: Seat
  lane?: number
  card_id?: string
  card_ids?: string[]
  ability_id?: string
  equipment_id?: string
  creature_id?: string
  reason?: string
  damage?: number
  def_after?: number
  killed?: boolean
  mutual_kill?: boolean
  spill?: number
  advantage_permille?: number
  spill_multiplier_permille?: number
  modifiers?: Modifier[]
  changes?: StatChange[]
}
type DeckCard = { id: string; kind: GameCard['kind']; rank?: number; suit?: SuitId; subfaction?: string; atk?: number; def?: number; name?: string }
type Totals = { damage_dealt: number; spill_damage: number; kills: number; creatures_lost: number }
type CardTotals = { side: Seat; card_id: string; damage_dealt: number; spill_damage: number; kills: number }
type Hand = { hand_type: string; attacker_lanes: number[]; multiplier_permille: number }
type Turn = {
  turn: number
  evaluation: Record<Seat, Hand>
  opening?: LogEvent[]
  cycle_1: LogEvent[]
  cycle_2: LogEvent[]
  refill: LogEvent[]
  end_of_turn?: LogEvent[]
  board: Board
}
type FullLog = {
  header: { starting_life: number; players: Record<Seat, { faction: string; deck: DeckCard[] }> }
  setup: { board: Board }
  turns: Turn[]
  result: { outcome: 'A_WIN' | 'B_WIN' | 'DRAW'; reason: string; turns_played: number; life: Record<Seat, number> }
  totals: { players: Record<Seat, Totals>; cards: CardTotals[] }
}

// ---------- Battute ----------
export type LaneKey = { side: Seat; lane: number } // lane da 1 a 5
export type Strike = { from: LaneKey; to: LaneKey; damage: number; spill: number; killed: boolean }
export type Float = { side: Seat; lane: number | null; text: string; tone: 'danno' | 'cura' | 'forza' } // lane null = sulla vita
export type StoryPart = { text: string; side?: Seat } // side = nome di una carta o di un giocatore, nel colore del suo lato
export type Beat = {
  kind: 'schieramento' | 'mani' | 'colpi' | 'effetti' | 'rifornimento'
  turn: number
  label: string
  story: StoryPart[]
  tags: string[] // abilità ed effetti particolari di questa battuta
  ms: number // durata a velocità ×1
  board: Board
  hands: Record<Seat, string> | null
  attackers: Record<Seat, number[]>
  strikes: Strike[]
  floats: Float[]
  focus: LaneKey[] // le carte protagoniste: le altre si abbassano
}

export type SideStats = { damage: number; spill: number; kills: number; lost: number }
export type GameStats = {
  n: number
  result: Result
  reason: string
  turns: number
  lifeMe: number
  lifeThem: number
  best: string
  me: SideStats
  them: SideStats
  mvp: { card: GameCard; damage: number; kills: number } | null
}
export type BattleGame = {
  n: number
  me: Seat
  beats: Beat[]
  startingLife: number
  factions: Record<Seat, FactionId>
  decks: Record<Seat, Map<string, DeckCard>>
  stats: GameStats
}

const BEAT_MS = { schieramento: 2200, mani: 2400, colpi: 2100, effetti: 1900, rifornimento: 1600 }
const KILL_MS = 400 // un colpo mortale resta a schermo un po' di più
const MAX_GAME_MS = 240_000 // a ×1 una partita non dura più di 4 minuti, anche se arriva a 50 turni

const REASONS: Record<string, string> = {
  NO_CREATURES: 'Creature esaurite',
  LIFE_ZERO: 'Vita azzerata',
  BOTH_LOST: 'Sconfitti entrambi',
  TURN_50_LIFE: 'Turno 50, più vita',
  TURN_50_HAND: 'Turno 50, mano migliore',
  TURN_50_DRAW: 'Turno 50, parità',
}

const SUIT_BY_ID: Record<string, string> = { VINCOLO: 'Vincolo', FRATTURA: 'Frattura', SPIRA: 'Spira', VESTIGIO: 'Vestigio' }

export const fmt = (n: number) => n.toLocaleString('it-IT')
const mult = (permille: number) => (permille / 1000).toLocaleString('it-IT', { maximumFractionDigits: 2 })
const clone = (b: Board): Board => structuredClone(b)
const NO_ATTACKERS: Record<Seat, number[]> = { A: [], B: [] }

const fallbackName = (c: DeckCard) =>
  c.suit ? `${rankLabel(c.rank ?? null)} di ${SUIT_NAMES[c.suit]}` : c.kind === 'SPELL' ? 'Magia' : 'Equipaggiamento'

function toCard(c: DeckCard, faction: FactionId, atk = c.atk ?? null, def = c.def ?? null): GameCard {
  const suit = c.suit ?? null
  return {
    cardId: c.id,
    code: c.id,
    faction,
    kind: c.kind,
    name: c.name ?? fallbackName(c),
    subfaction: c.subfaction ?? null,
    suit,
    rank: c.rank ?? null,
    atk,
    def,
    slot: c.id,
    flavour: null,
    textLocale: 'it',
    sets: [],
  }
}

/** La carta in una corsia, con i valori attuali. */
export function cardOn(game: BattleGame, side: Seat, lane: LaneState): GameCard | null {
  const c = game.decks[side].get(lane.card_id)
  return c ? toCard(c, game.factions[side], lane.atk, lane.def) : null
}

/** DEF stampata sulla carta, per la barra della salute. */
export const baseDef = (game: BattleGame, side: Seat, cardId: string) => game.decks[side].get(cardId)?.def ?? 0

const isCreatureRef = (t: LogEvent['target']): t is CreatureRef => !!t && 'card_id' in t

const sameLane = (a: LaneKey, b: LaneKey) => a.side === b.side && a.lane === b.lane

function changeFloat(side: Seat, lane: number | null, c: StatChange): Float | null {
  const delta = c.after - c.before
  if (!delta) return null
  const sign = delta > 0 ? '+' : '−'
  const stat = c.stat === 'life' ? 'Vita' : c.stat.toUpperCase()
  const tone = delta < 0 ? 'danno' : c.stat === 'atk' ? 'forza' : 'cura'
  return { side, lane, text: `${sign}${fmt(Math.abs(delta))} ${stat}`, tone }
}

/** Trasforma il log di una partita nelle battute da animare e nelle sue statistiche, viste da chi siede in `me`. */
export function buildBattle(raw: MatchLog, me: Seat, n: number, names: BattleNames = NO_NAMES): BattleGame {
  const log = raw as unknown as FullLog
  const them = otherSeat(me)
  const factions = { A: toFaction(log.header.players.A.faction), B: toFaction(log.header.players.B.faction) }
  const named = (c: DeckCard): [string, DeckCard] => [c.id, { ...c, name: names.cards.get(c.id) }]
  const decks = { A: new Map(log.header.players.A.deck.map(named)), B: new Map(log.header.players.B.deck.map(named)) }

  const cardName = (side: Seat, id: string): StoryPart => {
    const c = decks[side].get(id)
    return { text: c ? (c.name ?? fallbackName(c)) : id, side }
  }
  const who = (side: Seat): StoryPart => ({ text: side === me ? 'Tu' : 'Avversario', side })
  const abilityName = (id: string | undefined) => {
    if (!id) return 'Abilità'
    const known = names.abilities.get(id)
    if (known) return known
    const synergy = /^(VINCOLO|FRATTURA|SPIRA|VESTIGIO)_(\d)$/.exec(id)
    if (synergy) return `Sinergia ${SUIT_BY_ID[synergy[1]]} ${synergy[2]}`
    return 'Abilità'
  }
  const modifierTag = (m: Modifier) => {
    const name = abilityName(m.ability_id)
    switch (m.kind) {
      case 'atk_bonus': return `${name} +${fmt(m.value)} ATK`
      case 'atk_malus': return `${name} −${fmt(m.value)} ATK`
      case 'atk_set': return `${name}: ATK ${fmt(m.value)}`
      case 'damage_reduction_pct': return `${name} −${m.value}% danni`
      case 'damage_reduction_flat': return `${name} −${fmt(m.value)} danni`
      case 'damage_prevented': return `${name}: ${fmt(m.value)} danni evitati`
      case 'pierce_pct': return `${name}: ignora ${m.value}% difese`
      case 'spill_multiplier': return `${name}: traboccamento ×${mult(m.value)}`
      case 'spill_def_permille': return `${name}: traboccamento potenziato`
      case 'lethal': return `${name}: distrugge il bersaglio`
      default: return name
    }
  }

  let board = clone(log.setup.board)
  const beats: Beat[] = [
    {
      kind: 'schieramento', turn: 0, label: 'Schieramento', story: [{ text: 'Le creature prendono posto nelle cinque corsie.' }], tags: [],
      ms: BEAT_MS.schieramento, board: clone(board), hands: null, attackers: NO_ATTACKERS, strikes: [], floats: [], focus: [],
    },
  ]

  for (const t of log.turns) {
    const hands = { A: handName(t.evaluation.A.hand_type), B: handName(t.evaluation.B.hand_type) }
    const attackers = { A: t.evaluation.A.attacker_lanes, B: t.evaluation.B.attacker_lanes }
    type Extra = Partial<Pick<Beat, 'strikes' | 'floats' | 'tags' | 'focus'>>
    const push = (kind: Beat['kind'], label: string, story: StoryPart[], extra: Extra = {}) => {
      const strikes = extra.strikes ?? []
      const ms = BEAT_MS[kind] + (strikes.some((s) => s.killed) ? KILL_MS : 0)
      beats.push({ kind, turn: t.turn, label, story, tags: extra.tags ?? [], ms, board: clone(board), hands, attackers, strikes, floats: extra.floats ?? [], focus: extra.focus ?? [] })
    }

    const handStory = (side: Seat): StoryPart[] => {
      const h = t.evaluation[side]
      const count = h.attacker_lanes.length
      return [who(side), { text: `: ${handName(h.hand_type)} (×${mult(h.multiplier_permille)}), ${count} ${count === 1 ? 'attaccante' : 'attaccanti'}` }]
    }
    push('mani', 'Si valutano le mani', [...handStory(me), { text: ' · ' }, ...handStory(them)], {
      focus: [...attackers.A.map((lane) => ({ side: 'A' as Seat, lane })), ...attackers.B.map((lane) => ({ side: 'B' as Seat, lane }))],
    })

    // Legge una fase. Il Ciclo 1 è simultaneo: si mostra un colpo alla volta per capirlo, ma le creature cadono tutte insieme alla fine.
    const readPhase = (events: LogEvent[], label: string, simultaneous: boolean) => {
      let kills: CreatureRef[] = []
      type EffectGroup = { key: string; head: StoryPart[]; tag: string; changes: StoryPart[][]; floats: Float[]; focus: LaneKey[] }
      let group = null as EffectGroup | null // effetti consecutivi della stessa abilità: una battuta sola

      const flushEffects = () => {
        if (group?.floats.length) {
          const shown = group.changes.slice(0, 3).flatMap((parts, i) => (i ? [{ text: ', ' }, ...parts] : parts))
          const more = group.changes.length > 3 ? [{ text: ` e altri ${group.changes.length - 3}` }] : []
          push('effetti', 'Effetto speciale', [...group.head, { text: ': ' }, ...shown, ...more], { floats: group.floats, tags: [group.tag], focus: group.focus })
        }
        group = null
      }
      const flushKills = () => {
        if (!kills.length) return
        const fallen = kills
        kills = []
        for (const k of fallen) {
          const lane = board[k.side].lanes[k.lane - 1]
          if (lane) lane.destroyed = true
        }
        const list = fallen.flatMap((k, i) => (i ? [{ text: ', ' }, cardName(k.side, k.card_id)] : [cardName(k.side, k.card_id)]))
        push('effetti', 'Fine del ciclo', [...list, { text: fallen.length === 1 ? ' cade.' : ' cadono.' }], {
          floats: fallen.map((k) => ({ side: k.side, lane: k.lane, text: 'Distrutta', tone: 'danno' as const })),
          focus: fallen.map((k) => ({ side: k.side, lane: k.lane })),
        })
      }
      const settle = () => {
        flushEffects()
        flushKills()
      }

      for (const ev of events) {
        if (ev.type === 'attack_resolved' && ev.attacker && isCreatureRef(ev.target)) {
          flushEffects()
          const from = ev.attacker
          const to = ev.target
          const lane = board[to.side].lanes[to.lane - 1]
          if (lane) lane.def = ev.def_after ?? lane.def
          board[to.side].life -= ev.spill ?? 0
          if (ev.killed) {
            if (simultaneous) kills.push(to)
            else if (lane) lane.destroyed = true
          }
          const tags = (ev.modifiers ?? []).map(modifierTag)
          if (ev.advantage_permille && ev.advantage_permille > 1000) tags.push(`Vantaggio di sottofazione +${Math.round(ev.advantage_permille / 10 - 100)}%`)
          if (ev.advantage_permille && ev.advantage_permille < 1000) tags.push(`Svantaggio di sottofazione −${Math.round(100 - ev.advantage_permille / 10)}%`)
          const story: StoryPart[] = [cardName(from.side, from.card_id), { text: ' colpisce ' }, cardName(to.side, to.card_id), { text: `: ${fmt(ev.damage ?? 0)} danni.` }]
          // nel Ciclo 1 i colpi si mostrano uno alla volta: "a vicenda" si può dire solo al secondo dei due
          if (ev.mutual_kill && kills.some((k) => sameLane(k, from))) story.push({ text: ' Si distruggono a vicenda.' })
          else if (ev.killed) story.push({ text: simultaneous ? ' Cadrà a fine ciclo.' : ' Distrutta.' })
          if (ev.spill) story.push({ text: ` ${fmt(ev.spill)} traboccano sulla vita (×${mult(ev.spill_multiplier_permille ?? 1000)}).` })
          push('colpi', label, story, {
            strikes: [{ from: { side: from.side, lane: from.lane }, to: { side: to.side, lane: to.lane }, damage: ev.damage ?? 0, spill: ev.spill ?? 0, killed: !!ev.killed }],
            tags: [...new Set(tags)],
            focus: [{ side: from.side, lane: from.lane }, { side: to.side, lane: to.lane }],
          })
        } else if (ev.type === 'attack_redirected' && ev.attacker && ev.to) {
          settle()
          push('effetti', 'Effetto speciale', [cardName(ev.attacker.side, ev.attacker.card_id), { text: ' viene deviata su ' }, cardName(ev.to.side, ev.to.card_id), { text: '.' }], {
            tags: [abilityName(ev.ability_id)],
            focus: [{ side: ev.attacker.side, lane: ev.attacker.lane }, { side: ev.to.side, lane: ev.to.lane }],
          })
        } else if (ev.type === 'attack_skipped' && ev.reason === 'PREVENTED' && ev.attacker) {
          settle()
          push('effetti', 'Effetto speciale', [cardName(ev.attacker.side, ev.attacker.card_id), { text: ': attacco impedito.' }], {
            tags: ['Attacco impedito'],
            focus: [{ side: ev.attacker.side, lane: ev.attacker.lane }],
          })
        } else if (ev.type === 'creature_destroyed' && ev.creature) {
          const c = ev.creature
          if (kills.some((k) => sameLane(k, c))) continue
          const lane = board[c.side].lanes[c.lane - 1]
          if (lane && !lane.destroyed) {
            settle()
            lane.destroyed = true
            push('effetti', 'Effetto speciale', [cardName(c.side, c.card_id), { text: ' viene distrutta.' }], {
              floats: [{ side: c.side, lane: c.lane, text: 'Distrutta', tone: 'danno' }],
              focus: [{ side: c.side, lane: c.lane }],
            })
          }
        } else if (ev.type === 'creature_saved' && ev.creature) {
          const c = ev.creature
          kills = kills.filter((k) => !sameLane(k, c))
          settle()
          const lane = board[c.side].lanes[c.lane - 1]
          if (lane) lane.destroyed = false
          push('effetti', 'Effetto speciale', [cardName(c.side, c.card_id), { text: ' si salva dalla distruzione.' }], {
            floats: [{ side: c.side, lane: c.lane, text: 'Salvata', tone: 'cura' }],
            tags: [abilityName(ev.ability_id)],
            focus: [{ side: c.side, lane: c.lane }],
          })
        } else if ((ev.type === 'effect_resolved' || ev.type === 'card_effect_resolved') && ev.changes?.length && ev.target) {
          flushKills()
          const target = ev.target
          const creature = isCreatureRef(target) ? board[target.side].lanes[target.lane - 1] : null
          const sourceSide = ev.source?.side ?? ev.side ?? target.side
          const sourceId = ev.source?.card_id ?? ev.card_id ?? ''
          const key = `${sourceSide}:${sourceId}:${ev.ability_id}`
          if (group?.key !== key) {
            flushEffects()
            const isCard = ev.type === 'card_effect_resolved'
            const kind = decks[sourceSide].get(sourceId)?.kind
            const tag = isCard ? (kind === 'SPELL' ? 'Magia' : kind === 'EQUIPMENT' ? 'Equipaggiamento' : abilityName(ev.ability_id)) : abilityName(ev.ability_id)
            const head: StoryPart[] = [cardName(sourceSide, sourceId)]
            if (tag !== 'Abilità' && !isCard) head.push({ text: ` attiva ${tag}` })
            group = { key, head, tag, changes: [], floats: [], focus: ev.source ? [{ side: ev.source.side, lane: ev.source.lane }] : [] }
          }
          const current = group!
          for (const c of ev.changes) {
            if (c.stat === 'life') board[target.side].life = c.after
            else if (creature) creature[c.stat] = c.after
            const onCreature = c.stat !== 'life' && isCreatureRef(target)
            const float = changeFloat(target.side, onCreature ? target.lane : null, c)
            if (!float) continue
            current.floats.push(float)
            const onItself = onCreature && target.side === sourceSide && target.card_id === sourceId // non ripete il nome della carta
            current.changes.push(onItself ? [{ text: float.text }] : onCreature ? [cardName(target.side, target.card_id), { text: ` ${float.text}` }] : [who(target.side), { text: ` ${float.text}` }])
            if (onCreature && !current.focus.some((k) => sameLane(k, target))) current.focus.push({ side: target.side, lane: target.lane })
          }
        } else if (ev.type === 'creature_discarded' && ev.creature) {
          settle()
          board[ev.creature.side].lanes[ev.creature.lane - 1] = null
        } else if ((ev.type === 'card_drawn' || ev.type === 'creature_returned') && ev.side && ev.lane && ev.card_id) {
          flushEffects()
          const c = decks[ev.side].get(ev.card_id)
          board[ev.side].lanes[ev.lane - 1] = { card_id: ev.card_id, atk: c?.atk ?? 0, def: c?.def ?? 0, destroyed: false }
          if (ev.type === 'creature_returned') {
            push('effetti', 'Effetto speciale', [cardName(ev.side, ev.card_id), { text: ' torna in campo dagli scarti.' }], {
              tags: ['Ritorno'],
              focus: [{ side: ev.side, lane: ev.lane }],
            })
          }
        } else if (ev.type === 'spell_resolved' && ev.side && ev.card_id) {
          settle()
          push('effetti', 'Magia pescata', [{ text: ev.side === me ? 'Lanci ' : 'L’avversario lancia ' }, cardName(ev.side, ev.card_id), { text: '.' }], { tags: ['Magia'] })
        } else if (ev.type === 'equipment_applied' && ev.side && ev.lane && ev.equipment_id && ev.creature_id) {
          settle()
          push('effetti', 'Equipaggiamento', [cardName(ev.side, ev.equipment_id), { text: ' va a ' }, cardName(ev.side, ev.creature_id), { text: '.' }], {
            tags: ['Equipaggiamento'],
            focus: [{ side: ev.side, lane: ev.lane }],
          })
        } else if (ev.type === 'cards_milled' && ev.side && ev.card_ids?.length) {
          settle()
          push('effetti', 'Effetto speciale', [who(ev.side), { text: `: ${ev.card_ids.length} ${ev.card_ids.length === 1 ? 'carta scartata' : 'carte scartate'} dal mazzo.` }], { tags: [abilityName(ev.ability_id)] })
        }
      }
      settle()
    }

    readPhase(t.opening ?? [], 'Inizio del turno', false)
    readPhase(t.cycle_1, 'Ciclo 1 · colpi simultanei', true)
    readPhase(t.cycle_2, 'Ciclo 2 · un colpo alla volta', false)
    readPhase(t.refill, 'Rifornimento', false)
    readPhase(t.end_of_turn ?? [], 'Fine del turno', false)

    // Rifornimento: il campo diventa quello scritto nel log.
    const drawn = (side: Seat) => t.refill.filter((e) => e.type === 'card_drawn' && e.side === side).length
    board = clone(t.board)
    if (drawn(me) + drawn(them) > 0) {
      const part = (side: Seat): StoryPart[] => [who(side), { text: ` ${drawn(side)} ${drawn(side) === 1 ? 'nuova creatura' : 'nuove creature'}` }]
      const parts = [me, them].filter((s) => drawn(s) > 0).flatMap((s, i) => (i ? [{ text: ', ' }, ...part(s)] : part(s)))
      push('rifornimento', 'Rifornimento', [{ text: 'Le corsie si riempiono. ' }, ...parts, { text: '.' }])
    } else {
      beats[beats.length - 1].board = clone(board)
    }
  }

  // Partite molto lunghe: il ritmo si accorcia in proporzione, così ×1 resta guardabile.
  const total = beats.reduce((sum, b) => sum + b.ms, 0)
  if (total > MAX_GAME_MS) for (const b of beats) b.ms = Math.round((b.ms * MAX_GAME_MS) / total)

  const side = (s: Seat): SideStats => {
    const p = log.totals.players[s]
    return { damage: p.damage_dealt, spill: p.spill_damage, kills: p.kills, lost: p.creatures_lost }
  }
  const myCards = log.totals.cards.filter((c) => c.side === me && c.damage_dealt + c.spill_damage > 0)
  const top = myCards.reduce<CardTotals | null>((best, c) => (!best || c.damage_dealt + c.spill_damage > best.damage_dealt + best.spill_damage ? c : best), null)
  const topCard = top ? decks[me].get(top.card_id) : undefined
  const best = log.turns.map((t) => handName(t.evaluation[me].hand_type)).sort((a, b) => handOrder.indexOf(b) - handOrder.indexOf(a))[0] ?? handName('NONE')

  return {
    n,
    me,
    beats,
    startingLife: log.header.starting_life,
    factions,
    decks,
    stats: {
      n,
      result: resultFor(log.result.outcome, me),
      reason: REASONS[log.result.reason] ?? '',
      turns: log.result.turns_played,
      lifeMe: Math.max(0, log.result.life[me]),
      lifeThem: Math.max(0, log.result.life[them]),
      best,
      me: side(me),
      them: side(them),
      mvp: top && topCard ? { card: toCard(topCard, factions[me]), damage: top.damage_dealt + top.spill_damage, kills: top.kills } : null,
    },
  }
}
