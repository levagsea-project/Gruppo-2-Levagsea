type Props<T extends string> = { options: T[]; value: T; label: string; onChange: (value: T) => void }

// Scelta tra poche opzioni (es. Lenta / Normale / Veloce).
export default function Segmented<T extends string>({ options, value, label, onChange }: Props<T>) {
  return (
    <div className="pf-seg" role="radiogroup" aria-label={label}>
      {options.map((o) => (
        <button key={o} type="button" role="radio" aria-checked={o === value} onClick={() => onChange(o)}>
          {o}
        </button>
      ))}
    </div>
  )
}
