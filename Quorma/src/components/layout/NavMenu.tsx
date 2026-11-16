import { navItems } from '../../data/navItems'
import NavItem from './NavItem'

// Lista di tutte le voci del menu.
export default function NavMenu() {
  return (
    <nav>
      <ul className="nav-menu">
        {navItems.map((item) => (
          <NavItem key={item.label} {...item} />
        ))}
      </ul>
    </nav>
  )
}
