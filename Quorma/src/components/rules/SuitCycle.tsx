import type { CSSProperties } from 'react'

type Node = { short: string; name: string; color: string }

// Posizioni dei 4 nodi (in senso orario) e delle 4 frecce che li collegano.
const SPOTS = [
  [50, 50],
  [170, 50],
  [170, 170],
  [50, 170],
]
const ARROWS = ['M70 52 Q110 30 150 52', 'M168 70 Q190 110 168 150', 'M150 168 Q110 190 70 168', 'M52 150 Q30 110 52 70']

// Ciclo di 4 sottofazioni: ognuna è forte contro la successiva.
export default function SuitCycle({ nodes }: { nodes: Node[] }) {
  const names = nodes.map((n) => n.name)
  const label = 'Ciclo: ' + names.map((n, i) => `${n} forte contro ${names[(i + 1) % names.length]}`).join(', ')
  const markerId = `freccia-${nodes[0].short}`

  return (
    <figure className="r-ciclo">
      <svg viewBox="0 0 220 220" role="img" aria-label={label}>
        <defs>
          <marker id={markerId} viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto">
            <path d="M0 0 10 5 0 10z" fill="#8FD6DB" />
          </marker>
        </defs>
        {ARROWS.map((d) => (
          <path key={d} d={d} className="r-freccia" markerEnd={`url(#${markerId})`} />
        ))}
        {nodes.map((n, i) => {
          const [x, y] = SPOTS[i]
          return (
            <g key={n.short} className="r-nodo" style={{ '--c': n.color } as CSSProperties}>
              <circle cx={x} cy={y} r="22" />
              <text x={x} y={y + 4}>{n.short}</text>
            </g>
          )
        })}
      </svg>
      <figcaption>{[...names, names[0]].join(' → ')}</figcaption>
    </figure>
  )
}
