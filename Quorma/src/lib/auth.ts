// Accesso e registrazione verso il backend (POST /auth/login e /auth/register).
// In sviluppo /auth passa dal proxy di Vite (vite.config.ts), quindi l'indirizzo è relativo.

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

// ---------- Validazione: stesse regole del backend ----------
// Il backend risponde 400 senza dire quale campo è sbagliato, quindi controlliamo prima qui.
const USERNAME_RE = /^[A-Za-z0-9_.-]{3,32}$/
const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/

export function validateRegister(d: RegisterData, termsAccepted: boolean): FieldErrors {
  const errors: FieldErrors = {}
  if (!USERNAME_RE.test(d.username)) errors.username = 'Da 3 a 32 caratteri: lettere, cifre, _ . -'
  if (!EMAIL_RE.test(d.email) || d.email.length > 254) errors.email = 'Inserisci un indirizzo email valido.'
  const bytes = new TextEncoder().encode(d.password).length
  if (d.password.length < 8 || bytes > 72) errors.password = 'Da 8 a 72 caratteri.'
  if (!termsAccepted) errors.terms = 'Per registrarti devi accettare i termini.'
  return errors
}

export function validateLogin(d: LoginData): FieldErrors {
  const errors: FieldErrors = {}
  if (!d.username.trim()) errors.username = 'Inserisci il nome del giocatore.'
  if (!d.password) errors.password = 'Inserisci la password.'
  return errors
}

// ---------- Chiamate al backend ----------
export class AuthError extends Error {}

// Messaggi in italiano per le risposte del backend (che sono in inglese).
const MESSAGES: Record<string, string> = {
  'Username already taken': 'Questo nome del giocatore è già in uso.',
  'Email already registered': 'Questa email è già registrata.',
  'Username or email already in use': 'Nome del giocatore o email già in uso.',
  'Invalid username or password': 'Nome del giocatore o password non corretti.',
}

async function post(path: string, body: unknown): Promise<AuthResponse> {
  let res: Response
  try {
    res = await fetch(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
  } catch {
    throw new AuthError('Server non raggiungibile. Riprova tra poco.')
  }

  const data = await res.json().catch(() => null)
  if (res.ok && data?.token) return data as AuthResponse

  const detail: string | undefined = data?.detail
  if (detail && MESSAGES[detail]) throw new AuthError(MESSAGES[detail])
  if (res.status === 400) throw new AuthError('Controlla i dati inseriti.')
  if (res.status >= 500) throw new AuthError('Server non raggiungibile. Riprova tra poco.')
  throw new AuthError('Qualcosa è andato storto. Riprova.')
}

export const login = (d: LoginData) => post('/auth/login', d)
export const register = (d: RegisterData) => post('/auth/register', d)

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
