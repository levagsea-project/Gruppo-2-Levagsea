type Props = { onPlay: () => void; onEdit: () => void; onDelete: () => void; canPlay: boolean; canDelete: boolean; deleteHint?: string }

const Star = () => (
  <svg className="q-stella" aria-hidden="true" viewBox="0 0 20 28">
    <path d="M10 0l2.2 11.6L20 14l-7.8 2.4L10 28l-2.2-11.6L0 14l7.8-2.4z" fill="#8FD6DB" />
  </svg>
)

// I pulsanti in fondo al pannello, come nel design: sigillo GIOCA (solo se il mazzo è usabile),
// Modifica (argento, apre l'editor), Duplica ed Elimina (acciaio). Duplica non esiste ancora: "in arrivo".
export default function DeckActions({ onPlay, onEdit, onDelete, canPlay, canDelete, deleteHint }: Props) {
  return (
    <div className="deck-actions">
      {canPlay && (
        <button type="button" className="q-btn q-btn--sigillo deck-actions__gioca" onClick={onPlay}>
          <span className="q-btn__in">
            <Star />
            Gioca
          </span>
          <span className="q-borchia q-borchia--w" aria-hidden="true" />
          <span className="q-borchia q-borchia--e" aria-hidden="true" />
        </button>
      )}
      <button type="button" className="q-btn q-btn--argento" onClick={onEdit}>
        <span className="q-btn__in">Modifica</span>
      </button>
      <div className="deck-actions__riga">
        <button type="button" className="q-btn q-btn--acciaio" aria-disabled="true" title="Duplica · in arrivo">
          <span className="q-btn__in">
            Duplica<span className="sr-only">, in arrivo</span>
          </span>
        </button>
        <button type="button" className="q-btn q-btn--acciaio" onClick={onDelete} disabled={!canDelete} title={deleteHint}>
          <span className="q-btn__in">Elimina</span>
        </button>
      </div>
    </div>
  )
}
