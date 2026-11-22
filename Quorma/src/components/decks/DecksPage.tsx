import type { Deck } from '../../types/deck'
import type { LoadState } from '../../hooks/useDecks'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import DeckGrid from './DeckGrid'
import DeckDetails from './DeckDetails'

type Props = {
  decks: Deck[]
  state: LoadState // caricamento della lista dal backend
  error: string
  onRetry: () => void
  selected: Deck | null
  onSelect: (id: string) => void
  onEdit: (deck: Deck) => void
  onDuplicate: (deck: Deck) => Promise<void>
  onDelete: (id: string) => Promise<void>
  onSetDefense: (id: string) => Promise<void>
  onNewDeck: () => void
  onPlay: () => void
}

// Pagina Mazzi, dal design "Pagina-Mazzi": titolo + griglia a sinistra, dettagli a destra.
// Larghezze (styles/decks.css): pannello 290px sotto i 1280, 330px fino a 1599, 360px oltre; le colonne di carte
// si adattano allo spazio. La pagina scorre se i mazzi non ci stanno, il pannello resta fermo.
export default function DecksPage({ decks, state, error, onRetry, selected, onSelect, onEdit, onDuplicate, onDelete, onSetDefense, onNewDeck, onPlay }: Props) {
  const outdated = decks.filter((d) => d.outdated).length

  return (
    <main className="page decks-page">
      <CurrencyBar />
      <PageTitle title="MAZZI" subtitle="I TUOI MAZZI" />

      <div className="decks-body">
        <section className="decks-lista" aria-labelledby="i-tuoi-mazzi">
          <div className="q-marker">
            <h2 id="i-tuoi-mazzi" className="q-marker__title">I tuoi mazzi</h2>
            <span className="q-marker__line" aria-hidden="true" />
            <span className="decks-conta">{state === 'ready' ? `${decks.length} ${decks.length === 1 ? 'mazzo' : 'mazzi'}` : ''}</span>
          </div>

          <div aria-live="polite">
            {state === 'loading' && <p className="decks-avviso">Caricamento dei mazzi…</p>}
            {state === 'error' && (
              <p className="decks-avviso decks-avviso--errore" role="alert">
                {error || 'Impossibile caricare i mazzi.'}
                <button type="button" onClick={onRetry}>Riprova</button>
              </p>
            )}
            {state === 'ready' && outdated > 0 && (
              <p className="decks-avviso decks-avviso--errore">
                {outdated === 1 ? 'Un mazzo è obsoleto' : `${outdated} mazzi sono obsoleti`}: il pool di carte è
                cambiato, modificali per usarli di nuovo.
              </p>
            )}
          </div>

          <DeckGrid
            decks={decks}
            selectedId={selected?.id}
            onSelect={onSelect}
            onNewDeck={onNewDeck}
          />
        </section>

        <DeckDetails deck={selected} onPlay={onPlay} onEdit={onEdit} onDuplicate={onDuplicate} onDelete={onDelete} onSetDefense={onSetDefense} />
      </div>
    </main>
  )
}
