import { getSession } from './auth'

// Nome e immagine scelti in "Modifica profilo". Il backend non ha ancora un modo per cambiarli,
// quindi stanno nel browser (localStorage), uno per ogni account.
export type StoredProfile = { name?: string; avatar?: string }

const key = () => `quorma.profile.${getSession()?.username ?? 'ospite'}`

export function loadProfile(): StoredProfile {
  try {
    const raw = localStorage.getItem(key())
    if (raw) return JSON.parse(raw) as StoredProfile
  } catch {
    /* niente salvataggio o dati rovinati: si usano i valori di base */
  }
  return {}
}

/** true se il salvataggio è riuscito. */
export function saveProfile(profile: StoredProfile): boolean {
  try {
    localStorage.setItem(key(), JSON.stringify(profile))
    return true
  } catch {
    return false
  }
}

export const NAME_HINT = 'Da 3 a 20 caratteri: lettere, numeri, spazi, punto, trattino e underscore.'

/** Testo dell'errore se il nome non va bene, altrimenti stringa vuota. */
export function validateName(name: string): string {
  const n = name.trim()
  if (n.length < 3 || n.length > 20) return 'Il nome deve avere da 3 a 20 caratteri.'
  if (!/^[A-Za-z0-9À-ÿ _.-]+$/.test(n)) return 'Usa solo lettere, numeri, spazi, punto, trattino e underscore.'
  return ''
}
