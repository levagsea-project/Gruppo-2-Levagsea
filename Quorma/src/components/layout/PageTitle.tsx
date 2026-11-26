// Titolo grande di pagina con sottotitolo azzurro (es. MAZZI / I TUOI MAZZI).
// Sopra il titolo il fregio del giglio del design system Cattedrale (styles/cattedrale.css).
export default function PageTitle({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div className="page-title">
      <span className="page-title-fregio" aria-hidden="true"><span className="q-orn q-orn--giglio" /></span>
      <h1>{title}</h1>
      <p>{subtitle}</p>
    </div>
  )
}
