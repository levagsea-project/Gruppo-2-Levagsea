// Le voci del menu laterale. Per cambiare il menu basta modificare questa lista.
import type { NavIconName } from '../components/layout/NavIcon'

// Tutte le pagine che esistono nell'app. Aggiungi qui il nome di una nuova pagina.
export type Page = 'home' | 'gioca' | 'mazzi' | 'nuovo-mazzo' | 'regole' | 'altro' // 'altro' esiste solo su telefono

export type NavItemData = {
  label: string
  icon: NavIconName // icona dentro l'ogiva (components/layout/NavIcon.tsx)
  page?: Page // pagina a cui porta. Se manca, la voce non fa ancora nulla
  alert?: string // "braci" arancioni: c'è una novità. Il testo è letto dagli screen reader
}

export const navItems: NavItemData[] = [
  { label: 'Home', icon: 'home', page: 'home' },
  { label: 'Gioca', icon: 'gioca', page: 'gioca' },
  { label: 'Sfide', icon: 'sfide', alert: 'ricompense da riscuotere' },
  { label: 'Collezione', icon: 'collezione', alert: 'buste da aprire' },
  { label: 'Mazzi', icon: 'mazzi', page: 'mazzi' },
  { label: 'Scambi', icon: 'scambi', alert: 'nuove proposte' },
  { label: 'Negozio', icon: 'negozio' },
  { label: 'Regole', icon: 'regole', page: 'regole' },
]

export const footerLinks = {
  info: ['Chi siamo', 'FAQ', 'Glossario', 'Contatti'],
  legal: ['Privacy', 'Cookie', 'Termini'],
}
