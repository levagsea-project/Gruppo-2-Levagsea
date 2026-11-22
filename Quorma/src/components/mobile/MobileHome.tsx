import type { CSSProperties } from 'react'
import { factions } from '../../data/factions'
import type { Page } from '../../data/navItems'
import { currentPlayer } from '../../data/player'
import type { LoadState } from '../../hooks/useDecks'
import type { PlayNotice } from '../../hooks/usePlayStatus'
import type { Deck } from '../../types/deck'
import { resultLabel, type Series } from '../../data/profile'
import { formatWhen } from '../../lib/matches'

const Star = () => (
  <svg className="q-stella" aria-hidden="true" viewBox="0 0 20 28">
    <path d="M10 0l2.2 11.6L20 14l-7.8 2.4L10 28l-2.2-11.6L0 14l7.8-2.4z" fill="#8FD6DB" />
  </svg>
)

// Titolo di sezione con la linea azzurra; id serve ad aria-labelledby della sezione.
function Marker({ id, title }: { id: string; title: string }) {
  return (
    <div className="q-marker">
      <h2 id={id} className="q-marker__title">{title}</h2>
      <span className="q-marker__line" aria-hidden="true" />
    </div>
  )
}

type Props = {
  lastDeck: Deck | null
  decksState: LoadState // caricamento dei mazzi: finché non è "ready" non sappiamo se ne hai
  onRetry: () => void
  notice: PlayNotice // perché non si può ancora giocare (mostrato solo se non è "info")
  onNoticeAction: (action: NonNullable<PlayNotice['action']>) => void
  onNavigate: (page: Page) => void
  latestSeries: Series | null // ultima serie giocata
  onOpenMatches: () => void // profilo, scheda Partite
}

const ACTION_LABEL: Record<NonNullable<PlayNotice['action']>, string> = { mazzi: 'Vai ai mazzi', riprova: 'Riprova' }

// Home su telefono: riprendi a giocare, partite, scorciatoie.
export default function MobileHome({ lastDeck, decksState, onRetry, notice, onNoticeAction, onNavigate, latestSeries, onOpenMatches }: Props) {
  const action = notice.action
  const player = currentPlayer()
  const faction = lastDeck ? factions[lastDeck.faction] : factions.artificiali
  const ready = decksState === 'ready'

  // Testi della card in alto: caricamento, errore, mazzo scelto oppure nessun mazzo.
  const label = !ready ? 'Mazzi' : lastDeck ? 'Mazzo selezionato' : 'Nessun mazzo'
  const title = decksState === 'loading' ? 'Caricamento…' : decksState === 'error' ? 'Mazzi non disponibili' : lastDeck ? lastDeck.name : 'Il tuo primo mazzo'
  const sub =
    decksState === 'loading'
      ? 'Sto leggendo i tuoi mazzi'
      : decksState === 'error'
        ? 'Non riesco a caricarli: riprova'
        : lastDeck
          ? `${faction.name} · ${lastDeck.cards} / ${lastDeck.maxCards}`
          : 'Scegli una fazione e costruiscilo'

  return (
    <div className="home-mobile">
      <div className="mb-targa">
        <h1>Bentornato</h1>
        <span className="mb-nome">{player.name}</span>
      </div>

      {/* Riprendi a giocare: il mazzo scelto, oppure l'invito a crearne uno */}
      <section className="q-tela q-tela--arco q-carta q-acceso" style={{ '--arte-h': '190px' } as CSSProperties} aria-labelledby="h-gioca" aria-busy={decksState === 'loading'}>
        <div className="q-tela__in">
          <span className="q-carta__arte" style={{ backgroundImage: `url(${faction.cover})`, backgroundPosition: faction.coverFocus }} />
          <span className="q-divisorio" />
          <div className="mbh-gioca">
            <div className="mbh-mazzo" aria-live="polite">
              <span className="mb-etichetta">{label}</span>
              <h2 id="h-gioca">{title}</h2>
              <span className="mbh-sotto">{sub}</span>
              {ready && lastDeck && notice.tone !== 'info' && (
                <span className="mbh-avviso">
                  {notice.text}
                  {action && (
                    <button type="button" className="play-hint__azione" onClick={() => onNoticeAction(action)}>
                      {ACTION_LABEL[action]}
                    </button>
                  )}
                </span>
              )}
            </div>
            {decksState === 'error' && (
              <button type="button" className="q-btn q-btn--argento" onClick={onRetry}>
                <span className="q-btn__in">Riprova</span>
              </button>
            )}
            {ready && lastDeck && (
              <button type="button" className="q-btn q-btn--sigillo" onClick={() => onNavigate('gioca')}>
                <span className="q-btn__in">
                  <Star />
                  Gioca
                </span>
                <span className="q-borchia q-borchia--w" />
                <span className="q-borchia q-borchia--e" />
              </button>
            )}
            {ready && !lastDeck && (
              <button type="button" className="q-btn q-btn--argento" onClick={() => onNavigate('nuovo-mazzo')}>
                <span className="q-btn__in">Crea mazzo</span>
              </button>
            )}
          </div>
        </div>
        <span className="q-borchia q-borchia--n" aria-hidden="true" />
      </section>

      <section className="mbh-sezione" aria-labelledby="h-partite">
        <Marker id="h-partite" title="Partite" />
        <div className="q-tela">
          {latestSeries ? (
            // L'ultima serie: tocca per vederla nel profilo
            <button type="button" className="q-tela__in mbh-partita mbh-partita--link" onClick={onOpenMatches}>
              <span className={`mbh-esito is-${latestSeries.result}`}>{resultLabel[latestSeries.result]}</span>
              <span className="mbh-avversario">
                <span>contro {latestSeries.opponent}</span>
                <small>{latestSeries.mode} · {latestSeries.won}–{latestSeries.lost} · {formatWhen(latestSeries.finishedAt)}</small>
              </span>
              <svg className="mbh-freccia" aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="m9 6 6 6-6 6" />
              </svg>
            </button>
          ) : (
            <div className="q-tela__in mbh-partita">
              <span className="mbh-avversario">
                <span>Nessuna partita giocata</span>
                <small>Qui vedrai l'esito della tua ultima serie.</small>
              </span>
            </div>
          )}
        </div>
      </section>

      <section className="mbh-scorciatoie" aria-label="Scorciatoie">
        <button type="button" className="q-btn q-btn--acciaio" onClick={() => onNavigate('negozio')}>
          <span className="q-btn__in">Negozio</span>
        </button>
        <button type="button" className="q-btn q-btn--acciaio" onClick={() => onNavigate('regole')}>
          <span className="q-btn__in">Regole</span>
        </button>
      </section>
    </div>
  )
}
