// Bottone con le punte laterali (esagono allungato). Tre varianti di colore.
type Props = {
  children: string
  variant?: 'dark' | 'light' | 'primary'
  icon?: string
  onClick?: () => void
  disabled?: boolean
  title?: string // spiega perché il bottone è disattivato
}

export default function HexButton({ children, variant = 'dark', icon, onClick, disabled, title }: Props) {
  return (
    <button className={`hex-btn hex-${variant}`} onClick={onClick} disabled={disabled} title={title}>
      <span className="hex-inner">
        {icon && <span className="hex-icon">{icon}</span>}
        {children}
      </span>
    </button>
  )
}
