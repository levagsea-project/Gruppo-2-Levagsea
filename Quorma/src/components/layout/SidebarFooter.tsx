import type { InfoLink, Page } from '../../data/navItems'
import { footerLinks } from '../../data/navItems'

// Un link del footer: porta alla sua pagina.
function FooterLink({ link, onNavigate }: { link: InfoLink; onNavigate: (page: Page) => void }) {
  return (
    <a
      href="#"
      onClick={(e) => {
        e.preventDefault()
        onNavigate(link.page)
      }}
    >
      {link.label}
    </a>
  )
}

// Link piccoli in fondo + copyright.
export default function SidebarFooter({ onNavigate }: { onNavigate: (page: Page) => void }) {
  return (
    <footer className="q-navata__piede">
      <div className="q-navata__info">
        {footerLinks.info.map((link) => (
          <FooterLink key={link.page} link={link} onNavigate={onNavigate} />
        ))}
      </div>
      <div className="q-navata__legali">
        {footerLinks.legal.map((link) => (
          <FooterLink key={link.page} link={link} onNavigate={onNavigate} />
        ))}
      </div>
      <span className="q-navata__copy">© 2026 QUORMA</span>
    </footer>
  )
}
