import Logo from './Logo'
import NavMenu from './NavMenu'
import PlayerCard from './PlayerCard'
import SidebarFooter from './SidebarFooter'

// La barra laterale = logo + menu + giocatore + footer.
export default function Sidebar() {
  return (
    <aside className="sidebar">
      <Logo />
      <NavMenu />
      <div className="sidebar-bottom">
        <PlayerCard />
        <SidebarFooter />
      </div>
    </aside>
  )
}
