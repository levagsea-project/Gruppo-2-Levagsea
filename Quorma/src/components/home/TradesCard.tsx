import InfoCard, { CardButton } from './InfoCard'

// Gli scambi non esistono ancora nel backend: la card resta visibile, con il pulsante non premibile e senza numeri inventati.
export default function TradesCard() {
  return (
    <InfoCard seed="vincolo" title="Scambi" meta="" lead="Scambi tra giocatori">
      <p className="info-card-body">Scambia i doppioni con altri giocatori e completa la tua collezione.</p>
      <CardButton>Vai agli scambi</CardButton>
    </InfoCard>
  )
}
