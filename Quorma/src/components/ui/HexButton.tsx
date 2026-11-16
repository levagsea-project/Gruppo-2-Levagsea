// Bottone con le punte laterali (esagono allungato). Tre varianti di colore.
type Props = {
  children: string
  variant?: 'dark' | 'light' | 'primary'
  icon?: string
  onClick?: () => void
}

export default function HexButton({ children, variant = 'dark', icon, onClick }: Props) {
  return (
    <button className={`hex-btn hex-${variant}`} onClick={onClick}>
      <span className="hex-inner">
        {icon && <span className="hex-icon">{icon}</span>}
        {children}
      </span>
    </button>
  )
}
