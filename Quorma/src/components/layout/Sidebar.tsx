import type { Page } from '../../data/navItems'
import Logo from './Logo'
import NavMenu from './NavMenu'
import PlayerCard from './PlayerCard'
import SidebarFooter from './SidebarFooter'
import '../../styles/landing/quorma-ds.css' // colori e materiali del design system

type Props = { current: Page; onNavigate: (page: Page) => void }

// La barra laterale ("navata") = logo + menu + giocatore + footer.
export default function Sidebar({ current, onNavigate }: Props) {
  return (
    <aside className="q-navata">
      <Logo />
      <NavMenu current={current} onNavigate={onNavigate} />
      <PlayerCard />
      <SidebarFooter onNavigate={onNavigate} />
    </aside>
  )
}
