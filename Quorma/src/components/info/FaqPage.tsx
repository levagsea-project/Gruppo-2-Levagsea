import { useState } from 'react'
import type { Page } from '../../data/navItems'
import { faq, faqTopics } from '../../data/info'
import InfoPage from './InfoPage'
import { InfoLink } from './InfoSections'
import SearchBox from './SearchBox'
import SideList from './SideList'

const ALL = 'Tutti'

// Confronto senza maiuscole né accenti: "perche" trova "perché".
const fold = (s: string) => s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '')

// FAQ: argomenti a sinistra, ricerca e domande che si aprono una alla volta, aiuto a destra.
export default function FaqPage({ onNavigate }: { onNavigate: (page: Page) => void }) {
  const [topic, setTopic] = useState(ALL)
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState<string | null>(faq[0].q) // la prima domanda è aperta, come nel design

  const topics = [ALL, ...faqTopics]
  const counts = topics.map((t) => (t === ALL ? faq.length : faq.filter((f) => f.topic === t).length))

  const q = fold(query.trim())
  const shown = faq.filter((f) => (topic === ALL || f.topic === topic) && (!q || fold(f.q + ' ' + f.a).includes(q)))
  const groups = faqTopics.map((t) => ({ topic: t, items: shown.filter((f) => f.topic === t) })).filter((g) => g.items.length > 0)

  return (
    <InfoPage
      page="faq"
      title="FAQ"
      subtitle="DOMANDE FREQUENTI"
      sideTop={
        <SideList
          title="Argomenti"
          items={topics.map((t, i) => ({ label: t, count: counts[i] }))}
          active={topics.indexOf(topic)}
          onSelect={(i) => setTopic(topics[i])}
        />
      }
      sideLabel={topic === ALL ? 'Tutti gli argomenti' : `Argomento: ${topic}`}
      aside={
        <>
          <div className="q-tela q-tela--arco info-aiuto">
            <div className="q-tela__in">
              <span className="info-aiuto__titolo">Non trovi la risposta?</span>
              <p>Il glossario spiega i termini del gioco, il regolamento tutte le sue regole.</p>
              <button type="button" className="q-btn q-btn--argento" onClick={() => onNavigate('glossario')}>
                <span className="q-btn__in">Apri il glossario</span>
              </button>
            </div>
            <span className="q-borchia q-borchia--n" aria-hidden="true" />
          </div>
          <button type="button" className="q-btn q-btn--acciaio" onClick={() => onNavigate('regole')}>
            <span className="q-btn__in">Leggi le regole</span>
          </button>
        </>
      }
      onNavigate={onNavigate}
    >
      <SearchBox value={query} onChange={setQuery} placeholder="Cerca una domanda" label="Cerca nelle FAQ" />

      <div className="info-faq" aria-live="polite">
        {groups.map((g) => (
          <section key={g.topic} aria-label={g.topic}>
            <h2 className="info-gruppo">{g.topic}</h2>
            {g.items.map((f) => {
              const isOpen = open === f.q
              const answerId = `faq-${faq.indexOf(f)}`
              return (
                <div key={f.q} className="info-domanda">
                  <button type="button" className="info-domanda__btn" aria-expanded={isOpen} aria-controls={answerId} onClick={() => setOpen(isOpen ? null : f.q)}>
                    <span>{f.q}</span>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                      <path d="m6 9 6 6 6-6" />
                    </svg>
                  </button>
                  <div id={answerId} className="info-risposta" hidden={!isOpen}>
                    <p>{f.a}</p>
                    {f.link && <InfoLink label={f.link.label} page={f.link.page} onNavigate={onNavigate} />}
                  </div>
                </div>
              )
            })}
          </section>
        ))}
        {groups.length === 0 && (
          <p className="info-vuoto">
            Nessuna domanda trovata. Prova con altre parole oppure{' '}
            <InfoLink label="scrivici" page="contatti" onNavigate={onNavigate} />.
          </p>
        )}
      </div>
    </InfoPage>
  )
}
