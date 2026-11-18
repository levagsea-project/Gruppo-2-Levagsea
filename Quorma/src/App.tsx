import { useState } from 'react'
import type { Page } from './data/navItems'
import { useDecks } from './hooks/useDecks'
import Sidebar from './components/layout/Sidebar'
import MobileTopBar from './components/mobile/MobileTopBar'
import MobileTabBar from './components/mobile/MobileTabBar'
import MorePage from './components/mobile/MorePage'
import HomePage from './components/home/HomePage'
import DecksPage from './components/decks/DecksPage'
import NewDeckPage from './components/newdeck/NewDeckPage'
import RulesPage from './components/rules/RulesPage'
import './styles/layout.css'
import './styles/sidebar.css'
import './styles/home.css'
import './styles/ui.css'
import './styles/pages.css'
import './styles/decks.css'
import './styles/newdeck.css'
import './styles/mobile.css' // per ultimo: su telefono sovrascrive gli stili sopra

// Tutta l'app: sidebar a sinistra + la pagina scelta a destra.
// Su telefono la sidebar sparisce e al suo posto ci sono la barra in alto e le schede in basso.
export default function App() {
  const [page, setPage] = useState<Page>('home') // pagina aperta ora
  const { decks, selected, setSelectedId, deleteDeck } = useDecks() // sta qui così non si perde cambiando pagina

  const open = (next: Page) => {
    setPage(next)
    window.scrollTo({ top: 0 }) // su telefono scorre tutta la finestra: la nuova pagina parte dall'alto
  }

  return (
    <div className="app">
      <Sidebar current={page} onNavigate={open} />
      <MobileTopBar onHome={() => open('home')} />

      {page === 'home' && <HomePage lastDeck={selected} onNavigate={open} />}

      {page === 'mazzi' && (
        <DecksPage
          decks={decks}
          selected={selected}
          onSelect={setSelectedId}
          onDelete={deleteDeck}
          onNewDeck={() => open('nuovo-mazzo')}
        />
      )}

      {page === 'nuovo-mazzo' && <NewDeckPage onCancel={() => open('mazzi')} />}

      {page === 'regole' && <RulesPage />}

      {page === 'altro' && <MorePage onNavigate={open} />}

      <MobileTabBar current={page} onNavigate={open} />
    </div>
  )
}
