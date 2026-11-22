// Campo di ricerca con la lente, in cima a FAQ e Glossario.
type Props = { value: string; onChange: (v: string) => void; placeholder: string; label: string }

export default function SearchBox({ value, onChange, placeholder, label }: Props) {
  return (
    <div className="info-cerca">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
        <circle cx="11" cy="11" r="7" />
        <path d="m20 20-4-4" />
      </svg>
      <input type="search" value={value} placeholder={placeholder} aria-label={label} onChange={(e) => onChange(e.target.value)} />
    </div>
  )
}
