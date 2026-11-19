import type { Page } from '../../data/navItems'
import InfoPage from './InfoPage'
import LetterGrid from './LetterGrid'

export default function GlossaryPage({ onNavigate }: { onNavigate: (page: Page) => void }) {
  return <InfoPage page="glossario" title="GLOSSARIO" subtitle="I TERMINI DEL GIOCO" sideTop={<LetterGrid />} onNavigate={onNavigate} />
}
