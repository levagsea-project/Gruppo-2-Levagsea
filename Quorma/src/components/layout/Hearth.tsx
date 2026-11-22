import type { CSSProperties } from 'react'

// Una fiammella o una favilla: posizione orizzontale + tempi dell'animazione.
type Flame = { left: string; delay?: string; duration?: string; dx?: string }

const css = ({ left, delay, duration, dx }: Flame) =>
  ({ left, animationDelay: delay, animationDuration: duration, '--dx': dx }) as CSSProperties

// "Focolare": il fuoco azzurro animato dietro l'icona attiva (e nel profilo).
export default function Hearth({ flames, sparks }: { flames: Flame[]; sparks: Flame[] }) {
  return (
    <span className="q-focolare" aria-hidden="true">
      <span className="q-focolare__base" />
      {flames.map((f, i) => (
        <span key={`l${i}`} className="q-focolare__lingua" style={css(f)} />
      ))}
      {sparks.map((s, i) => (
        <span key={`f${i}`} className="q-focolare__favilla" style={css(s)} />
      ))}
    </span>
  )
}
