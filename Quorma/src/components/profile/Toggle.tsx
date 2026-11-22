type Props = { checked: boolean; label: string; onChange?: (value: boolean) => void; disabled?: boolean }

// Interruttore acceso/spento.
export default function Toggle({ checked, label, onChange, disabled = false }: Props) {
  return (
    <button type="button" role="switch" aria-checked={checked} aria-label={label} className="pf-toggle" disabled={disabled} onClick={() => onChange?.(!checked)}>
      <span />
    </button>
  )
}
