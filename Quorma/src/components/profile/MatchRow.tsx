import type { Match } from '../../data/profile'
import { formatWhen } from '../../lib/matches'
import ResultLabel from './ResultLabel'

type Props = { match: Match; selected: boolean; onSelect: () => void }

// Una riga dello storico: esito, avversario, dettagli e orario.
export default function MatchRow({ match, selected, onSelect }: Props) {
  return (
    <button type="button" className="pf-match" aria-current={selected ? 'true' : undefined} onClick={onSelect}>
      <ResultLabel result={match.result} />
      <span className="pf-match-main">
        <b>Contro {match.opponent}</b>
        <small>{match.mode} · {match.deck} · {match.turns} turni</small>
      </span>
      <time className="pf-match-when">{formatWhen(match.playedAt)}</time>
    </button>
  )
}
