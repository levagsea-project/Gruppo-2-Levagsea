import { factions } from '../../data/factions'
import type { LoadState } from '../../hooks/useDecks'
import type { Deck, FactionId } from '../../types/deck'
import InfoCard, { CardButton } from './InfoCard'

type Props = { decks: Deck[]; state: LoadState; onManage: () => void }

// L'ordine delle fazioni nella card.
const ORDER: FactionId[] = ['umani', 'artificiali']

type Row = { faction: FactionId; tone: 'ok' | 'manca' | 'obsoleto'; text: string }

// Stato del mazzo di difesa di una fazione, dai mazzi del giocatore (GET /decks).
function rowFor(faction: FactionId, decks: Deck[]): Row {
  const defense = decks.find((d) => d.faction === faction && d.defense)
  if (!defense) return { faction, tone: 'manca', text: 'Da scegliere' }
  if (defense.outdated) return { faction, tone: 'obsoleto', text: `${defense.name} · obsoleto` }
  return { faction, tone: 'ok', text: defense.name }
}

// Icone disegnate: spunta (pronto), cerchio vuoto (manca), triangolo (obsoleto). Il testo dice lo stesso.
function StatusIcon({ tone }: { tone: Row['tone'] }) {
  return (
    <svg className="difesa__ico" aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      {tone === 'ok' && <path d="m5 12.5 4.5 4.5L19 7.5" />}
      {tone === 'manca' && <circle cx="12" cy="12" r="8" strokeDasharray="3 3" />}
      {tone === 'obsoleto' && (
        <>
          <path d="M12 3.5 21.5 20h-19z" />
          <path d="M12 10v4.5M12 17.2v.1" />
        </>
      )}
    </svg>
  )
}

// Card "Mazzi di difesa": per giocare serve un mazzo di difesa valido per ogni fazione.
// Mostra lo stato reale per fazione, al posto di una frase.
export default function DefenseCard({ decks, state, onManage }: Props) {
  const rows = ORDER.map((f) => rowFor(f, decks))
  const ready = rows.filter((r) => r.tone === 'ok').length
  const loaded = state === 'ready'

  return (
    <InfoCard
      seed="spira"
      title="Mazzi di difesa"
      meta={loaded ? `${ready} di 2 pronti` : state === 'loading' ? 'Caricamento…' : 'Non disponibile'}
      metaTone={loaded && ready < 2 ? 'ember' : undefined}
      lead="Ne serve uno valido per ogni fazione per giocare."
    >
      {loaded ? (
        <ul className="difesa">
          {rows.map((r) => (
            <li key={r.faction} className={`difesa__riga difesa__riga--${r.tone}`}>
              <StatusIcon tone={r.tone} />
              <span className="difesa__fazione">{factions[r.faction].name}</span>
              <span className="difesa__stato">{r.text}</span>
            </li>
          ))}
        </ul>
      ) : (
        <p className="info-card-body">
          {state === 'loading' ? 'Sto leggendo i tuoi mazzi.' : 'Non riesco a caricare i tuoi mazzi.'}
        </p>
      )}
      <CardButton onClick={onManage}>Gestisci mazzi</CardButton>
    </InfoCard>
  )
}
