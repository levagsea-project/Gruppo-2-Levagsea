import type { Result } from '../../data/profile'
import { resultLabel } from '../../data/profile'

// La parola VITTORIA / SCONFITTA / PAREGGIO, colorata in base all'esito.
export default function ResultLabel({ result, className = '' }: { result: Result; className?: string }) {
  return <span className={`pf-result is-${result} ${className}`}>{resultLabel[result]}</span>
}
