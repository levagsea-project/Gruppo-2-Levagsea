import TradesCard from './TradesCard'
import NewsCard from './NewsCard'

// Le due card affiancate in fondo alla pagina.
export default function InfoCards() {
  return (
    <div className="info-cards">
      <TradesCard />
      <NewsCard />
    </div>
  )
}
