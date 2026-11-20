import type { CSSProperties } from 'react'

type Props = { value: number; label: string; onChange: (value: number) => void }

// Cursore da 0 a 100 con il numero a destra.
export default function Slider({ value, label, onChange }: Props) {
  return (
    <span className="pf-slider-wrap">
      <input
        type="range"
        className="pf-slider"
        min={0}
        max={100}
        value={value}
        aria-label={label}
        style={{ '--v': `${value}%` } as CSSProperties}
        onChange={(e) => onChange(Number(e.target.value))}
      />
      <output>{value}</output>
    </span>
  )
}
