import { defaultSettings } from '../data/profile'
import type { Settings } from '../data/profile'

// Le impostazioni si salvano nel browser (localStorage): il backend non le ha ancora.
const KEY = 'quorma.settings'

// Quanto la velocità scelta accelera o rallenta i turni della battaglia, prima del ×1/×2/×4 della battaglia stessa.
const SPEED: Record<Settings['speed'], { attr: string; rate: number }> = {
  Lenta: { attr: 'slow', rate: 0.7 },
  Normale: { attr: 'normal', rate: 1 },
  Veloce: { attr: 'fast', rate: 1.5 },
}

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

/** Porta movimento, contrasto e velocità su :root (styles/preferences.css). Vale solo nell'area di gioco. */
export function applySettings(s: Settings) {
  const root = document.documentElement
  root.dataset.motion = s.reduceMotion ? 'reduced' : 'full'
  root.dataset.contrast = s.highContrast ? 'high' : 'normal'
  root.dataset.speed = SPEED[s.speed]?.attr ?? SPEED.Normale.attr
}

/** Uscendo dall'area di gioco la landing torna com'è. */
export function clearAppliedSettings() {
  const root = document.documentElement
  delete root.dataset.motion
  delete root.dataset.contrast
  delete root.dataset.speed
}

/** Moltiplicatore dei tempi della battaglia, dalla velocità applicata. */
export function battleRate() {
  const attr = document.documentElement.dataset.speed
  return Object.values(SPEED).find((v) => v.attr === attr)?.rate ?? 1
}

/** Movimento ridotto: dall'impostazione del sito o da quella del sistema. */
export const prefersReducedMotion = () =>
  document.documentElement.dataset.motion === 'reduced' || !!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
