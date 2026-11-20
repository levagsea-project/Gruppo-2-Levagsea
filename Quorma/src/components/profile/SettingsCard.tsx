import type { ReactNode } from 'react'

// Riquadro delle impostazioni con titoletto (es. ACCOUNT, AUDIO).
export default function SettingsCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="q-tela pf-card" aria-label={title}>
      <div className="q-tela__in">
        <span className="pf-label">{title}</span>
        {children}
      </div>
    </section>
  )
}
