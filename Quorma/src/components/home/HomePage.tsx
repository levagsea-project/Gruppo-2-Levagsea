import type { Page } from '../../data/navItems'
import type { Deck } from '../../types/deck'
import CurrencyBar from '../layout/CurrencyBar'
import MobileHome from '../mobile/MobileHome'
import HeroText from './HeroText'
import InfoCards from './InfoCards'

type Props = { lastDeck: Deck | null; onNavigate: (page: Page) => void }

// La pagina Home: sfondo (in CSS) + valute + testo + card.
// Su telefono si vede invece MobileHome (il cambio lo fa styles/mobile.css).
export default function HomePage({ lastDeck, onNavigate }: Props) {
  return (
    <main className="home">
      <div className="home-desktop">
        <CurrencyBar />
        <HeroText onPlay={() => onNavigate('gioca')} />
        <InfoCards />
      </div>
      <MobileHome lastDeck={lastDeck} onNavigate={onNavigate} />
    </main>
  )
}
