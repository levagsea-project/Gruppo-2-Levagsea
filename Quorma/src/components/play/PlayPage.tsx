import { useState, type CSSProperties } from 'react'
import { factions } from '../../data/factions'
import type { Page } from '../../data/navItems'
import { playModes } from '../../data/play'
import type { Deck, PlayStatus } from '../../types/deck'
import { playBlockReason } from '../../hooks/usePlayStatus'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import '../../styles/play.css'

// Titolo numerato di un passo ("1 · Scegli la modalità").
function Step({ num, id, title, extra }: { num: string; id: string; title: string; extra?: React.ReactNode }) {
  return (
    <div className="q-marker">
      <span className="q-marker__num">
        <span>{num}</span>
      </span>
      <h2 id={id} className="q-marker__title">{title}</h2>
      <span className="q-marker__line" />
      {extra}
    </div>
  )
}

const Star = () => (
  <svg className="q-stella" aria-hidden="true" viewBox="0 0 20 28">
    <path d="M10 0l2.2 11.6L20 14l-7.8 2.4L10 28l-2.2-11.6L0 14l7.8-2.4z" fill="#8FD6DB" />
  </svg>
)

type Props = { decks: Deck[]; playStatus: PlayStatus | null; onNavigate: (page: Page) => void }

// Pagina Gioca: 1) modalità, 2) mazzo, poi riepilogo e "Inizia partita".
// Si gioca solo con un mazzo non obsoleto, e solo con un mazzo di difesa valido per entrambe le fazioni
// (GET /decks/play-status). Per ora "Inizia partita" non fa nulla.
export default function PlayPage({ decks, playStatus, onNavigate }: Props) {
  const [modeId, setModeId] = useState(playModes[0].id)
  const [chosenDeckId, setChosenDeckId] = useState<string | null>(null)

  // Il backend salva solo mazzi completi: un mazzo non si può usare solo se è diventato obsoleto.
  const isComplete = (d: Deck) => !d.outdated
  const blocked = playBlockReason(playStatus)
  const mode = playModes.find((m) => m.id === modeId) ?? playModes[0]
  // Mazzo scelto: quello cliccato se è completo, altrimenti il primo completo.
  const deck = decks.find((d) => d.id === chosenDeckId && isComplete(d)) ?? decks.find(isComplete) ?? null

  const summary = deck ? `${mode.title} · ${deck.name}` : mode.title
  const hint = !deck
    ? decks.length
      ? 'I tuoi mazzi sono obsoleti: modificane uno per giocare.'
      : 'Crea un mazzo per giocare.'
    : blocked || mode.hint

  return (
    <main className="page play-page">
      <CurrencyBar />
      <PageTitle title="GIOCA" subtitle="SCEGLI MODALITÀ E MAZZO" />

      <section className="play-step" aria-labelledby="g-mod">
        <Step num="1" id="g-mod" title="Scegli la modalità" />
        <div role="radiogroup" aria-labelledby="g-mod" className="play-modi">
          {playModes.map((m) => {
            const on = m.id === modeId
            return (
              <button key={m.id} type="button" role="radio" aria-checked={on} className="pick" onClick={() => setModeId(m.id)}>
                <span className={`q-tela q-tela--arco play-modo ${on ? 'q-acceso' : ''}`}>
                  <span className="q-tela__in">
                    <span className="play-modo__testa">
                      <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
                        {m.paths.map((d) => (
                          <path key={d} d={d} />
                        ))}
                      </svg>
                      <span className="play-modo__titolo">{m.title}</span>
                      <span className="play-modo__linea" aria-hidden="true" />
                    </span>
                    <span className="play-modo__desc">{m.desc}</span>
                    <span className="play-modo__meta">{m.meta}</span>
                  </span>
                  <span className="q-borchia q-borchia--n" />
                </span>
              </button>
            )
          })}
        </div>
      </section>

      <section className="play-step" aria-labelledby="g-deck">
        <Step
          num="2"
          id="g-deck"
          title="Scegli il mazzo"
          extra={
            <button type="button" className="play-gestisci" onClick={() => onNavigate('mazzi')}>
              Gestisci
            </button>
          }
        />

        {decks.length === 0 ? (
          <div className="q-tela play-vuoto">
            <div className="q-tela__in">
              <p>Non hai ancora nessun mazzo.</p>
              <button type="button" className="q-btn q-btn--argento" onClick={() => onNavigate('nuovo-mazzo')}>
                <span className="q-btn__in">Crea mazzo</span>
              </button>
            </div>
          </div>
        ) : (
          <div role="radiogroup" aria-labelledby="g-deck" className="play-mazzi">
            {decks.map((d) => {
              const ok = isComplete(d)
              const on = d.id === deck?.id
              const faction = factions[d.faction]
              return (
                <button
                  key={d.id}
                  type="button"
                  role="radio"
                  aria-checked={on}
                  aria-disabled={!ok}
                  className={ok ? 'pick' : 'pick pick--off'}
                  onClick={() => ok && setChosenDeckId(d.id)}
                >
                  <span className="q-mazzo">
                    <span className="q-mazzo__strato" aria-hidden="true" />
                    <span className="q-mazzo__strato" aria-hidden="true" />
                    <span className={`q-tela q-tela--arco q-carta ${on ? 'q-acceso' : ''}`}>
                      <span className="q-tela__in">
                        <span className="q-carta__arte" style={{ backgroundImage: `url(${faction.image})` } as CSSProperties} />
                        <span className="q-divisorio" />
                        <span className="q-carta__targa">
                          <span className="q-carta__nome">{d.name}</span>
                          <span className="q-carta__sotto">{faction.name}</span>
                          <span className={ok ? 'play-conta' : 'play-conta play-conta--off'}>
                            {ok ? `${d.cards} / ${d.maxCards} carte${d.defense ? ' · Difesa' : ''}` : `${d.cards} / ${d.maxCards} · Obsoleto`}
                          </span>
                        </span>
                      </span>
                      <span className="q-borchia q-borchia--n" />
                    </span>
                  </span>
                </button>
              )
            })}
          </div>
        )}
      </section>

      <div className="play-azione">
        <span className="q-divisorio" />
        <div className="play-azione__riga">
          <div className="play-riepilogo" aria-live="polite">
            <span>{summary}</span>
            <small>{hint}</small>
          </div>
          <div className="play-bottoni">
            <button type="button" className="q-btn q-btn--acciaio play-gestisci-btn" onClick={() => onNavigate('mazzi')}>
              <span className="q-btn__in">Gestisci mazzi</span>
            </button>
            {/* Per ora non fa nulla: la partita non esiste ancora */}
            <button type="button" className="q-btn q-btn--sigillo" disabled={!deck || !!blocked}>
              <span className="q-btn__in">
                <Star />
                Inizia partita
              </span>
              <span className="q-borchia q-borchia--w" />
              <span className="q-borchia q-borchia--e" />
            </button>
          </div>
        </div>
      </div>
    </main>
  )
}
