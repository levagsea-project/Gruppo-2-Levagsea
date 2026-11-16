// La carta "+ NUOVO MAZZO": porta alla schermata di creazione.
export default function NewDeckCard({ onClick }: { onClick: () => void }) {
  return (
    <button className="deck-card new-deck-card" onClick={onClick}>
      <span className="deck-diamond" />
      <span className="new-deck-plus">+</span>
      <span className="new-deck-title">
        NUOVO
        <br />
        MAZZO
      </span>
      <span className="new-deck-sub">32 CARTE</span>
    </button>
  )
}
