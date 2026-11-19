import { useState } from 'react'
import { sectionAnchor, type Section } from '../../data/info'
import SideList from './SideList'

// Indice "In questa pagina": scegliendo una voce il testo scorre fino a quella sezione.
// Con "riduci movimento" attivo lo scorrimento è immediato invece che animato.
export default function SectionList({ title, sections }: { title: string; sections: Section[] }) {
  const [active, setActive] = useState(0)

  const go = (i: number) => {
    setActive(i)
    const target = document.getElementById(sectionAnchor(sections[i].id))
    const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    target?.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'start' })
  }

  return <SideList title={title} items={sections.map((s) => ({ label: s.title }))} active={active} onSelect={go} />
}
