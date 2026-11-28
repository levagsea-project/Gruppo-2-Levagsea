import { faq } from '../../data/landing'

// Domande frequenti. <details> si apre e chiude senza JavaScript e le risposte restano nell'HTML.
export default function FaqSection() {
  return (
    <section id="faq" className="l-sez" aria-labelledby="h-faq">
      <div className="l-wrap l-faq">
        <div>
          <p className="l-kicker l-rivela">FAQ</p>
          <h2 id="h-faq" className="l-h2 l-rivela">Domande frequenti</h2>
          <p className="l-lead l-rivela">Le risposte alle domande più comuni su partite, mazzi e carte.</p>
        </div>
        <div>
          {faq.map((item, i) => (
            <details key={item.q} className="l-domanda l-rivela" open={i === 0}>
              <summary>
                <span>{item.q}</span>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                  <path d="m6 9 6 6 6-6" />
                </svg>
              </summary>
              <p>{item.a}</p>
            </details>
          ))}
        </div>
      </div>
    </section>
  )
}
