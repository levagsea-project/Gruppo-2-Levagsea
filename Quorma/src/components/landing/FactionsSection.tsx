import type { CSSProperties } from 'react'
import { cycles, factions, suits } from '../../data/landing'
import DoctrineCycle from './DoctrineCycle'

// "Fazioni e semi": le due fazioni con le sottofazioni, i quattro semi e i cicli delle dottrine.
export default function FactionsSection() {
  return (
    <section id="fazioni" className="l-sez" aria-labelledby="h-fazioni">
      <div className="l-wrap">
        <p className="l-kicker">Fazioni e semi</p>
        <h2 id="h-fazioni" className="l-h2">Scegli da che parte stare</h2>

        <div className="l-griglia-2">
          {factions.map((f) => (
            <article key={f.name} className="q-tela q-tela--arco q-carta l-fazione">
              <div className="q-tela__in">
                <span className="q-carta__arte">
                  <img src={f.image} alt={f.alt} width={1024} height={427} loading="lazy" decoding="async" />
                </span>
                <span className="q-divisorio" />
                <div className="l-fazione__corpo">
                  <h3 className="l-fazione__nome">{f.name}</h3>
                  <p className="l-testo">{f.text}</p>
                  <ul className="l-sottofazioni" aria-label={`Sottofazioni: ${f.name}`}>
                    {f.subfactions.map((s) => (
                      <li key={s.name}>
                        <span
                          className="q-emblema"
                          aria-hidden="true"
                          style={{ '--c': s.color, '--cd': s.deep } as CSSProperties}
                        >
                          {s.short}
                        </span>
                        {s.name}
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
              <span className="q-borchia q-borchia--n" />
            </article>
          ))}
        </div>

        <div className="l-semi-blocco">
          <div>
            <h3 className="l-h3">I quattro semi</h3>
            <p>I semi sono condivisi tra le fazioni. Con tre o più creature dello stesso seme in campo si attiva una sinergia.</p>
            <ul className="l-semi">
              {suits.map((s) => (
                <li key={s.id}>
                  <i className={`l-seme l-seme--${s.id}`} aria-hidden="true" />
                  <span><b>{s.name}</b>{s.text}</span>
                </li>
              ))}
            </ul>
          </div>
          <figure className="l-cicli">
            <div className="l-cicli__svg">
              {cycles.map((c) => <DoctrineCycle key={c.name} name={c.name} order={c.order} />)}
            </div>
            <figcaption>Ogni sottofazione è forte contro un'altra: la freccia va dalla forte alla debole.</figcaption>
          </figure>
        </div>
      </div>
    </section>
  )
}
