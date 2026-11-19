import { useEffect, useState, type CSSProperties } from 'react'
import type { DeckDetail } from '../../types/deck'
import { factions } from '../../data/factions'
import { GENERIC } from '../../lib/api'
import { getDeck, setDefenseDeck, statsOf } from '../../lib/decks'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import '../../styles/editor.css'
import '../../styles/play.css' // q-mazzo: la carta-mazzo con gli strati

type Props = {
  deckId: string
  saved: DeckDetail | null // il mazzo appena salvato (evita di rileggerlo); null = leggilo dal backend
  onPlay: () => void
  onEdit: (deck: DeckDetail) => void
  onBack: () => void
  onDefenseChanged: () => void // dopo "Usa come mazzo di difesa": rileggere mazzi e "posso giocare?"
}

const Star = () => (
  <svg className="q-stella" aria-hidden="true" viewBox="0 0 20 28">
    <path d="M10 0l2.2 11.6L20 14l-7.8 2.4L10 28l-2.2-11.6L0 14l7.8-2.4z" fill="#8FD6DB" />
  </svg>
)

// Riepilogo del mazzo appena salvato, dal design "Pagina-Mazzi-Salvato": carta-mazzo, conteggi, azioni.
// In più rispetto al design: "Usa come mazzo di difesa" (POST /decks/{id}/defense), che serve per poter giocare.
// La scelta della copertina del design non c'è: il backend non salva una copertina.
export default function DeckSavedPage({ deckId, saved, onPlay, onEdit, onBack, onDefenseChanged }: Props) {
  const [deck, setDeck] = useState<DeckDetail | null>(saved)
  const [loadError, setLoadError] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (saved) return
    let current = true
    getDeck(deckId)
      .then((d) => current && setDeck(d))
      .catch((err: unknown) => current && setLoadError(err instanceof Error ? err.message : GENERIC))
    return () => {
      current = false
    }
  }, [deckId, saved])

  async function makeDefense() {
    if (!deck || busy) return
    setBusy(true)
    setError('')
    try {
      setDeck(await setDefenseDeck(deck.id))
      onDefenseChanged()
    } catch (err) {
      setError(err instanceof Error ? err.message : GENERIC)
    } finally {
      setBusy(false)
    }
  }

  if (!deck) {
    return (
      <main className="page saved-page">
        <PageTitle title="MAZZO SALVATO" subtitle="" />
        <p className={loadError ? 'decks-avviso decks-avviso--errore' : 'deck-empty'} role={loadError ? 'alert' : undefined}>
          {loadError || 'Caricamento del mazzo…'}
        </p>
      </main>
    )
  }

  const faction = factions[deck.faction]
  const stats = statsOf(deck.cardList)
  const status = deck.outdated
    ? 'Il mazzo è obsoleto per il pool di carte attuale: modificalo per usarlo.'
    : deck.defense
      ? `Il mazzo rispetta tutte le regole ed è il tuo mazzo di difesa della ${faction.name}.`
      : 'Il mazzo rispetta tutte le regole ed è pronto per giocare.'

  return (
    <main className="page saved-page">
      <CurrencyBar />
      <PageTitle title="MAZZO SALVATO" subtitle={`${deck.name} · ${faction.name}`.toUpperCase()} />

      <div className="saved-corpo">
        <div className="saved-carta">
          <span className="q-mazzo">
            <span className="q-mazzo__strato" aria-hidden="true" />
            <span className="q-mazzo__strato" aria-hidden="true" />
            <span className="q-tela q-tela--arco q-carta q-acceso" style={{ '--arte-h': '300px' } as CSSProperties}>
              <span className="q-tela__in">
                <span className="q-carta__arte" style={{ backgroundImage: `url(${faction.image})` }} />
                <span className="q-divisorio" />
                <span className="q-carta__targa saved-carta__targa">
                  <span className="q-carta__nome">{deck.name}</span>
                  <span className="q-carta__sotto">{faction.name}</span>
                </span>
              </span>
              <span className="q-borchia q-borchia--n" aria-hidden="true" />
            </span>
          </span>
        </div>

        <div className="saved-dati">
          <p className={`m-regola saved-stato ${deck.outdated ? 'm-regola--off' : 'm-ok'}`} aria-live="polite">{status}</p>

          <div className="saved-conte">
            <span className="m-conta"><b>{deck.cards}</b><small>Carte</small></span>
            <span className="m-conta"><b>{stats.creatures}</b><small>Creature</small></span>
            <span className="m-conta"><b>{stats.spells}</b><small>Magie / Equip.</small></span>
            <span className="m-conta">
              <b>{stats.suits.map((s) => s.value).join(' · ')}</b>
              <small>{stats.suits.map((s) => s.name).join(' · ')}</small>
            </span>
          </div>

          <span className="q-divisorio" />

          {!deck.defense && !deck.outdated && (
            <div className="saved-difesa">
              <p>
                Per giocare ti serve un mazzo di difesa valido per ogni fazione. Puoi usare questo come mazzo di difesa della {faction.name}: sostituirà quello attuale, se c’è.
              </p>
              <button type="button" className="q-btn q-btn--argento" onClick={makeDefense} aria-disabled={busy}>
                <span className="q-btn__in">{busy ? 'Attendi…' : 'Usa come mazzo di difesa'}</span>
              </button>
            </div>
          )}
          {error && <p className="m-regola m-regola--errore" role="alert">{error}</p>}

          <div className="saved-azioni">
            {!deck.outdated && (
              <button type="button" className="q-btn q-btn--sigillo" onClick={onPlay}>
                <span className="q-btn__in">
                  <Star />
                  Gioca con questo mazzo
                </span>
                <span className="q-borchia q-borchia--w" aria-hidden="true" />
                <span className="q-borchia q-borchia--e" aria-hidden="true" />
              </button>
            )}
            <button type="button" className="q-btn q-btn--acciaio" onClick={() => onEdit(deck)}>
              <span className="q-btn__in">Modifica</span>
            </button>
            <button type="button" className="q-btn q-btn--acciaio" onClick={onBack}>
              <span className="q-btn__in">Torna ai mazzi</span>
            </button>
          </div>
        </div>
      </div>
    </main>
  )
}
