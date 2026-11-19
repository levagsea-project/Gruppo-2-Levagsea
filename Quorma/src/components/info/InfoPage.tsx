import type { ReactNode } from 'react'
import type { Page } from '../../data/navItems'
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
  aside?: ReactNode // colonna a destra (solo FAQ e Contatti)
  onNavigate: (page: Page) => void
  children?: ReactNode // contenuto del riquadro grande (per ora vuoto)
}

// Scheletro comune delle 7 pagine: titolo, colonna a sinistra, riquadro grande, colonna a destra.
export default function InfoPage({ page, title, subtitle, note, sideTop, aside, onNavigate, children }: Props) {
  return (
    <main className="page info-page">
      <header className="info-header">
        <PageTitle title={title} subtitle={subtitle} />
        {note && <p className="info-note">{note}</p>}
      </header>

      <div className={aside ? 'info-body has-aside' : 'info-body'}>
        <div className="info-side">
          {sideTop}
          <InfoMenu current={page} onNavigate={onNavigate} />
        </div>

        <article className="q-tela info-tela">
          <div className="q-tela__in">{children}</div>
        </article>

        {aside && <aside className="info-aside">{aside}</aside>}
      </div>
    </main>
  )
}
