import { currencies, player } from '../../data/player'

// Barra in alto su telefono: logo, valute, avatar del giocatore.
export default function MobileTopBar({ onHome }: { onHome: () => void }) {
  const [v1, v2] = currencies

  return (
    <header className="mb-top">
      <a
        className="mb-logo"
        href="#"
        aria-label="Quorma – Home"
        onClick={(e) => {
          e.preventDefault()
          onHome()
        }}
      >
        <img src="/images/ui/logo-quorma.png" alt="Quorma" />
      </a>
      <div className="mb-valute" aria-label={`${v1.label}: ${v1.value} · ${v2.label}: ${v2.value}`}>
        <span>
          <svg aria-hidden="true" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--silver-100)" strokeWidth="1.8">
            <circle cx="12" cy="12" r="9" />
            <circle cx="12" cy="12" r="4.5" />
          </svg>
          {v1.value}
        </span>
        <span>
          <svg aria-hidden="true" width="14" height="14" viewBox="0 0 24 24" fill="rgba(74,166,173,0.25)" stroke="var(--petrol-500)" strokeWidth="1.8" strokeLinejoin="round">
            <path d="M12 2 21 12l-9 10-9-10z" />
          </svg>
          {v2.value}
        </span>
      </div>
      {/* Il profilo non esiste ancora: l'avatar per ora non porta da nessuna parte */}
      <span className="mb-avatar" role="img" aria-label={`${player.name}, livello ${player.level}`}>
        <span className="q-ogiva">
          <span className="q-ogiva__in">
            <b>{player.initials}</b>
          </span>
        </span>
      </span>
    </header>
  )
}
