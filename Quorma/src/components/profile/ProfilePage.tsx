import { useState } from 'react'
import { currentPlayer } from '../../data/player'
import { useProfile } from '../../hooks/useProfile'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import ProfileTabs, { tabs } from './ProfileTabs'
import type { Tab } from './ProfileTabs'
import OverviewTab from './OverviewTab'
import MatchesTab from './MatchesTab'
import SettingsTab from './SettingsTab'
import '../../styles/landing/quorma-ds.css'
import '../../styles/profile.css'

// Pagina Profilo: tre schede (Panoramica, Partite, Impostazioni). Si apre dal riquadro del giocatore.
export default function ProfilePage() {
  const [tab, setTab] = useState<Tab>('panoramica')
  const { overview, matches } = useProfile()
  const subtitle = tabs.find((t) => t.id === tab)?.subtitle || currentPlayer().name.toUpperCase()

  return (
    <main className="page profile-page">
      <CurrencyBar />
      <div className="pf-head">
        <PageTitle title="PROFILO" subtitle={subtitle} />
        <ProfileTabs current={tab} onChange={setTab} />
      </div>

      {tab === 'panoramica' && <OverviewTab overview={overview} matches={matches} onChangeTab={setTab} />}
      {tab === 'partite' && <MatchesTab matches={matches} />}
      {tab === 'impostazioni' && <SettingsTab />}
    </main>
  )
}
