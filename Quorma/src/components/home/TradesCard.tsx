import InfoCard from './InfoCard'
import OutlineButton from '../ui/OutlineButton'

export default function TradesCard() {
  return (
    <InfoCard icon="⇄" title="SCAMBI" meta="3 IN ATTESA">
      <h3>3 proposte ricevute</h3>
      <p>Scambia i doppioni con altri giocatori e completa la tua collezione.</p>
      <OutlineButton>VAI AGLI SCAMBI</OutlineButton>
    </InfoCard>
  )
}
