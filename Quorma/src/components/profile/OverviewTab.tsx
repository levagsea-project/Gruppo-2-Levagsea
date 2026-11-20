import type { Match, Overview } from '../../data/profile'
import type { Tab } from './ProfileTabs'
import PlayerPanel from './PlayerPanel'
import StatsPanel from './StatsPanel'
import RecentMatches from './RecentMatches'

type Props = { overview: Overview; matches: Match[]; onChangeTab: (tab: Tab) => void; onEditProfile: () => void }

// Scheda "Panoramica": giocatore | statistiche | ultime partite.
export default function OverviewTab({ overview, matches, onChangeTab, onEditProfile }: Props) {
  return (
    <div className="pf-body pf-overview">
      <PlayerPanel onEdit={onEditProfile} />
      <StatsPanel overview={overview} />
      <RecentMatches matches={matches} onAll={() => onChangeTab('partite')} />
    </div>
  )
}
