import type { FormEvent } from 'react'
import type { Page } from '../../data/navItems'
import { contactTabs } from '../../data/info'
import InfoPage from './InfoPage'
import { InfoLink } from './InfoSections'

// Colonna a sinistra: dove trovare le risposte, visto che scrivere non è possibile.
function WhereToLook({ onNavigate }: { onNavigate: (page: Page) => void }) {
  return (
    <div className="info-list">
      <span className="info-list-title">Dove trovare risposte</span>
      <p className="info-hint">
        Consulta le <InfoLink label="FAQ" page="faq" onNavigate={onNavigate} /> e le{' '}
        <InfoLink label="Regole" page="regole" onNavigate={onNavigate} />.
      </p>
    </div>
  )
}

// Contatti: manca il servizio che riceve i messaggi, quindi il modulo resta visibile ma non si può compilare né inviare.
export default function ContactsPage({ onNavigate }: { onNavigate: (page: Page) => void }) {
  const block = (e: FormEvent) => e.preventDefault()

  return (
    <InfoPage page="contatti" title="CONTATTI" subtitle="ASSISTENZA" sideTop={<WhereToLook onNavigate={onNavigate} />} onNavigate={onNavigate}>
      <form className="info-modulo" noValidate onSubmit={block} aria-describedby="contatti-stato">
        <p id="contatti-stato" className="info-avviso">
          <b>Il modulo non è attivo.</b> Il sito non può ricevere messaggi, quindi per ora non è possibile scriverci da qui.
        </p>

        <fieldset className="info-form" disabled>
          <fieldset className="info-campo">
            <legend className="info-etichetta">Argomento</legend>
            <div className="info-chips">
              {contactTabs.map((t) => (
                <button key={t} type="button" className="info-chip">{t}</button>
              ))}
            </div>
          </fieldset>

          <label className="info-campo">
            <span className="info-etichetta">Email</span>
            <input className="info-input" type="email" />
          </label>

          <label className="info-campo">
            <span className="info-etichetta">Oggetto <small>(facoltativo)</small></span>
            <input className="info-input" type="text" />
          </label>

          <label className="info-campo">
            <span className="info-etichetta">Messaggio</span>
            <textarea className="info-input" />
          </label>

          <div className="info-invia">
            <button type="submit" className="q-btn q-btn--acciaio">
              <span className="q-btn__in">Invio non attivo</span>
            </button>
          </div>
        </fieldset>
      </form>
    </InfoPage>
  )
}
