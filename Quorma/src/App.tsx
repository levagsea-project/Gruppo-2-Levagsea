import { useState } from 'react'
import type { Page } from './data/navItems'
import { useDecks } from './hooks/useDecks'
import Sidebar from './components/layout/Sidebar'
import HomePage from './components/home/HomePage'
import DecksPage from './components/decks/DecksPage'
import NewDeckPage from './components/newdeck/NewDeckPage'
import './styles/layout.css'
import './styles/sidebar.css'
import './styles/home.css'
import './styles/ui.css'
import './styles/pages.css'
import './styles/decks.css'
import './styles/newdeck.css'

// Tutta l'app: sidebar a sinistra + la pagina scelta a destra.
export default function App() {
  const [page, setPage] = useState<Page>('home') // pagina aperta ora
  const { decks, selected, setSelectedId, deleteDeck } = useDecks() // sta qui così non si perde cambiando pagina

  return (
    <div className="app">
      <Sidebar current={page} onNavigate={setPage} />

      {page === 'home' && <HomePage />}

      {page === 'mazzi' && (
        <DecksPage
          decks={decks}
          selected={selected}
          onSelect={setSelectedId}
          onDelete={deleteDeck}
          onNewDeck={() => setPage('nuovo-mazzo')}
        />
      )}

      {page === 'nuovo-mazzo' && <NewDeckPage onCancel={() => setPage('mazzi')} />}
    </div>
  )
}
