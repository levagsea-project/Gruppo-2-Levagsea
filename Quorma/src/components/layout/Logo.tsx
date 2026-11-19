// Logo in cima alla barra laterale: riporta alla Home, come il logo della barra in alto su telefono.
export default function Logo({ onHome }: { onHome: () => void }) {
  return (
    <a
      className="q-navata__logo"
      href="#"
      aria-label="Quorma – Home"
      onClick={(e) => {
        e.preventDefault()
        onHome()
      }}
    >
      <img src="/images/ui/logo-quorma.png" alt="Quorma" />
    </a>
  )
}
