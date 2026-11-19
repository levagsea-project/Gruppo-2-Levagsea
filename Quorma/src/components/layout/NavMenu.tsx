import { navItems } from '../../data/navItems'
import type { Page } from '../../data/navItems'
import NavItem from './NavItem'

type Props = {
  current: Page // pagina aperta ora
  onNavigate: (page: Page) => void
}

// Lista di tutte le voci del menu.
export default function NavMenu({ current, onNavigate }: Props) {
  // Nuovo mazzo, editor e mazzo salvato fanno parte della sezione Mazzi, quindi Mazzi resta acceso.
  const section = current === 'nuovo-mazzo' || current === 'editor-mazzo' || current === 'mazzo-salvato' ? 'mazzi' : current

  return (
    <nav aria-label="Navigazione principale">
      <ul className="q-navata__menu">
        {navItems.map((item) => (
          <NavItem
            key={item.label}
            {...item}
            active={item.page === section}
            onClick={() => item.page && onNavigate(item.page)}
          />
        ))}
      </ul>
    </nav>
  )
}
