import type { Page } from '../../data/navItems'
import InfoPage from './InfoPage'

// Colonna a sinistra: invito a leggere FAQ e Regole prima di scrivere.
function BeforeWriting({ onNavigate }: { onNavigate: (page: Page) => void }) {
  return (
    <div className="info-list">
      <span className="info-list-title">Prima di scrivere</span>
      <p className="info-hint">
        Molte risposte sono già nelle{' '}
        <button type="button" onClick={() => onNavigate('faq')}>FAQ</button> e nelle{' '}
        <button type="button" onClick={() => onNavigate('regole')}>Regole</button>.
      </p>
    </div>
  )
}

export default function ContactsPage({ onNavigate }: { onNavigate: (page: Page) => void }) {
  return (
    <InfoPage
      page="contatti"
      title="CONTATTI"
      subtitle="SCRIVI ALLA SQUADRA"
      sideTop={<BeforeWriting onNavigate={onNavigate} />}
      aside={
        // riquadro "Altri canali" (vuoto)
        <div className="q-tela info-channels">
          <div className="q-tela__in" />
        </div>
      }
      onNavigate={onNavigate}
    />
  )
}
