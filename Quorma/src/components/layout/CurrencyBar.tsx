import { currencies } from '../../data/player'

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
// Finché il backend non dà il saldo, al posto del numero c'è un trattino ("in arrivo").
export default function CurrencyBar() {
  return (
    <div className="currency-bar q-tela q-tela--guglia">
      <div className="q-tela__in">
        {currencies.map((c, i) => (
          <div className="q-valore" key={c.label} title={c.value === null ? `${c.label}: saldo in arrivo` : undefined}>
            {ICONS[i]}
            <span className="q-valore__testo">
              <span className="q-valore__etichetta">{c.label}</span>
              {c.value === null ? (
                <span className="q-valore__numero q-valore__numero--vuoto">
                  <span aria-hidden="true">—</span>
                  <span className="sr-only">saldo in arrivo</span>
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
