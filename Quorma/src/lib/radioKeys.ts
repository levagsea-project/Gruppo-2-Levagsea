import type { KeyboardEvent } from 'react'

const STEP: Record<string, number> = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 }

// Tastiera per i gruppi role="radiogroup": il gruppo è una sola tappa del Tab (roving tabindex, lo decide chi disegna
// le voci) e le frecce, Inizio e Fine spostano la scelta. Le voci non disponibili si saltano.
export function onRadioKeyDown(e: KeyboardEvent<HTMLElement>) {
  if (!(e.key in STEP) && e.key !== 'Home' && e.key !== 'End') return
  const radios = [...e.currentTarget.querySelectorAll<HTMLElement>('[role="radio"]')].filter(
    (r) => !r.hasAttribute('disabled') && r.getAttribute('aria-disabled') !== 'true',
  )
  if (radios.length === 0) return
  e.preventDefault()
  const here = radios.indexOf(document.activeElement as HTMLElement)
  const next =
    e.key === 'Home'
      ? 0
      : e.key === 'End'
        ? radios.length - 1
        : here < 0
          ? 0
          : (here + STEP[e.key] + radios.length) % radios.length
  radios[next].focus()
  radios[next].click()
}
