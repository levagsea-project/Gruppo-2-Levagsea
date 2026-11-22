import { useCallback, useEffect, useRef, useState } from 'react'
import { useBlocker } from 'react-router'

export type EditorStatus = { dirty: boolean; saving: boolean }
export const EDITOR_CLEAN: EditorStatus = { dirty: false, saving: false }

// Protegge le modifiche non salvate dell'editor su ogni uscita:
// - dentro l'app (menu, schede, Annulla): `leave` chiede conferma con LeaveDialog
// - freccia Indietro del browser: la blocca il router e chiede la stessa conferma
// - chiusura o ricarica della scheda: avviso del browser (beforeunload)
// Durante un salvataggio l'uscita aspetta l'esito: se riesce si va dove avevi chiesto, se fallisce si chiede conferma.
export function useLeaveGuard() {
  const [status, setStatus] = useState<EditorStatus>(EDITOR_CLEAN)
  const [asking, setAsking] = useState(false)
  const pending = useRef<(() => void) | null>(null)
  const active = status.dirty || status.saving

  const blocker = useBlocker(({ currentLocation, nextLocation }) => active && currentLocation.pathname !== nextLocation.pathname)

  const runPending = useCallback(() => {
    const action = pending.current
    pending.current = null
    setAsking(false)
    action?.()
  }, [])

  useEffect(() => {
    if (blocker.state !== 'blocked') return
    pending.current = () => blocker.proceed()
    setAsking(true)
  }, [blocker])

  // Un salvataggio fallito senza modifiche da perdere: non c'è nulla da confermare.
  useEffect(() => {
    if (asking && !status.dirty && !status.saving) runPending()
  }, [asking, status, runPending])

  useEffect(() => {
    if (!active) return
    const warn = (e: BeforeUnloadEvent) => {
      e.preventDefault()
      e.returnValue = '' // serve ai browser meno recenti; il testo dell'avviso lo decide il browser
    }
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [active])

  /** Esegue l'uscita subito se non c'è nulla da perdere, altrimenti chiede conferma. */
  const leave = (action: () => void) => {
    if (!active) return action()
    if (blocker.state === 'blocked') blocker.reset()
    pending.current = action
    setAsking(true)
  }

  const stay = () => {
    pending.current = null
    setAsking(false)
    if (blocker.state === 'blocked') blocker.reset()
  }

  /** Salvataggio riuscito: si va dove l'utente aveva chiesto nel frattempo, altrimenti a `fallback`. */
  const finishSave = (fallback: () => void) => {
    const action = pending.current ?? fallback
    pending.current = null
    setAsking(false)
    action()
  }

  return { reportEditor: setStatus, leave, stay, confirmLeave: runPending, finishSave, askingToLeave: asking && !status.saving }
}
