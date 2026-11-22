import { useRef, useState } from 'react'
import { advantage, matchFacts, pokerHands, ruleSections } from '../../data/rules'
import PageTitle from '../layout/PageTitle'
import '../../styles/landing/quorma-ds.css'
import '../../styles/rules.css'

// Una riga del riferimento rapido: numero (facoltativo), voce, valore.
function Row({ n, label, value }: { n?: number; label: string; value: string }) {
  return (
    <div className="r-mano">
      <span>{n}</span>
      <span>{label}</span>
      <span>{value}</span>
    </div>
  )
}

// Pagina Regole: indice a sinistra, una sezione alla volta al centro, riferimento rapido a destra.
export default function RulesPage() {
  const [index, setIndex] = useState(0) // sezione aperta
  const textRef = useRef<HTMLDivElement>(null)
  const section = ruleSections[index]
  const prev = ruleSections[index - 1]
  const next = ruleSections[index + 1]

  const open = (i: number) => {
    setIndex(i)
    const text = textRef.current
    if (!text) return
    // Sotto i 1024px scorre tutta la pagina: si torna all'inizio del testo
    // e la voce scelta resta visibile nella striscia dell'indice.
    if (window.matchMedia('(max-width: 1023px)').matches) {
      text.scrollIntoView({ block: 'start' })
      document.querySelectorAll('.r-voce')[i]?.scrollIntoView({ block: 'nearest', inline: 'center' })
    } else {
      text.scrollTo({ top: 0 }) // la nuova sezione parte dall'alto
    }
  }

  return (
    <main className="page rules-page">
      <header className="rules-header">
        <PageTitle title="REGOLE" subtitle="REGOLAMENTO BASE · V1.0-ALPHA" />
        <p className="rules-alpha">Versione alpha: le regole e i numeri tra parentesi quadre possono cambiare con il playtesting.</p>
      </header>

      <div className="rules-body">
        <nav className="r-indice" aria-label="Indice del regolamento">
          <span className="r-etichetta">Indice</span>
          {ruleSections.map((s, i) => (
            <button
              key={s.id}
              type="button"
              className="r-voce"
              aria-current={i === index ? 'true' : undefined}
              onClick={() => open(i)}
            >
              <span className="r-voce-num">{s.num}</span>
              {s.title}
            </button>
          ))}
        </nav>

        <article className="q-tela rules-tela">
          <div ref={textRef} className="q-tela__in r-testo">
            <section aria-labelledby={`t-${section.id}`}>
              <div className="r-titolo">
                <span className="q-marker__num">
                  <span>{section.num}</span>
                </span>
                <h2 id={`t-${section.id}`}>{section.title}</h2>
              </div>
              {section.body}
            </section>

            <div className="r-sfoglia">
              <button type="button" className="q-btn q-btn--acciaio" disabled={!prev} onClick={() => open(index - 1)}>
                <span className="q-btn__in">{prev ? `← ${prev.title}` : 'Inizio'}</span>
              </button>
              <button type="button" className="q-btn q-btn--acciaio" disabled={!next} onClick={() => open(index + 1)}>
                <span className="q-btn__in">{next ? `${next.title} →` : 'Fine'}</span>
              </button>
            </div>
          </div>
        </article>

        <aside className="r-rapido" aria-label="Riferimento rapido">
          <div className="r-gruppo">
            <span className="r-etichetta">Mani di poker</span>
            {pokerHands.map(([label, value], i) => (
              <Row key={label} n={i + 1} label={label} value={`×${value}`} />
            ))}
            <span className="r-nota-piccola">Moltiplicatore del danno da traboccamento</span>
          </div>
          <div className="r-gruppo">
            <span className="r-etichetta">Vantaggio</span>
            {advantage.map(([label, value]) => (
              <Row key={label} label={label} value={value} />
            ))}
          </div>
          <div className="r-gruppo">
            <span className="r-etichetta">Partita</span>
            {matchFacts.map(([label, value]) => (
              <Row key={label} label={label} value={value} />
            ))}
          </div>
        </aside>
      </div>
    </main>
  )
}
