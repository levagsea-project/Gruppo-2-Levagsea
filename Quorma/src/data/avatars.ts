// Le immagini del profilo (cartella public/avatars). Per aggiungerne una: metti il file lì e una riga qui.
export type AvatarOption = { id: string; name: string; image: string }

export const avatars: AvatarOption[] = [
  { id: 'arbitro', name: 'Arbitro', image: '/avatars/arbitro.jpg' },
  { id: 'viandante', name: 'Il viandante', image: '/avatars/viandante.jpg' },
  { id: 'colosso', name: 'Il colosso', image: '/avatars/colosso.jpg' },
  { id: 'cattedrale', name: 'La cattedrale', image: '/avatars/cattedrale.jpg' },
  { id: 'rovine', name: 'Le rovine', image: '/avatars/rovine.jpg' },
  { id: 'torcia', name: 'La torcia', image: '/avatars/torcia.jpg' },
]

/** L'immagine con questo id, oppure la prima se non esiste. */
export const findAvatar = (id?: string) => avatars.find((a) => a.id === id) ?? avatars[0]
