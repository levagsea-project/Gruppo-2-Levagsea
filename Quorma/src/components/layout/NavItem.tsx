import type { CSSProperties } from 'react'
import type { NavItemData } from '../../data/navItems'
import Hearth from './Hearth'
import NavIcon from './NavIcon'

type Props = NavItemData & {
  active: boolean
  onClick: () => void
}

// Una voce del menu: ogiva con icona + testo.
// Attiva = fuoco azzurro nell'ogiva. Con una novità (alert) = braci arancioni e scintille.
export default function NavItem({ label, icon, active, alert, onClick }: Props) {
  return (
    <li>
      <a
        href="#"
        className="q-navata__item"
        aria-current={active ? 'page' : undefined}
        aria-label={alert ? `${label}, ${alert}` : undefined}
        onClick={(e) => {
          e.preventDefault() // evita che la pagina si ricarichi
          onClick()
        }}
      >
        <span className={alert ? 'q-ogiva-wrap q-braci' : 'q-ogiva-wrap'} aria-hidden="true">
          <span className="q-ogiva">
            <span className="q-ogiva__in">
              {active && (
                <Hearth
                  flames={[
                    { left: '38%' },
                    { left: '58%', delay: '0.5s', duration: '1.3s' },
                    { left: '48%', delay: '0.95s', duration: '1.7s' },
                  ]}
                  sparks={[
                    { left: '40%', dx: '-3px', delay: '0.2s' },
                    { left: '62%', dx: '4px', delay: '1.3s', duration: '2.8s' },
                  ]}
                />
              )}
              <NavIcon name={icon} />
            </span>
          </span>
          {alert && (
            <>
              <span className="q-scintilla" style={{ left: '30%', '--dx': '-4px' } as CSSProperties} />
              <span className="q-scintilla" style={{ left: '62%', '--dx': '5px', animationDelay: '1.3s' } as CSSProperties} />
            </>
          )}
        </span>
        <span className="q-navata__label">{label}</span>
      </a>
    </li>
  )
}
