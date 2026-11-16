import CurrencyBar from '../layout/CurrencyBar'
import HeroText from './HeroText'
import InfoCards from './InfoCards'

// La pagina Home: sfondo (in CSS) + valute + testo + card.
export default function HomePage() {
  return (
    <main className="home">
      <CurrencyBar />
      <HeroText />
      <InfoCards />
    </main>
  )
}
