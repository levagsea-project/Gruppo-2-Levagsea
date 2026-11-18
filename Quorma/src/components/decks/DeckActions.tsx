import HexButton from '../ui/HexButton'

// I bottoni in fondo al pannello. GIOCA apre la pagina Gioca, ELIMINA cancella; gli altri non fanno ancora nulla.
export default function DeckActions({ onPlay, onDelete }: { onPlay: () => void; onDelete: () => void }) {
  return (
    <div className="deck-actions">
      <div className="deck-play">
        <span className="play-dot" />
        <HexButton variant="primary" icon="✦" onClick={onPlay}>GIOCA</HexButton>
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
