import { useEffect, useMemo, useRef, useState, type CSSProperties } from 'react'
import { factions } from '../../data/factions'
import { creatureTier } from '../../lib/cards'
import { packName, pendingCount, pendingPacks, removePending } from '../../lib/shop'
import CardFace from '../cards/CardFace'
import PageTitle from '../layout/PageTitle'
import '../../styles/collection.css'

type Props = { queue: string[]; onCollection: () => void; onShop: () => void; onPacks: () => void }

// Apertura delle buste (design: Pagina-Apertura). Le carte sono già nella collezione dal momento dell'acquisto:
// qui si rivelano. Tutte le buste scelte stanno in una sola schermata, una riga per busta, e le carte si girano
// da sole una dopo l'altra (le rare si fermano un attimo col lampo). Premere una carta la gira subito,
// "Rivela tutte" salta la cascata. Quando una busta ha tutte le carte girate esce da "Buste da aprire".
export default function PackOpeningPage({ queue, onCollection, onShop, onPacks }: Props) {
  const packs = useMemo(() => {
    const all = pendingPacks()
    return queue.flatMap((id) => all.filter((p) => p.id === id))
  }, [queue])
  // tutte le carte in fila, nell'ordine in cui la cascata le gira; chiave "busta:carta"
  const order = useMemo(
    () => packs.flatMap((p, pi) => p.cards.map((c, i) => ({ key: `${pi}:${i}`, pi, i, tier: c.kind === 'CREATURE' ? creatureTier(c.rank) : 0 }))),
    [packs],
  )
  const [revealed, setRevealed] = useState<Set<string>>(new Set())
  const [cascading, setCascading] = useState(true)
  const revealedRef = useRef(revealed)
  revealedRef.current = revealed
  const rows = useRef<(HTMLElement | null)[]>([])
  const done = useRef(new Set<string>())

  const multi = packs.length > 1
  const total = order.length
  const allRevealed = total > 0 && revealed.size === total
  const reveal = (key: string) => setRevealed((r) => (r.has(key) ? r : new Set(r).add(key)))
  const revealAll = () => {
    setCascading(false)
    setRevealed(new Set(order.map((o) => o.key)))
  }

  // la cascata: più buste ci sono, più è svelta; dopo una carta rara una pausa per il lampo
  useEffect(() => {
    if (!cascading || !total) return
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      revealAll()
      return
    }
    const step = total <= 5 ? 320 : total <= 15 ? 200 : 130
    let k = 0
    let timer = 0
    const next = () => {
      while (k < total && revealedRef.current.has(order[k].key)) k++
      if (k >= total) {
        setCascading(false)
        return
      }
      const o = order[k++]
      reveal(o.key)
      if (multi && o.i === 0 && o.pi > 0) rows.current[o.pi]?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
      timer = window.setTimeout(next, step + (o.tier >= 3 ? 650 : 0))
    }
    timer = window.setTimeout(next, 650) // dopo l'arrivo delle carte
    return () => window.clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cascading, order])

  // una busta con tutte le carte girate esce dalle buste da aprire
  useEffect(() => {
    packs.forEach((p, pi) => {
      if (done.current.has(p.id) || !p.cards.every((_, i) => revealed.has(`${pi}:${i}`))) return
      done.current.add(p.id)
      removePending(p.id)
    })
  }, [revealed, packs])

  if (!packs.length) {
    return (
      <main className="page open-page">
        <PageTitle title="APERTURA" subtitle="NESSUN PACCHETTO" />
        <div className="coll-vuoto">
          <p>Non ci sono pacchetti da aprire.</p>
          <button type="button" className="q-btn q-btn--argento" onClick={onShop}><span className="q-btn__in">Vai al negozio</span></button>
        </div>
      </main>
    )
  }

  const known = (pi: number, i: number) => revealed.has(`${pi}:${i}`) && packs[pi].isNew[i] !== null && packs[pi].isNew[i] !== undefined
  const countNew = (pi: number) => packs[pi].cards.filter((_, i) => known(pi, i) && packs[pi].isNew[i]).length
  const nNew = packs.reduce((s, _, pi) => s + countNew(pi), 0)
  const nDup = packs.reduce((s, p, pi) => s + p.cards.filter((_, i) => known(pi, i) && !p.isNew[i]).length, 0)

  const first = packs[0]
  const sameKind = packs.every((p) => p.packType === first.packType && p.faction === first.faction)
  const subtitle = sameKind ? `${packName(first.packType)} · ${factions[first.faction].name}` : `${packs.length} pacchetti`

  return (
    <main className={`page open-page ${multi ? 'open-page--multi' : ''}`}>
      <div className="open-testa">
        <PageTitle title="APERTURA" subtitle={subtitle} />
        {multi && <span className="coll-nota">{packs.length} pacchetti · {total} carte</span>}
      </div>
      <p className="open-suggerimento" aria-live="polite">
        {allRevealed ? 'Tutte le carte sono nella tua collezione.' : cascading ? 'Le carte si stanno girando.' : 'Premi una carta per girarla.'}
      </p>

      <div className="open-buste">
        {packs.map((p, pi) => {
          const nuove = countNew(pi)
          return (
            <section key={p.id} ref={(el) => { rows.current[pi] = el }} className="open-busta" aria-label={multi ? `Pacchetto ${pi + 1}` : 'Carte del pacchetto'}>
              {multi && (
                <h2 className="open-busta__testa">
                  <span>Pacchetto {pi + 1}</span>
                  {nuove > 0 && <span className="open-busta__nuove">{nuove === 1 ? '1 nuova' : `${nuove} nuove`}</span>}
                </h2>
              )}
              <ul className="open-carte" style={{ '--n': p.cards.length } as CSSProperties}>
                {p.cards.map((c, i) => {
                  const key = `${pi}:${i}`
                  const on = revealed.has(key)
                  const isNew = p.isNew[i]
                  const note = !on || isNew === null || isNew === undefined ? '' : isNew ? 'Nuova' : 'Doppione'
                  return (
                    <li key={c.instanceId} className="open-carta" style={{ '--i': Math.min(i + pi * 2, 14) } as CSSProperties}>
                      <button
                        type="button"
                        className={`q-giro b-carta ${c.kind === 'CREATURE' ? `b-carta--t${creatureTier(c.rank)}` : ''} ${on && isNew ? 'b-nuova' : ''}`}
                        aria-pressed={on}
                        aria-label={on ? `${c.name}${note ? `, ${note.toLowerCase()}` : ''}` : `Carta coperta ${i + 1}, premi per girarla`}
                        onClick={() => reveal(key)}
                      >
                        <span className="q-giro__dentro">
                          <span className="q-giro__faccia q-cg">
                            <span className="q-cg__campo b-dorso"><img src="/images/ui/logo-quorma.png" alt="" /></span>
                          </span>
                          <span className="q-giro__faccia q-giro__retro">
                            <CardFace card={c} sizes={multi ? '(max-width: 767px) 30vw, 160px' : '(max-width: 767px) 44vw, 240px'} />
                          </span>
                        </span>
                      </button>
                      <span className={`b-nota ${isNew ? 'b-nota--nuova' : ''}`}>{note}</span>
                    </li>
                  )
                })}
              </ul>
            </section>
          )
        })}
      </div>

      <div className="open-azioni">
        <div className="open-conte" aria-live="polite">
          <span className="m-conta"><b>{revealed.size} / {total}</b><small>Rivelate</small></span>
          <span className="m-conta"><b>{nNew}</b><small>Nuove</small></span>
          <span className="m-conta"><b>{nDup}</b><small>Doppioni</small></span>
        </div>
        <div className="open-bottoni">
          {!allRevealed && (
            <button type="button" className="q-btn q-btn--acciaio" onClick={revealAll}>
              <span className="q-btn__in">Rivela tutte</span>
            </button>
          )}
          <button type="button" className="q-btn q-btn--acciaio" onClick={onCollection}><span className="q-btn__in">Vai alla Collezione</span></button>
          {/* finite queste, se restano altre buste comprate si torna a sceglierle */}
          {allRevealed && pendingCount() > 0 && (
            <button type="button" className="q-btn q-btn--argento" onClick={onPacks}><span className="q-btn__in">Altri pacchetti da aprire</span></button>
          )}
        </div>
      </div>
    </main>
  )
}
