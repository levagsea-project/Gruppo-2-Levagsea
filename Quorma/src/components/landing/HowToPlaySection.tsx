import { steps } from '../../data/landing'

// Esempio di tavolo: le corsie dell'avversario sopra, le tue sotto. Tre Queen = Tris.
const opponent = ['3', 'K', '7', '9', '2']
const mine = [
  { rank: 'Q', suit: 'vincolo', attacks: true },
  { rank: '4', suit: 'spira', attacks: false },
  { rank: 'Q', suit: 'frattura', attacks: true },
  { rank: '8', suit: 'vestigio', attacks: false },
  { rank: 'Q', suit: 'spira', attacks: true },
]

// "Come si gioca": i quattro passi del turno e un esempio di tavolo.
export default function HowToPlaySection() {
  return (
    <section id="come-si-gioca" className="l-sez" aria-labelledby="h-come">
      <div className="l-wrap l-come">
        <div>
          <p className="l-kicker">Come si gioca</p>
          <h2 id="h-come" className="l-h2">Quattro passi</h2>
          <ol className="l-passi">
            {steps.map((s, i) => (
              <li key={s.title} className="l-passo">
                <span className="q-marker__num" aria-hidden="true"><span>{i + 1}</span></span>
                <h3 className="l-h3">{s.title}</h3>
                <p className="l-testo">{s.text}</p>
              </li>
            ))}
          </ol>
        </div>

        <figure className="q-tela l-tavolo">
          <div className="q-tela__in">
            <div aria-hidden="true" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <span className="l-kicker">Avversario</span>
              <div className="l-corsie">
                {opponent.map((r, i) => <span key={i} className="l-slot"><b>{r}</b></span>)}
              </div>
              <div className="l-frecce">
                {mine.map((c, i) => <span key={i} className={c.attacks ? '' : 'l-no'} />)}
              </div>
              <div className="l-corsie">
                {mine.map((c, i) => (
                  <span key={i} className={c.attacks ? 'l-slot l-slot--att' : 'l-slot'}>
                    <b>{c.rank}</b>
                    <i className={`l-seme l-seme--${c.suit}`} />
                  </span>
                ))}
              </div>
              <div className="l-corsie">
                {mine.map((_, i) => <span key={i} className="l-num">{i + 1}</span>)}
              </div>
            </div>
            <figcaption>
              Tre Queen formano un <b>Tris</b>: attaccano le tre creature evidenziate.
            </figcaption>
          </div>
        </figure>
      </div>
    </section>
  )
}
