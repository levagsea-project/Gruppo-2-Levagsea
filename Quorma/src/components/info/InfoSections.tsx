import type { Page } from '../../data/navItems'
import { sectionAnchor, type Block, type Section } from '../../data/info'

// Collegamento testuale a un'altra pagina del sito (es. "Vai ai contatti").
export function InfoLink({ label, page, onNavigate }: { label: string; page: Page; onNavigate: (page: Page) => void }) {
  return (
    <button type="button" className="info-link" onClick={() => onNavigate(page)}>
      {label}
    </button>
  )
}

function BlockView({ block, onNavigate }: { block: Block; onNavigate: (page: Page) => void }) {
  if ('ul' in block) {
    return (
      <ul className="info-ul">
        {block.ul.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    )
  }
  return (
    <p>
      {block.p}
      {block.link && (
        <>
          {' '}
          <InfoLink label={block.link.label} page={block.link.page} onNavigate={onNavigate} />
        </>
      )}
    </p>
  )
}

type Props = {
  sections: Section[]
  onNavigate: (page: Page) => void
  extra?: Partial<Record<string, React.ReactNode>> // contenuto in più in fondo a una sezione, per id (es. la squadra)
}

// Testo di una pagina informativa: una sezione per titolo, separate da un filo, come nel design.
export default function InfoSections({ sections, onNavigate, extra }: Props) {
  return (
    <div className="info-testo">
      {sections.map((s) => (
        <section key={s.id} id={sectionAnchor(s.id)} className="info-sez" aria-labelledby={`${sectionAnchor(s.id)}-t`}>
          <h2 id={`${sectionAnchor(s.id)}-t`}>{s.title}</h2>
          {s.blocks.map((b, i) => (
            <BlockView key={i} block={b} onNavigate={onNavigate} />
          ))}
          {extra?.[s.id]}
        </section>
      ))}
    </div>
  )
}
