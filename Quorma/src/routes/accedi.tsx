import LandingPage from '../components/landing/LandingPage'
import { pageMeta } from '../lib/site'

export const meta = () =>
  pageMeta({
    title: 'Accedi – QUORMA',
    description: 'Accedi al tuo account QUORMA per costruire i tuoi mazzi e giocare.',
    path: '/accedi',
    noindex: true,
  })

export default function Accedi() {
  return <LandingPage auth="login" />
}
