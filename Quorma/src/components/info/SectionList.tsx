import { useState } from 'react'
import SideList from './SideList'
import type { SideItem } from './SideList'

// SideList che si ricorda da sola quale voce hai scelto (serve a "In questa pagina", "Argomenti"...).
export default function SectionList({ title, items }: { title: string; items: SideItem[] }) {
  const [active, setActive] = useState(0)
  return <SideList title={title} items={items} active={active} onSelect={setActive} />
}
