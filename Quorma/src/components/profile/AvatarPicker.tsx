import { avatars } from '../../data/avatars'

type Props = { value: string; onChange: (id: string) => void }

// Le immagini tra cui scegliere: una piccola ogiva per ognuna, con il nome sotto.
export default function AvatarPicker({ value, onChange }: Props) {
  return (
    <div className="pe-picker" role="radiogroup" aria-label="Immagine del profilo">
      {avatars.map((a) => (
        <button key={a.id} type="button" role="radio" aria-checked={a.id === value} className="pe-pick" onClick={() => onChange(a.id)}>
          <span className="pe-thumb">
            <img src={a.image} alt="" />
          </span>
          <span className="pe-pick-name">{a.name}</span>
        </button>
      ))}
    </div>
  )
}
