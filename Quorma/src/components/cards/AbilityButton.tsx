import { useEffect, useRef, useState, type MouseEvent, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import type { AbilityText } from '../../lib/abilities'
import '../../styles/card-frame.css'

const kindOf = (a: AbilityText) => (a.id.startsWith('EVO_') ? 'Evoluzione' : a.id.startsWith('K_') ? 'Parola chiave' : 'Effetto')
const plainText = (a: AbilityText) => a.text.replace(/^Evoluzione — /, '')

function Explanation({ ability }: { ability: AbilityText }) {
  return (
    <>
      <p>{plainText(ability)}</p>
      {ability.id.startsWith('EVO_') && <p className="q-abilita-pop__nota">Si attiva una sola volta per partita.</p>}
    </>
  )
}

// La finestra con la spiegazione. Sta in document.body perché può aprirsi da dentro la carta ingrandita:
// <dialog> la mette sopra tutto, si chiude con Esc o toccando fuori e riporta il focus su chi l'ha aperta.
function AbilityDialog({ ability, onClose }: { ability: AbilityText; onClose: () => void }) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null
    const dialog = ref.current
    if (dialog && !dialog.open) dialog.showModal()
    return () => opener?.focus?.()
  }, [])

  const onClick = (e: MouseEvent<HTMLDialogElement>) => {
    e.stopPropagation()
    if (e.target === ref.current) onClose()
  }
  const related = ability.related ?? []

  return createPortal(
    <dialog
      ref={ref}
      className="q-abilita-pop"
      aria-labelledby="q-abilita-titolo"
      onClick={onClick}
      onPointerDown={(e) => e.stopPropagation()}
      onCancel={(e) => e.stopPropagation()}
      onClose={(e) => {
        e.stopPropagation()
        onClose()
      }}
    >
      <div className="q-abilita-pop__dentro">
        <span className="q-abilita-pop__tipo">{kindOf(ability)}</span>
        <h2 id="q-abilita-titolo">{ability.free ? 'Effetto della carta' : ability.name}</h2>
        <Explanation ability={ability} />
        {related.map((r) => (
          <section key={r.id} className="q-abilita-pop__collegata">
            <h3>{r.name}</h3>
            <Explanation ability={r} />
          </section>
        ))}
        <button type="button" className="q-abilita-pop__ok" onClick={onClose}>Ho capito</button>
      </div>
    </dialog>,
    document.body,
  )
}

/** Una parola chiave premibile: apre il pop-up che spiega cosa fa. Non va messa dentro un altro bottone. */
export default function AbilityButton({ ability, className = '', children }: { ability: AbilityText; className?: string; children: ReactNode }) {
  const [open, setOpen] = useState(false)
  return (
    <>
      <button
        type="button"
        className={`q-abilita ${className}`}
        aria-haspopup="dialog"
        aria-label={`Cosa fa ${ability.free ? 'questo effetto' : ability.name}`}
        onClick={(e) => {
          e.stopPropagation()
          setOpen(true)
        }}
      >
        {children}
      </button>
      {open && <AbilityDialog ability={ability} onClose={() => setOpen(false)} />}
    </>
  )
}
