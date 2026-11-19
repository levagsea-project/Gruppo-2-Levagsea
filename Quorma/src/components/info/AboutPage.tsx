import type { Page } from '../../data/navItems'
import { aboutSections } from '../../data/info'
import InfoPage from './InfoPage'
import SectionList from './SectionList'

export default function AboutPage({ onNavigate }: { onNavigate: (page: Page) => void }) {
  return (
    <InfoPage
      page="chi-siamo"
      title="CHI SIAMO"
      subtitle="LEVAGSEA E QUORMA"
      sideTop={<SectionList title="In questa pagina" items={aboutSections.map((label) => ({ label }))} />}
      onNavigate={onNavigate}
    />
  )
}
