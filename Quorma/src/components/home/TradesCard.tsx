import InfoCard, { CardButton } from './InfoCard'

// Porta alla pagina Scambi. Niente numeri: il backend non ha ancora gli scambi (lib/trades.ts), meglio nessun dato che uno inventato.
export default function TradesCard({ onOpen }: { onOpen: () => void }) {
  return (
    <InfoCard seed="vincolo" title="Scambi" meta="" lead="Scambi tra giocatori">
      <p className="info-card-body">Scambia i doppioni con altri giocatori e completa la tua collezione.</p>
      <CardButton onClick={onOpen}>Vai agli scambi</CardButton>
    </InfoCard>
  )
}
