import { useState } from 'react'
import type { Page } from '../../data/navItems'
import { glossary } from '../../data/info'
import InfoPage from './InfoPage'
import LetterGrid, { ALL_LETTERS } from './LetterGrid'
import SearchBox from './SearchBox'

const fold = (s: string) => s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '')
const initial = (term: string) => term[0].toUpperCase()

// Glossario: lettere a sinistra, ricerca e termini con definizione e rimando al regolamento.
export default function GlossaryPage({ onNavigate }: { onNavigate: (page: Page) => void }) {
  const [letter, setLetter] = useState(ALL_LETTERS)
  const [query, setQuery] = useState('')

  const available = new Set(glossary.map((g) => initial(g.term)))
  const q = fold(query.trim())
  const shown = glossary.filter((g) => (letter === ALL_LETTERS || initial(g.term) === letter) && (!q || fold(g.term + ' ' + g.text).includes(q)))

  return (
    <InfoPage
      page="glossario"
      title="GLOSSARIO"
      subtitle="I TERMINI DEL GIOCO"
      sideTop={<LetterGrid available={available} active={letter} onPick={setLetter} />}
      onNavigate={onNavigate}
    >
      <SearchBox value={query} onChange={setQuery} placeholder="Cerca un termine" label="Cerca nel glossario" />
      <p className="info-conteggio" aria-live="polite">
        {shown.length === 1 ? '1 termine' : `${shown.length} termini`}
      </p>
      <dl className="info-voci">
        {shown.map((g) => (
          <div key={g.term} className="info-voce">
            <span className="info-voce__ini" aria-hidden="true">{initial(g.term)}</span>
            <dt>{g.term}</dt>
            <dd>
              <p>{g.text}</p>
              {g.rules && (
                <button type="button" className="info-voce__link" onClick={() => onNavigate('regole')}>
                  Vedi nelle Regole · {g.rules}
                </button>
              )}
            </dd>
          </div>
        ))}
      </dl>
      {shown.length === 0 && <p className="info-vuoto">Nessun termine trovato. Prova con un’altra parola o scegli “·” per vederli tutti.</p>}
    </InfoPage>
  )
}
