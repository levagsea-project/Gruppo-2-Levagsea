import { useCallback, useEffect, useMemo, useState } from 'react'
import type { Game, Series } from '../data/profile'
import { ApiError, GENERIC } from '../lib/api'
import { isDemoSession } from '../lib/auth'
import { loadGameDetail, loadHistory, toSeriesList } from '../lib/matches'
import type { SeriesResponse } from '../lib/series'
import { computeOverview } from '../lib/stats'

export type ProfileState = 'loading' | 'ready' | 'error'

// Storico e statistiche del giocatore: lo storico arriva dal backend (GET /matches) a ogni apertura del profilo,
// le statistiche si ricalcolano da lì. loadDetail chiede al replay il dettaglio di una partita che il browser
// non conosce ancora: arrivato, si aggiornano anche le statistiche sulle mani.
export function useProfile() {
  const [list, setList] = useState<SeriesResponse[]>([])
  const [state, setState] = useState<ProfileState>('loading')
  const [error, setError] = useState('')
  const [complete, setComplete] = useState(true)
  const [attempt, setAttempt] = useState(0) // incrementarlo rilegge lo storico
  const [version, setVersion] = useState(0) // incrementato quando il browser impara il dettaglio di una partita

  useEffect(() => {
    if (isDemoSession()) {
      setState('ready') // modalità demo: niente backend, storico vuoto
      return
    }
    let current = true
    setState('loading')
    loadHistory()
      .then((h) => {
        if (!current) return
        setList(h.list)
        setComplete(h.complete)
        setState('ready')
      })
      .catch((err) => {
        if (!current) return
        setError(err instanceof ApiError ? err.message : GENERIC)
        setState('error')
      })
    return () => {
      current = false
    }
  }, [attempt])

  // version tra le dipendenze: dopo un loadDetail le serie si rileggono con il nuovo dettaglio
  const series = useMemo(() => toSeriesList(list), [list, version])
  const overview = useMemo(() => computeOverview(series), [series])

  const reload = useCallback(() => setAttempt((n) => n + 1), [])
  const loadDetail = useCallback(async (s: Series, g: Game) => {
    await loadGameDetail(s, g)
    setVersion((v) => v + 1)
  }, [])

  return { state, error, complete, series, overview, reload, loadDetail }
}
