// L'ultima configurazione della pagina Gioca: il backend non salva queste preferenze, quindi le ricorda il browser,
// una per account. Sono solo un suggerimento: un mazzo eliminato o obsoleto viene ignorato dalla pagina.
import { getSession } from './auth'
import type { PlayModeId } from '../data/play'
import type { FactionId } from '../types/deck'

export type PlayPrefs = {
  mode: PlayModeId
  start: FactionId
  decks: Partial<Record<FactionId, string>>
}

const DEFAULTS: PlayPrefs = { mode: 'libera', start: 'artificiali', decks: {} }
const MODES: PlayModeId[] = ['libera', 'stanza', 'sfida']
const key = () => `quorma.gioca.${getSession()?.username ?? 'ospite'}`
const text = (v: unknown) => (typeof v === 'string' && v ? v : undefined)

export function readPlayPrefs(): PlayPrefs {
  try {
    const raw = JSON.parse(localStorage.getItem(key()) ?? 'null')
    if (!raw || typeof raw !== 'object') return DEFAULTS
    return {
      mode: MODES.includes(raw.mode) ? raw.mode : DEFAULTS.mode,
      start: raw.start === 'umani' || raw.start === 'artificiali' ? raw.start : DEFAULTS.start,
      decks: { artificiali: text(raw.decks?.artificiali), umani: text(raw.decks?.umani) },
    }
  } catch {
    return DEFAULTS
  }
}

export function writePlayPrefs(prefs: PlayPrefs) {
  try {
    localStorage.setItem(key(), JSON.stringify(prefs))
  } catch {
    /* browser pieno o senza salvataggio: la prossima volta si riparte dai mazzi di difesa */
  }
}
