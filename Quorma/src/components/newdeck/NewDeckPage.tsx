import { useState } from 'react'
import type { FactionId } from '../../types/deck'
import { factions } from '../../data/factions'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import HexButton from '../ui/HexButton'
import FactionCard from './FactionCard'

// Pagina "Nuovo mazzo": scegli la fazione. ANNULLA torna indietro.
export default function NewDeckPage({ onCancel }: { onCancel: () => void }) {
  const [chosen, setChosen] = useState<FactionId>('artificiali') // fazione scelta

  return (
    <main className="page">
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
        <HexButton onClick={onCancel}>ANNULLA</HexButton>
        {/* Nessun onClick: per ora CREA MAZZO non fa nulla */}
        <HexButton variant="light">CREA MAZZO</HexButton>
      </div>
    </main>
  )
}
