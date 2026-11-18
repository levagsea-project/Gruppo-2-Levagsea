import type { CSSProperties } from 'react'
import { factions } from '../../data/factions'
import type { Page } from '../../data/navItems'
import { lastMatch, pending, player } from '../../data/player'
import type { Deck } from '../../types/deck'
import NavIcon from '../layout/NavIcon'
import Arch from './Arch'
import MobileRow from './MobileRow'

const Star = () => (
  <svg className="q-stella" aria-hidden="true" viewBox="0 0 20 28">
    <path d="M10 0l2.2 11.6L20 14l-7.8 2.4L10 28l-2.2-11.6L0 14l7.8-2.4z" fill="#8FD6DB" />
  </svg>
)

// Titolo di sezione con la linea azzurra.
function Marker({ title, extra }: { title: string; extra?: React.ReactNode }) {
  return (
    <div className="q-marker">
      <h2 className="q-marker__title">{title}</h2>
      <span className="q-marker__line" />
      {extra}
    </div>
  )
}

type Props = { lastDeck: Deck | null; onNavigate: (page: Page) => void }

// Home su telefono: riprendi a giocare, cose in attesa, ultima partita, scorciatoie.
export default function MobileHome({ lastDeck, onNavigate }: Props) {
  const faction = lastDeck ? factions[lastDeck.faction] : factions.artificiali

  return (
    <div className="home-mobile">
      <div className="mb-targa">
        <h1>Bentornato</h1>
        <span>
          {player.name} · Livello {player.level}
        </span>
      </div>

      {/* Riprendi a giocare: l'ultimo mazzo, oppure l'invito a crearne uno */}
      <section className="q-tela q-tela--arco q-carta q-acceso" style={{ '--arte-h': '190px' } as CSSProperties} aria-labelledby="h-gioca">
        <div className="q-tela__in">
          <span className="q-carta__arte" style={{ backgroundImage: `url(${faction.image})` }} />
          <span className="q-divisorio" />
          <div className="mbh-gioca">
            <div className="mbh-mazzo">
              <span className="mb-etichetta">{lastDeck ? 'Ultimo mazzo usato' : 'Nessun mazzo'}</span>
              <h2 id="h-gioca">{lastDeck ? lastDeck.name : 'Il tuo primo mazzo'}</h2>
              <span className="mbh-sotto">
                {lastDeck ? `${faction.name} · ${lastDeck.cards} / ${lastDeck.maxCards}` : 'Scegli una fazione e costruiscilo'}
              </span>
            </div>
            {lastDeck ? (
              <button type="button" className="q-btn q-btn--sigillo">
                <span className="q-btn__in">
                  <Star />
                  Gioca
                </span>
                <span className="q-borchia q-borchia--w" />
                <span className="q-borchia q-borchia--e" />
              </button>
            ) : (
              <button type="button" className="q-btn q-btn--argento" onClick={() => onNavigate('nuovo-mazzo')}>
                <span className="q-btn__in">Crea mazzo</span>
              </button>
            )}
          </div>
        </div>
        <span className="q-borchia q-borchia--n" />
      </section>

      <section className="mbh-sezione" aria-labelledby="h-attesa">
        <Marker title="In attesa" />
        <div className="q-tela">
          <div className="q-tela__in mb-lista">
            {pending.map((p) => (
              <MobileRow key={p.id} title={p.label} detail={p.detail} icon={<Arch alert><NavIcon name={p.icon} /></Arch>} />
            ))}
          </div>
        </div>
      </section>

      <section className="mbh-sezione" aria-label="Ultima partita">
        <Marker title="Ultima partita" extra={<span className="mbh-storico">Storico</span>} />
        <div className="q-tela">
          <div className="q-tela__in mbh-partita">
            <span className="mbh-esito">{lastMatch.result}</span>
            <span className="mbh-avversario">
              <span>contro {lastMatch.opponent}</span>
              <small>{lastMatch.detail}</small>
            </span>
          </div>
        </div>
      </section>

      <section className="mbh-scorciatoie" aria-label="Scorciatoie">
        <button type="button" className="q-btn q-btn--acciaio">
          <span className="q-btn__in">Negozio</span>
        </button>
        <button type="button" className="q-btn q-btn--acciaio" onClick={() => onNavigate('regole')}>
          <span className="q-btn__in">Regole</span>
        </button>
      </section>
    </div>
  )
}
