import { useRef, useState } from 'react'
import type { FactionId } from '../../types/deck'
import { factions } from '../../data/factions'
import { DECK_NAME_MAX } from '../../lib/decks'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import FactionCard from './FactionCard'
import '../../styles/editor.css' // .editor-nome__input, lo stesso campo del nome dell'editor

type Props = {
  initialFaction: FactionId
  initialName: string // nome proposto (es. "Mazzo 03"), o quello scritto prima di tornare indietro dall'editor
  onCancel: () => void
  onCreate: (faction: FactionId, name: string) => void
}

// Pagina "Nuovo mazzo", dal design "Pagina-Mazzi-Fazione": scegli la fazione e il nome, poi si apre l'editor.
export default function NewDeckPage({ initialFaction, initialName, onCancel, onCreate }: Props) {
  const [chosen, setChosen] = useState<FactionId>(initialFaction)
  const [name, setName] = useState(initialName)
  const [nameError, setNameError] = useState(false)
  const nameRef = useRef<HTMLInputElement>(null)

  const create = () => {
    if (!name.trim()) {
      setNameError(true)
      nameRef.current?.focus()
      return
    }
    onCreate(chosen, name.trim())
  }

  return (
    <main className="page newdeck-page">
      <CurrencyBar />
      <PageTitle title="NUOVO MAZZO" subtitle="SCEGLI LA FAZIONE" />

      <div className="faction-list" role="group" aria-label="Fazione principale">
        {Object.values(factions).map((faction) => (
          <FactionCard key={faction.id} faction={faction} selected={faction.id === chosen} onSelect={() => setChosen(faction.id)} />
        ))}
      </div>

      <div className="newdeck-piede">
        <label className="newdeck-nome">
          <span className="q-elenco__titolo">Nome del mazzo</span>
          <input
            ref={nameRef}
            className="editor-nome__input"
            type="text"
            maxLength={DECK_NAME_MAX}
            value={name}
            onChange={(e) => {
              setName(e.target.value)
              setNameError(false)
            }}
            onKeyDown={(e) => e.key === 'Enter' && create()}
            aria-invalid={nameError || undefined}
            aria-describedby={nameError ? 'newdeck-nome-err' : undefined}
          />
          {nameError && <span id="newdeck-nome-err" className="newdeck-errore">Scrivi il nome del mazzo.</span>}
        </label>
        <div className="newdeck-buttons">
          <button type="button" className="q-btn q-btn--acciaio" onClick={onCancel}>
            <span className="q-btn__in">Annulla</span>
          </button>
          <button type="button" className="q-btn q-btn--argento" aria-label={`Crea mazzo: ${factions[chosen].name}`} onClick={create}>
            <span className="q-btn__in">Crea mazzo</span>
          </button>
        </div>
      </div>
    </main>
  )
}
