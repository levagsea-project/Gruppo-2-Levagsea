import { sections } from '../../data/landing'

// Piè di pagina. Le pagine informative (Chi siamo, Privacy, Termini…) non esistono ancora:
// quando ci saranno, aggiungi qui i link.
export default function LandingFooter() {
  return (
    <footer className="l-foot">
      <div className="l-wrap">
        <div>
          <img src="/images/ui/logo-quorma.png" alt="QUORMA" width={640} height={179} loading="lazy" />
          <small>© 2026 QUORMA · Un gioco di LEVAGSEA</small>
        </div>
        <nav aria-label="Sezioni della pagina">
          {sections.map((s) => <a key={s.id} href={`#${s.id}`}>{s.label}</a>)}
        </nav>
      </div>
    </footer>
  )
}
