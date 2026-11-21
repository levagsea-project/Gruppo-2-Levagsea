import { useCallback, useEffect, useRef, useState } from 'react'
import { getSession, isDemoSession } from '../lib/auth'
import { listChallenges } from '../lib/challenges'

const POLL_MS = 10_000 // il backend non manda notifiche: le sfide si rileggono a intervalli

// Quante sfide ricevute aspettano una risposta: accende le "braci" sulla voce Sfide.
// Si rilegge da solo finché la scheda è visibile, e subito quando si torna sulla scheda.
// Se la lettura non riesce il segnale resta com'era: non è un errore da mostrare.
export function useChallengeAlert() {
  const [pending, setPending] = useState(0)
  const seq = useRef(0) // scarta le risposte arrivate dopo una lettura più recente

  const reload = useCallback(() => {
    if (isDemoSession()) return
    const me = getSession()?.username
    if (!me) return
    const current = ++seq.current
    listChallenges()
      .then((list) => {
        if (current !== seq.current) return
        setPending(list.filter((c) => c.status === 'PENDING' && c.opponent.username === me).length)
      })
      .catch(() => {})
  }, [])

  useEffect(() => {
    reload()
    const timer = window.setInterval(() => {
      if (!document.hidden) reload()
    }, POLL_MS)
    const onVisible = () => {
      if (!document.hidden) reload()
    }
    document.addEventListener('visibilitychange', onVisible)
    return () => {
      window.clearInterval(timer)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [reload])

  return { pendingChallenges: pending, reloadChallenges: reload }
}
