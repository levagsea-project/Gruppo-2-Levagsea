import type { Page } from '../../data/navItems'
import Logo from './Logo'
import NavMenu from './NavMenu'
import PlayerCard from './PlayerCard'
import SidebarFooter from './SidebarFooter'

type Props = { current: Page; onNavigate: (page: Page) => void }

// La barra laterale = logo + menu + giocatore + footer.
export default function Sidebar({ current, onNavigate }: Props) {
  return (
    <aside className="sidebar">
      <Logo />
      <NavMenu current={current} onNavigate={onNavigate} />
      <div className="sidebar-bottom">
        <PlayerCard />
        <SidebarFooter />
      </div>
    </aside>
  )
}
