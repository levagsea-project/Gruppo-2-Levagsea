// Chiamate al backend e traduzione dei suoi errori.
//
// Il backend non manda mai testo per il giocatore: ogni errore ha un `code` (es. "auth.invalid_credentials")
// che è una chiave di QUORMA-backend-engine/data/labels/it.json. Leggiamo le frasi da lì, così restano
// sempre allineate al backend. Forma della risposta (ProblemDetail, RFC 9457):
//   { status, detail, code }                          errori normali (detail ripete il code)
//   { status, detail, code: "validation.failed",
//     fieldErrors: { campo: { code, params } } }      400 di validazione, un errore per campo
// `params` riempie i segnaposto {min}, {max}, {value} della frase.
import labels from '../../../QUORMA-backend-engine/data/labels/it.json'

const LABELS: Record<string, string> = labels

// Indirizzo del backend senza "/" finale. Vuoto = stesso sito: in sviluppo ci pensa il proxy di Vite.
// Se è impostato, il browser chiama il backend direttamente (CORS: l'origine deve essere in
// cors.allowed-origins del backend, di default http://localhost:5173 e http://127.0.0.1:5173).
const API_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/+$/, '')

export const UNREACHABLE = 'Server non raggiungibile. Riprova tra poco.'
export const GENERIC = 'Qualcosa è andato storto. Riprova.'

export type Params = Record<string, unknown>
export type FieldError = { code: string; params?: Params }

/** Frase italiana per un codice del backend; se il codice è sconosciuto, un messaggio generico. */
export function translate(code: string | null | undefined, params: Params = {}): string {
  const text = code ? LABELS[code] : undefined
  if (!text) return GENERIC
  return text.replace(/\{(\w+)\}/g, (all, key: string) => (key in params ? String(params[key]) : all))
}

export class ApiError extends Error {
  status: number
  code: string | null
  fieldErrors: Record<string, FieldError>
  errors: FieldError[] // regole violate, una per voce (es. mazzo non valido: "deck.size", "deck.creature_count"...)

  constructor(status: number, code: string | null, message: string, fieldErrors: Record<string, FieldError> = {}, errors: FieldError[] = []) {
    super(message)
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
    this.errors = errors
  }
}

// Lingua dei testi delle carte (nome, testo di colore, nome del set). L'interfaccia è in italiano, quindi chiediamo
// l'italiano: il backend usa la prima lingua che la carta ha e ripiega sull'italiano carta per carta (textLocale).
const TEXT_LANGUAGE = 'it'

type RequestOptions = { body?: unknown; token?: string }

/** Chiama il backend e restituisce il JSON; in caso di errore lancia ApiError con il messaggio già tradotto. */
export async function request<T>(method: string, path: string, { body, token }: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { 'Accept-Language': TEXT_LANGUAGE }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  let res: Response
  try {
    res = await fetch(API_URL + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    // Rete assente, backend spento o origine non ammessa dal CORS: il browser non distingue i casi.
    throw new ApiError(0, null, UNREACHABLE)
  }

  const data = await res.json().catch(() => null)
  if (res.ok) return data as T

  const code: string | null = typeof data?.code === 'string' ? data.code : null
  const fieldErrors: Record<string, FieldError> = data?.fieldErrors ?? {}
  const errors: FieldError[] = Array.isArray(data?.errors) ? data.errors : []
  let message: string
  if (code && LABELS[code]) message = translate(code)
  else if (res.status >= 500 || !data) message = UNREACHABLE
  else message = GENERIC
  throw new ApiError(res.status, code, message, fieldErrors, errors)
}
