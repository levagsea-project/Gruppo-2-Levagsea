import { useState } from 'react'
import { findAvatar } from '../../data/avatars'
import type { Page } from '../../data/navItems'
import { currentPlayer } from '../../data/player'
import { NAME_HINT, NAME_MAX, saveProfile, validateName } from '../../lib/profileStore'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import AvatarPicker from './AvatarPicker'
import ProfileCard from './ProfileCard'
import '../../styles/landing/quorma-ds.css'
import '../../styles/profile.css'

// Pagina "Modifica profilo": scegli nome e immagine, con l'anteprima a destra.
// ANNULLA torna al profilo senza salvare, SALVA controlla il nome e poi torna al profilo.
export default function EditProfilePage({ onNavigate }: { onNavigate: (page: Page) => void }) {
  const player = currentPlayer()
  const [name, setName] = useState(player.name)
  const [avatarId, setAvatarId] = useState(player.avatar.id)
  const [error, setError] = useState('')

  const save = () => {
    const problem = validateName(name)
    if (problem) return setError(problem)
    if (!saveProfile({ name: name.trim(), avatar: avatarId })) return setError('Non è stato possibile salvare.')
    onNavigate('profilo')
  }

  return (
    <main className="page profile-page">
      <CurrencyBar />
      <PageTitle title="MODIFICA PROFILO" subtitle="NOME E IMMAGINE" />

      <div className="pf-body pe-body">
        <div className="pe-form">
          <label className="pf-label" htmlFor="pe-name">Nome del giocatore</label>
          <input
            id="pe-name"
            className="pe-input"
            value={name}
            maxLength={NAME_MAX}
            aria-invalid={!!error}
            aria-describedby="pe-hint"
            onChange={(e) => {
              setName(e.target.value)
              setError('')
            }}
          />
          <p id="pe-hint" className="pe-hint">{NAME_HINT}</p>
          <p role="alert" className="pe-error">{error}</p>

          <span className="pf-label">Immagine del profilo</span>
          <AvatarPicker value={avatarId} onChange={setAvatarId} />
          <p className="pe-hint">Nome e immagine si vedono solo su questo browser.</p>
        </div>

        <div className="pe-preview">
          <span className="pf-label">Anteprima</span>
          <ProfileCard name={name.trim().toUpperCase() || '—'} image={findAvatar(avatarId).image} />
        </div>
      </div>

      <div className="pe-actions">
        <button type="button" className="q-btn q-btn--acciaio" onClick={() => onNavigate('profilo')}>
          <span className="q-btn__in">Annulla</span>
        </button>
        <button type="button" className="q-btn q-btn--argento" onClick={save}>
          <span className="q-btn__in">Salva</span>
        </button>
      </div>
    </main>
  )
}
