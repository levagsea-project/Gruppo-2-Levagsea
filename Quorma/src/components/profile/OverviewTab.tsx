import type { Match, Overview } from '../../data/profile'
import type { Tab } from './ProfileTabs'
import PlayerPanel from './PlayerPanel'
import StatsPanel from './StatsPanel'
import RecentMatches from './RecentMatches'

type Props = { overview: Overview | null; matches: Match[]; onChangeTab: (tab: Tab) => void }

// Scheda "Panoramica": giocatore | statistiche | ultime partite.
export default function OverviewTab({ overview, matches, onChangeTab }: Props) {
  return (
    <div className="pf-body pf-overview">
      <PlayerPanel onEdit={() => onChangeTab('impostazioni')} />
      <StatsPanel overview={overview} />
      <RecentMatches matches={matches} onAll={() => onChangeTab('partite')} />
    </div>
  )
}
