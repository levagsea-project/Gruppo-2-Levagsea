import { glossaryLetters } from '../../data/info'

export const ALL_LETTERS = '·'

type Props = {
  available: Set<string> // lettere con almeno un termine; le altre sono spente
  active: string // lettera scelta, oppure ALL_LETTERS
  onPick: (letter: string) => void
}

// Griglia delle lettere del glossario. Il primo quadrato ("·") significa "tutte".
export default function LetterGrid({ available, active, onPick }: Props) {
  return (
    <div className="info-list">
      <span className="info-list-title" id="lettere-titolo">Lettere</span>
      <div className="letter-grid" role="group" aria-labelledby="lettere-titolo">
        {[ALL_LETTERS, ...glossaryLetters].map((letter) => {
          const enabled = letter === ALL_LETTERS || available.has(letter)
          return (
            <button
              key={letter}
              type="button"
              className="letter-cell"
              disabled={!enabled}
              aria-pressed={letter === active}
              aria-label={letter === ALL_LETTERS ? 'Tutte le lettere' : `Lettera ${letter}`}
              onClick={() => onPick(letter)}
            >
              {letter}
            </button>
          )
        })}
      </div>
    </div>
  )
}
