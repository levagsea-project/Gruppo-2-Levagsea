import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { sections } from '../../data/landing'

// Barra fissa in alto: logo, sezioni della pagina, accesso e registrazione.
// Il link della sezione che si sta leggendo resta acceso, con la riga azzurra sotto (design: Landing-Quorma).
export default function LandingHeader() {
  const [current, setCurrent] = useState('')

  useEffect(() => {
    if (!('IntersectionObserver' in window)) return
    const io = new IntersectionObserver(
      (entries) => {
        for (const e of entries) {
          if (e.isIntersecting && e.intersectionRatio > 0.3) setCurrent(e.target.id)
          else if (!e.isIntersecting) setCurrent((c) => (c === e.target.id ? '' : c))
        }
      },
      { threshold: [0, 0.3, 0.6] },
    )
    for (const s of sections) {
      const el = document.getElementById(s.id)
      if (el) io.observe(el)
    }
    return () => io.disconnect()
  }, [])

  return (
    <header className="l-bar">
      <div className="l-wrap">
        <a href="#top" className="l-bar__logo" aria-label="QUORMA, torna all'inizio">
          <img src="/images/ui/logo-quorma.png" alt="" width={640} height={179} />
        </a>
        <nav aria-label="Sezioni">
          {sections.map((s) => (
            <a key={s.id} href={`#${s.id}`} className="l-link" aria-current={current === s.id ? 'location' : undefined}>{s.label}</a>
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
