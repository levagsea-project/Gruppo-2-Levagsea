import { useSyncExternalStore } from 'react'

const QUERY = '(max-width: 767px)' // lo stesso limite di styles/mobile.css

const subscribe = (notify: () => void) => {
  const media = window.matchMedia(QUERY)
  media.addEventListener('change', notify)
  return () => media.removeEventListener('change', notify)
}

/** true sotto i 768px, per le poche parti che su telefono hanno una struttura diversa e non solo un altro stile. */
export const useIsPhone = () => useSyncExternalStore(subscribe, () => window.matchMedia(QUERY).matches, () => false)
