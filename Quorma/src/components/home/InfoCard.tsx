import { useId, type ReactNode } from 'react'

// Card ad arco a sesto acuto del design system, con il pinnacolo in cima. DefenseCard, TradesCard e NewsCard la riusano.
type Props = {
  seed: 'vincolo' | 'frattura' | 'spira' | 'vestigio' // icona del seme accanto al titolo (public/images/ui/semi)
  title: string
  meta: string // testo piccolo a destra del titolo
  metaTone?: 'ember' // arancione: c'è qualcosa di vero in attesa
  lead: string // frase in evidenza
  children: ReactNode // testo e pulsante sotto
}

export default function InfoCard({ seed, title, meta, metaTone, lead, children }: Props) {
  const titleId = useId()

  return (
    <section className="q-tela q-tela--arco info-card" aria-labelledby={titleId}>
      <div className="q-tela__in">
        <header className="info-card-header">
          <span className={`info-card-seme info-card-seme--${seed}`} aria-hidden="true" />
          <h2 id={titleId}>{title}</h2>
          <span className="info-card-line" aria-hidden="true" />
          <span className={`info-card-meta ${metaTone ? `info-card-meta--${metaTone}` : ''}`}>{meta}</span>
        </header>
        <p className="info-card-lead">{lead}</p>
        {children}
      </div>
      <span className="q-borchia q-borchia--n" aria-hidden="true" />
    </section>
  )
}

// Pulsante d'acciaio con la freccia, in fondo a ogni card.
// Senza onClick il pulsante resta visibile, attenuato e non premibile.
export function CardButton({ children, onClick }: { children: string; onClick?: () => void }) {
  if (!onClick) {
    return (
      <button type="button" className="q-btn q-btn--acciaio info-card-btn" aria-disabled="true">
        <span className="q-btn__in">{children}</span>
      </button>
    )
  }

  return (
    <button type="button" className="q-btn q-btn--acciaio info-card-btn" onClick={onClick}>
      <span className="q-btn__in">
        {children}
        <svg aria-hidden="true" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M5 12h14" />
          <path d="m13 6 6 6-6 6" />
        </svg>
      </span>
    </button>
  )
}
