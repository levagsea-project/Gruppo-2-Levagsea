import type { ReactNode } from 'react'

type Props = { name: string; image: string; children?: ReactNode }

// La scheda del giocatore ad arco: immagine, nome e livello. La usano il Profilo e l'anteprima di "Modifica profilo".
// Livello ed esperienza non esistono ancora nel backend: al posto c'è "Livelli in arrivo".
export default function ProfileCard({ name, image, children }: Props) {
  return (
    <section className="q-tela q-tela--arco pf-player" aria-label="Il tuo profilo">
      <div className="q-tela__in">
        <span className="pf-avatar" aria-hidden="true">
          <img src={image} alt="" />
        </span>
        <h2 className="pf-player-name">{name}</h2>
        <p className="pf-level pf-level--vuoto">Livelli in arrivo</p>
        {children}
      </div>
    </section>
  )
}
