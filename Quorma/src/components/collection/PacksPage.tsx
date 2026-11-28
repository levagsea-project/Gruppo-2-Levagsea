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
  const [qty, setQty] = useState(1) // quante buste del tipo scelto aprire insieme
  const n = groups.length
  const i = n ? index % n : 0
  const sel = groups[i]
  const owned = sel?.ids.length ?? 0
  const q = Math.min(Math.max(qty, 1), Math.max(owned, 1))
  const go = (step: number) => {
    setIndex((v) => (((v + step) % n) + n) % n)
    setQty(1)
  }
  const more = (d: number) => setQty(Math.min(Math.max(q + d, 1), owned))

  // tastiera: frecce per scorrere le buste, + e − per quante aprirne, Esc per tornare alla Collezione
  // (non mentre si scrive in un campo)
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLElement && e.target.closest('input, textarea, select, dialog')) return
      if (e.key === 'Escape') onCollection()
      if (e.key === '+' || e.key === '=') more(1)
      if (e.key === '-') more(-1)
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
            {/* quante aprirne insieme: si aprono tutte in una sola schermata. C'è sempre, anche con una busta sola
                (spento), così la colonna non cambia altezza passando da un tipo all'altro */}
            <div className="buste-quante" role="group" aria-labelledby="buste-quante-titolo">
              <span id="buste-quante-titolo" className="q-elenco__titolo">Quante aprirne</span>
              <div className="buste-quante__riga">
                <button type="button" className={`m-chip buste-passo ${q <= 1 ? 'm-chip--off' : ''}`} aria-label="Una busta in meno" disabled={q <= 1} onClick={() => more(-1)}>−</button>
                <output className="buste-numero" aria-live="polite">{q} <span className="m-chip__n">di {owned}</span></output>
                <button type="button" className={`m-chip buste-passo ${q >= owned ? 'm-chip--off' : ''}`} aria-label="Una busta in più" disabled={q >= owned} onClick={() => more(1)}>+</button>
                <button type="button" className={`m-chip buste-tutte ${owned <= 1 ? 'm-chip--off' : ''}`} data-on={owned > 1 && q === owned} aria-pressed={owned > 1 && q === owned} disabled={owned <= 1} onClick={() => setQty(owned)}>Tutte</button>
              </div>
            </div>
            <button type="button" className="q-btn q-btn--argento q-btn--gioca buste-apri" onClick={() => onOpen(sel.ids.slice(0, q))}>
              <span className="q-btn__in">{q === 1 ? 'Apri la busta' : 'Apri le buste'}<span className="buste-conta">× {q}</span></span>
            </button>
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

        {/* Carosello: la busta scelta al centro, le altre ai lati nell'ordine delle frecce (le ultime girano a sinistra).
            Ogni busta ha la sua posizione (data-d = distanza dalla scelta) e scivola alla nuova quando si cambia. */}
        <div className="buste-scena" role="group" aria-roledescription="carosello" aria-label="Buste da aprire">
          {groups.map((g, gi) => {
            let d = (gi - i + n) % n
            if (d > n / 2) d -= n
            const far = Math.abs(d) > 2
            const pos = far ? (d < 0 ? '-lontano' : 'lontano') : String(d)
            return (
              // la busta resta sempre montata (niente scatti quando cambia posto); le laterali hanno sopra un pulsante trasparente
              <div key={g.key} className="buste-slot" data-d={pos} aria-hidden={far || undefined}>
                <div className="buste-slot__busta">
                  <SealedPack code={g.packType} faction={g.faction} size={g.size} setName={g.setName} count={g.ids.length} />
                </div>
                {d !== 0 && (
                  <button type="button" className="buste-slot__vai" tabIndex={far ? -1 : undefined} aria-label={`Passa a ${packName(g.packType)}, ${factions[g.faction].name}`} onClick={() => go(d)} />
                )}
              </div>
            )
          })}
        </div>
      </div>
    </main>
  )
}
