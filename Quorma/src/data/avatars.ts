// Le immagini del profilo (cartella public/avatars): ritratti di personaggi del set base, tre per fazione.
// Per aggiungerne una: metti il file lì e una riga qui.
export type AvatarOption = { id: string; name: string; image: string }

export const avatars: AvatarOption[] = [
  { id: 'atona', name: 'Atona', image: '/avatars/atona.webp' },
  { id: 'sereno', name: 'Sereno', image: '/avatars/sereno.webp' },
  { id: 'ordito', name: 'Ordito', image: '/avatars/ordito.webp' },
  { id: 'brigida', name: 'Brigida', image: '/avatars/brigida.webp' },
  { id: 'elio', name: 'Elio', image: '/avatars/elio.webp' },
  { id: 'berenice', name: 'Berenice', image: '/avatars/berenice.webp' },
]

// Le immagini di prima: chi ne aveva scelta una nel browser riceve il ritratto che la sostituisce.
const REPLACED: Record<string, string> = {
  arbitro: 'atona',
  cattedrale: 'sereno',
  colosso: 'ordito',
  torcia: 'brigida',
  viandante: 'elio',
  rovine: 'berenice',
}

/** L'immagine con questo id, oppure la prima se non esiste. */
export const findAvatar = (id?: string) => {
  const current = id ? (REPLACED[id] ?? id) : id
  return avatars.find((a) => a.id === current) ?? avatars[0]
}
