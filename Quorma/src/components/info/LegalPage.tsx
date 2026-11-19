import type { Page } from '../../data/navItems'
import { LAST_UPDATE, legalPages } from '../../data/info'
import InfoPage from './InfoPage'
import SectionList from './SectionList'

// Una sola pagina per Termini, Privacy e Cookie: cambia solo `kind`.
export default function LegalPage({ kind, onNavigate }: { kind: keyof typeof legalPages; onNavigate: (page: Page) => void }) {
  const { page, title, subtitle, sections } = legalPages[kind]
  return (
    <InfoPage
      page={page}
      title={title}
      subtitle={subtitle}
      note={LAST_UPDATE}
      sideTop={<SectionList title="In questa pagina" items={sections.map((label) => ({ label }))} />}
      onNavigate={onNavigate}
    />
  )
}
