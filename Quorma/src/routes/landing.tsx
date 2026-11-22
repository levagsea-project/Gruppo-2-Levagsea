import LandingPage from '../components/landing/LandingPage'
import { pageMeta } from '../lib/site'

export const meta = () =>
  pageMeta({
    title: 'QUORMA – Gioco di carte collezionabili dark fantasy',
    description:
      'QUORMA è un gioco di carte collezionabili digitale e post-apocalittico: Resistenza Umana contro Potenze Artificiali. Costruisci un mazzo di 32 carte, la partita si gioca da sola.',
    path: '/',
  })

export default function Landing() {
  return <LandingPage />
}
