import { currentPlayer } from '../../data/player'

// Riquadro del giocatore in basso a sinistra: avatar nell'ogiva e nome di chi ha fatto l'accesso.
// Livello ed esperienza non esistono ancora nel backend: al posto della barra c'è "Livelli in arrivo".
export default function PlayerCard() {
  const player = currentPlayer()

  return (
    <div className="q-profilo" title={player.name}>
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
    </div>
  )
}
