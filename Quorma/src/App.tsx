import { useState, useSyncExternalStore } from 'react'
import type { Page } from './data/navItems'
import { useDecks } from './hooks/useDecks'
import { usePlayStatus } from './hooks/usePlayStatus'
import { useMatchNews } from './hooks/useMatchNews'
import AwayResults from './components/layout/AwayResults'
import type { Tab as ProfileTab } from './components/profile/ProfileTabs'
import Sidebar from './components/layout/Sidebar'
import MobileTopBar from './components/mobile/MobileTopBar'
import MobileTabBar from './components/mobile/MobileTabBar'
import MorePage from './components/mobile/MorePage'
import HomePage from './components/home/HomePage'
import DecksPage from './components/decks/DecksPage'
import NewDeckPage from './components/newdeck/NewDeckPage'
import DeckEditorPage from './components/editor/DeckEditorPage'
import DeckSavedPage from './components/editor/DeckSavedPage'
import { duplicateDeck, setDefenseDeck } from './lib/decks'
import { pendingCount, subscribePending } from './lib/shop'
import type { DeckDetail, FactionId } from './types/deck'
import RulesPage from './components/rules/RulesPage'
import PlayPage from './components/play/PlayPage'
import AboutPage from './components/info/AboutPage'
import FaqPage from './components/info/FaqPage'
import GlossaryPage from './components/info/GlossaryPage'
import ContactsPage from './components/info/ContactsPage'
import LegalPage from './components/info/LegalPage'
import ProfilePage from './components/profile/ProfilePage'
import EditProfilePage from './components/profile/EditProfilePage'
import ChallengesPage from './components/challenges/ChallengesPage'
import CollectionPage, { type CollectionTab } from './components/collection/CollectionPage'
import PackOpeningPage from './components/collection/PackOpeningPage'
import ShopPage from './components/shop/ShopPage'
import { useChallengeAlert } from './hooks/useChallengeAlert'
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
  // Novità sulle partite: risultati non letti (finestra "Mentre eri via") e ultima serie (home su telefono).
  const { unread, latest, reload: reloadNews, markAllSeen } = useMatchNews()
  const [awayOpen, setAwayOpen] = useState(true) // la finestra si mostra una volta per ingresso nell'app
  const [profileTab, setProfileTab] = useState<ProfileTab>('panoramica') // scheda con cui si apre il profilo
  // Mazzo in lavorazione nell'editor (deckId null = nuovo) e mazzo mostrato in "Mazzo salvato".
  const [editing, setEditing] = useState<{ faction: FactionId; deckId: string | null }>({ faction: 'artificiali', deckId: null })
  const [saved, setSaved] = useState<{ id: string; deck: DeckDetail | null } | null>(null)
  const { pendingChallenges, reloadChallenges } = useChallengeAlert() // sfide ricevute in attesa (braci sul menu)
  const challengeAlert = pendingChallenges > 0 ? `${pendingChallenges} ${pendingChallenges === 1 ? 'sfida' : 'sfide'} in attesa` : undefined
  const packsToOpen = useSyncExternalStore(subscribePending, pendingCount) // buste comprate e non ancora rivelate
  const packAlert = packsToOpen > 0 ? `${packsToOpen} ${packsToOpen === 1 ? 'busta' : 'buste'} da aprire` : undefined
  const [collectionTab, setCollectionTab] = useState<CollectionTab>('carte')
  const [packQueue, setPackQueue] = useState<string[]>([]) // buste da rivelare nella pagina Apertura

  const open = (next: Page) => {
    setPage(next)
    window.scrollTo({ top: 0 }) // su telefono scorre tutta la finestra: la nuova pagina parte dall'alto
    // Il backend chiede di rileggere "posso giocare?" quando si apre la home: lo facciamo anche per Gioca e Sfide.
    if (next === 'home' || next === 'gioca' || next === 'sfide') reloadPlayStatus()
    if (next === 'home') reloadNews() // dopo una partita l'ultima serie della home cambia
    if (next === 'home' || next === 'sfide') reloadChallenges()
    if (next === 'profilo') setProfileTab('panoramica')
    if (next === 'collezione') setCollectionTab(packsToOpen > 0 ? 'buste' : 'carte') // chi ha buste da aprire arriva lì
  }

  const openPacks = (ids: string[]) => {
    setPackQueue(ids)
    open('apertura')
  }

  // Il profilo aperto direttamente sulla scheda Partite (da "Mentre eri via" e dall'ultima serie della home).
  const openMatches = () => {
    open('profilo')
    setProfileTab('partite')
  }

  const closeAway = () => {
    setAwayOpen(false)
    markAllSeen()
  }

  const openEditor = (faction: FactionId, deckId: string | null) => {
    setEditing({ faction, deckId })
    open('editor-mazzo')
  }

  // Dopo il salvataggio: la lista dei mazzi e "posso giocare?" cambiano, il mazzo salvato diventa quello scelto.
  const onSaved = (deck: DeckDetail) => {
    reloadDecks()
    reloadPlayStatus()
    setSelectedId(deck.id)
    setSaved({ id: deck.id, deck })
    open('mazzo-salvato')
  }

  // La copia diventa il mazzo scelto, così si vede subito nel pannello.
  const copyDeck = async (deck: { id: string }) => {
    const copy = await duplicateDeck(deck.id, decks.map((d) => d.name))
    reloadDecks()
    reloadPlayStatus()
    setSelectedId(copy.id)
  }

  const makeDefense = async (id: string) => {
    await setDefenseDeck(id)
    reloadDecks()
    reloadPlayStatus()
  }

  // Nome proposto per un mazzo nuovo: "Mazzo 03" se ne hai già due (il primo nome libero).
  const names = new Set(decks.map((d) => d.name.toLowerCase()))
  let n = decks.length + 1
  while (names.has(`mazzo ${String(n).padStart(2, '0')}`)) n++
  const defaultName = `Mazzo ${String(n).padStart(2, '0')}`

  // Dopo un'eliminazione cambiano i mazzi obsoleti: si rilegge anche "posso giocare?".
  const removeDeck = async (id: string) => {
    await deleteDeck(id)
    reloadPlayStatus()
  }

  return (
    <div className="app">
      <Sidebar current={page} onNavigate={open} challengeAlert={challengeAlert} packAlert={packAlert} />
      <MobileTopBar onHome={() => open('home')} onProfile={() => open('profilo')} />

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
          latestSeries={latest}
          onOpenMatches={openMatches}
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
          onEdit={(d) => openEditor(d.faction, d.id)}
          onDuplicate={copyDeck}
          onDelete={removeDeck}
          onSetDefense={makeDefense}
          onNewDeck={() => open('nuovo-mazzo')}
          onPlay={() => open('gioca')}
        />
      )}

      {page === 'nuovo-mazzo' && <NewDeckPage onCancel={() => open('mazzi')} onCreate={(f) => openEditor(f, null)} />}

      {page === 'editor-mazzo' && (
        <DeckEditorPage
          key={editing.deckId ?? `nuovo-${editing.faction}`}
          faction={editing.faction}
          deckId={editing.deckId}
          defaultName={defaultName}
          onCancel={() => open(editing.deckId ? 'mazzi' : 'nuovo-mazzo')}
          onSaved={onSaved}
        />
      )}

      {page === 'mazzo-salvato' && saved && (
        <DeckSavedPage
          key={saved.id}
          deckId={saved.id}
          saved={saved.deck}
          onPlay={() => open('gioca')}
          onEdit={(d) => openEditor(d.faction, d.id)}
          onBack={() => open('mazzi')}
          onDefenseChanged={() => {
            reloadDecks()
            reloadPlayStatus()
          }}
        />
      )}

      {page === 'gioca' && (
        <PlayPage
          decks={decks}
          decksState={decksState}
          playStatus={playStatus}
          playStatusFailed={playStatusFailed}
          onNavigate={open}
          onOpenMatches={openMatches}
          onDecksChanged={() => {
            reloadDecks()
            reloadPlayStatus()
          }}
        />
      )}

      {page === 'sfide' && (
        <ChallengesPage
          decks={decks}
          playStatus={playStatus}
          onNavigate={open}
          onOpenMatches={openMatches}
          onChanged={() => {
            reloadDecks()
            reloadPlayStatus()
            reloadChallenges()
          }}
        />
      )}

      {page === 'collezione' && <CollectionPage key={collectionTab} initialTab={collectionTab} onNavigate={open} onOpenPacks={openPacks} />}
      {page === 'apertura' && (
        <PackOpeningPage
          key={packQueue.join()}
          queue={packQueue}
          onCollection={() => open('collezione')}
          onShop={() => open('negozio')}
        />
      )}
      {page === 'negozio' && <ShopPage onOpenPacks={openPacks} />}

      {page === 'regole' && <RulesPage />}

      {page === 'chi-siamo' && <AboutPage onNavigate={open} />}
      {page === 'faq' && <FaqPage onNavigate={open} />}
      {page === 'glossario' && <GlossaryPage onNavigate={open} />}
      {page === 'contatti' && <ContactsPage onNavigate={open} />}
      {page === 'privacy' && <LegalPage kind="privacy" onNavigate={open} />}
      {page === 'cookie' && <LegalPage kind="cookie" onNavigate={open} />}
      {page === 'termini' && <LegalPage kind="termini" onNavigate={open} />}

      {page === 'profilo' && <ProfilePage key={profileTab} initialTab={profileTab} onNavigate={open} />}
      {page === 'modifica-profilo' && <EditProfilePage onNavigate={open} />}

      {page === 'altro' && (
        <MorePage
          onNavigate={open}
          onOpenSettings={() => {
            open('profilo')
            setProfileTab('impostazioni')
          }}
          challengeAlert={challengeAlert}
        />
      )}

      <MobileTabBar current={page} onNavigate={open} challengeAlert={challengeAlert} packAlert={packAlert} />

      {awayOpen && unread.length > 0 && (
        <AwayResults
          series={unread}
          onClose={closeAway}
          onOpenProfile={() => {
            closeAway()
            openMatches()
          }}
        />
      )}
    </div>
  )
}
