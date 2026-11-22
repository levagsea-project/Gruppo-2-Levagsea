import { redirect } from 'react-router'
import App from '../App'
import { getSession } from '../lib/auth'
import { pageMeta } from '../lib/site'

// L'area di gioco: senza accesso si torna alla finestra di login.
export function clientLoader() {
  if (!getSession()) throw redirect('/accedi')
  return null
}

export const meta = () =>
  pageMeta({ title: 'QUORMA', description: 'Area di gioco di QUORMA.', path: '/app', noindex: true })

// Per ora l'app esistente è montata così com'è: le sue schermate cambiano ancora con lo stato interno.
export default function GameApp() {
  return <App />
}
