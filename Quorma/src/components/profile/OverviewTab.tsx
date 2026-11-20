import type { Overview, Series } from '../../data/profile'
import type { Tab } from './ProfileTabs'
import PlayerPanel from './PlayerPanel'
import StatsPanel from './StatsPanel'
import RecentMatches from './RecentMatches'

type Props = { ready: boolean; overview: Overview; series: Series[]; onChangeTab: (tab: Tab) => void; onEditProfile: () => void }

// Scheda "Panoramica": giocatore | statistiche | ultime serie.
export default function OverviewTab({ ready, overview, series, onChangeTab, onEditProfile }: Props) {
  return (
    <div className="pf-body pf-overview">
      <PlayerPanel onEdit={onEditProfile} />
      <StatsPanel ready={ready} overview={overview} />
      <RecentMatches ready={ready} series={series} onAll={() => onChangeTab('partite')} />
    </div>
  )
}
