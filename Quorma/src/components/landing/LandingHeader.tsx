import { Link } from 'react-router'
import { sections } from '../../data/landing'

// Barra fissa in alto: logo, sezioni della pagina, accesso e registrazione.
export default function LandingHeader() {
  return (
    <header className="l-bar">
      <div className="l-wrap">
        <a href="#top" className="l-bar__logo" aria-label="QUORMA, torna all'inizio">
          <img src="/images/ui/logo-quorma.png" alt="" width={640} height={179} />
        </a>
        <nav aria-label="Sezioni">
          {sections.map((s) => (
            <a key={s.id} href={`#${s.id}`} className="l-link">{s.label}</a>
          ))}
        </nav>
        <div className="l-bar__azioni">
          <Link to="/accedi" preventScrollReset className="q-btn q-btn--acciaio">
            <span className="q-btn__in">Accedi</span>
          </Link>
          <Link to="/registrati" preventScrollReset className="q-btn q-btn--argento l-bar__registrati">
            <span className="q-btn__in">Registrati</span>
          </Link>
        </div>
      </div>
    </header>
  )
}
