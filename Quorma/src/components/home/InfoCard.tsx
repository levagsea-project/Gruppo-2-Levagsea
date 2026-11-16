import type { ReactNode } from 'react'
import Diamond from '../ui/Diamond'

// Card generica con la forma ad arco. TradesCard e NewsCard la riusano.
type Props = {
  icon: string
  title: string
  meta: string // testo piccolo a destra del titolo
  children: ReactNode // il contenuto sotto l'intestazione
}

export default function InfoCard({ icon, title, meta, children }: Props) {
  return (
    <section className="info-card">
      <Diamond className="info-card-diamond" />
      <header className="info-card-header">
        <span className="info-card-icon">{icon}</span>
        <h2>{title}</h2>
        <span className="info-card-line" />
        <span className="info-card-meta">{meta}</span>
      </header>
      {children}
    </section>
  )
}
