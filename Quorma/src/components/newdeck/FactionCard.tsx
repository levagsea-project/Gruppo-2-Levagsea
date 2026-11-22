import type { Faction } from '../../data/factions'
import type { CSSProperties } from 'react'
import Stat from '../ui/Stat'

type Props = { faction: Faction; selected: boolean; onSelect: () => void }

// Una delle due grandi card: immagine, nome, numeri e sottofazioni.
export default function FactionCard({ faction, selected, onSelect }: Props) {
  return (
    <button className={`faction-card ${selected ? 'is-selected' : ''}`} onClick={onSelect}>
      <span className="deck-diamond" />
      {/* illustrazione grande della fazione (1024×427), ritagliata sul soggetto come nella pagina Gioca */}
      <img
        className="faction-img"
        src={faction.cover}
        alt=""
        width={1024}
        height={427}
        decoding="async"
        style={{ objectPosition: faction.coverFocus }}
      />
      <span className="faction-body">
        <span className="faction-name">{faction.name.toUpperCase()}</span>
        <span className="faction-stats">
          <Stat value={faction.cards} label="CARTE" />
          <Stat value={faction.creatures} label="CREATURE" />
          <Stat value={faction.spells} label="MAGIE / EQUIP." />
        </span>
        <span className="subfactions">
          {faction.subfactions.map((s) => (
            <span className="subfaction" key={s.name} style={{ '--c': s.color } as CSSProperties}>
              <span className="subfaction-badge">{s.short}</span>
              {s.name}
            </span>
          ))}
        </span>
      </span>
    </button>
  )
}
