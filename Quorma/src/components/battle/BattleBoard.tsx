import type { CSSProperties } from 'react'
import { factions } from '../../data/factions'
import { baseDef, cardOn, fmt, type BattleGame, type Beat, type Board, type Float, type StoryPart } from '../../lib/battle'
import { otherSeat, type Seat } from '../../lib/series'
import CreatureCard from '../cards/CreatureCard'

type Props = {
  game: BattleGame
  beat: Beat
  beatIndex: number
  board: Board // il campo da mostrare ora: prima o dopo l'impatto
  impacted: boolean
  names: { me: string; them: string }
}

function Floats({ items }: { items: Float[] }) {
  return items.map((f, i) => (
    <span key={`${f.text}-${i}`} className={`bt-float bt-float--${f.tone}`} style={{ '--i': i } as CSSProperties}>
      {f.text}
    </span>
  ))
}

// Una frase del racconto: i nomi hanno il colore del loro lato (petrolio io, brace l'avversario).
export function Story({ parts, me }: { parts: StoryPart[]; me: Seat }) {
  return parts.map((p, i) => (p.side ? <b key={i} className={p.side === me ? 'bt-nome--mio' : 'bt-nome--suo'}>{p.text}</b> : <span key={i}>{p.text}</span>))
}

// Una riga di cinque corsie.
function Lanes({ game, beat, beatIndex, board, impacted, side }: Omit<Props, 'names'> & { side: Seat }) {
  const up = side === game.me // le mie carte colpiscono verso l'alto
  return (
    <div className="bt-corsie">
      {board[side].lanes.map((state, i) => {
        const lane = i + 1
        const card = state ? cardOn(game, side, state) : null
        const strike = beat.strikes.find((s) => s.from.side === side && s.from.lane === lane)
        const hit = impacted && beat.strikes.find((s) => s.to.side === side && s.to.lane === lane)
        const attacker = beat.kind === 'mani' && beat.attackers[side].includes(lane)
        const aimed = !impacted && beat.strikes.some((s) => s.to.side === side && s.to.lane === lane)
        const inFocus = beat.focus.some((k) => k.side === side && k.lane === lane)
        const floats: Float[] = impacted ? beat.floats.filter((f) => f.side === side && f.lane === lane) : []
        if (hit) floats.unshift({ side, lane, text: `−${fmt(hit.damage)}`, tone: 'danno' })
        const max = state ? Math.max(baseDef(game, side, state.card_id), state.def, 1) : 1
        const health = state ? Math.max(0, Math.min(100, (state.def / max) * 100)) : 0
        const cls = ['bt-carta']
        if (strike) cls.push('bt-carta--attacca')
        if (hit) cls.push('bt-carta--colpita')
        if (attacker) cls.push('bt-carta--pronta')
        if (aimed) cls.push('bt-carta--bersaglio')
        if (inFocus) cls.push('bt-carta--protagonista')
        if (inFocus && beat.kind === 'effetti') cls.push('bt-carta--effetto')
        if (state?.destroyed) cls.push('bt-carta--distrutta')
        const style = strike
          ? ({ '--dir': up ? -1 : 1, '--corsie': strike.to.lane - lane } as CSSProperties)
          : undefined
        return (
          <div key={lane} className="bt-slot">
            {state && card ? (
              <div key={state.card_id} className={cls.join(' ')} style={style} data-ritmo={beatIndex % 2}>
                <CreatureCard card={card} sizes="200px" />
                <span className="bt-salute" aria-hidden="true"><span style={{ width: `${health}%` }} /></span>
                <span className="bt-valori" aria-hidden="true">
                  <b>{fmt(state.atk)}</b>
                  <b>{fmt(Math.max(0, state.def))}</b>
                </span>
              </div>
            ) : (
              <span className="bt-vuoto" aria-hidden="true" />
            )}
            {floats.length > 0 && <span key={beatIndex} className="bt-floats"><Floats items={floats} /></span>}
          </div>
        )
      })}
    </div>
  )
}

// La targa di un giocatore: nome, fazione, vita, mano del turno, mazzo e scarti.
function Plate({ game, beat, beatIndex, board, impacted, side, name }: Omit<Props, 'names'> & { side: Seat; name: string }) {
  const p = board[side]
  const life = Math.max(0, p.life)
  const pct = Math.min(100, (life / game.startingLife) * 100)
  const spill = impacted ? beat.strikes.filter((s) => s.to.side === side).reduce((sum, s) => sum + s.spill, 0) : 0
  const floats: Float[] = impacted ? beat.floats.filter((f) => f.side === side && f.lane === null) : []
  if (spill) floats.unshift({ side, lane: null, text: `−${fmt(spill)} Vita`, tone: 'danno' })
  return (
    <div className={`bt-targa ${side === game.me ? 'bt-targa--mia' : ''}`}>
      <div className="bt-targa__chi">
        <strong>{name}</strong>
        <span>{factions[game.factions[side]].name}</span>
      </div>
      <div className={`bt-vita ${spill ? 'bt-vita--colpita' : ''}`}>
        <span className="bt-vita__num">{fmt(life)}</span>
        <span className="bt-vita__barra"><span style={{ width: `${pct}%` }} /></span>
        {floats.length > 0 && <span key={beatIndex} className="bt-floats bt-floats--vita"><Floats items={floats} /></span>}
      </div>
      <div className="bt-targa__mano">{beat.hands ? beat.hands[side] : ''}</div>
      <div className="bt-targa__pile">
        <span>Mazzo <b>{p.deck_count}</b></span>
        <span>Scarti <b>{p.discard_count}</b></span>
      </div>
    </div>
  )
}

// Il campo: avversario in alto, io in basso, con turno e fase sull'asse centrale.
export default function BattleBoard(props: Props) {
  const { game, beat, names } = props
  const them = otherSeat(game.me)
  return (
    <div className={`bt-campo ${beat.focus.length ? 'bt-campo--focus' : ''}`}>
      <Plate {...props} side={them} name={names.them} />
      <Lanes {...props} side={them} />
      <div className="bt-asse">
        <div className="bt-asse__riga">
          <span className="q-divisorio" aria-hidden="true" />
          <div className="bt-asse__testo">
            <span className="bt-rombo" aria-hidden="true" />
            <strong>{beat.turn ? `Turno ${beat.turn}` : 'Inizio'}</strong>
            <span className="bt-asse__fase">{beat.label}</span>
            <span className="bt-rombo" aria-hidden="true" />
          </div>
          <span className="q-divisorio" aria-hidden="true" />
        </div>
        <div key={props.beatIndex} className="bt-racconto">
          <p><Story parts={beat.story} me={game.me} /></p>
          {beat.tags.length > 0 && (
            <ul className="bt-tag" aria-label="Effetti particolari">
              {beat.tags.map((tag) => <li key={tag}>{tag}</li>)}
            </ul>
          )}
        </div>
      </div>
      <Lanes {...props} side={game.me} />
      <Plate {...props} side={game.me} name={names.me} />
    </div>
  )
}
