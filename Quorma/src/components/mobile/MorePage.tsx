import { useNavigate } from 'react-router'
import type { Page } from '../../data/navItems'
import { footerLinks } from '../../data/navItems'
import { currentPlayer } from '../../data/player'
import { clearSession } from '../../lib/auth'
import NavIcon from '../layout/NavIcon'
import Arch from './Arch'
import MobileRow from './MobileRow'

// Un gruppo dell'elenco: titoletto + riquadro con le righe.
function Group({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mb-gruppo" aria-label={title}>
      <span className="mb-etichetta">{title}</span>
      <div className="q-tela">
        <div className="q-tela__in mb-lista">{children}</div>
      </div>
    </section>
  )
}

const ProfileIcon = () => (
  <svg className="q-navata__ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <circle cx="12" cy="8" r="4" />
    <path d="M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6" />
  </svg>
)
const SettingsIcon = () => (
  <svg className="q-navata__ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <path d="M4 7h10M18 7h2M4 17h4M12 17h8" />
    <circle cx="16" cy="7" r="2" />
    <circle cx="10" cy="17" r="2" />
  </svg>
)

// Pagina "Altro" (solo telefono): tutte le sezioni che non stanno nella barra in basso.
// Le righe senza onClick sono sezioni che non esistono ancora: MobileRow le mostra "in arrivo".
export default function MorePage({ onNavigate }: { onNavigate: (page: Page) => void }) {
  const navigate = useNavigate()
  const player = currentPlayer()

  const logout = () => {
    clearSession()
    navigate('/')
  }

  return (
    <main className="page more-page">
      <div className="mb-targa">
        <h1>Altro</h1>
        <span>Tutte le sezioni</span>
      </div>

      <Group title="Gioco">
        <MobileRow title="Scambi" detail="Scambia i doppioni con altri giocatori" icon={<Arch><NavIcon name="scambi" /></Arch>} />
        <MobileRow title="Sfide" detail="Sfida gli altri giocatori" icon={<Arch><NavIcon name="sfide" /></Arch>} />
        <MobileRow title="Negozio" detail="Buste e offerte" icon={<Arch><NavIcon name="negozio" /></Arch>} />
        <MobileRow
          title="Regole"
          detail="Regolamento base · v1.0-alpha"
          icon={<Arch><NavIcon name="regole" /></Arch>}
          onClick={() => onNavigate('regole')}
        />
      </Group>

      <Group title="Account">
        <MobileRow title="Profilo" detail={player.name} icon={<Arch><ProfileIcon /></Arch>} onClick={() => onNavigate('profilo')} />
        <MobileRow title="Impostazioni" detail="Account, partita, audio" icon={<Arch><SettingsIcon /></Arch>} />
      </Group>

      <Group title="Informazioni">
        {footerLinks.info.map((link) => (
          <MobileRow key={link.page} title={link.label} onClick={() => onNavigate(link.page)} />
        ))}
      </Group>

      <Group title="Note legali">
        {footerLinks.legal.map((link) => (
          <MobileRow key={link.page} title={link.label} onClick={() => onNavigate(link.page)} />
        ))}
      </Group>

      <div className="mb-esci">
        <button type="button" className="q-btn q-btn--acciaio" onClick={logout}>
          <span className="q-btn__in">Esci</span>
        </button>
        <span>© 2026 QUORMA</span>
      </div>
    </main>
  )
}
