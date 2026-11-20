type Props = { checked: boolean; label: string; onChange: (value: boolean) => void }

// Interruttore acceso/spento.
export default function Toggle({ checked, label, onChange }: Props) {
  return (
    <button type="button" role="switch" aria-checked={checked} aria-label={label} className="pf-toggle" onClick={() => onChange(!checked)}>
      <span />
    </button>
  )
}
