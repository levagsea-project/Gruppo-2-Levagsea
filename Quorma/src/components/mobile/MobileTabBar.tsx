import type { Page } from '../../data/navItems'
import NavIcon, { type NavIconName } from '../layout/NavIcon'
import Arch from './Arch'

type Tab = { label: string; icon: NavIconName | 'altro'; page?: Page; alert?: string; section: Page[] }

// Le 5 schede in basso. "section" = le pagine in cui la scheda resta accesa.
// Senza page la sezione non esiste ancora: scheda attenuata e non cliccabile.
// alert = braci arancioni, solo per novità vere (oggi nessuna: il backend non le fornisce ancora).
const TABS: Tab[] = [
  { label: 'Home', icon: 'home', page: 'home', section: ['home'] },
  { label: 'Gioca', icon: 'gioca', page: 'gioca', section: ['gioca'] },
  { label: 'Collezione', icon: 'collezione', page: 'collezione', section: ['collezione', 'apertura'] },
  { label: 'Mazzi', icon: 'mazzi', page: 'mazzi', section: ['mazzi', 'nuovo-mazzo', 'editor-mazzo', 'mazzo-salvato'] },
  { label: 'Altro', icon: 'altro', page: 'altro', section: ['altro', 'regole', 'sfide', 'negozio'] },
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
// Le sfide stanno sotto "Altro": le braci delle sfide in attesa si accendono lì.
export default function MobileTabBar({ current, onNavigate, challengeAlert }: { current: Page; onNavigate: (page: Page) => void; challengeAlert?: string }) {
  return (
    <nav className="mb-tab" aria-label="Navigazione principale">
      {TABS.map((tab) => {
        const t = tab.page === 'altro' && challengeAlert ? { ...tab, alert: challengeAlert } : tab
        const active = t.section.includes(current)
        const icon = (
          <Arch active={active} alert={!!t.alert}>
            {t.icon === 'altro' ? <MenuIcon /> : <NavIcon name={t.icon} />}
          </Arch>
        )

        if (!t.page) {
          return (
            <a key={t.label} className="q-navata__item" role="link" aria-disabled="true">
              {icon}
              {t.label}
              <span className="sr-only">, in arrivo</span>
            </a>
          )
        }

        const page = t.page
        return (
          <a
            key={t.label}
            href="#"
            className="q-navata__item"
            aria-current={active ? 'page' : undefined}
            aria-label={t.alert ? `${t.label}, ${t.alert}` : undefined}
            onClick={(e) => {
              e.preventDefault()
              onNavigate(page)
            }}
          >
            {icon}
            {t.label}
          </a>
        )
      })}
    </nav>
  )
}
