import { footerLinks } from '../../data/navItems'

// Link piccoli in fondo + copyright.
export default function SidebarFooter() {
  return (
    <footer className="q-navata__piede">
      <div className="q-navata__info">
        {footerLinks.info.map((link) => (
          <a href="#" key={link}>{link}</a>
        ))}
      </div>
      <div className="q-navata__legali">
        {footerLinks.legal.map((link) => (
          <a href="#" key={link}>{link}</a>
        ))}
      </div>
      <span className="q-navata__copy">© 2026 QUORMA</span>
    </footer>
  )
}
