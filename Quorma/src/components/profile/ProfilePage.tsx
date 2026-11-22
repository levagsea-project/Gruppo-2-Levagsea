import { useState } from 'react'
import { currentPlayer } from '../../data/player'
import type { Page } from '../../data/navItems'
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
// Storico e statistiche vengono dal backend (GET /matches) a ogni apertura (hooks/useProfile.ts).
type Props = { initialTab?: Tab; onNavigate: (page: Page) => void }

export default function ProfilePage({ initialTab = 'panoramica', onNavigate }: Props) {
  const [tab, setTab] = useState<Tab>(initialTab)
  const { state, error, complete, series, overview, reload, loadDetail } = useProfile()
  const subtitle = tabs.find((t) => t.id === tab)?.subtitle || currentPlayer().name.toUpperCase()
  const ready = state === 'ready'

  return (
    <main className="page profile-page">
      <CurrencyBar />
      <div className="pf-head">
        <PageTitle title="PROFILO" subtitle={subtitle} />
        <ProfileTabs current={tab} onChange={setTab} />
      </div>

      {tab !== 'impostazioni' && (
        <div aria-live="polite">
          {state === 'loading' && <p className="pf-avviso">Carico le tue partite…</p>}
          {state === 'error' && (
            <p className="pf-avviso pf-avviso--errore" role="alert">
              {error}{' '}
              <button type="button" onClick={reload}>Riprova</button>
            </p>
          )}
          {ready && !complete && <p className="pf-avviso">Mostro le {series.length} serie più recenti.</p>}
        </div>
      )}

      {tab === 'panoramica' && (
        <OverviewTab
          ready={ready}
          overview={overview}
          series={series}
          onChangeTab={setTab}
          onEditProfile={() => onNavigate('modifica-profilo')}
        />
      )}
      {tab === 'partite' && <MatchesTab ready={ready} series={series} onLoadDetail={loadDetail} onPlay={() => onNavigate('gioca')} />}
      {tab === 'impostazioni' && <SettingsTab />}
    </main>
  )
}
