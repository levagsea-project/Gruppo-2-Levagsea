import { useRef, type CSSProperties, type PointerEvent } from 'react'
import { m, useMotionTemplate, useMotionValue, useReducedMotion, useSpring, useTransform } from 'motion/react'
import type { factions } from '../../data/landing'

type Faction = (typeof factions)[number]

// Luce che segue il puntatore sull'illustrazione: torcia calda per gli umani, ciano degli occhi delle IA.
const LUCE: Record<string, string> = { 'Resistenza Umana': '224,135,74', 'Potenze Artificiali': '74,166,173' }
const MOLLA = { stiffness: 170, damping: 24, mass: 0.6 }

// Carta di una fazione. Con il mouse sopra, una luce segue il puntatore e l'illustrazione si sposta appena in
// profondità; la carta resta ferma, così non scivola via da sotto il puntatore. Sul touch non succede nulla;
// con il movimento ridotto resta la luce ma l'immagine non si muove.
export default function FactionCard({ f }: { f: Faction }) {
  const reduced = useReducedMotion()
  const arte = useRef<HTMLSpanElement>(null)
  const px = useMotionValue(0.5)
  const py = useMotionValue(0.35)
  const acceso = useMotionValue(0)
  const sx = useSpring(px, MOLLA)
  const sy = useSpring(py, MOLLA)
  const luce = useSpring(acceso, { stiffness: 120, damping: 22 })
  const lx = useTransform(sx, (v) => `${v * 100}%`)
  const ly = useTransform(sy, (v) => `${v * 100}%`)
  const rgb = LUCE[f.name] ?? LUCE['Potenze Artificiali']
  const alone = useMotionTemplate`radial-gradient(240px circle at ${lx} ${ly}, rgba(${rgb},.42), rgba(${rgb},.12) 45%, rgba(${rgb},0) 75%)`
  const imgX = useTransform(sx, [0, 1], [9, -9])
  const imgY = useTransform(sy, [0, 1], [5, -5])

  const muovi = (e: PointerEvent<HTMLElement>) => {
    if (e.pointerType === 'touch' || !arte.current) return
    const r = arte.current.getBoundingClientRect()
    px.set((e.clientX - r.left) / r.width)
    py.set((e.clientY - r.top) / r.height)
    acceso.set(1)
  }
  const esci = () => {
    acceso.set(0)
    px.set(0.5)
    py.set(0.35)
  }

  return (
    <article className="q-tela q-tela--arco q-carta l-fazione l-rivela" onPointerMove={muovi} onPointerLeave={esci}>
      <div className="q-tela__in">
        <span ref={arte} className="q-carta__arte l-fazione__arte">
          <m.img
            src={f.image}
            alt={f.alt}
            width={1024}
            height={427}
            loading="lazy"
            decoding="async"
            style={reduced ? { x: 0, y: 0, scale: 1 } : { x: imgX, y: imgY, scale: 1.06 }}
          />
          <m.span className="l-fazione__luce" aria-hidden="true" style={{ background: alone, opacity: luce }} />
        </span>
        <span className="q-divisorio" />
        <div className="l-fazione__corpo">
          <h3 className="l-fazione__nome">{f.name}</h3>
          <p className="l-testo">{f.text}</p>
          <ul className="l-sottofazioni" aria-label={`Sottofazioni: ${f.name}`}>
            {f.subfactions.map((s) => (
              <li key={s.name}>
                <span className="q-emblema" aria-hidden="true" style={{ '--c': s.color, '--cd': s.deep } as CSSProperties}>
                  {s.short}
                </span>
                {s.name}
              </li>
            ))}
          </ul>
        </div>
      </div>
      <span className="q-borchia q-borchia--n" />
    </article>
  )
}
