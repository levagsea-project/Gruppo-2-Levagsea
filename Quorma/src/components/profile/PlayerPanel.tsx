import { currentPlayer } from '../../data/player'

// Scheda a sinistra della panoramica: avatar con le iniziali, nome e livello.
// Livello ed esperienza non esistono ancora nel backend: al posto c'è "Livelli in arrivo".
export default function PlayerPanel({ onEdit }: { onEdit: () => void }) {
  const player = currentPlayer()

  return (
    <section className="q-tela q-tela--arco pf-player" aria-label="Il tuo profilo">
      <div className="q-tela__in">
        <span className="pf-avatar" aria-hidden="true">{player.initials}</span>
        <h2 className="pf-player-name">{player.name}</h2>
        <p className="pf-level pf-level--vuoto">Livelli in arrivo</p>
        <button type="button" className="q-btn q-btn--acciaio" onClick={onEdit}>
          <span className="q-btn__in">Modifica profilo</span>
        </button>
      </div>
    </section>
  )
}
