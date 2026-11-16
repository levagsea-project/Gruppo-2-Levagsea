// Titolo grande di pagina con sottotitolo azzurro (es. MAZZI / I TUOI MAZZI).
export default function PageTitle({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div className="page-title">
      <span className="page-title-line" />
      <h1>{title}</h1>
      <p>{subtitle}</p>
    </div>
  )
}
