import { useCallback, useEffect, useState } from 'react'
import type { Series } from '../data/profile'
import { isDemoSession } from '../lib/auth'
import { toSeriesList } from '../lib/matches'
import { listSeries, markSeriesSeen } from '../lib/series'

// Novità sulle partite, per tutta l'app (letto in App.tsx):
//   - unread: le serie con un risultato che non ho ancora letto. Sono quelle giocate mentre non c'ero
//     (una sfida accettata, o il matchmaking di un altro che ha scelto me): il backend le consegna così
//     al login, senza notifiche push (GET /matches?unread=true);
//   - latest: la mia serie più recente, per la home su telefono.
// Si rilegge all'ingresso nell'app e tornando alla home. Se il backend non risponde non mostriamo nulla:
// sono novità, non devono bloccare niente.
export function useMatchNews() {
  const [unread, setUnread] = useState<Series[]>([])
  const [latest, setLatest] = useState<Series | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    if (isDemoSession()) return
    let current = true
    Promise.all([listSeries(0, 50, true), listSeries(0, 1)])
      .then(([u, l]) => {
        if (!current) return
        setUnread(toSeriesList(u))
        setLatest(toSeriesList(l)[0] ?? null)
      })
      .catch(() => {
        /* nessuna novità da mostrare */
      })
    return () => {
      current = false
    }
  }, [attempt])

  const reload = useCallback(() => setAttempt((n) => n + 1), [])

  /** Segna come lette tutte le serie mostrate. Le toglie subito dall'elenco; il backend si aggiorna dietro. */
  const markAllSeen = useCallback(() => {
    const ids = unread.map((s) => s.id)
    setUnread([])
    void Promise.allSettled(ids.map((id) => markSeriesSeen(id)))
  }, [unread])

  return { unread, latest, reload, markAllSeen }
}
