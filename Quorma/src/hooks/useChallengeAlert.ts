import { useCallback, useEffect, useState } from 'react'
import { getSession, isDemoSession } from '../lib/auth'
import { listChallenges } from '../lib/challenges'

// Quante sfide ricevute aspettano una risposta: accende le "braci" sulla voce Sfide.
// Se la lettura non riesce il segnale resta com'era: non è un errore da mostrare.
export function useChallengeAlert() {
  const [pending, setPending] = useState(0)

  const reload = useCallback(() => {
    if (isDemoSession()) return
    const me = getSession()?.username
    listChallenges()
      .then((list) => setPending(list.filter((c) => c.status === 'PENDING' && c.opponent.username === me).length))
      .catch(() => {})
  }, [])

  useEffect(reload, [reload])

  return { pendingChallenges: pending, reloadChallenges: reload }
}
