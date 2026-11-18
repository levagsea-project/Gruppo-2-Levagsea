import type { Deck } from '../../types/deck'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import DeckGrid from './DeckGrid'
import DeckDetails from './DeckDetails'

type Props = {
  decks: Deck[]
  selected: Deck | null
  onSelect: (id: string) => void
  onDelete: (id: string) => void
  onNewDeck: () => void
  onPlay: () => void
}

// Pagina Mazzi: titolo + griglia a sinistra, dettagli a destra.
export default function DecksPage({ decks, selected, onSelect, onDelete, onNewDeck, onPlay }: Props) {
  return (
    <main className="page decks-page">
      <CurrencyBar />
      <PageTitle title="MAZZI" subtitle="I TUOI MAZZI" />

      <div className="decks-body">
        <section>
          <header className="section-header">
            <h2>I TUOI MAZZI</h2>
            <span className="section-line" />
            <span>{decks.length} {decks.length === 1 ? 'MAZZO' : 'MAZZI'}</span>
          </header>
          <DeckGrid
            decks={decks}
            selectedId={selected?.id}
            onSelect={onSelect}
            onNewDeck={onNewDeck}
          />
        </section>

        <DeckDetails deck={selected} onPlay={onPlay} onDelete={onDelete} />
      </div>
    </main>
  )
}
