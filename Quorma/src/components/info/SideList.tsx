// Lista della colonna a sinistra: titoletto con quadratino + voci. La voce attiva è la barra chiara.
export type SideItem = { label: string; count?: number }

type Props = {
  title: string
  items: SideItem[]
  active: number // indice della voce attiva
  onSelect?: (index: number) => void
}

export default function SideList({ title, items, active, onSelect }: Props) {
  return (
    <nav className="info-list" aria-label={title}>
      <span className="info-list-title">{title}</span>
      {items.map((item, i) => (
        <button
          key={item.label}
          type="button"
          className="info-list-item"
          aria-current={i === active ? 'true' : undefined}
          onClick={() => onSelect?.(i)}
        >
          <span className="info-list-label">{item.label}</span>
          {item.count !== undefined && <span className="info-list-count">{item.count}</span>}
        </button>
      ))}
    </nav>
  )
}
