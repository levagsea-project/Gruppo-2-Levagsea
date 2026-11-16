import InfoCard from './InfoCard'
import OutlineButton from '../ui/OutlineButton'

export default function NewsCard() {
  return (
    <InfoCard icon="▤" title="NEWS" meta="24.10.2026">
      <h3>Novità del gioco</h3>
      <p>Qui troverai aggiornamenti e comunicazioni sul gioco.</p>
      <OutlineButton>TUTTE LE NEWS</OutlineButton>
    </InfoCard>
  )
}
