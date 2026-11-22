import { useState } from 'react'
import type { FactionId } from '../../types/deck'
import { factions } from '../../data/factions'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import FactionCard from './FactionCard'

// Pagina "Nuovo mazzo": scegli la fazione. ANNULLA torna indietro, il pulsante dice con quale fazione apre l'editor.
export default function NewDeckPage({ onCancel, onCreate }: { onCancel: () => void; onCreate: (faction: FactionId) => void }) {
  const [chosen, setChosen] = useState<FactionId>('artificiali') // fazione scelta
  const name = factions[chosen].name

  return (
    <main className="page newdeck-page">
      <CurrencyBar />
      <PageTitle title="NUOVO MAZZO" subtitle="SCEGLI LA FAZIONE PRINCIPALE" />

      <p className="newdeck-intro">
        Un mazzo usa le carte di una sola fazione: puoi mescolare tutte le sue sottofazioni.
        La fazione decide quante creature e quante magie ed equipaggiamenti contiene.
      </p>

      <div className="faction-list">
        {Object.values(factions).map((faction) => (
          <FactionCard
            key={faction.id}
            faction={faction}
            selected={faction.id === chosen}
            onSelect={() => setChosen(faction.id)}
          />
        ))}
      </div>

      <div className="newdeck-buttons">
        <button type="button" className="q-btn q-btn--acciaio" onClick={onCancel}>
          <span className="q-btn__in">Annulla</span>
        </button>
        <button type="button" className="q-btn q-btn--argento" aria-label={`Crea mazzo: ${name}`} onClick={() => onCreate(chosen)}>
          <span className="q-btn__in">Crea<span className="newdeck-parola"> mazzo</span> · {name}</span>
        </button>
      </div>
    </main>
  )
}
