import type { Overview, Series } from '../data/profile'
import { handOrder } from '../data/profile'
import type { FactionId } from '../types/deck'

// Dallo storico alle statistiche del profilo. Si ricalcola ogni volta: niente numeri salvati che si scollegano.
// Quando il backend avrà le statistiche del giocatore (previste, non ancora fatte), si cambia solo questo file.
export function computeOverview(series: Series[]): Overview {
  const count = (r: Series['result']) => series.filter((s) => s.result === r).length
  const games = series.flatMap((s) => s.games)

  // Mani: solo dalle partite di cui conosciamo il log (giocate da qui o già aperte nel dettaglio).
  const detailed = games.filter((g) => g.detail)
  const handCounts = new Map<string, number>()
  let turns = 0
  for (const g of detailed) {
    for (const [mine] of g.detail!.turnHands) {
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

  // Fazione con cui vinco di più. In una serie le fazioni si scambiano, quindi "la più giocata" sarebbe sempre
  // circa metà e metà: conta la percentuale di partite vinte con ciascuna (a parità, quella con più partite).
  const byFaction = new Map<FactionId, { played: number; won: number }>()
  for (const g of games) {
    const f = byFaction.get(g.myFaction) ?? { played: 0, won: 0 }
    f.played++
    if (g.result === 'vittoria') f.won++
    byFaction.set(g.myFaction, f)
  }
  const ranked = [...byFaction.entries()].sort(([, a], [, b]) => b.won / b.played - a.won / a.played || b.played - a.played)
  const faction = ranked.length
    ? { id: ranked[0][0], percent: Math.round((ranked[0][1].won / ranked[0][1].played) * 100), games: ranked[0][1].played }
    : null

  return {
    total: series.length,
    wins: count('vittoria'),
    losses: count('sconfitta'),
    draws: count('pareggio'),
    detailed: detailed.length,
    hands,
    bestHand,
    faction,
  }
}
