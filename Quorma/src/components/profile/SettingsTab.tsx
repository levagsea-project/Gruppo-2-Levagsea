import { useEffect, useState } from 'react'
import type { Settings } from '../../data/profile'
import { defaultSettings, speeds } from '../../data/profile'
import { applySettings, loadSettings, saveSettings } from '../../lib/settings'
import SettingsCard from './SettingsCard'
import SettingRow from './SettingRow'
import Toggle from './Toggle'
import Segmented from './Segmented'
import Slider from './Slider'

// Scheda "Impostazioni": quattro riquadri + RIPRISTINA / SALVA.
// SALVA tiene le impostazioni nel browser (il backend non le ha ancora).
// Funzionano davvero velocità, movimento e contrasto: si vedono subito e, senza Salva, uscendo tornano come prima.
// Le opzioni senza una funzione dietro (audio, mani, conferma, altre lingue) restano visibili ma non premibili.
export default function SettingsTab() {
  const [s, setS] = useState<Settings>(loadSettings)
  const [message, setMessage] = useState('')

  useEffect(() => applySettings(s), [s])
  useEffect(() => () => applySettings(loadSettings()), [])

  // set('speed', 'Veloce') cambia solo quel valore e tiene gli altri.
  const set = <K extends keyof Settings>(key: K, value: Settings[K]) => {
    setS((old) => ({ ...old, [key]: value }))
    setMessage('')
  }

  const save = () => {
    setMessage(saveSettings(s) ? 'Impostazioni salvate.' : 'Non è stato possibile salvare.')
    setTimeout(() => setMessage(''), 2500)
  }

  return (
    <div className="pf-settings">
      <div className="pf-cards">
        <div className="pf-col">
          <SettingsCard title="Account">
            {/* Email e password non si possono ancora leggere né cambiare dal backend: righe visibili, pulsanti disattivati */}
            <SettingRow label="Email">
              <button type="button" className="pf-text-btn" disabled>Cambia</button>
            </SettingRow>
            <SettingRow label="Password">
              <button type="button" className="pf-text-btn" disabled>Cambia</button>
            </SettingRow>
            <SettingRow label="Lingua" hint="L'italiano è l'unica lingua disponibile">
              <button type="button" className="pf-chip" disabled>Italiano</button>
            </SettingRow>
          </SettingsCard>

          <SettingsCard title="Partita">
            <SettingRow label="Velocità delle animazioni" hint="Quanto velocemente si svolgono i turni">
              <Segmented options={speeds} value={s.speed} label="Velocità delle animazioni" onChange={(v) => set('speed', v)} />
            </SettingRow>
            <SettingRow label="Mostra le mani di poker" hint="Opzione non disponibile">
              <Toggle checked={false} label="Mostra le mani di poker" disabled />
            </SettingRow>
            <SettingRow label="Conferma prima di giocare" hint="Opzione non disponibile">
              <Toggle checked={false} label="Conferma prima di giocare" disabled />
            </SettingRow>
          </SettingsCard>
        </div>

        <div className="pf-col">
          <SettingsCard title="Audio">
            <SettingRow label="Musica" hint="Il gioco non ha audio">
              <Slider value={0} label="Volume della musica" disabled />
            </SettingRow>
            <SettingRow label="Effetti" hint="Il gioco non ha audio">
              <Slider value={0} label="Volume degli effetti" disabled />
            </SettingRow>
            <SettingRow label="Silenzia tutto" hint="Il gioco non ha audio">
              <Toggle checked={false} label="Silenzia tutto" disabled />
            </SettingRow>
          </SettingsCard>

          <SettingsCard title="Accessibilità">
            <SettingRow label="Riduci il movimento" hint="Ferma bagliori, scintille e giri delle carte">
              <Toggle checked={s.reduceMotion} label="Riduci il movimento" onChange={(v) => set('reduceMotion', v)} />
            </SettingRow>
            <SettingRow label="Contrasto elevato" hint="Testi e bordi più marcati">
              <Toggle checked={s.highContrast} label="Contrasto elevato" onChange={(v) => set('highContrast', v)} />
            </SettingRow>
          </SettingsCard>
        </div>
      </div>

      <div className="pf-actions">
        <span role="status" className="pf-saved">{message}</span>
        <button type="button" className="q-btn q-btn--acciaio" onClick={() => { setS(defaultSettings); setMessage('') }}>
          <span className="q-btn__in">Ripristina</span>
        </button>
        <button type="button" className="q-btn q-btn--argento" onClick={save}>
          <span className="q-btn__in">Salva</span>
        </button>
      </div>
    </div>
  )
}
