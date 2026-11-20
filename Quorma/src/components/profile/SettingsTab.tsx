import { useState } from 'react'
import type { Settings } from '../../data/profile'
import { defaultSettings, speeds } from '../../data/profile'
import { loadSettings, saveSettings } from '../../lib/settings'
import SettingsCard from './SettingsCard'
import SettingRow from './SettingRow'
import Toggle from './Toggle'
import Segmented from './Segmented'
import Slider from './Slider'

// Scheda "Impostazioni": quattro riquadri + RIPRISTINA / SALVA.
// SALVA tiene le impostazioni nel browser (il backend non le ha ancora). Per ora non cambiano audio o animazioni del gioco.
export default function SettingsTab() {
  const [s, setS] = useState<Settings>(loadSettings)
  const [message, setMessage] = useState('')

  // set('music', 50) cambia solo quel valore e tiene gli altri.
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
            {/* Email e password non si possono ancora leggere né cambiare dal backend */}
            <SettingRow label="Email" hint="Modifica in arrivo">
              <button type="button" className="pf-text-btn" disabled>Cambia</button>
            </SettingRow>
            <SettingRow label="Password" hint="Modifica in arrivo">
              <button type="button" className="pf-text-btn" disabled>Cambia</button>
            </SettingRow>
            <SettingRow label="Lingua" hint="Lingua dell'interfaccia e delle carte">
              <button type="button" className="pf-chip">Italiano</button>
            </SettingRow>
          </SettingsCard>

          <SettingsCard title="Partita">
            <SettingRow label="Velocità delle animazioni" hint="Quanto velocemente si svolgono i turni">
              <Segmented options={speeds} value={s.speed} label="Velocità delle animazioni" onChange={(v) => set('speed', v)} />
            </SettingRow>
            <SettingRow label="Mostra le mani di poker" hint="Evidenzia gli attaccanti scelti in ogni turno">
              <Toggle checked={s.showHands} label="Mostra le mani di poker" onChange={(v) => set('showHands', v)} />
            </SettingRow>
            <SettingRow label="Conferma prima di giocare" hint="Chiedi conferma prima di cercare un avversario">
              <Toggle checked={s.confirmPlay} label="Conferma prima di giocare" onChange={(v) => set('confirmPlay', v)} />
            </SettingRow>
          </SettingsCard>
        </div>

        <div className="pf-col">
          <SettingsCard title="Audio">
            <SettingRow label="Musica">
              <Slider value={s.music} label="Volume della musica" onChange={(v) => set('music', v)} />
            </SettingRow>
            <SettingRow label="Effetti">
              <Slider value={s.effects} label="Volume degli effetti" onChange={(v) => set('effects', v)} />
            </SettingRow>
            <SettingRow label="Silenzia tutto" hint="Disattiva musica ed effetti">
              <Toggle checked={s.muteAll} label="Silenzia tutto" onChange={(v) => set('muteAll', v)} />
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
