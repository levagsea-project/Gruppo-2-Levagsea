import { useEffect, useRef, type CSSProperties, type MouseEvent, type PointerEvent, type ReactNode } from 'react'

type Rule = { ok: boolean; text: string }
type Props = {
  total: number
  size: number
  rules: Rule[]
  canSave: boolean
  saving: boolean
  onSave: () => void
  onClose: () => void
  children: ReactNode // i pezzi del mazzo, gli stessi della colonna su desktop
}

// "Il tuo mazzo" su telefono: pannello dal basso con regole, pezzi e Salva sempre a portata.
// Usa <dialog> del browser: blocca il resto, si chiude con Esc, con la X o toccando fuori.
export default function DeckSheet({ total, size, rules, canSave, saving, onSave, onClose, children }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const downOnBackdrop = useRef(false)

  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null
    const dialog = dialogRef.current
    if (dialog && !dialog.open) dialog.showModal()
    // ruotando o allargando la finestra il mazzo torna nella sua colonna: il pannello non serve più
    const wide = window.matchMedia('(min-width: 768px)')
    const closeIfWide = () => wide.matches && onClose()
    wide.addEventListener('change', closeIfWide)
    return () => {
      wide.removeEventListener('change', closeIfWide)
      opener?.focus?.()
    }
  }, [onClose])

  return (
    <dialog
      ref={dialogRef}
      className="editor-foglio"
      aria-labelledby="e-foglio-titolo"
      onCancel={(e) => {
        e.preventDefault()
        onClose()
      }}
      onPointerDown={(e: PointerEvent<HTMLDialogElement>) => (downOnBackdrop.current = e.target === dialogRef.current)}
      onClick={(e: MouseEvent<HTMLDialogElement>) => e.target === dialogRef.current && downOnBackdrop.current && onClose()}
    >
      <div className="editor-foglio__testa">
        <div className="editor-foglio__titolo">
          <h2 id="e-foglio-titolo">Il tuo mazzo</h2>
          <span>{total}/{size}</span>
          <button type="button" className="editor-foglio__chiudi" aria-label="Chiudi il mazzo" onClick={onClose}>
            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
              <path d="M6 6l12 12M18 6 6 18" />
            </svg>
          </button>
        </div>
        <span className="q-xp" style={{ '--xp': `${Math.min(100, (total / size) * 100)}%` } as CSSProperties}>
          <span className="q-xp__bar" role="progressbar" aria-label="Carte nel mazzo" aria-valuenow={total} aria-valuemin={0} aria-valuemax={size} />
        </span>
        <div className="editor-foglio__regole" aria-live="polite">
          {rules.map((r) => (
            <span key={r.text} className={`m-regola ${r.ok ? 'm-ok' : ''}`}>{r.text}</span>
          ))}
        </div>
      </div>

      <div className="editor-foglio__corpo" tabIndex={0} aria-label="Carte nel mazzo">
        {children}
      </div>

      <div className="editor-foglio__piede">
        <button type="button" className="q-btn q-btn--argento" aria-disabled={!canSave || saving} onClick={onSave}>
          <span className="q-btn__in">{saving ? 'Salvataggio…' : 'Salva mazzo'}</span>
        </button>
      </div>
    </dialog>
  )
}
