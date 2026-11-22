import { useEffect, useRef, useState, type FormEvent, type MouseEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import {
  DEMO_AUTH,
  PASSWORD_HINT,
  USERNAME_HINT,
  formErrors,
  login,
  register,
  saveSession,
  validateLogin,
  validateRegister,
  type FieldErrors,
} from '../../lib/auth'

export type AuthMode = 'login' | 'signup'

// Finestra di accesso e registrazione. Si apre su /accedi e /registrati, sopra la landing.
// Usa <dialog> del browser: blocca il resto della pagina, si chiude con Esc e tiene il focus dentro.
export default function AuthDialog({ mode }: { mode: AuthMode }) {
  const signup = mode === 'signup'
  const navigate = useNavigate()
  const dialogRef = useRef<HTMLDialogElement>(null)

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [remember, setRemember] = useState(true)
  const [terms, setTerms] = useState(false)
  const [errors, setErrors] = useState<FieldErrors>({})
  const [serverError, setServerError] = useState('')
  const [sending, setSending] = useState(false)

  // Apre la finestra appena la pagina è pronta nel browser.
  useEffect(() => {
    const dialog = dialogRef.current
    if (dialog && !dialog.open) dialog.showModal()
  }, [])

  const close = () => navigate('/', { preventScrollReset: true })

  // Click sullo sfondo scuro (fuori dal riquadro) = chiudi.
  const onBackdrop = (e: MouseEvent<HTMLDialogElement>) => {
    if (e.target === dialogRef.current) close()
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    const found = signup ? validateRegister({ username, email, password }, terms) : validateLogin({ username, password })
    setErrors(found)
    setServerError('')
    if (Object.keys(found).length > 0) return

    setSending(true)
    try {
      const res = signup ? await register({ username, email, password }) : await login({ username, password })
      saveSession(res, remember)
      navigate('/app')
    } catch (err) {
      // Il backend dice quale campo è sbagliato: l'errore va sotto quel campo, il resto nel riquadro.
      const { fields, message } = formErrors(err)
      setErrors(fields)
      setServerError(message)
      setSending(false)
    }
  }

  // Quando il giocatore corregge un campo, il suo errore sparisce.
  const edit = (name: keyof FieldErrors, set: (v: string) => void) => (v: string) => {
    set(v)
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: undefined }))
  }

  return (
    <dialog
      ref={dialogRef}
      className="l-dialog"
      aria-labelledby="auth-titolo"
      onCancel={(e) => { e.preventDefault(); close() }}
      onClick={onBackdrop}
    >
      <div className="q-tela q-tela--arco">
        <div className="q-tela__in">
          <button type="button" className="l-chiudi" aria-label="Chiudi" onClick={close}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
              <path d="M6 6l12 12M18 6 6 18" />
            </svg>
          </button>
          <img className="l-dialog__logo" src="/images/ui/logo-quorma.png" alt="" width={640} height={179} />
          <h2 id="auth-titolo">{signup ? 'Crea il tuo account' : 'Bentornato'}</h2>

          <nav className="l-schede" aria-label="Accesso o registrazione">
            <Link to="/accedi" replace preventScrollReset className="l-scheda" aria-current={signup ? undefined : 'page'}>Accedi</Link>
            <Link to="/registrati" replace preventScrollReset className="l-scheda" aria-current={signup ? 'page' : undefined}>Registrati</Link>
          </nav>

          {DEMO_AUTH && (
            <p className="l-campo__hint" role="note">
              Modalità demo attiva: va bene qualsiasi nome e password, il backend non viene chiamato.
            </p>
          )}

          <form className="l-form" noValidate onSubmit={onSubmit}>
            <Field
              label="Nome del giocatore"
              name="username"
              value={username}
              onChange={edit('username', setUsername)}
              error={errors.username}
              autoComplete="username"
              autoFocus
              hint={signup ? USERNAME_HINT : undefined}
            />
            {signup && (
              <Field label="Email" name="email" type="email" value={email} onChange={edit('email', setEmail)} error={errors.email} autoComplete="email" />
            )}
            <Field
              label="Password"
              name="password"
              type="password"
              value={password}
              onChange={edit('password', setPassword)}
              error={errors.password}
              autoComplete={signup ? 'new-password' : 'current-password'}
              hint={signup ? PASSWORD_HINT : undefined}
            />

            <label className="l-spunta">
              <input type="checkbox" name="remember" checked={remember} onChange={(e) => setRemember(e.target.checked)} />
              Ricordami su questo dispositivo
            </label>

            {signup && (
              <div>
                <label className="l-spunta">
                  <input
                    type="checkbox"
                    name="terms"
                    checked={terms}
                    onChange={(e) => {
                      setTerms(e.target.checked)
                      if (errors.terms) setErrors((prev) => ({ ...prev, terms: undefined }))
                    }}
                    aria-invalid={errors.terms ? true : undefined}
                    aria-describedby={errors.terms ? 'terms-err' : undefined}
                  />
                  <span>Accetto i Termini e l'informativa sulla privacy.</span>
                </label>
                {errors.terms && <p id="terms-err" className="l-errore" style={{ marginTop: 6 }}>{errors.terms}</p>}
              </div>
            )}

            {serverError && <p className="l-errore l-errore--box" role="alert">{serverError}</p>}

            <button type="submit" className="q-btn q-btn--argento" disabled={sending}>
              <span className="q-btn__in">
                {sending ? 'Attendi…' : signup ? 'Crea account ed entra' : 'Accedi ed entra'}
              </span>
            </button>
          </form>

          <p className="l-cambia">
            {signup ? 'Hai già un account? ' : 'Non hai un account? '}
            <Link to={signup ? '/accedi' : '/registrati'} replace preventScrollReset>
              {signup ? 'Accedi' : 'Registrati'}
            </Link>
          </p>
        </div>
        <span className="q-borchia q-borchia--n" />
      </div>
    </dialog>
  )
}

type FieldProps = {
  label: string
  name: string
  value: string
  onChange: (v: string) => void
  error?: string
  hint?: string
  type?: string
  autoComplete?: string
  autoFocus?: boolean
}

// Un campo del modulo con etichetta, suggerimento ed eventuale errore collegati all'input.
function Field({ label, name, value, onChange, error, hint, type = 'text', autoComplete, autoFocus }: FieldProps) {
  const msgId = `${name}-msg`
  return (
    <label className="l-campo">
      <span>{label}</span>
      <input
        className="l-input"
        name={name}
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        autoComplete={autoComplete}
        autoFocus={autoFocus}
        aria-invalid={error ? true : undefined}
        aria-describedby={error || hint ? msgId : undefined}
      />
      {(error || hint) && (
        <span id={msgId} className={error ? 'l-errore' : 'l-campo__hint'}>{error ?? hint}</span>
      )}
    </label>
  )
}
