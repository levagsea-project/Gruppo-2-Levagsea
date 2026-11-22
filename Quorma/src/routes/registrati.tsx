import LandingPage from '../components/landing/LandingPage'
import { pageMeta } from '../lib/site'

export const meta = () =>
  pageMeta({
    title: 'Registrati – QUORMA',
    description: 'Crea il tuo account QUORMA e costruisci il tuo primo mazzo.',
    path: '/registrati',
    noindex: true,
  })

export default function Registrati() {
  return <LandingPage auth="signup" />
}
