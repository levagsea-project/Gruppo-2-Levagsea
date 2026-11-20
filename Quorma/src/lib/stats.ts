import type { Match, Overview } from '../data/profile'
import { handOrder } from '../data/profile'

// Dalle partite alle statistiche del profilo. Si ricalcola ogni volta: niente numeri salvati che si scollegano.
export function computeOverview(matches: Match[]): Overview {
  const count = (r: Match['result']) => matches.filter((m) => m.result === r).length

  // Quante volte hai fatto ogni mano, su tutti i turni di tutte le partite.
  const handCounts = new Map<string, number>()
  let turns = 0
  for (const m of matches) {
    for (const [mine] of m.turnHands) {
      handCounts.set(mine, (handCounts.get(mine) ?? 0) + 1)
      turns++
    }
  }
  const hands = [...handCounts.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([name, n]) => ({ name, percent: Math.round((n / turns) * 100) }))

  // Mano migliore: la più forte tra tutte quelle fatte.
  const strongest = [...handCounts.keys()].sort((a, b) => handOrder.indexOf(b) - handOrder.indexOf(a))[0]
  const bestHand = strongest ? { name: strongest, times: handCounts.get(strongest) ?? 0 } : null

  // Fazione più giocata e il mazzo che usi di più con quella fazione.
  let faction: Overview['faction'] = null
  if (matches.length) {
    const byFaction = new Map<string, number>()
    for (const m of matches) byFaction.set(m.myFaction, (byFaction.get(m.myFaction) ?? 0) + 1)
    const [id, n] = [...byFaction.entries()].sort((a, b) => b[1] - a[1])[0]
    const byDeck = new Map<string, number>()
    for (const m of matches.filter((m) => m.myFaction === id)) byDeck.set(m.deck, (byDeck.get(m.deck) ?? 0) + 1)
    faction = {
      id: id as Match['myFaction'],
      percent: Math.round((n / matches.length) * 100),
      deck: [...byDeck.entries()].sort((a, b) => b[1] - a[1])[0][0],
    }
  }

  return { total: matches.length, wins: count('vittoria'), losses: count('sconfitta'), draws: count('pareggio'), hands, bestHand, faction }
}
