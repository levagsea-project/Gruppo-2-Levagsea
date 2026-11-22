import { useEffect, useRef, type MouseEvent, type PointerEvent } from 'react'
import { formatCredits as fmt } from '../../lib/balance'
import '../../styles/away.css'

type Props = {
  qty: number
  packLabel: string
  factionName: string
  total: number
  credits: number | null // ultimo saldo noto; null = non si sa
  onCancel: () => void
  onConfirm: () => void
}

// Conferma prima di comprare più buste insieme. Il focus parte da Annulla: Invio, Esc o un tocco fuori non spendono nulla.
export default function BuyConfirm({ qty, packLabel, factionName, total, credits, onCancel, onConfirm }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const cancelRef = useRef<HTMLButtonElement>(null)
  const downOnBackdrop = useRef(false)

  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null
    const dialog = dialogRef.current
    if (dialog && !dialog.open) dialog.showModal()
    cancelRef.current?.focus()
    return () => opener?.focus?.()
  }, [])

  const short = credits !== null && total > credits

  return (
    <dialog
      ref={dialogRef}
      className="away shop-conferma"
      aria-labelledby="acquisto-titolo"
      aria-describedby="acquisto-testo"
      onCancel={(e) => {
        e.preventDefault()
        onCancel()
      }}
      onPointerDown={(e: PointerEvent<HTMLDialogElement>) => (downOnBackdrop.current = e.target === dialogRef.current)}
      onClick={(e: MouseEvent<HTMLDialogElement>) => e.target === dialogRef.current && downOnBackdrop.current && onCancel()}
    >
      <div className="q-tela q-tela--arco">
        <div className="q-tela__in">
          <h2 id="acquisto-titolo">Acquistare {qty} buste?</h2>
          <p id="acquisto-testo" className="away__lead">{packLabel} · {factionName}. Le buste si aprono con l'acquisto, che non si può annullare.</p>
          <dl className="shop-conti">
            <div><dt>Totale</dt><dd>{fmt(total)} Valuta 2</dd></div>
            {credits !== null && (
              <div className="shop-conti__dopo"><dt>Dopo l'acquisto</dt><dd>{short ? 'Saldo noto insufficiente' : `${fmt(credits - total)} Valuta 2`}</dd></div>
            )}
          </dl>
          <div className="away__azioni">
            <button ref={cancelRef} type="button" className="q-btn q-btn--acciaio" onClick={onCancel}>
              <span className="q-btn__in">Annulla</span>
            </button>
            <button type="button" className="q-btn q-btn--argento" onClick={onConfirm}>
              <span className="q-btn__in">Acquista · {fmt(total)}</span>
            </button>
          </div>
        </div>
      </div>
    </dialog>
  )
}
