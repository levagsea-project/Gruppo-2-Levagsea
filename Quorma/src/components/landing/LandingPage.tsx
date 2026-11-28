import { useEffect, useRef } from 'react'
import { LazyMotion, MotionConfig, domAnimation } from 'motion/react'
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

// Comparsa allo scorrimento (design: Landing-Quorma): gli elementi con .l-rivela salgono e appaiono quando entrano
// nello schermo, una volta sola. La pagina arriva come HTML statico già visibile: la classe l-js, che li nasconde
// finché non si vedono, la mette solo il browser, e non la mette con il movimento ridotto.
function useReveal() {
  const root = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const el = root.current
    if (!el || !('IntersectionObserver' in window) || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const io = new IntersectionObserver(
      (entries) => {
        for (const e of entries) {
          if (!e.isIntersecting) continue
          e.target.classList.add('l-visto')
          io.unobserve(e.target)
        }
      },
      { threshold: 0.12, rootMargin: '0px 0px -40px 0px' },
    )
    el.querySelectorAll('.l-rivela').forEach((r) => io.observe(r))
    // si nascondono di colpo, senza la dissolvenza all'indietro: poi la comparsa parte da zero come nel design
    el.classList.add('l-js', 'l-js-avvio')
    void el.offsetHeight
    el.classList.remove('l-js-avvio')
    return () => io.disconnect()
  }, [])
  return root
}

// La landing: una sola pagina con le sezioni del gioco.
// Su /accedi e /registrati è la stessa pagina con sopra la finestra di accesso.
export default function LandingPage({ auth }: { auth?: AuthMode }) {
  const root = useReveal()
  return (
    // Motion caricato leggero (solo le animazioni DOM); con il movimento ridotto Motion toglie gli spostamenti
    <LazyMotion features={domAnimation} strict>
    <MotionConfig reducedMotion="user">
    <div ref={root} className="l-page">
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
    </MotionConfig>
    </LazyMotion>
  )
}
