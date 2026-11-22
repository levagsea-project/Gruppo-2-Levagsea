import type { Page } from '../../data/navItems'
import { aboutSections, team } from '../../data/info'
import InfoPage from './InfoPage'
import InfoSections from './InfoSections'
import SectionList from './SectionList'

// Elenco della squadra, in fondo alla sezione "La squadra".
function Team() {
  return (
    <ul className="info-squadra">
      {team.map((m, i) => (
        <li key={`${m.name}-${i}`}>
          <b>{m.name}</b>
          <span>{m.role}</span>
        </li>
      ))}
    </ul>
  )
}

// "Lavora con noi" finisce con il pulsante per scrivere alla squadra.
function WriteUs({ onNavigate }: { onNavigate: (page: Page) => void }) {
  return (
    <button type="button" className="q-btn q-btn--acciaio info-cta" onClick={() => onNavigate('contatti')}>
      <span className="q-btn__in">Scrivici</span>
    </button>
  )
}

export default function AboutPage({ onNavigate }: { onNavigate: (page: Page) => void }) {
  return (
    <InfoPage
      page="chi-siamo"
      title="CHI SIAMO"
      subtitle="LEVAGSEA E QUORMA"
      sideTop={<SectionList title="In questa pagina" sections={aboutSections} />}
      sideLabel="In questa pagina"
      onNavigate={onNavigate}
    >
      <div className="info-hero" role="img" aria-label="Un viandante con una torcia davanti alle rovine di una cattedrale e a un colosso meccanico abbattuto">
        <span aria-hidden="true">LEVAGSEA</span>
      </div>
      <InfoSections
        sections={aboutSections}
        onNavigate={onNavigate}
        extra={{ squadra: <Team />, lavora: <WriteUs onNavigate={onNavigate} /> }}
      />
    </InfoPage>
  )
}
