import type { Page } from '../../data/navItems'
import type { Deck, PlayStatus } from '../../types/deck'
import type { Series } from '../../data/profile'
import type { LoadState } from '../../hooks/useDecks'
import { playNotice, type PlayNotice } from '../../hooks/usePlayStatus'
import MobileHome from '../mobile/MobileHome'
import HeroText from './HeroText'
import InfoCards from './InfoCards'

type Props = {
  decks: Deck[]
  lastDeck: Deck | null
  decksState: LoadState
  onRetryDecks: () => void
  playStatus: PlayStatus | null
  playStatusFailed: boolean // il controllo "posso giocare?" non è riuscito (backend non raggiungibile)
  onRetryPlayStatus: () => void
  onNavigate: (page: Page) => void
  latestSeries: Series | null // ultima serie giocata (home su telefono)
  onOpenMatches: () => void // profilo, scheda Partite
}

// Rosone di QUORMA dietro al testo: solo decorazione, ruota lentissimo.
function Rose() {
  return (
    <svg className="home-rosone" aria-hidden="true" viewBox="0 0 800 800" fill="none" stroke="rgba(160,215,220,0.13)" strokeWidth="1">
      <circle cx="400" cy="400" r="390" />
      <circle cx="400" cy="400" r="330" />
      <circle cx="400" cy="400" r="250" />
      <circle cx="400" cy="400" r="150" />
      <circle cx="400" cy="400" r="70" />
      <path d="M400 10v780M10 400h780M124 124l552 552M676 124 124 676M250 52l300 696M550 52 250 748M52 250l696 300M52 550l696-300" />
      {[[400, 70], [400, 730], [70, 400], [730, 400], [167, 167], [633, 167], [167, 633], [633, 633]].map(([cx, cy]) => (
        <circle key={`${cx}-${cy}`} cx={cx} cy={cy} r="80" />
      ))}
    </svg>
  )
}

// La pagina Home, dal design "Home-Quorma": sfondo con velo e griglia, rosone,
// "Pronto a giocare?" con il sigillo GIOCA e tre card ad arco in fondo (mazzi di difesa, scambi, news).
// Su telefono si vede invece MobileHome (il cambio lo fa styles/mobile.css).
export default function HomePage(props: Props) {
  const { decks, lastDeck, decksState, onRetryDecks, playStatus, playStatusFailed, onRetryPlayStatus, onNavigate, latestSeries, onOpenMatches } = props
  // Frase accanto a GIOCA: cosa fare, perché non si può giocare, oppure che il controllo non è riuscito.
  const notice = playNotice(playStatus, playStatusFailed)
  const onNoticeAction = (action: NonNullable<PlayNotice['action']>) =>
    action === 'mazzi' ? onNavigate('mazzi') : onRetryPlayStatus()

  return (
    <main className="home">
      <div className="home-desktop">
        {/* Decorazioni in un livello ritagliato: non allungano lo scorrimento quando lo schermo è basso */}
        <div className="home-decoro" aria-hidden="true">
          <div className="home-griglia" />
          <Rose />
        </div>
        <section className="home-santuario">
          <HeroText onPlay={() => onNavigate('gioca')} notice={notice} onNoticeAction={onNoticeAction} />
        </section>
        <InfoCards decks={decks} decksState={decksState} onManageDecks={() => onNavigate('mazzi')} onTrades={() => onNavigate('scambi')} />
      </div>
      <MobileHome
        lastDeck={lastDeck}
        decksState={decksState}
        onRetry={onRetryDecks}
        notice={notice}
        onNoticeAction={onNoticeAction}
        onNavigate={onNavigate}
        latestSeries={latestSeries}
        onOpenMatches={onOpenMatches}
      />
    </main>
  )
}
