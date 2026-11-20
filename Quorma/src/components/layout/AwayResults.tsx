import { useEffect, useRef, type MouseEvent, type PointerEvent } from 'react'
import type { Series } from '../../data/profile'
import { formatWhen } from '../../lib/matches'
import ResultLabel from '../profile/ResultLabel'
import '../../styles/profile.css' // colori degli esiti (pf-result)
import '../../styles/away.css'

type Props = { series: Series[]; onClose: () => void; onOpenProfile: () => void }

// "Mentre eri via": i risultati delle serie giocate mentre non c'eri (sfide accettate, matchmaking di altri
// giocatori che hanno scelto te). Si apre entrando nell'app se ce ne sono; chiudendola i risultati
// diventano letti (POST /matches/{id}/seen, in hooks/useMatchNews.ts) e non si ripresentano.
// Usa <dialog> del browser: blocca il resto, si chiude con Esc o toccando fuori, tiene il focus dentro.
export default function AwayResults({ series, onClose, onOpenProfile }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const downOnBackdrop = useRef(false)

  useEffect(() => {
    const dialog = dialogRef.current
    if (dialog && !dialog.open) dialog.showModal()
  }, [])

  const wins = series.filter((s) => s.result === 'vittoria').length
  const lead =
    series.length === 1
      ? 'Mentre non c’eri si è conclusa una tua serie.'
      : `Mentre non c’eri si sono concluse ${series.length} tue serie: ne hai vinte ${wins}.`

  return (
    <dialog
      ref={dialogRef}
      className="away"
      aria-labelledby="away-titolo"
      onCancel={(e) => {
        e.preventDefault()
        onClose()
      }}
      onPointerDown={(e: PointerEvent<HTMLDialogElement>) => (downOnBackdrop.current = e.target === dialogRef.current)}
      onClick={(e: MouseEvent<HTMLDialogElement>) => e.target === dialogRef.current && downOnBackdrop.current && onClose()}
    >
      <div className="q-tela q-tela--arco">
        <div className="q-tela__in">
          <h2 id="away-titolo">Mentre eri via</h2>
          <p className="away__lead">{lead}</p>

          <ul className="away__lista">
            {series.map((s) => (
              <li key={s.id}>
                <ResultLabel result={s.result} />
                <span>
                  <b>contro {s.opponent}</b>
                  <small>{s.mode} · {s.won}–{s.lost} · {formatWhen(s.finishedAt)}</small>
                </span>
              </li>
            ))}
          </ul>

          <div className="away__azioni">
            <button type="button" className="q-btn q-btn--acciaio" onClick={onClose}>
              <span className="q-btn__in">Chiudi</span>
            </button>
            <button type="button" className="q-btn q-btn--argento" onClick={onOpenProfile}>
              <span className="q-btn__in">Vedi le partite</span>
            </button>
          </div>
        </div>
      </div>
    </dialog>
  )
}
