import type { NavItemData } from '../../data/navItems'

// Una singola voce del menu: cornice con icona + testo.
export default function NavItem({ label, icon, active, alert }: NavItemData) {
  const classes = ['nav-item', active && 'is-active', alert && 'is-alert']
    .filter(Boolean)
    .join(' ')

  return (
    <li>
      <a href="#" className={classes}>
        <span className="nav-icon">{icon}</span>
        <span className="nav-label">{label}</span>
      </a>
    </li>
  )
}
