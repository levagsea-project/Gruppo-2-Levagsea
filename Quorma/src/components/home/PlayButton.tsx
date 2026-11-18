// Bottone grande esagonale GIOCA + frasetta a destra.
export default function PlayButton({ onClick }: { onClick: () => void }) {
  return (
    <div className="play-row">
      <span className="play-dot" />
      <button className="play-btn" onClick={onClick}>
        <span className="play-star">✦</span> GIOCA
      </button>
      <span className="play-dot" />
      <p className="play-hint">Accedi al gioco per iniziare una partita.</p>
    </div>
  )
}
