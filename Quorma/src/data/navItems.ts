// Le voci del menu laterale. Per cambiare il menu basta modificare questa lista.
import type { NavIconName } from '../components/layout/NavIcon'

// Tutte le pagine che esistono nell'app. Aggiungi qui il nome di una nuova pagina.
export type Page =
  | 'home' | 'gioca' | 'mazzi' | 'nuovo-mazzo' | 'editor-mazzo' | 'mazzo-salvato' | 'regole'
  | 'chi-siamo' | 'faq' | 'glossario' | 'contatti' | 'privacy' | 'cookie' | 'termini' // pagine "Informazioni"
  | 'sfide' | 'collezione' | 'apertura' | 'negozio'
  | 'profilo' | 'modifica-profilo' // si apre dal riquadro del giocatore
  | 'altro' // 'altro' esiste solo su telefono

export type NavItemData = {
  label: string
  icon: NavIconName // icona dentro l'ogiva (components/layout/NavIcon.tsx)
  page?: Page // pagina a cui porta. Se manca, la sezione è "in arrivo": voce attenuata e non cliccabile
  alert?: string // "braci" arancioni: c'è una novità vera (dal backend). Il testo è letto dagli screen reader
}

export const navItems: NavItemData[] = [
  { label: 'Home', icon: 'home', page: 'home' },
  { label: 'Gioca', icon: 'gioca', page: 'gioca' },
  { label: 'Sfide', icon: 'sfide', page: 'sfide' },
  { label: 'Collezione', icon: 'collezione', page: 'collezione' },
  { label: 'Mazzi', icon: 'mazzi', page: 'mazzi' },
  { label: 'Scambi', icon: 'scambi' },
  { label: 'Negozio', icon: 'negozio', page: 'negozio' },
  { label: 'Regole', icon: 'regole', page: 'regole' },
]

// Link del footer e del sotto-menu "Informazioni": ognuno porta a una pagina.
export type InfoLink = { label: string; page: Page }

export const footerLinks: { info: InfoLink[]; legal: InfoLink[] } = {
  info: [
    { label: 'Chi siamo', page: 'chi-siamo' },
    { label: 'FAQ', page: 'faq' },
    { label: 'Glossario', page: 'glossario' },
    { label: 'Contatti', page: 'contatti' },
  ],
  legal: [
    { label: 'Privacy', page: 'privacy' },
    { label: 'Cookie', page: 'cookie' },
    { label: 'Termini', page: 'termini' },
  ],
}

// Tutte e 7 insieme, nell'ordine del sotto-menu "Informazioni".
export const infoLinks: InfoLink[] = [...footerLinks.info, ...footerLinks.legal]
