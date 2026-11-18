import type { CSSProperties } from 'react'
import { player } from '../../data/player'

// Riquadro del giocatore in basso a sinistra: avatar nell'ogiva, livello e barra dell'esperienza.
export default function PlayerCard() {
  const percent = Math.round((player.xp / player.xpNext) * 100)

  return (
    <div className="q-profilo">
      <span className="q-profilo__in">
        <span className="q-profilo__testa">
          <span className="q-profilo__avatar" aria-hidden="true">
            <span className="q-ogiva">
              <span className="q-ogiva__in">{player.initials}</span>
            </span>
          </span>
          <span className="q-profilo__dati">
            <span className="q-profilo__nome">{player.name}</span>
            <span className="q-profilo__liv">Livello {player.level}</span>
          </span>
        </span>
        <span className="q-profilo__xp">
          <span className="q-xp" style={{ '--xp': `${percent}%` } as CSSProperties}>
            <span
              className="q-xp__bar"
              role="progressbar"
              aria-label={`Esperienza verso il livello ${player.level + 1}`}
              aria-valuenow={percent}
              aria-valuemin={0}
              aria-valuemax={100}
            >
              <span className="q-xp__segno" />
            </span>
          </span>
          <span className="q-xp__dati">
            <span>
              <b>{player.xp.toLocaleString('it-IT')}</b> / {player.xpNext.toLocaleString('it-IT')} XP
            </span>
            <span>Liv. {player.level + 1}</span>
          </span>
        </span>
      </span>
    </div>
  )
}
