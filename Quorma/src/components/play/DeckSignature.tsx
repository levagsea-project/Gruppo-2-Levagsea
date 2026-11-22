import { useDeckStats } from '../../hooks/useDeckStats'

// La firma di un mazzo sulla sua carta in Gioca: le creature per seme e quante creature, magie ed equipaggiamenti contiene.
// Finché i conteggi non arrivano lo spazio resta riservato; se la lettura fallisce la carta mostra solo nome ed etichetta.
export default function DeckSignature({ deckId }: { deckId: string }) {
  const { stats, failed } = useDeckStats(deckId)
  if (failed) return null
  if (!stats) return <span className="play-firma play-firma--attesa" aria-hidden="true" />

  const total = `${stats.creatures} creature, ${stats.magic} magie, ${stats.equipment} equipaggiamenti`
  return (
    <span className="play-firma">
      <span className="sr-only">
        {stats.suits.map((s) => `${s.name} ${s.value}`).join(', ')}. {total}.
      </span>
      <span className="play-firma__semi" aria-hidden="true">
        {stats.suits.map((s) => (
          <span key={s.name} className="play-firma__seme" title={`${s.name}: ${s.value} creature`}>
            <span className={`deck-seme deck-seme--${s.name.toLowerCase()}`} />
            {s.value}
          </span>
        ))}
      </span>
      <span className="play-firma__tipi" aria-hidden="true" title={total}>
        {stats.creatures} Cr · {stats.magic} Ma · {stats.equipment} Eq
      </span>
    </span>
  )
}
