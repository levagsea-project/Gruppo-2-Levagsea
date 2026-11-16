// Le voci del menu laterale. Per cambiare il menu basta modificare questa lista.

// Tutte le pagine che esistono nell'app. Aggiungi qui il nome di una nuova pagina.
export type Page = 'home' | 'mazzi' | 'nuovo-mazzo'

export type NavItemData = {
  label: string
  icon: string // simbolo dentro la cornice (le vere icone arriveranno dopo)
  page?: Page // pagina a cui porta. Se manca, la voce non fa ancora nulla
  alert?: boolean // evidenziata in arancione (es. ci sono novità)
}

export const navItems: NavItemData[] = [
  { label: 'Home', icon: '⌂', page: 'home' },
  { label: 'Gioca', icon: '◈' },
  { label: 'Collezione', icon: '♜', alert: true },
  { label: 'Mazzi', icon: '▤', page: 'mazzi' },
  { label: 'Scambi', icon: '⇄', alert: true },
  { label: 'Negozio', icon: '♦' },
  { label: 'Regole', icon: '▯' },
]

export const footerLinks = [
  ['Chi siamo', 'FAQ'],
  ['Glossario', 'Contatti'],
  ['Privacy', 'Cookie', 'Termini'],
]
