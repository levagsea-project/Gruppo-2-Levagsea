import type { CSSProperties } from 'react'
import { factions } from '../../data/factions'
import type { Page } from '../../data/navItems'
import { comingSoon, currentPlayer } from '../../data/player'
import type { LoadState } from '../../hooks/useDecks'
import type { PlayNotice } from '../../hooks/usePlayStatus'
import type { Deck } from '../../types/deck'
import NavIcon from '../layout/NavIcon'
import Arch from './Arch'
import MobileRow from './MobileRow'

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
}

const ACTION_LABEL: Record<NonNullable<PlayNotice['action']>, string> = { mazzi: 'Vai ai mazzi', riprova: 'Riprova' }

// Home su telefono: riprendi a giocare, sezioni in arrivo, partite, scorciatoie.
export default function MobileHome({ lastDeck, decksState, onRetry, notice, onNoticeAction, onNavigate }: Props) {
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

      {/* Sezioni del gioco che non esistono ancora: cosa saranno, senza numeri inventati */}
      <section className="mbh-sezione" aria-labelledby="h-arrivo">
        <Marker id="h-arrivo" title="In arrivo" />
        <div className="q-tela">
          <div className="q-tela__in mb-lista">
            {comingSoon.map((p) => (
              <MobileRow key={p.id} title={p.label} detail={p.detail} icon={<Arch><NavIcon name={p.icon} /></Arch>} />
            ))}
          </div>
        </div>
      </section>

      <section className="mbh-sezione" aria-labelledby="h-partite">
        <Marker id="h-partite" title="Partite" />
        <div className="q-tela">
          <div className="q-tela__in mbh-partita">
            <span className="mbh-avversario">
              <span>Nessuna partita giocata</span>
              <small>Qui vedrai l'esito delle tue ultime partite.</small>
            </span>
          </div>
        </div>
      </section>

      <section className="mbh-scorciatoie" aria-label="Scorciatoie">
        <button type="button" className="q-btn q-btn--acciaio" aria-disabled="true" title="Negozio · in arrivo">
          <span className="q-btn__in">
            Negozio<span className="sr-only">, in arrivo</span>
          </span>
        </button>
        <button type="button" className="q-btn q-btn--acciaio" onClick={() => onNavigate('regole')}>
          <span className="q-btn__in">Regole</span>
        </button>
      </section>
    </div>
  )
}
