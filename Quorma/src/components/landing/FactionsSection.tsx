import { cycles, factions, suits } from '../../data/landing'
import DoctrineCycle from './DoctrineCycle'
import FactionCard from './FactionCard'

// "Fazioni e semi": le due fazioni con le sottofazioni, i quattro semi e i cicli delle dottrine.
export default function FactionsSection() {
  return (
    <section id="fazioni" className="l-sez" aria-labelledby="h-fazioni">
      <div className="l-wrap">
        <p className="l-kicker l-rivela">Fazioni e semi</p>
        <h2 id="h-fazioni" className="l-h2 l-rivela">Scegli da che parte stare</h2>

        <div className="l-griglia-2">
          {factions.map((f) => (
            <FactionCard key={f.name} f={f} />
          ))}
        </div>

        <div className="l-semi-blocco">
          <div>
            <h3 className="l-h3">I quattro semi</h3>
            <p>I semi sono condivisi tra le fazioni. Con tre o più creature dello stesso seme in campo si attiva una sinergia.</p>
            <ul className="l-semi">
              {suits.map((s) => (
                <li key={s.id} className="l-rivela">
                  <i className={`l-seme l-seme--${s.id}`} aria-hidden="true" />
                  <span><b>{s.name}</b>{s.text}</span>
                </li>
              ))}
            </ul>
          </div>
          <figure className="l-cicli l-rivela">
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
