// Grafica delle carte: cornici e illustrazioni. Il backend non ha immagini: tutto sta in public/images/carte/.
//
// CORNICI (public/images/carte/cornici/): una per Tier di rarità, solo per le creature.
//   creatura-tier<N>.webp (800×1200) e creatura-tier<N>-400.webp (400×600, per le carte piccole).
//   I sorgenti PNG sono in tools/cornici/: per rifare i WebP si parte da lì.
//
// ILLUSTRAZIONI (public/images/carte/arte/): un file per carta, chiamato col codice della carta.
//   es. public/images/carte/arte/BASE-RES-2-FRA-ADA.webp, ritagliata per la finestra della cornice
//   (proporzioni circa 1:1, es. 768×784). Quando aggiungi un file, aggiungi il codice a CARD_ART:
//   così il sito non chiede immagini che non esistono e le carte senza illustrazione mostrano rango e seme.

/** Codici delle carte che hanno un'illustrazione in public/images/carte/arte/. */
export const CARD_ART = new Set<string>([
  // 'BASE-RES-2-FRA-ADA',
])

/** Illustrazione della carta, o null se non c'è ancora. */
export const cardArt = (code: string): string | null => (CARD_ART.has(code) ? `/images/carte/arte/${code}.webp` : null)

export type Tier = 1 | 2 | 3 | 4
export const TIER_NAMES: Record<Tier, string> = { 1: 'Comune', 2: 'Non comune', 3: 'Rara', 4: 'Epica' }

/** Cornice della creatura per Tier: le due misure, per srcSet. */
export const creatureFrame = (tier: Tier) => {
  const base = `/images/carte/cornici/creatura-tier${tier}`
  return { src: `${base}.webp`, srcSet: `${base}-400.webp 400w, ${base}.webp 800w` }
}
