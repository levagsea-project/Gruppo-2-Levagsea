import type { Page } from '../../data/navItems'
import { faqTopics } from '../../data/info'
import InfoPage from './InfoPage'
import SectionList from './SectionList'

export default function FaqPage({ onNavigate }: { onNavigate: (page: Page) => void }) {
  return (
    <InfoPage
      page="faq"
      title="FAQ"
      subtitle="DOMANDE FREQUENTI"
      sideTop={<SectionList title="Argomenti" items={faqTopics} />}
      aside={
        <>
          {/* riquadro ad arco "Non trovi la risposta?" (vuoto) */}
          <div className="q-tela q-tela--arco info-help">
            <div className="q-tela__in" />
          </div>
          <button type="button" className="q-btn q-btn--acciaio" onClick={() => onNavigate('regole')}>
            <span className="q-btn__in">Leggi le regole</span>
          </button>
        </>
      }
      onNavigate={onNavigate}
    />
  )
}
