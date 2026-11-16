import type { NavItemData } from '../../data/navItems'

type Props = NavItemData & {
  active: boolean
  onClick: () => void
}

// Una singola voce del menu: cornice con icona + testo.
export default function NavItem({ label, icon, active, alert, onClick }: Props) {
  const classes = ['nav-item', active && 'is-active', alert && 'is-alert']
    .filter(Boolean)
    .join(' ')

  return (
    <li>
      <a
        href="#"
        className={classes}
        onClick={(e) => {
          e.preventDefault() // evita che la pagina si ricarichi
          onClick()
        }}
      >
        <span className="nav-icon">{icon}</span>
        <span className="nav-label">{label}</span>
      </a>
    </li>
  )
}
