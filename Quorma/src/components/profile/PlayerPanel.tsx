import { currentPlayer } from '../../data/player'
import ProfileCard from './ProfileCard'

// Scheda a sinistra della panoramica, con il bottone che apre "Modifica profilo".
export default function PlayerPanel({ onEdit }: { onEdit: () => void }) {
  const player = currentPlayer()

  return (
    <ProfileCard name={player.name} image={player.avatar.image}>
      <button type="button" className="q-btn q-btn--acciaio" onClick={onEdit}>
        <span className="q-btn__in">Modifica profilo</span>
      </button>
    </ProfileCard>
  )
}
