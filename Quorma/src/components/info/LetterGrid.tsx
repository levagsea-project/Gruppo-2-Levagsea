import { useState } from 'react'
import { glossaryLetters } from '../../data/info'

// Griglia delle lettere del glossario. Il primo quadrato ("·") significa "tutte".
export default function LetterGrid() {
  const [active, setActive] = useState<string>('·')

  return (
    <div className="info-list">
      <span className="info-list-title">Lettere</span>
      <div className="letter-grid">
        {[{ letter: '·', available: true }, ...glossaryLetters].map(({ letter, available }) => (
          <button
            key={letter}
            type="button"
            className="letter-cell"
            disabled={!available}
            aria-current={letter === active ? 'true' : undefined}
            onClick={() => setActive(letter)}
          >
            {letter}
          </button>
        ))}
      </div>
    </div>
  )
}
