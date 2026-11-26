import { useEffect, useMemo, useState, type CSSProperties } from 'react'
import { factions } from '../../data/factions'
import { creatureTier } from '../../lib/cards'
import { packName, pendingCount, pendingPacks, removePending } from '../../lib/shop'
import CardFace from '../cards/CardFace'
import PageTitle from '../layout/PageTitle'
import '../../styles/collection.css'

type Props = { queue: string[]; onCollection: () => void; onShop: () => void; onPacks: () => void }

// Apertura delle buste (design: Pagina-Apertura). Le carte sono già nella collezione dal momento dell'acquisto:
// qui si rivelano. Quando sono tutte girate la busta esce da "Buste da aprire".
export default function PackOpeningPage({ queue, onCollection, onShop, onPacks }: Props) {
  const packs = useMemo(() => {
    const all = pendingPacks()
    return queue.flatMap((id) => all.filter((p) => p.id === id))
  }, [queue])
  const [index, setIndex] = useState(0)
  const [revealed, setRevealed] = useState<Set<number>>(new Set())

  const pack = packs[index]
  const allRevealed = !!pack && revealed.size === pack.cards.length
  const left = packs.length - index - 1

  useEffect(() => {
    if (allRevealed) removePending(pack.id)
  }, [allRevealed, pack])

  if (!pack) {
    return (
      <main className="page open-page">
        <PageTitle title="APERTURA" subtitle="NESSUNA BUSTA" />
        <div className="coll-vuoto">
          <p>Non ci sono buste da aprire.</p>
          <button type="button" className="q-btn q-btn--argento" onClick={onShop}><span className="q-btn__in">Vai al negozio</span></button>
        </div>
      </main>
    )
  }

  const known = (i: number) => revealed.has(i) && pack.isNew[i] !== null && pack.isNew[i] !== undefined
  const nNew = pack.cards.filter((_, i) => known(i) && pack.isNew[i]).length
  const nDup = pack.cards.filter((_, i) => known(i) && !pack.isNew[i]).length

  return (
    <main className="page open-page">
      <div className="open-testa">
        <PageTitle title="APERTURA" subtitle={`${packName(pack.packType)} · ${factions[pack.faction].name}`} />
        <span className="coll-nota">{left > 0 ? `Altre ${left} ${left === 1 ? 'busta' : 'buste'} da aprire` : 'Ultima busta'}</span>
      </div>
      <p className="open-suggerimento" aria-live="polite">
        {allRevealed ? 'Tutte le carte sono nella tua collezione.' : 'Premi una carta per girarla.'}
      </p>

      <ul key={pack.id} className="open-carte" aria-label="Carte della busta">
        {pack.cards.map((c, i) => {
          const on = revealed.has(i)
          const isNew = pack.isNew[i]
          const note = !on || isNew === null || isNew === undefined ? '' : isNew ? 'Nuova' : 'Doppione'
          return (
            <li key={c.instanceId} className="open-carta" style={{ '--i': i } as CSSProperties}>
              <button
                type="button"
                className={`q-giro b-carta ${c.kind === 'CREATURE' ? `b-carta--t${creatureTier(c.rank)}` : ''} ${on && isNew ? 'b-nuova' : ''}`}
                aria-pressed={on}
                aria-label={on ? `${c.name}${note ? `, ${note.toLowerCase()}` : ''}` : `Carta coperta ${i + 1}, premi per girarla`}
                onClick={() => setRevealed((r) => new Set(r).add(i))}
              >
                <span className="q-giro__dentro">
                  <span className="q-giro__faccia q-cg">
                    <span className="q-cg__campo b-dorso"><img src="/images/ui/logo-quorma.png" alt="" /></span>
                  </span>
                  <span className="q-giro__faccia q-giro__retro"><CardFace card={c} sizes="(max-width: 767px) 44vw, 240px" /></span>
                </span>
              </button>
              <span className={`b-nota ${isNew ? 'b-nota--nuova' : ''}`}>{note}</span>
            </li>
          )
        })}
      </ul>

      <div className="open-azioni">
        <div className="open-conte" aria-live="polite">
          <span className="m-conta"><b>{revealed.size} / {pack.cards.length}</b><small>Rivelate</small></span>
          <span className="m-conta"><b>{nNew}</b><small>Nuove</small></span>
          <span className="m-conta"><b>{nDup}</b><small>Doppioni</small></span>
        </div>
        <div className="open-bottoni">
          {!allRevealed && (
            <button type="button" className="q-btn q-btn--acciaio" onClick={() => setRevealed(new Set(pack.cards.map((_, i) => i)))}>
              <span className="q-btn__in">Rivela tutte</span>
            </button>
          )}
          <button type="button" className="q-btn q-btn--acciaio" onClick={onCollection}><span className="q-btn__in">Vai alla Collezione</span></button>
          {/* finita la coda, se restano altre buste comprate si torna a sceglierle */}
          {left === 0 && allRevealed && pendingCount() > 0 && (
            <button type="button" className="q-btn q-btn--argento" onClick={onPacks}><span className="q-btn__in">Altre buste da aprire</span></button>
          )}
          {left > 0 && (
            <button
              type="button"
              className="q-btn q-btn--argento"
              disabled={!allRevealed}
              onClick={() => {
                setIndex(index + 1)
                setRevealed(new Set())
              }}
            >
              <span className="q-btn__in">Apri la prossima</span>
            </button>
          )}
        </div>
      </div>
    </main>
  )
}
