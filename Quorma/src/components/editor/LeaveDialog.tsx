import { useEffect, useRef, type MouseEvent, type PointerEvent } from 'react'
import '../../styles/away.css'

type Props = { onStay: () => void; onLeave: () => void }

// Conferma prima di uscire dall'editor con modifiche non salvate. Il focus parte da "Continua a modificare":
// Invio, Esc o un tocco fuori non fanno perdere il lavoro.
export default function LeaveDialog({ onStay, onLeave }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const stayRef = useRef<HTMLButtonElement>(null)
  const downOnBackdrop = useRef(false)

  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null
    const dialog = dialogRef.current
    if (dialog && !dialog.open) dialog.showModal()
    stayRef.current?.focus()
    return () => opener?.focus?.()
  }, [])

  return (
    <dialog
      ref={dialogRef}
      className="away"
      aria-labelledby="uscita-titolo"
      aria-describedby="uscita-testo"
      onCancel={(e) => {
        e.preventDefault()
        onStay()
      }}
      onPointerDown={(e: PointerEvent<HTMLDialogElement>) => (downOnBackdrop.current = e.target === dialogRef.current)}
      onClick={(e: MouseEvent<HTMLDialogElement>) => e.target === dialogRef.current && downOnBackdrop.current && onStay()}
    >
      <div className="q-tela q-tela--arco">
        <div className="q-tela__in">
          <h2 id="uscita-titolo">Uscire senza salvare?</h2>
          <p id="uscita-testo" className="away__lead">Le modifiche a questo mazzo andranno perse.</p>
          <div className="away__azioni away__azioni--colonna">
            <button ref={stayRef} type="button" className="q-btn q-btn--argento" onClick={onStay}>
              <span className="q-btn__in">Continua a modificare</span>
            </button>
            <button type="button" className="q-btn q-btn--acciaio" onClick={onLeave}>
              <span className="q-btn__in">Esci senza salvare</span>
            </button>
          </div>
        </div>
      </div>
    </dialog>
  )
}
