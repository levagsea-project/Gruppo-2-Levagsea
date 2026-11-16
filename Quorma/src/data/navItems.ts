// Le voci del menu laterale. Per cambiare il menu basta modificare questa lista.
export type NavItemData = {
  label: string
  icon: string // simbolo dentro la cornice (le vere icone arriveranno dopo)
  active?: boolean // pagina corrente
  alert?: boolean // evidenziata in arancione (es. ci sono novità)
}

export const navItems: NavItemData[] = [
  { label: 'Home', icon: '⌂', active: true },
  { label: 'Gioca', icon: '◈' },
  { label: 'Collezione', icon: '♜' },
  { label: 'Mazzi', icon: '▤' },
  { label: 'Scambi', icon: '⇄', alert: true },
  { label: 'Negozio', icon: '♦' },
  { label: 'Regole', icon: '▯' },
]

export const footerLinks = [
  ['Chi siamo', 'FAQ'],
  ['Glossario', 'Contatti'],
  ['Privacy', 'Cookie', 'Termini'],
]
