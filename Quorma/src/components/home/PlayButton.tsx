import { useId } from 'react'
import type { PlayNotice } from '../../hooks/usePlayStatus'

type Props = {
  onClick: () => void
  notice: PlayNotice // frase accanto al sigillo: cosa fare, oppure perché non si può ancora giocare
  onNoticeAction?: (action: NonNullable<PlayNotice['action']>) => void
}

const ACTION_LABEL: Record<NonNullable<PlayNotice['action']>, string> = {
  mazzi: 'Vai ai mazzi',
  riprova: 'Riprova',
}

// Il sigillo GIOCA del design system + frase a destra.
// Se non puoi giocare il sigillo ha il filo color brace e la frase offre dove sistemare; GIOCA porta
// comunque alla pagina Gioca, dove "Inizia partita" resta disattivato.
export default function PlayButton({ onClick, notice, onNoticeAction }: Props) {
  const hintId = useId()
  const blocked = notice.tone === 'bloccato'
  const action = notice.action

  return (
    <div className={`play-row play-row--${notice.tone}`}>
      <button
        type="button"
        className={`q-btn q-btn--sigillo ${blocked ? 'q-btn--sigillo-bloccato' : ''}`}
        onClick={onClick}
        aria-describedby={hintId}
      >
        <span className="q-btn__in">
          <svg className="q-stella" aria-hidden="true" viewBox="0 0 20 28">
            <path d="M10 0l2.2 11.6L20 14l-7.8 2.4L10 28l-2.2-11.6L0 14l7.8-2.4z" fill={blocked ? '#FFD7B0' : '#8FD6DB'} />
          </svg>
          Gioca
        </span>
        <span className="q-borchia q-borchia--w" aria-hidden="true" />
        <span className="q-borchia q-borchia--e" aria-hidden="true" />
      </button>
      <p className="play-hint" id={hintId} aria-live="polite">
        <span>{notice.text}</span>
        {action && onNoticeAction && (
          <button type="button" className="play-hint__azione" onClick={() => onNoticeAction(action)}>
            {ACTION_LABEL[action]}
            <svg aria-hidden="true" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M5 12h14" />
              <path d="m13 6 6 6-6 6" />
            </svg>
          </button>
        )}
      </p>
    </div>
  )
}
