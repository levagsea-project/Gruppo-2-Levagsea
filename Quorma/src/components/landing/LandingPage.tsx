import { SITE_URL, OG_IMAGE } from '../../lib/site'
import AuthDialog, { type AuthMode } from './AuthDialog'
import FactionsSection from './FactionsSection'
import FaqSection from './FaqSection'
import FinalCta from './FinalCta'
import GameSection from './GameSection'
import HeroSection from './HeroSection'
import HowToPlaySection from './HowToPlaySection'
import LandingFooter from './LandingFooter'
import LandingHeader from './LandingHeader'
import '../../styles/landing/quorma-ds.css'
import '../../styles/landing/landing.css'

// Dati strutturati per Google: descrivono il gioco (schema.org/VideoGame).
const jsonLd = {
  '@context': 'https://schema.org',
  '@type': 'VideoGame',
  name: 'QUORMA',
  description:
    'Gioco di carte collezionabili digitale, dark fantasy e post-apocalittico: Resistenza Umana contro Potenze Artificiali. Partite deterministiche decise dalla costruzione del mazzo.',
  genre: ['Gioco di carte collezionabili', 'Strategia'],
  gamePlatform: 'Web browser',
  inLanguage: 'it',
  author: { '@type': 'Organization', name: 'LEVAGSEA' },
  ...(SITE_URL && { url: SITE_URL + '/', image: SITE_URL + OG_IMAGE }),
}

// La landing: una sola pagina con le sezioni del gioco.
// Su /accedi e /registrati è la stessa pagina con sopra la finestra di accesso.
export default function LandingPage({ auth }: { auth?: AuthMode }) {
  return (
    <div className="l-page">
      <a className="l-skip" href="#contenuto">Vai al contenuto</a>
      <LandingHeader />
      <main id="contenuto">
        <HeroSection />
        <GameSection />
        <HowToPlaySection />
        <FactionsSection />
        <FaqSection />
        <FinalCta />
      </main>
      <LandingFooter />
      {auth && <AuthDialog key={auth} mode={auth} />}
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
    </div>
  )
}
