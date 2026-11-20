import { currencies, currentPlayer } from '../../data/player'

// Saldo di una valuta: un trattino finché il backend non lo fornisce.
const show = (value: string | null) => value ?? '—'
const speak = (label: string, value: string | null) => `${label}: ${value ?? 'saldo in arrivo'}`

// Barra in alto su telefono: logo, valute, avatar del giocatore.
export default function MobileTopBar({ onHome, onProfile }: { onHome: () => void; onProfile: () => void }) {
  const [v1, v2] = currencies
  const player = currentPlayer()

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
      <div className="mb-valute" role="group" aria-label={`${speak(v1.label, v1.value)} · ${speak(v2.label, v2.value)}`}>
        <span aria-hidden="true">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--silver-100)" strokeWidth="1.8">
            <circle cx="12" cy="12" r="9" />
            <circle cx="12" cy="12" r="4.5" />
          </svg>
          {show(v1.value)}
        </span>
        <span aria-hidden="true">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="rgba(74,166,173,0.25)" stroke="var(--petrol-500)" strokeWidth="1.8" strokeLinejoin="round">
            <path d="M12 2 21 12l-9 10-9-10z" />
          </svg>
          {show(v2.value)}
        </span>
      </div>
      <button type="button" className="mb-avatar" aria-label={`Profilo di ${player.name}`} onClick={onProfile}>
        <span className="q-ogiva">
          <span className="q-ogiva__in">
            <b>{player.initials}</b>
          </span>
        </span>
      </button>
    </header>
  )
}
