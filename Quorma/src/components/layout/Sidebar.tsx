import type { Page } from '../../data/navItems'
import Logo from './Logo'
import NavMenu from './NavMenu'
import PlayerCard from './PlayerCard'
import SidebarFooter from './SidebarFooter'
import '../../styles/landing/quorma-ds.css' // colori e materiali del design system

type Props = { current: Page; onNavigate: (page: Page) => void; challengeAlert?: string; packAlert?: string }

// La barra laterale ("navata") = logo + menu + giocatore + footer.
export default function Sidebar({ current, onNavigate, challengeAlert, packAlert }: Props) {
  return (
    <aside className="q-navata">
      <Logo onHome={() => onNavigate('home')} />
      {/* Divisorio "Lama": lama d'argento tra logo e menu, più spessa al centro e sottile alle punte */}
      <span className="q-navata__lama" aria-hidden="true">
        <svg viewBox="0 0 192 8" preserveAspectRatio="none">
          <defs>
            <linearGradient id="q-lama-argento" x1="0" x2="1">
              <stop offset="0" stopColor="#4AA6AD" stopOpacity="0" />
              <stop offset=".3" stopColor="#8FA4AD" stopOpacity=".7" />
              <stop offset=".5" stopColor="#F1F6F7" />
              <stop offset=".7" stopColor="#8FA4AD" stopOpacity=".7" />
              <stop offset="1" stopColor="#4AA6AD" stopOpacity="0" />
            </linearGradient>
          </defs>
          <path d="M0 4 Q96 1 192 4 Q96 7 0 4z" fill="url(#q-lama-argento)" />
          <path d="M40 4h112" stroke="#F1F6F7" strokeOpacity=".5" strokeWidth=".4" />
        </svg>
      </span>
      <NavMenu current={current} onNavigate={onNavigate} challengeAlert={challengeAlert} packAlert={packAlert} />
      <PlayerCard current={current} onNavigate={onNavigate} />
      <SidebarFooter onNavigate={onNavigate} />
    </aside>
  )
}
