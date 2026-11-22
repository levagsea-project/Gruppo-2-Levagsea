import type { ReactNode } from 'react'

type Props = {
  title: string
  detail?: string // riga piccola sotto il titolo
  icon?: ReactNode // di solito un <Arch>; senza icona è una riga di solo testo
  onClick?: () => void // senza onClick la riga non è cliccabile
}

function Arrow() {
  return (
    <svg className="mb-freccia" aria-hidden="true" width="8" height="14" viewBox="0 0 8 14">
      <path d="M1 1l6 6-6 6" fill="none" stroke="currentColor" strokeWidth="1.6" />
    </svg>
  )
}

// Una riga degli elenchi mobile (Home, pagina Altro): icona, testo, freccia.
export default function MobileRow({ title, detail, icon, onClick }: Props) {
  const className = icon ? 'mb-riga' : 'mb-riga mb-riga--testo'
  const text = icon ? (
    <span>
      <b>{title}</b>
      {detail && <small>{detail}</small>}
    </span>
  ) : (
    <span>{title}</span>
  )

  if (!onClick) {
    return (
      <div className={`${className} mb-riga--presto`}>
        {icon}
        {text}
      </div>
    )
  }

  return (
    <button type="button" className={className} onClick={onClick}>
      {icon}
      {text}
      <Arrow />
    </button>
  )
}
