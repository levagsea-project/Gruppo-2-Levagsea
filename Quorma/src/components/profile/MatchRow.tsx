import type { Series } from '../../data/profile'
import { formatWhen } from '../../lib/matches'
import ResultLabel from './ResultLabel'

type Props = { series: Series; selected: boolean; onSelect: () => void }

// Una riga dello storico: esito della serie, avversario, modalità e punteggio, orario.
export default function MatchRow({ series, selected, onSelect }: Props) {
  return (
    <button type="button" className="pf-match" aria-current={selected ? 'true' : undefined} onClick={onSelect}>
      <ResultLabel result={series.result} />
      <span className="pf-match-main">
        <b>Contro {series.opponent}</b>
        <small>{series.mode} · {series.won}–{series.lost}</small>
      </span>
      <time className="pf-match-when">{formatWhen(series.finishedAt)}</time>
    </button>
  )
}
