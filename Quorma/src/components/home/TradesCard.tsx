import InfoCard, { CardButton } from './InfoCard'

// Gli scambi non esistono ancora nel backend: la card spiega cosa saranno, senza numeri inventati.
// Quando arriveranno, meta e frase in evidenza mostreranno le proposte vere (metaTone="ember" se ce ne sono).
export default function TradesCard() {
  return (
    <InfoCard seed="vincolo" title="Scambi" meta="In arrivo" lead="Scambi tra giocatori">
      <p className="info-card-body">Potrai scambiare i doppioni con altri giocatori e completare la tua collezione.</p>
      <CardButton>Vai agli scambi</CardButton>
    </InfoCard>
  )
}
