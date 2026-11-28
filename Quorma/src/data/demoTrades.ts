// Dati di prova per la pagina Scambi, solo in sviluppo (npm run dev) e solo finché il backend non ha gli endpoint
// di amici e scambi (lib/trades.ts, isMissingApi). Amici e proposte sono inventati; le carte sono le tue, prese da
// GET /collection, e quelle degli amici ne sono copie con altri id. Niente esce dal browser: accettare uno scambio
// qui cambia solo il suo stato, non la collezione. Ricaricando la pagina si riparte da capo.
import { ApiError } from '../lib/api'
import type { Friend, FriendRequest, FriendRequests, Player, Trade, TradesApi } from '../lib/trades'
import type { OwnedCard } from '../types/deck'

const ago = (days: number, hours = 0) => new Date(Date.now() - (days * 24 + hours) * 3_600_000).toISOString()
const player = (username: string, displayName: string, avatar: string): Player => ({ username, displayName, avatar })

const ME = player('tu', 'Tu', 'atona')
const FRIENDS: Friend[] = [
  { ...player('vedetta_08', 'Vedetta 08', 'elio'), since: ago(40) },
  { ...player('brina', 'Brina', 'berenice'), since: ago(21) },
  { ...player('corvo_13', 'Corvo 13', 'ordito'), since: ago(9) },
  { ...player('lume', 'Lume', 'brigida'), since: ago(3) },
]

let seq = 0
const id = (p: string) => `demo-${p}-${++seq}`
const copy = (c: OwnedCard): OwnedCard => ({ ...c, instanceId: id('carta') })
const fail = (code: string, status = 409) => Promise.reject(new ApiError(status, code, 'Qualcosa è andato storto. Riprova.'))
const later = <T,>(v: T) => new Promise<T>((ok) => window.setTimeout(() => ok(v), 250)) // un attimo di "rete"

/** Una sorgente di dati di prova costruita sulle tue carte. */
export function createDemoTrades(mine: OwnedCard[]): TradesApi {
  // una carta per tipo, in ordine sparso ma sempre uguale
  const distinct = [...new Map(mine.map((c) => [c.cardId, c])).values()]
  const pick = (from: number, n: number) => Array.from({ length: Math.min(n, distinct.length) }, (_, i) => distinct[(from + i * 7) % distinct.length])
  const collections = new Map(FRIENDS.map((f, i) => [f.username, pick(i * 5 + 3, 14).map(copy)]))
  const mineOf = (n: number, from: number) => pick(from, n).map((c) => mine.find((m) => m.cardId === c.cardId)!)

  let friends = [...FRIENDS]
  let requests: FriendRequests = {
    received: [{ ...player('sentinella_77', 'Sentinella 77', 'sereno'), id: id('rich'), createdAt: ago(0, 5) }],
    sent: [],
  }
  const trade = (other: Player, myRole: Trade['myRole'], give: OwnedCard[], get: OwnedCard[], status: Trade['status'], createdAt: string): Trade => ({
    id: id('scambio'),
    status,
    myRole,
    proposer: myRole === 'PROPOSER' ? ME : other,
    recipient: myRole === 'PROPOSER' ? other : ME,
    offer: myRole === 'PROPOSER' ? give : get,
    request: myRole === 'PROPOSER' ? get : give,
    createdAt,
    resolvedAt: status === 'PENDING' ? null : createdAt,
  })
  const theirs = (username: string, n: number) => (collections.get(username) ?? []).slice(0, n)
  let trades: Trade[] = distinct.length
    ? [
        trade(FRIENDS[0], 'RECIPIENT', mineOf(1, 2), theirs(FRIENDS[0].username, 2), 'PENDING', ago(0, 2)),
        trade(FRIENDS[2], 'RECIPIENT', mineOf(2, 9), theirs(FRIENDS[2].username, 1), 'PENDING', ago(0, 9)),
        trade(FRIENDS[1], 'PROPOSER', mineOf(1, 4), theirs(FRIENDS[1].username, 1), 'PENDING', ago(1, 3)),
        trade(FRIENDS[3], 'PROPOSER', mineOf(1, 6), theirs(FRIENDS[3].username, 1), 'ACCEPTED', ago(12)),
        trade(FRIENDS[0], 'RECIPIENT', mineOf(1, 11), theirs(FRIENDS[0].username, 1), 'REJECTED', ago(14)),
      ]
    : []

  const settle = (tradeId: string, status: Trade['status'], who: Trade['myRole']) => {
    const t = trades.find((x) => x.id === tradeId)
    if (!t) return fail('trade.not_found', 404)
    if (t.status !== 'PENDING' || t.myRole !== who) return fail('trade.not_pending')
    const done = { ...t, status, resolvedAt: new Date().toISOString() }
    trades = trades.map((x) => (x.id === tradeId ? done : x))
    return later(done)
  }

  return {
    listFriends: () => later([...friends].sort((a, b) => a.displayName.localeCompare(b.displayName, 'it'))),
    listFriendRequests: () => later(requests),
    sendFriendRequest: (username) => {
      const name = username.trim()
      if (friends.some((f) => f.username.toLowerCase() === name.toLowerCase())) return fail('friend.already_friends')
      if (requests.sent.some((r) => r.username.toLowerCase() === name.toLowerCase())) return fail('friend.request_pending')
      const r: FriendRequest = { ...player(name, name, 'sereno'), id: id('rich'), createdAt: new Date().toISOString() }
      requests = { ...requests, sent: [r, ...requests.sent] }
      return later(r)
    },
    acceptFriendRequest: (requestId) => {
      const r = requests.received.find((x) => x.id === requestId)
      if (!r) return fail('friend.request_not_found', 404)
      const f: Friend = { username: r.username, displayName: r.displayName, avatar: r.avatar, since: new Date().toISOString() }
      friends = [...friends, f]
      collections.set(f.username, pick(17, 10).map(copy))
      requests = { ...requests, received: requests.received.filter((x) => x.id !== requestId) }
      return later(f)
    },
    declineFriendRequest: (requestId) => {
      requests = { ...requests, received: requests.received.filter((x) => x.id !== requestId) }
      return later(null)
    },
    getFriendCollection: (username) => (collections.has(username) ? later(collections.get(username)!) : fail('friend.not_found', 404)),
    listTrades: () => later([...trades].sort((a, b) => b.createdAt.localeCompare(a.createdAt))),
    createTrade: (recipientUsername, offer, request) => {
      const other = friends.find((f) => f.username === recipientUsername)
      if (!other) return fail('trade.not_friends', 403)
      const give = offer.map((iid) => mine.find((c) => c.instanceId === iid)).filter((c): c is OwnedCard => !!c)
      const get = request.map((iid) => collections.get(recipientUsername)?.find((c) => c.instanceId === iid)).filter((c): c is OwnedCard => !!c)
      const t = trade(other, 'PROPOSER', give, get, 'PENDING', new Date().toISOString())
      trades = [t, ...trades]
      return later(t)
    },
    acceptTrade: (tradeId) => settle(tradeId, 'ACCEPTED', 'RECIPIENT'),
    declineTrade: (tradeId) => settle(tradeId, 'REJECTED', 'RECIPIENT'),
    cancelTrade: (tradeId) => settle(tradeId, 'CANCELLED', 'PROPOSER'),
  }
}
