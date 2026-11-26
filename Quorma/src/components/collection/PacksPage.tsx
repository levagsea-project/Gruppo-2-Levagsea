import { useEffect, useMemo, useState, type CSSProperties } from 'react'
import { factions } from '../../data/factions'
import type { FactionId } from '../../types/deck'
import { packName, pendingPacks } from '../../lib/shop'
import CurrencyBar from '../layout/CurrencyBar'
import SealedPack from '../shop/SealedPack'
import '../../styles/collection.css'
import '../../styles/buste.css'

type Props = { onOpen: (ids: string[]) => void; onShop: () => void; onCollection: () => void }
type Group = { key: string; packType: string; faction: FactionId; size: number; setName?: string; ids: string[] }

// Buste da aprire, una alla volta come in una vetrina: a sinistra il nome della busta fra due frecce, lo stato,
// l'azione e la descrizione; a destra la busta grande, con la successiva inclinata dietro.
// Le buste comprate e non ancora rivelate sono raggruppate per tipo e fazione. Poi si passa alla pagina Apertura.
export default function PacksPage({ onOpen, onShop, onCollection }: Props) {
  const packs = pendingPacks()
  const groups = useMemo(() => {
    const map = new Map<string, Group>()
    for (const p of packs) {
      const key = `${p.packType}:${p.faction}`
      const g = map.get(key)
      if (g) g.ids.push(p.id)
      else map.set(key, { key, packType: p.packType, faction: p.faction, size: p.cards.length, setName: p.cards[0]?.sets[0]?.name, ids: [p.id] })
    }
    return [...map.values()]
  }, [packs])
  const [index, setIndex] = useState(0)
  const n = groups.length
  const i = n ? index % n : 0
  const sel = groups[i]
  const next = n > 1 ? groups[(i + 1) % n] : null
  const go = (step: number) => setIndex((v) => (((v + step) % n) + n) % n)

  // tastiera: frecce per scorrere le buste, Esc per tornare alla Collezione (non mentre si scrive in un campo)
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLElement && e.target.closest('input, textarea, select, dialog')) return
      if (e.key === 'Escape') onCollection()
      if (n < 2) return
      if (e.key === 'ArrowLeft') go(-1)
      if (e.key === 'ArrowRight') go(1)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  })

  const back = (
    <button type="button" className="buste-indietro" onClick={onCollection}>
      <svg viewBox="0 0 12 12" aria-hidden="true"><path d="M7.5 2 3.5 6l4 4" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" /></svg>
      Collezione
      <kbd className="buste-esc" aria-hidden="true">Esc</kbd>
    </button>
  )

  if (!sel) {
    return (
      <main className="page coll-page buste-page">
        <CurrencyBar />
        {back}
        <div className="buste-vuoto">
          <h1 className="buste-nome">Nessuna busta</h1>
          <p>Non hai buste da aprire. Le buste che compri nel negozio aspettano qui finché non le apri.</p>
          <button type="button" className="q-btn q-btn--argento q-btn--grande" onClick={onShop}><span className="q-btn__in">Vai al negozio</span></button>
        </div>
      </main>
    )
  }

  const f = factions[sel.faction]
  const name = packName(sel.packType)

  return (
    <main className="page coll-page buste-page" style={{ '--tinta': f.tint, '--tinta-chiara': f.tintText } as CSSProperties}>
      <CurrencyBar />
      <div className="buste-corpo">
        <section className="buste-lato" aria-label="Busta scelta">
          {back}

          {/* lo stemma della fazione: l'illustrazione in un rombo d'argento */}
          <span className="buste-stemma" aria-hidden="true"><span style={{ backgroundImage: `url(${f.cover})`, backgroundPosition: f.coverFocus }} /></span>

          <div className="buste-giro">
            {n > 1 && (
              <button type="button" className="buste-freccia" aria-label="Busta precedente" onClick={() => go(-1)}>
                <svg viewBox="0 0 16 32" aria-hidden="true"><path d="M12 3 4 16l8 13" /></svg>
              </button>
            )}
            <div className="buste-titolo" aria-live="polite">
              <h1 className="buste-nome">{name.replace(/^Pacchetto\s+/i, '')}</h1>
              <span className="buste-fazione">{f.name}</span>
            </div>
            {n > 1 && (
              <button type="button" className="buste-freccia" aria-label="Busta successiva" onClick={() => go(1)}>
                <svg viewBox="0 0 16 32" aria-hidden="true"><path d="M4 3l8 13-8 13" /></svg>
              </button>
            )}
          </div>

          <div className="buste-stato">
            <span className="buste-sigillo">
              {sel.ids.length === 1 ? '1 da aprire' : `${sel.ids.length} da aprire`}
              <svg viewBox="0 0 12 12" aria-hidden="true"><path d="M6 1l5 5-5 5-5-5z" /></svg>
            </span>
            {n > 1 && <span className="buste-posizione">{i + 1} di {n} tipi</span>}
          </div>

          <div className="buste-azioni">
            <button type="button" className="q-btn q-btn--argento q-btn--gioca buste-apri" onClick={() => onOpen([sel.ids[0]])}>
              <span className="q-btn__in">Apri la busta<span className="buste-conta">× 1</span></span>
            </button>
            {sel.ids.length > 1 && (
              <button type="button" className="q-btn q-btn--acciaio" onClick={() => onOpen(sel.ids)}>
                <span className="q-btn__in">Apri tutte ({sel.ids.length})</span>
              </button>
            )}
          </div>

          {/* Tela doppia e ornata del design system (styles/cattedrale.css): l'unica ornata della schermata */}
          <div className="q-tela q-tela--doppia q-tela--ornata buste-descrizione">
            <div className="q-tela__in">
              <span className="q-tela__titolo">{`Busta da ${sel.size} carte`}</span>
              <span className="q-divisorio-giglio" aria-hidden="true"><span className="q-orn q-orn--giglio" /></span>
              <p className="q-tela__testo">Le carte sono già nella tua collezione: si rivelano una alla volta, premendole per girarle, oppure tutte insieme.</p>
              {sel.setName && <span className="q-tela__nota">{sel.setName}</span>}
            </div>
          </div>
        </section>

        <div className="buste-scena">
          {next && (
            <button type="button" className="buste-dietro" aria-label={`Passa a ${packName(next.packType)}, ${factions[next.faction].name}`} onClick={() => go(1)}>
              <SealedPack code={next.packType} faction={next.faction} size={next.size} setName={next.setName} />
            </button>
          )}
          <div key={sel.key} className="buste-davanti">
            <SealedPack code={sel.packType} faction={sel.faction} size={sel.size} setName={sel.setName} count={sel.ids.length} />
          </div>
        </div>
      </div>
    </main>
  )
}
