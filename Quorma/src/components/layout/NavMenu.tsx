import { navItems } from '../../data/navItems'
import type { Page } from '../../data/navItems'
import NavItem from './NavItem'

type Props = {
  current: Page // pagina aperta ora
  onNavigate: (page: Page) => void
  challengeAlert?: string // sfide ricevute in attesa: braci sulla voce Sfide
}

// Lista di tutte le voci del menu.
export default function NavMenu({ current, onNavigate, challengeAlert }: Props) {
  // Nuovo mazzo, editor e mazzo salvato fanno parte di Mazzi; l'apertura delle buste fa parte di Collezione.
  const section =
    current === 'nuovo-mazzo' || current === 'editor-mazzo' || current === 'mazzo-salvato'
      ? 'mazzi'
      : current === 'apertura'
        ? 'collezione'
        : current

  return (
    <nav aria-label="Navigazione principale">
      <ul className="q-navata__menu">
        {navItems.map((item) => (
          <NavItem
            key={item.label}
            {...item}
            alert={item.page === 'sfide' ? challengeAlert : item.alert}
            active={item.page === section}
            onClick={() => item.page && onNavigate(item.page)}
          />
        ))}
      </ul>
    </nav>
  )
}
