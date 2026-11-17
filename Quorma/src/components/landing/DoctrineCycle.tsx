import type { CSSProperties } from 'react'
import { factions } from '../../data/landing'

// Posizioni dei quattro nodi (in senso orario) e frecce fra un nodo e il successivo.
const NODES = [
  { x: 50, y: 50 },
  { x: 170, y: 50 },
  { x: 170, y: 170 },
  { x: 50, y: 170 },
]
const ARROWS = ['M70 52 Q110 30 150 52', 'M168 70 Q190 110 168 150', 'M150 168 Q110 190 70 168', 'M52 150 Q30 110 52 70']
const ALL = factions.flatMap((f) => f.subfactions)

// Un ciclo delle dottrine: ogni sottofazione è forte contro quella successiva.
export default function DoctrineCycle({ name, order }: { name: string; order: string[] }) {
  const subs = order.map((n) => ALL.find((s) => s.name === n)!)
  const label = `${name}: ` + subs.map((s, i) => `${s.name} forte contro ${subs[(i + 1) % 4].name}`).join(', ')
  const markerId = `freccia-${name.replace(/\W+/g, '')}`

  return (
    <svg viewBox="0 0 220 220" role="img" aria-label={label}>
      <defs>
        <marker id={markerId} viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto">
          <path d="M0 0 10 5 0 10z" fill="#8FD6DB" />
        </marker>
      </defs>
      {ARROWS.map((d) => <path key={d} d={d} className="r-freccia" markerEnd={`url(#${markerId})`} />)}
      {subs.map((s, i) => (
        <g key={s.name} className="r-nodo" style={{ '--c': s.color } as CSSProperties}>
          <circle cx={NODES[i].x} cy={NODES[i].y} r="22" />
          <text x={NODES[i].x} y={NODES[i].y + 4}>{s.short}</text>
        </g>
      ))}
    </svg>
  )
}
