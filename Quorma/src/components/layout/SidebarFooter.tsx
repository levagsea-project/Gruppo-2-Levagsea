import { footerLinks } from '../../data/navItems'

// Link piccoli in fondo + copyright.
export default function SidebarFooter() {
  return (
    <footer className="sidebar-footer">
      {footerLinks.map((row, i) => (
        <div className="footer-row" key={i}>
          {row.map((link) => (
            <a href="#" key={link}>{link}</a>
          ))}
        </div>
      ))}
      <p>© 2026 QUORMA</p>
    </footer>
  )
}
