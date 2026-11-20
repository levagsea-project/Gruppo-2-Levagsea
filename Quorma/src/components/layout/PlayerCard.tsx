import type { Page } from '../../data/navItems'
import { currentPlayer } from '../../data/player'

// Riquadro del giocatore in basso a sinistra: avatar nell'ogiva e nome di chi ha fatto l'accesso.
// Livello ed esperienza non esistono ancora nel backend: al posto della barra c'è "Livelli in arrivo".
type Props = { current: Page; onNavigate: (page: Page) => void }

// Cliccandolo si apre la pagina Profilo.
export default function PlayerCard({ current, onNavigate }: Props) {
  const player = currentPlayer()

  return (
    <a
      href="#"
      className="q-profilo"
      title={player.name}
      aria-label={`Apri il profilo di ${player.name}`}
      aria-current={current === 'profilo' ? 'page' : undefined}
      onClick={(e) => {
        e.preventDefault()
        onNavigate('profilo')
      }}
    >
      <span className="q-profilo__in">
        <span className="q-profilo__testa">
          <span className="q-profilo__avatar" aria-hidden="true">
            <span className="q-ogiva">
              <span className="q-ogiva__in">{player.initials}</span>
            </span>
          </span>
          <span className="q-profilo__dati">
            <span className="q-profilo__nome">{player.name}</span>
            <span className="q-profilo__liv q-profilo__liv--vuoto">Livelli in arrivo</span>
          </span>
        </span>
      </span>
    </a>
  )
}
