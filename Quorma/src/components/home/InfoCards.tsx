import { news } from '../../data/news'
import type { LoadState } from '../../hooks/useDecks'
import type { Deck } from '../../types/deck'
import DefenseCard from './DefenseCard'
import NewsCard from './NewsCard'
import TradesCard from './TradesCard'

type Props = { decks: Deck[]; decksState: LoadState; onManageDecks: () => void }

// Le tre card in fondo: lo stato reale dei mazzi di difesa, gli scambi (in arrivo) e le news.
export default function InfoCards({ decks, decksState, onManageDecks }: Props) {
  return (
    <div className="info-cards">
      <DefenseCard decks={decks} state={decksState} onManage={onManageDecks} />
      <TradesCard />
      <NewsCard items={news} />
    </div>
  )
}
