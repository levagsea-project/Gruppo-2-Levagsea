import type { MouseEvent, ReactNode } from 'react'
import type { Page } from '../../data/navItems'
import { useIsPhone } from '../../hooks/useIsPhone'
import PageTitle from '../layout/PageTitle'
import InfoMenu from './InfoMenu'
import '../../styles/landing/quorma-ds.css'
import '../../styles/info.css'

type Props = {
  page: Page // pagina aperta (serve per evidenziarla nel sotto-menu)
  title: string
  subtitle: string
  note?: string // testo a destra del titolo (es. data di aggiornamento)
  sideTop: ReactNode // lista specifica della pagina, sopra al sotto-menu
  sideLabel?: string // su telefono la lista si comprime in una voce con questo nome
  aside?: ReactNode // colonna a destra (solo FAQ)
  onNavigate: (page: Page) => void
  children?: ReactNode // contenuto del riquadro grande
}

// Scegliendo una voce l'indice si richiude prima di scorrere, così la posizione del testo è già quella definitiva.
const closeIndex = (e: MouseEvent<HTMLDetailsElement>) => {
  if ((e.target as HTMLElement).closest('button')) e.currentTarget.open = false
}

// Scheletro comune delle 7 pagine: titolo, colonna a sinistra, riquadro grande, colonna a destra.
// Su telefono il testo parte nella prima schermata: niente sotto-menu (le pagine sono in "Altro") e indice compresso.
export default function InfoPage({ page, title, subtitle, note, sideTop, sideLabel, aside, onNavigate, children }: Props) {
  const phone = useIsPhone()

  return (
    <main className="page info-page">
      <header className="info-header">
        <PageTitle title={title} subtitle={subtitle} />
        {note && <p className="info-note">{note}</p>}
      </header>

      <div className={aside ? 'info-body has-aside' : 'info-body'}>
        <div className="info-side">
          {phone && sideLabel ? (
            <details className="info-indice" onClickCapture={closeIndex}>
              <summary>
                {sideLabel}
                <svg aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="m6 9 6 6 6-6" />
                </svg>
              </summary>
              {sideTop}
            </details>
          ) : (
            sideTop
          )}
          {!phone && <InfoMenu current={page} onNavigate={onNavigate} />}
        </div>

        <article className="q-tela info-tela">
          <div className="q-tela__in">{children}</div>
        </article>

        {aside && <aside className="info-aside">{aside}</aside>}
      </div>
    </main>
  )
}
