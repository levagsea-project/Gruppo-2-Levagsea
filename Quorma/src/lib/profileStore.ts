import { USERNAME_MAX, USERNAME_MIN, getSession } from './auth'

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

// Stessa lunghezza del nome scelto alla registrazione.
export const NAME_MAX = USERNAME_MAX
export const NAME_HINT = `Da ${USERNAME_MIN} a ${NAME_MAX} caratteri: lettere, numeri, spazi, punto, trattino e underscore.`

/** Testo dell'errore se il nome non va bene, altrimenti stringa vuota. */
export function validateName(name: string): string {
  const n = name.trim()
  if (n.length < USERNAME_MIN || n.length > NAME_MAX) return `Il nome deve avere da ${USERNAME_MIN} a ${NAME_MAX} caratteri.`
  if (!/^[A-Za-z0-9À-ÿ _.-]+$/.test(n)) return 'Usa solo lettere, numeri, spazi, punto, trattino e underscore.'
  return ''
}
