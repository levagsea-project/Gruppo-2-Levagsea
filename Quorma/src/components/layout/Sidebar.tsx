import type { Page } from '../../data/navItems'
import Logo from './Logo'
import NavMenu from './NavMenu'
import PlayerCard from './PlayerCard'
import SidebarFooter from './SidebarFooter'
import '../../styles/landing/quorma-ds.css' // colori e materiali del design system

type Props = { current: Page; onNavigate: (page: Page) => void; challengeAlert?: string }

// La barra laterale ("navata") = logo + menu + giocatore + footer.
export default function Sidebar({ current, onNavigate, challengeAlert }: Props) {
  return (
    <aside className="q-navata">
      <Logo onHome={() => onNavigate('home')} />
      <NavMenu current={current} onNavigate={onNavigate} challengeAlert={challengeAlert} />
      <PlayerCard current={current} onNavigate={onNavigate} />
      <SidebarFooter onNavigate={onNavigate} />
    </aside>
  )
}
