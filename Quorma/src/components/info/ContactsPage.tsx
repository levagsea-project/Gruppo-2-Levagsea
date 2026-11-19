import { useState, type FormEvent } from 'react'
import type { Page } from '../../data/navItems'
import { CONTACT_EMAIL, contactChannels, contactTabs } from '../../data/info'
import InfoPage from './InfoPage'
import { InfoLink } from './InfoSections'

const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s.]{2,}$/ // stessa regola della registrazione
const MIN_MESSAGE = 20

// Colonna a sinistra: invito a leggere FAQ e Regole prima di scrivere.
function BeforeWriting({ onNavigate }: { onNavigate: (page: Page) => void }) {
  return (
    <div className="info-list">
      <span className="info-list-title">Prima di scrivere</span>
      <p className="info-hint">
        Molte risposte sono già nelle <InfoLink label="FAQ" page="faq" onNavigate={onNavigate} /> e nelle{' '}
        <InfoLink label="Regole" page="regole" onNavigate={onNavigate} />.
      </p>
    </div>
  )
}

// Colonna a destra: gli altri modi per raggiungerci.
function Channels() {
  return (
    <div className="q-tela info-canali">
      <div className="q-tela__in">
        <span className="info-aiuto__titolo">Altri canali</span>
        {contactChannels.map((c) => (
          <div key={c.label} className="info-canale">
            <small>{c.label}</small>
            <span>{c.value}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

// Contatti: modulo con argomento, email, oggetto e messaggio. L'invio non esiste ancora (manca il backend):
// se il modulo è valido lo diciamo chiaramente, invece di fingere che il messaggio sia partito.
export default function ContactsPage({ onNavigate }: { onNavigate: (page: Page) => void }) {
  const [topic, setTopic] = useState(contactTabs[0])
  const [email, setEmail] = useState('')
  const [subject, setSubject] = useState('')
  const [message, setMessage] = useState('')
  const [privacy, setPrivacy] = useState(false)
  const [tried, setTried] = useState(false) // dopo il primo invio mostriamo gli errori
  const [notice, setNotice] = useState(false)

  const emailOk = EMAIL_RE.test(email.trim())
  const messageOk = message.trim().length >= MIN_MESSAGE
  const valid = emailOk && messageOk && privacy

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    setTried(true)
    setNotice(valid)
  }

  return (
    <InfoPage
      page="contatti"
      title="CONTATTI"
      subtitle="SCRIVI ALLA SQUADRA"
      sideTop={<BeforeWriting onNavigate={onNavigate} />}
      aside={<Channels />}
      onNavigate={onNavigate}
    >
      <form className="info-form" noValidate onSubmit={onSubmit}>
        <fieldset className="info-campo">
          <legend className="info-etichetta">Argomento</legend>
          <div className="info-chips">
            {contactTabs.map((t) => (
              <button key={t} type="button" className="info-chip" aria-pressed={t === topic} onClick={() => setTopic(t)}>
                {t}
              </button>
            ))}
          </div>
        </fieldset>

        <label className="info-campo">
          <span className="info-etichetta">Email</span>
          <input
            className="info-input"
            type="email"
            autoComplete="email"
            placeholder="nome@esempio.it"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            aria-invalid={tried && !emailOk ? true : undefined}
            aria-describedby={tried && !emailOk ? 'err-email' : undefined}
          />
          {tried && !emailOk && <span id="err-email" className="info-errore">Inserisci un indirizzo email valido, ad esempio nome@esempio.it.</span>}
        </label>

        <label className="info-campo">
          <span className="info-etichetta">Oggetto <small>(facoltativo)</small></span>
          <input className="info-input" type="text" placeholder="Di cosa si tratta, in poche parole" value={subject} onChange={(e) => setSubject(e.target.value)} />
        </label>

        <label className="info-campo">
          <span className="info-etichetta">Messaggio</span>
          <textarea
            className="info-input"
            placeholder="Raccontaci cosa è successo o cosa ti serve. Se riguarda il tuo account, indica il nome del giocatore."
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            aria-invalid={tried && !messageOk ? true : undefined}
            aria-describedby={tried && !messageOk ? 'err-msg' : 'msg-conta'}
          />
          {tried && !messageOk ? (
            <span id="err-msg" className="info-errore">Scrivi almeno {MIN_MESSAGE} caratteri.</span>
          ) : (
            <span id="msg-conta" className="info-nota">Almeno {MIN_MESSAGE} caratteri.</span>
          )}
        </label>

        <label className="info-spunta">
          <input type="checkbox" checked={privacy} onChange={(e) => setPrivacy(e.target.checked)} aria-invalid={tried && !privacy ? true : undefined} />
          <span>
            Ho letto l’<InfoLink label="informativa sulla privacy" page="privacy" onNavigate={onNavigate} />.
          </span>
        </label>
        {tried && !privacy && <span className="info-errore">Per inviare il messaggio devi accettare l’informativa.</span>}

        <div className="info-invia">
          <button type="submit" className="q-btn q-btn--argento">
            <span className="q-btn__in">Invia messaggio</span>
          </button>
        </div>

        <p className="info-avviso" role="status">
          {notice && (
            <>
              L’invio dei messaggi dal sito non è ancora attivo. Nel frattempo puoi scriverci a <b>{CONTACT_EMAIL}</b>: il testo che hai scritto resta qui.
            </>
          )}
        </p>
      </form>
    </InfoPage>
  )
}
