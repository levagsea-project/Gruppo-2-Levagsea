import { useNavigate } from 'react-router'
import { currentPlayer } from '../../data/player'
import { clearSession } from '../../lib/auth'
import ProfileCard from './ProfileCard'

// Scheda a sinistra della panoramica: "Modifica profilo" e, sotto, Esci (come nella pagina Altro del telefono).
export default function PlayerPanel({ onEdit }: { onEdit: () => void }) {
  const player = currentPlayer()
  const navigate = useNavigate()

  // Esci: cancella l'accesso e torna alla landing. Dal profilo non c'è un editor aperto, quindi nulla da confermare.
  const logout = () => {
    clearSession()
    navigate('/')
  }

  return (
    <ProfileCard name={player.name} image={player.avatar.image}>
      <button type="button" className="q-btn q-btn--acciaio" onClick={onEdit}>
        <span className="q-btn__in">Modifica profilo</span>
      </button>
      <button type="button" className="pf-esci" onClick={logout}>
        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
          <path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4" />
          <path d="M10 16l-4-4 4-4M6 12h10" />
        </svg>
        Esci
      </button>
    </ProfileCard>
  )
}
