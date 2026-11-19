import type { Page } from '../../data/navItems'
import { LAST_UPDATE, legalPages } from '../../data/info'
import InfoPage from './InfoPage'
import InfoSections from './InfoSections'
import SectionList from './SectionList'

// Una sola pagina per Termini, Privacy e Cookie: cambia solo `kind`. I testi sono in data/info.ts.
export default function LegalPage({ kind, onNavigate }: { kind: keyof typeof legalPages; onNavigate: (page: Page) => void }) {
  const { page, title, subtitle, sections } = legalPages[kind]
  return (
    <InfoPage
      key={kind} // cambiando pagina legale l'indice riparte dalla prima voce e il testo dall'inizio
      page={page}
      title={title}
      subtitle={subtitle}
      note={LAST_UPDATE}
      sideTop={<SectionList title="In questa pagina" sections={sections} />}
      onNavigate={onNavigate}
    >
      <InfoSections sections={sections} onNavigate={onNavigate} />
    </InfoPage>
  )
}
