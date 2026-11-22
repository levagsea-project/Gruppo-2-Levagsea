import { currencies } from '../../data/player'
import { useBalance } from '../../lib/balance'

// Icone delle due valute: anello per la Valuta 1, rombo petrolio per la Valuta 2.
const ICONS = [
  <svg key="v1" aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--silver-100)" strokeWidth="1.6">
    <circle cx="12" cy="12" r="9" />
    <circle cx="12" cy="12" r="4.5" />
  </svg>,
  <svg key="v2" aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="rgba(74,166,173,0.25)" stroke="var(--petrol-500)" strokeWidth="1.6" strokeLinejoin="round">
    <path d="M12 2 21 12l-9 10-9-10z" />
  </svg>,
]

// Le due valute in alto a destra, nel riquadro a guglia del design system.
// La Valuta 2 mostra l'ultimo saldo noto (lib/balance.ts). Il backend non ha una lettura del saldo: finché non è noto
// la barra si vede solo dove serve per comprare (always, nel Negozio), con un trattino al posto del numero.
export default function CurrencyBar({ always = false }: { always?: boolean }) {
  const credits = useBalance()
  if (credits === null && !always) return null
  return (
    <div className="currency-bar q-tela q-tela--guglia">
      <div className="q-tela__in">
        {currencies(credits).map((c, i) => (
          <div className="q-valore" key={c.label} title={c.value === null ? `${c.label}: saldo non disponibile` : `${c.label}: saldo dopo l'ultimo acquisto`}>
            {ICONS[i]}
            <span className="q-valore__testo">
              <span className="q-valore__etichetta">{c.label}</span>
              {c.value === null ? (
                <span className="q-valore__numero q-valore__numero--vuoto">
                  <span aria-hidden="true">—</span>
                  <span className="sr-only">saldo non disponibile</span>
                </span>
              ) : (
                <span className="q-valore__numero">{c.value}</span>
              )}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
