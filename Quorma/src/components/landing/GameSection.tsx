import { features } from '../../data/landing'

// "Il gioco": cosa rende QUORMA diverso, in tre riquadri.
export default function GameSection() {
  return (
    <section id="il-gioco" className="l-sez" aria-labelledby="h-gioco">
      <div className="l-wrap">
        <p className="l-kicker">Il gioco</p>
        <h2 id="h-gioco" className="l-h2">La strategia è il tuo mazzo</h2>
        <p className="l-lead">
          In QUORMA non si gioca mossa per mossa: costruisci il mazzo, poi la partita si svolge da sola, sempre allo
          stesso modo a parità di mazzi. Vince chi ha preparato meglio.
        </p>
        <div className="l-griglia-3">
          {features.map((f) => (
            <article key={f.title} className="q-tela q-tela--arco l-feat">
              <div className="q-tela__in">
                <h3 className="l-h3">{f.title}</h3>
                <p className="l-testo">{f.text}</p>
              </div>
              <span className="q-borchia q-borchia--n" />
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
