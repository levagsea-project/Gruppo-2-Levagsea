import HexButton from '../ui/HexButton'

// I bottoni in fondo al pannello. Solo ELIMINA fa qualcosa, per ora.
export default function DeckActions({ onDelete }: { onDelete: () => void }) {
  return (
    <div className="deck-actions">
      <div className="deck-play">
        <span className="play-dot" />
        <HexButton variant="primary" icon="✦">GIOCA</HexButton>
        <span className="play-dot" />
      </div>
      <HexButton variant="light">MODIFICA</HexButton>
      <div className="deck-actions-row">
        <HexButton>DUPLICA</HexButton>
        <HexButton onClick={onDelete}>ELIMINA</HexButton>
      </div>
    </div>
  )
}
