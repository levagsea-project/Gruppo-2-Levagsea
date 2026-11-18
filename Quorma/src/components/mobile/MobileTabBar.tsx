import type { Page } from '../../data/navItems'
import NavIcon, { type NavIconName } from '../layout/NavIcon'
import Arch from './Arch'

type Tab = { label: string; icon: NavIconName | 'altro'; page?: Page; alert?: string; section: Page[] }

// Le 5 schede in basso. "section" = le pagine in cui la scheda resta accesa.
const TABS: Tab[] = [
  { label: 'Home', icon: 'home', page: 'home', section: ['home'] },
  { label: 'Gioca', icon: 'gioca', page: 'gioca', section: ['gioca'] },
  { label: 'Collezione', icon: 'collezione', alert: 'buste da aprire', section: [] },
  { label: 'Mazzi', icon: 'mazzi', page: 'mazzi', section: ['mazzi', 'nuovo-mazzo'] },
  { label: 'Altro', icon: 'altro', page: 'altro', alert: 'nuove proposte di scambio', section: ['altro', 'regole'] },
]

// Icona "Altro": tre righe.
function MenuIcon() {
  return (
    <svg className="q-navata__ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
      <path d="M4 7h16" />
      <path d="M4 12h16" />
      <path d="M4 17h10" />
    </svg>
  )
}

// Barra delle schede su telefono.
export default function MobileTabBar({ current, onNavigate }: { current: Page; onNavigate: (page: Page) => void }) {
  return (
    <nav className="mb-tab" aria-label="Navigazione principale">
      {TABS.map((t) => {
        const active = t.section.includes(current)
        return (
          <a
            key={t.label}
            href="#"
            className="q-navata__item"
            aria-current={active ? 'page' : undefined}
            aria-label={t.alert ? `${t.label}, ${t.alert}` : undefined}
            onClick={(e) => {
              e.preventDefault()
              if (t.page) onNavigate(t.page)
            }}
          >
            <Arch active={active} alert={!!t.alert}>
              {t.icon === 'altro' ? <MenuIcon /> : <NavIcon name={t.icon} />}
            </Arch>
            {t.label}
          </a>
        )
      })}
    </nav>
  )
}
