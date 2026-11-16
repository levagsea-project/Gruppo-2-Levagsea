import ProgressBar from '../ui/ProgressBar'

// Riquadro del giocatore in basso a sinistra.
export default function PlayerCard() {
  return (
    <div className="player-card">
      <div className="player-top">
        <div className="player-avatar">GI</div>
        <div>
          <div className="player-name">Giocatore 01</div>
          <div className="player-level">◆ LIVELLO 12</div>
        </div>
      </div>
      <ProgressBar value={1280} max={2000} />
      <div className="player-xp">
        <span>1.280 / 2.000 XP</span>
        <span>Liv. 13</span>
      </div>
    </div>
  )
}
