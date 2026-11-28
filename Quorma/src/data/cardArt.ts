// Grafica delle carte: cornici e illustrazioni. Il backend non ha immagini: tutto sta in public/images/carte/.
import type { CSSProperties } from 'react'
import type { FactionId } from '../types/deck'
import { factions } from './factions'

//
// CORNICI (public/images/carte/cornici/): una per Tier di rarità, solo per le creature.
//   creatura-tier<N>.webp (800×1200) e creatura-tier<N>-400.webp (400×600, per le carte piccole).
//   I sorgenti PNG sono in tools/cornici/: per rifare i WebP si parte da lì.
//
// ILLUSTRAZIONI (public/images/carte/arte/): un file per carta, chiamato col codice della carta.
//   es. BASE-AI-A-FRA-SEK.webp (768px) e BASE-AI-A-FRA-SEK-320.webp (320px, per le carte piccole), ritagliate per la
//   finestra della cornice (753:770). Quando aggiungi i file, aggiungi il codice a CARD_ART:
//   così il sito non chiede immagini che non esistono e le carte senza illustrazione mostrano rango e seme.

// Fine del codice delle creature IA (seme-sottofazione), per rango.
const AI_SUBFACTIONS: Record<string, string[]> = {
  A: ['FRA-SEK', 'SPI-YLM', 'VES-MER', 'VIN-EFF'],
  '2': ['FRA-YLM', 'SPI-SEK', 'VES-EFF', 'VIN-MER'],
  '3': ['FRA-MER', 'SPI-EFF', 'VES-SEK', 'VIN-YLM'],
  '4': ['FRA-EFF', 'SPI-MER', 'VES-YLM', 'VIN-SEK'],
  '5': ['FRA-EFF', 'SPI-YLM', 'VES-SEK', 'VIN-MER'],
  '6': ['FRA-SEK', 'SPI-EFF', 'VES-MER', 'VIN-YLM'],
  '7': ['FRA-YLM', 'SPI-MER', 'VES-SEK', 'VIN-EFF'],
  '8': ['FRA-MER', 'SPI-YLM', 'VES-EFF', 'VIN-SEK'],
  '9': ['FRA-EFF', 'SPI-SEK', 'VES-MER', 'VIN-YLM'],
  '10': ['FRA-SEK', 'SPI-EFF', 'VES-YLM', 'VIN-MER'],
  J: ['FRA-YLM', 'SPI-MER', 'VES-EFF', 'VIN-SEK'],
  Q: ['FRA-MER', 'SPI-SEK', 'VES-YLM', 'VIN-EFF'],
  K: ['FRA-SEK', 'SPI-EFF', 'VES-MER', 'VIN-YLM'],
}

/** Codici delle carte che hanno un'illustrazione in public/images/carte/arte/. */
export const CARD_ART = new Set<string>([
  // Potenze Artificiali: tutte le 52 creature
  ...['A', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K'].flatMap((rank) => AI_SUBFACTIONS[rank].map((end) => `BASE-AI-${rank}-${end}`)),
  // Resistenza Umana: Frattura, Vestigio e Vincolo complete; di Spira per ora c'è solo l'Asso
  ...['A-SPE', '2-ADA', '3-POR', '4-SPE', '5-SEG', '6-POR', '7-ADA', '8-SPE', '9-ADA', '10-POR', 'J-SEG', 'Q-SPE', 'K-SEG'].map((c) => {
    const [rank, sub] = c.split('-')
    return `BASE-RES-${rank}-FRA-${sub}`
  }),
  ...['A-POR', '2-SPE', '3-SEG', '4-ADA', '5-SPE', '6-SEG', '7-POR', '8-ADA', '9-POR', '10-ADA', 'J-SPE', 'Q-SEG', 'K-ADA'].map((c) => {
    const [rank, sub] = c.split('-')
    return `BASE-RES-${rank}-VES-${sub}`
  }),
  ...['A-SEG', '2-POR', '3-ADA', '4-SEG', '5-ADA', '6-SPE', '7-SEG', '8-POR', '9-SPE', '10-SEG', 'J-POR', 'Q-ADA', 'K-SPE'].map((c) => {
    const [rank, sub] = c.split('-')
    return `BASE-RES-${rank}-VIN-${sub}`
  }),
  'BASE-RES-A-SPI-ADA',
])

/** Illustrazione della carta nelle due misure, o null se non c'è ancora. */
export const cardArt = (code: string): { src: string; srcSet: string } | null => {
  if (!CARD_ART.has(code)) return null
  const base = `/images/carte/arte/${code}`
  return { src: `${base}.webp`, srcSet: `${base}-320.webp 320w, ${base}.webp 768w` }
}

// COPERTINE DELLE BUSTE (public/images/carte/buste/): busta-<tipo>-<ia|umani>.webp, 640×896 (5:7 come la busta).
//   Stile da copertina TCG cupa, con uno STYLE proprio: i workflow sono in tools/comfy/runs/busta-*-dark.json.
const PACK_COVERS = new Set(['standard', 'advanced', 'elite'])
const PACK_COVERS_VERSION = 5 // da alzare quando si rigenerano le copertine, così il browser non tiene le vecchie in cache

/** Sfondo della busta per tipo e fazione. Un tipo nuovo, senza copertina, usa l'illustrazione della fazione. */
export const packCover = (code: string, faction: FactionId): CSSProperties =>
  PACK_COVERS.has(code)
    ? { backgroundImage: `url(/images/carte/buste/busta-${code}-${faction === 'umani' ? 'umani' : 'ia'}.webp?v=${PACK_COVERS_VERSION})`, backgroundPosition: 'center' }
    : { backgroundImage: `url(${factions[faction].cover})`, backgroundPosition: factions[faction].coverFocus }

export type Tier = 1 | 2 | 3 | 4
export const TIER_NAMES: Record<Tier, string> = { 1: 'Comune', 2: 'Non comune', 3: 'Rara', 4: 'Epica' }

/** Cornice della creatura per Tier: le due misure, per srcSet. */
export const creatureFrame = (tier: Tier) => {
  const base = `/images/carte/cornici/creatura-tier${tier}`
  return { src: `${base}.webp`, srcSet: `${base}-400.webp 400w, ${base}.webp 800w` }
}

/** Retro con la lore (public/images/carte/retro/), uno per Tier: le due misure, per srcSet. */
export const cardBackFrame = (tier: Tier) => {
  const base = `/images/carte/retro/retro-tier${tier}`
  return { src: `${base}.webp`, srcSet: `${base}-400.webp 400w, ${base}.webp 800w` }
}
