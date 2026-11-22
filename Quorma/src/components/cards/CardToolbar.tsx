import { useEffect, useId, useRef, useState, type MouseEvent, type PointerEvent } from 'react'
import { SORT_OPTIONS, type CardSort } from '../../lib/cardFilters'
import '../../styles/card-tools.css'

export type FilterOption = { value: string; label: string; count: number; suit?: string }
export type FilterGroup = { id: string; label: string; value: string; defaultValue: string; options: FilterOption[]; onChange: (value: string) => void }

type Props = {
  query: string
  onQuery: (value: string) => void
  sort: CardSort
  onSort: (value: CardSort) => void
  groups: FilterGroup[]
  resultCount: number
  onReset: () => void // azzera filtri e ordine, non il testo cercato
}

const cards = (n: number) => (n === 1 ? '1 carta' : `${n} carte`)
const sortLabel = (sort: CardSort) => SORT_OPTIONS.find((o) => o.value === sort)?.label ?? ''

function CloseIcon() {
  return (
    <svg aria-hidden="true" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round">
      <path d="M6 6l12 12M18 6 6 18" />
    </svg>
  )
}

function OptionGroup({ label, name, value, options, onChange, className = '' }: { label: string; name: string; value: string; options: { value: string; label: string; count?: number; suit?: string }[]; onChange: (value: string) => void; className?: string }) {
  return (
    <fieldset className={`card-tools__gruppo ${className}`}>
      <legend>{label}</legend>
      {options.map((o) => (
        <label key={o.value} className="card-tools__opzione">
          <input type="radio" name={name} value={o.value} checked={value === o.value} onChange={() => onChange(o.value)} />
          {o.suit && <span className={`deck-seme deck-seme--${o.suit.toLowerCase()}`} aria-hidden="true" />}
          <span>{o.label}</span>
          {o.count !== undefined && <small>{o.count}</small>}
        </label>
      ))}
    </fieldset>
  )
}

// Pannello dei filtri: di lato su desktop, dal basso su telefono. Il risultato si aggiorna mentre scegli.
function FilterSheet({ groups, sort, onSort, resultCount, canReset, onReset, onClose }: Pick<Props, 'groups' | 'sort' | 'onSort' | 'resultCount' | 'onReset'> & { canReset: boolean; onClose: () => void }) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const downOnBackdrop = useRef(false)
  const id = useId()

  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null
    const dialog = dialogRef.current
    if (dialog && !dialog.open) dialog.showModal()
    return () => opener?.focus?.()
  }, [])

  return (
    <dialog
      ref={dialogRef}
      className="card-tools__foglio"
      aria-labelledby={`${id}-titolo`}
      onCancel={(e) => {
        e.preventDefault()
        onClose()
      }}
      onPointerDown={(e: PointerEvent<HTMLDialogElement>) => (downOnBackdrop.current = e.target === dialogRef.current)}
      onClick={(e: MouseEvent<HTMLDialogElement>) => e.target === dialogRef.current && downOnBackdrop.current && onClose()}
    >
      <div className="card-tools__testa">
        <h2 id={`${id}-titolo`}>Filtri</h2>
        <button type="button" className="card-tools__chiudi" aria-label="Chiudi i filtri" onClick={onClose}><CloseIcon /></button>
      </div>
      <div className="card-tools__corpo">
        <OptionGroup className="card-tools__gruppo--ordine" label="Ordina per" name={`${id}-ordine`} value={sort} options={SORT_OPTIONS} onChange={(v) => onSort(v as CardSort)} />
        {groups.map((g) => (
          <OptionGroup key={g.id} label={g.label} name={`${id}-${g.id}`} value={g.value} options={g.options} onChange={g.onChange} />
        ))}
      </div>
      <div className="card-tools__piede">
        <button type="button" className="q-btn q-btn--acciaio" disabled={!canReset} onClick={onReset}>
          <span className="q-btn__in">Azzera</span>
        </button>
        <button type="button" className="q-btn q-btn--argento" onClick={onClose}>
          <span className="q-btn__in">Mostra {cards(resultCount)}</span>
        </button>
      </div>
    </dialog>
  )
}

// Ricerca, ordine e filtri sopra una griglia di carte. I filtri stanno nel pannello: fuori restano solo quelli attivi.
export default function CardToolbar({ query, onQuery, sort, onSort, groups, resultCount, onReset }: Props) {
  const [open, setOpen] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const active = groups.filter((g) => g.value !== g.defaultValue)
  const canReset = active.length > 0 || sort !== 'base'

  return (
    <div className="card-tools">
      <div className="card-tools__riga">
        <div className="card-tools__cerca">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-4-4" />
          </svg>
          <input ref={inputRef} type="search" value={query} placeholder="Cerca per nome o abilità" aria-label="Cerca carte per nome o abilità" onChange={(e) => onQuery(e.target.value)} />
          {query && (
            <button
              type="button"
              aria-label="Cancella la ricerca"
              onClick={() => {
                onQuery('')
                inputRef.current?.focus()
              }}
            >
              <CloseIcon />
            </button>
          )}
        </div>
        <label className="card-tools__ordine">
          <span>Ordina</span>
          <select value={sort} onChange={(e) => onSort(e.target.value as CardSort)}>
            {SORT_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
          <svg aria-hidden="true" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="m6 9 6 6 6-6" /></svg>
        </label>
        <button
          type="button"
          className={`card-tools__filtri ${groups.length === 0 ? 'card-tools__filtri--solo-ordine' : ''}`}
          aria-haspopup="dialog"
          aria-expanded={open}
          aria-label={active.length ? `Filtri e ordine, ${active.length} attivi` : 'Filtri e ordine'}
          onClick={() => setOpen(true)}
        >
          <svg aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <path d="M4 7h10M18 7h2M4 17h2M10 17h10" />
            <circle cx="16" cy="7" r="2" />
            <circle cx="8" cy="17" r="2" />
          </svg>
          Filtri
          {active.length > 0 && <b>{active.length}</b>}
        </button>
      </div>

      {canReset && (
        <div className="card-tools__attivi" role="group" aria-label="Filtri attivi">
          {active.map((g) => (
            <button key={g.id} type="button" aria-label={`Togli il filtro ${g.label}: ${g.options.find((o) => o.value === g.value)?.label}`} onClick={() => g.onChange(g.defaultValue)}>
              {g.options.find((o) => o.value === g.value)?.label}
              <CloseIcon />
            </button>
          ))}
          {/* su desktop l'ordine si legge già nel menu: l'etichetta serve solo su telefono */}
          {sort !== 'base' && (
            <button type="button" className="card-tools__attivo-ordine" aria-label={`Togli l'ordine ${sortLabel(sort)}`} onClick={() => onSort('base')}>
              {sortLabel(sort)}
              <CloseIcon />
            </button>
          )}
        </div>
      )}

      <span className="sr-only" role="status">{cards(resultCount)}</span>

      {open && <FilterSheet groups={groups} sort={sort} onSort={onSort} resultCount={resultCount} canReset={canReset} onReset={onReset} onClose={() => setOpen(false)} />}
    </div>
  )
}
