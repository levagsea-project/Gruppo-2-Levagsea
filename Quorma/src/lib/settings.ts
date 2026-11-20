import { defaultSettings } from '../data/profile'
import type { Settings } from '../data/profile'

// Le impostazioni si salvano nel browser (localStorage): il backend non le ha ancora.
const KEY = 'quorma.settings'

export function loadSettings(): Settings {
  try {
    const raw = localStorage.getItem(KEY)
    if (raw) return { ...defaultSettings, ...JSON.parse(raw) }
  } catch {
    /* niente salvataggio o dati rovinati: si parte dai valori di base */
  }
  return defaultSettings
}

/** true se il salvataggio è riuscito. */
export function saveSettings(s: Settings): boolean {
  try {
    localStorage.setItem(KEY, JSON.stringify(s))
    return true
  } catch {
    return false
  }
}
