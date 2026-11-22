import type { GameCard } from '../../types/deck'

// Gli effetti di una carta per esteso, accanto alla carta (Collezione e carta ingrandita).
export default function AbilityList({ card }: { card: GameCard }) {
  const abilities = card.abilities ?? []
  const missing = card.untextedAbilities ?? 0
  return (
    <div className="q-effetti">
      <h3 className="q-effetti__titolo">Effetti</h3>
      {abilities.length > 0 && (
        <ul>
          {abilities.map((a) => (
            <li key={a.id}>
              {!a.free && <b>{a.name}</b>}
              <span>{a.text}</span>
            </li>
          ))}
        </ul>
      )}
      {missing > 0 && <p>{abilities.length > 0 ? 'Questa carta ha anche un altro effetto, la cui descrizione non è ancora disponibile.' : 'La descrizione di questo effetto non è ancora disponibile.'}</p>}
      {abilities.length === 0 && missing === 0 && <p>Questa carta non ha effetti.</p>}
    </div>
  )
}
