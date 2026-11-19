import { useCallback, useEffect, useState } from 'react'
import type { FactionId, PlayStatus } from '../types/deck'
import { translate } from '../lib/api'
import { isDemoSession } from '../lib/auth'
import { getPlayStatus } from '../lib/decks'
import { factions } from '../data/factions'

// "Posso giocare?" (GET /decks/play-status). Il backend chiede di rileggerlo quando si apre la home:
// lo stato può cambiare durante la sessione (un mazzo diventa obsoleto, ne scegli uno di difesa...).
// null = non ancora letto, oppure modalità demo, oppure backend non raggiungibile: in quel caso non blocchiamo nulla,
// ma failed = true permette di dire al giocatore che non siamo riusciti a controllare.
export function usePlayStatus() {
  const [status, setStatus] = useState<PlayStatus | null>(null)
  const [failed, setFailed] = useState(false)
  const [attempt, setAttempt] = useState(0) // incrementarlo fa rileggere lo stato

  useEffect(() => {
    if (isDemoSession()) return
    let current = true
    getPlayStatus()
      .then((s) => {
        if (!current) return
        setStatus(s)
        setFailed(false)
      })
      .catch(() => {
        if (!current) return
        setStatus(null)
        setFailed(true)
      })
    return () => {
      current = false
    }
  }, [attempt])

  const reload = useCallback(() => setAttempt((n) => n + 1), [])

  return { playStatus: status, playStatusFailed: failed, reloadPlayStatus: reload }
}

// ---------- Messaggio sotto GIOCA ----------
// Una frase breve (cosa manca e per quale fazione) e, se serve, un'azione. Niente parentesi né elenchi:
// il giocatore deve capire in un colpo d'occhio perché non può giocare e dove si sistema.

// Nome della fazione con l'articolo giusto: "della Resistenza Umana", "delle Potenze Artificiali".
const DI: Record<FactionId, string> = { umani: 'della', artificiali: 'delle' }
const ofFaction = (id: FactionId) => `${DI[id]} ${factions[id].name}`

export type PlayNotice = {
  tone: 'bloccato' | 'avviso' | 'info' // bloccato = non puoi giocare; avviso = non sappiamo; info = tutto a posto
  text: string
  action?: 'mazzi' | 'riprova' // cosa offrire accanto alla frase
}

/** Perché non si può giocare, in una frase. Vuoto se si può giocare (o se non lo sappiamo). */
export function playBlockReason(s: PlayStatus | null): string {
  if (!s || s.canPlay) return ''
  const missing = s.missingDefense
  const outdated = s.outdatedDefense
  if (missing.length === 2) return 'Ti manca il mazzo di difesa per entrambe le fazioni.'
  if (outdated.length === 2) return 'I tuoi mazzi di difesa sono obsoleti per entrambe le fazioni.'
  if (missing.length && outdated.length)
    return `Ti manca il mazzo di difesa ${ofFaction(missing[0])} e quello ${ofFaction(outdated[0])} è obsoleto.`
  if (missing.length) return `Ti manca il mazzo di difesa ${ofFaction(missing[0])}.`
  if (outdated.length) return `Il mazzo di difesa ${ofFaction(outdated[0])} è obsoleto.`
  return translate('deck.defense_required') // caso imprevisto: la frase generica del backend
}

/** Il messaggio sotto GIOCA, per ogni stato. */
export function playNotice(s: PlayStatus | null, failed: boolean): PlayNotice {
  if (failed) return { tone: 'avviso', text: 'Non riesco a controllare i tuoi mazzi di difesa.', action: 'riprova' }
  const reason = playBlockReason(s)
  if (reason) return { tone: 'bloccato', text: reason, action: 'mazzi' }
  return { tone: 'info', text: 'Scegli la modalità e il mazzo, poi inizia la partita.' }
}
