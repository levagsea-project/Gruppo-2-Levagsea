// Bottone grande esagonale GIOCA + frasetta a destra.
export default function PlayButton() {
  return (
    <div className="play-row">
      <span className="play-dot" />
      <button className="play-btn">
        <span className="play-star">✦</span> GIOCA
      </button>
      <span className="play-dot" />
      <p className="play-hint">Accedi al gioco per iniziare una partita.</p>
    </div>
  )
}
