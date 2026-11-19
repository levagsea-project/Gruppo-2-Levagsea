// Accesso e registrazione verso il backend (POST /auth/login e /auth/register).
// Le chiamate e la traduzione degli errori sono in api.ts.
import { ApiError, GENERIC, request, translate } from './api'

export type AuthResponse = {
  token: string
  tokenType: string
  expiresInSeconds: number
  userId: string
  username: string
  role: string
}

export type LoginData = { username: string; password: string }
export type RegisterData = LoginData & { email: string }
export type FieldErrors = Partial<Record<'username' | 'email' | 'password' | 'terms', string>>

// ---------- Validazione: stesse regole e stessi messaggi del backend ----------
// Specchio di RegisterRequest e LoginRequest (backend/.../auth): controlliamo prima qui per non fare
// una chiamata inutile, ma se qualcosa sfugge il backend risponde con l'errore sul campo giusto.
// Una regola per campo, nello stesso ordine del backend: obbligatorio, poi formato, poi lunghezza.
const USERNAME_RE = /^[A-Za-z0-9_.-]{3,32}$/
const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s.]{2,}$/
const EMAIL_MAX = 254
const PASSWORD_MIN = 8
const PASSWORD_MAX = 72 // caratteri e anche byte UTF-8: BCrypt legge solo i primi 72 byte

const blank = (s: string) => s.trim() === ''

export const USERNAME_HINT = translate('validation.username.pattern')
export const PASSWORD_HINT = translate('validation.password.size', { min: PASSWORD_MIN, max: PASSWORD_MAX })

export function validateRegister(d: RegisterData, termsAccepted: boolean): FieldErrors {
  const errors: FieldErrors = {}

  if (blank(d.username)) errors.username = translate('validation.required')
  else if (!USERNAME_RE.test(d.username)) errors.username = USERNAME_HINT

  if (blank(d.email)) errors.email = translate('validation.required')
  else if (!EMAIL_RE.test(d.email)) errors.email = translate('validation.email.invalid')
  else if (d.email.length > EMAIL_MAX) errors.email = translate('validation.email.max_length', { max: EMAIL_MAX })

  if (blank(d.password)) errors.password = translate('validation.required')
  else if (d.password.length < PASSWORD_MIN || d.password.length > PASSWORD_MAX) errors.password = PASSWORD_HINT
  else if (new TextEncoder().encode(d.password).length > PASSWORD_MAX)
    errors.password = translate('validation.max_utf8_bytes', { value: PASSWORD_MAX })

  // Solo frontend: il backend non riceve i termini.
  if (!termsAccepted) errors.terms = 'Per registrarti devi accettare i termini.'
  return errors
}

export function validateLogin(d: LoginData): FieldErrors {
  const errors: FieldErrors = {}
  if (blank(d.username)) errors.username = translate('validation.required')
  if (blank(d.password)) errors.password = translate('validation.required')
  return errors
}

// ---------- Errori del backend → modulo ----------
const FORM_FIELDS = ['username', 'email', 'password'] as const

// Errori che riguardano un solo campo anche se non arrivano da fieldErrors.
const CODE_FIELD: Record<string, (typeof FORM_FIELDS)[number]> = {
  'auth.username_taken': 'username',
  'auth.email_taken': 'email',
}

/**
 * Divide un errore del backend tra i campi del modulo (sotto l'input giusto) e un messaggio generale
 * (nel riquadro in fondo). Se tutto finisce sui campi, il messaggio generale è vuoto.
 */
export function formErrors(err: unknown): { fields: FieldErrors; message: string } {
  if (!(err instanceof ApiError)) return { fields: {}, message: GENERIC }

  const fields: FieldErrors = {}
  for (const name of FORM_FIELDS) {
    const f = err.fieldErrors[name]
    if (f) fields[name] = translate(f.code, f.params)
  }
  const single = err.code ? CODE_FIELD[err.code] : undefined
  if (single) fields[single] = err.message

  const onFields = Object.keys(fields).length > 0
  return { fields, message: onFields ? '' : err.message }
}

// ---------- Chiamate al backend ----------
// I nomi utente non possono contenere spazi: togliamo quelli finiti per sbaglio all'inizio o alla fine.
const post = (path: string, body: LoginData | RegisterData) =>
  request<AuthResponse>('POST', path, { body: { ...body, username: body.username.trim() } })

// ---------- Accesso demo (senza backend) ----------
// Con VITE_AUTH_DEMO=true nel file .env, accesso e registrazione accettano qualsiasi nome e password
// senza chiamare il backend: serve a chi lavora sul frontend senza Docker.
// Funziona solo con "npm run dev": nella build di produzione import.meta.env.DEV è false e il codice sparisce.
export const DEMO_AUTH = import.meta.env.DEV && import.meta.env.VITE_AUTH_DEMO === 'true'

function demoResponse(username: string): AuthResponse {
  return {
    token: 'demo',
    tokenType: 'Bearer',
    expiresInSeconds: 7 * 24 * 3600,
    userId: '00000000-0000-0000-0000-000000000000',
    username: username.trim() || 'demo',
    role: 'PLAYER',
  }
}

export const login = (d: LoginData) => (DEMO_AUTH ? Promise.resolve(demoResponse(d.username)) : post('/auth/login', d))
export const register = (d: RegisterData) =>
  DEMO_AUTH ? Promise.resolve(demoResponse(d.username)) : post('/auth/register', d)

/** Accesso fatto in modalità demo: il token è finto, quindi non si chiama il backend. */
export const isDemoSession = () => getSession()?.token === 'demo'

// ---------- Chiamate con il token ----------
// Per le chiamate che richiedono l'accesso (/decks, ...). Se il token non è più valido (scaduto, oppure
// il backend è stato riavviato con un'altra chiave), il backend risponde 401: si torna alla finestra di accesso.
export async function authedRequest<T>(method: string, path: string, body?: unknown): Promise<T> {
  const session = getSession()
  if (!session) {
    window.location.replace('/accedi')
    throw new ApiError(401, null, 'Sessione scaduta: accedi di nuovo.')
  }
  try {
    return await request<T>(method, path, { body, token: session.token })
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) {
      clearSession()
      window.location.replace('/accedi')
    }
    throw err
  }
}

// ---------- Sessione ----------
// "Ricordami" = localStorage (resta dopo la chiusura del browser), altrimenti sessionStorage.
const KEY = 'quorma.session'
type Session = { token: string; username: string; expiresAt: number }

export function saveSession(r: AuthResponse, remember: boolean) {
  const session: Session = { token: r.token, username: r.username, expiresAt: Date.now() + r.expiresInSeconds * 1000 }
  clearSession()
  try {
    ;(remember ? localStorage : sessionStorage).setItem(KEY, JSON.stringify(session))
  } catch {
    // archiviazione bloccata: l'accesso vale solo finché la pagina resta aperta
  }
}

export function getSession(): Session | null {
  try {
    const raw = localStorage.getItem(KEY) ?? sessionStorage.getItem(KEY)
    if (!raw) return null
    const s = JSON.parse(raw) as Session
    if (s.expiresAt <= Date.now()) {
      clearSession()
      return null
    }
    return s
  } catch {
    return null
  }
}

export function clearSession() {
  try {
    localStorage.removeItem(KEY)
    sessionStorage.removeItem(KEY)
  } catch {
    // archiviazione non disponibile (es. navigazione privata bloccata): niente da cancellare
  }
}
