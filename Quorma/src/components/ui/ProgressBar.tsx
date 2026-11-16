type Props = { value: number; max: number }

// Barra azzurra: la larghezza è la percentuale value / max.
export default function ProgressBar({ value, max }: Props) {
  const percent = (value / max) * 100
  return (
    <div className="progress">
      <div className="progress-fill" style={{ width: `${percent}%` }} />
    </div>
  )
}
