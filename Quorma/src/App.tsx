import { useState } from 'react'
import type { Page } from './data/navItems'
import { useDecks } from './hooks/useDecks'
import { usePlayStatus } from './hooks/usePlayStatus'
import Sidebar from './components/layout/Sidebar'
import MobileTopBar from './components/mobile/MobileTopBar'
import MobileTabBar from './components/mobile/MobileTabBar'
import MorePage from './components/mobile/MorePage'
import HomePage from './components/home/HomePage'
import DecksPage from './components/decks/DecksPage'
import NewDeckPage from './components/newdeck/NewDeckPage'
import RulesPage from './components/rules/RulesPage'
import PlayPage from './components/play/PlayPage'
import AboutPage from './components/info/AboutPage'
import FaqPage from './components/info/FaqPage'
import GlossaryPage from './components/info/GlossaryPage'
import ContactsPage from './components/info/ContactsPage'
import LegalPage from './components/info/LegalPage'
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
  // Stanno qui così non si perdono cambiando pagina. Entrambi letti dal backend.
  const { decks, state: decksState, error: decksError, reload: reloadDecks, selected, setSelectedId, deleteDeck } = useDecks()
  const { playStatus, playStatusFailed, reloadPlayStatus } = usePlayStatus()

  const open = (next: Page) => {
    setPage(next)
    window.scrollTo({ top: 0 }) // su telefono scorre tutta la finestra: la nuova pagina parte dall'alto
    // Il backend chiede di rileggere "posso giocare?" quando si apre la home: lo facciamo anche per Gioca.
    if (next === 'home' || next === 'gioca') reloadPlayStatus()
  }

  // Dopo un'eliminazione cambiano i mazzi obsoleti: si rilegge anche "posso giocare?".
  const removeDeck = async (id: string) => {
    await deleteDeck(id)
    reloadPlayStatus()
  }

  return (
    <div className="app">
      <Sidebar current={page} onNavigate={open} />
      <MobileTopBar onHome={() => open('home')} />

      {page === 'home' && (
        <HomePage
          decks={decks}
          lastDeck={selected}
          decksState={decksState}
          onRetryDecks={reloadDecks}
          playStatus={playStatus}
          playStatusFailed={playStatusFailed}
          onRetryPlayStatus={reloadPlayStatus}
          onNavigate={open}
        />
      )}

      {page === 'mazzi' && (
        <DecksPage
          decks={decks}
          state={decksState}
          error={decksError}
          onRetry={() => void reloadDecks()}
          selected={selected}
          onSelect={setSelectedId}
          onDelete={removeDeck}
          onNewDeck={() => open('nuovo-mazzo')}
          onPlay={() => open('gioca')}
        />
      )}

      {page === 'nuovo-mazzo' && <NewDeckPage onCancel={() => open('mazzi')} />}

      {page === 'gioca' && <PlayPage decks={decks} playStatus={playStatus} onNavigate={open} />}

      {page === 'regole' && <RulesPage />}

      {page === 'chi-siamo' && <AboutPage onNavigate={open} />}
      {page === 'faq' && <FaqPage onNavigate={open} />}
      {page === 'glossario' && <GlossaryPage onNavigate={open} />}
      {page === 'contatti' && <ContactsPage onNavigate={open} />}
      {page === 'privacy' && <LegalPage kind="privacy" onNavigate={open} />}
      {page === 'cookie' && <LegalPage kind="cookie" onNavigate={open} />}
      {page === 'termini' && <LegalPage kind="termini" onNavigate={open} />}

      {page === 'altro' && <MorePage onNavigate={open} />}

      <MobileTabBar current={page} onNavigate={open} />
    </div>
  )
}
