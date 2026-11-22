import type { NewsItem } from '../../data/news'
import InfoCard, { CardButton } from './InfoCard'

const MAX_ITEMS = 3 // nella Home bastano le ultime tre: le altre andranno nella pagina News

// Data all'italiana: "19 novembre 2026". Se la data non è valida si mostra com'è.
function formatDate(iso: string) {
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? iso : new Intl.DateTimeFormat('it-IT', { day: 'numeric', month: 'long', year: 'numeric' }).format(d)
}

type Props = {
  items: NewsItem[] // dalla più recente; vuota finché il backend non fornisce le news
  onOpenAll?: () => void // pagina "Tutte le news": senza, il pulsante non c'è
}

// Card News della Home: le ultime news con data e titolo, oppure uno stato vuoto onesto (niente date inventate).
export default function NewsCard({ items, onOpenAll }: Props) {
  const latest = [...items].sort((a, b) => b.date.localeCompare(a.date)).slice(0, MAX_ITEMS)
  const empty = latest.length === 0

  return (
    <InfoCard
      seed="vestigio"
      title="News"
      meta={empty ? 'Nessuna' : formatDate(latest[0].date)}
      lead={empty ? 'Ancora nessuna news' : 'Novità del gioco'}
    >
      {empty ? (
        <p className="info-card-body">Qui troverai aggiornamenti, nuove carte e comunicazioni sul gioco.</p>
      ) : (
        <ul className="news">
          {latest.map((n) => (
            <li key={n.id} className="news__voce">
              <time className="news__data" dateTime={n.date}>{formatDate(n.date)}</time>
              <span className="news__titolo">{n.title}</span>
              {n.summary && <span className="news__riassunto">{n.summary}</span>}
            </li>
          ))}
        </ul>
      )}
      {onOpenAll && <CardButton onClick={onOpenAll}>Tutte le news</CardButton>}
    </InfoCard>
  )
}
