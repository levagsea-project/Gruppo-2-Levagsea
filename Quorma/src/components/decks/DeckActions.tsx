type Props = {
  onPlay: () => void
  onEdit: () => void
  onDuplicate: () => void
  onDelete: () => void
  canPlay: boolean
  canDelete: boolean
  editLabel: string
  duplicating: boolean
  busy: boolean // un'altra azione sul mazzo è in corso
  deleteHint?: string
}

const Star = () => (
  <svg className="q-stella" aria-hidden="true" viewBox="0 0 20 28">
    <path d="M10 0l2.2 11.6L20 14l-7.8 2.4L10 28l-2.2-11.6L0 14l7.8-2.4z" fill="#8FD6DB" />
  </svg>
)

// I pulsanti in fondo al pannello, come nel design: sigillo GIOCA (solo se il mazzo è usabile),
// Modifica (argento, apre l'editor), poi Duplica ed Elimina come semplici voci di testo.
export default function DeckActions({ onPlay, onEdit, onDuplicate, onDelete, canPlay, canDelete, editLabel, duplicating, busy, deleteHint }: Props) {
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
        <span className="q-btn__in">{editLabel}</span>
      </button>
      <div className="d-azioni">
        <button type="button" onClick={onDuplicate} disabled={busy} aria-busy={duplicating}>
          {duplicating ? 'Copia…' : 'Duplica'}
        </button>
        <button type="button" onClick={onDelete} disabled={!canDelete} aria-describedby={deleteHint ? 'deck-elimina-motivo' : undefined}>
          Elimina
        </button>
      </div>
      {/* scritto per esteso: un title non si vede al tocco */}
      {deleteHint && <p id="deck-elimina-motivo" className="deck-actions__motivo">{deleteHint}</p>}
    </div>
  )
}
