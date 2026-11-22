import { infoLinks } from '../../data/navItems'
import type { Page } from '../../data/navItems'
import SideList from './SideList'

type Props = { current: Page; onNavigate: (page: Page) => void }

// Sotto-menu "INFORMAZIONI": le 7 pagine, quella aperta è evidenziata.
export default function InfoMenu({ current, onNavigate }: Props) {
  return (
    <SideList
      title="Informazioni"
      items={infoLinks.map((link) => ({ label: link.label }))}
      active={infoLinks.findIndex((link) => link.page === current)}
      onSelect={(i) => onNavigate(infoLinks[i].page)}
    />
  )
}
