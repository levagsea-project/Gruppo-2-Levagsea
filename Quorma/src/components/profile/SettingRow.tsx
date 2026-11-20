import type { ReactNode } from 'react'

type Props = { label: string; hint?: string; children: ReactNode }

// Una riga: nome + spiegazione a sinistra, controllo a destra.
export default function SettingRow({ label, hint, children }: Props) {
  return (
    <div className="pf-row">
      <span className="pf-row-text">
        <b>{label}</b>
        {hint && <small>{hint}</small>}
      </span>
      {children}
    </div>
  )
}
