// Nomi di carte e abilità per il racconto della battaglia: il log del motore ha solo i codici.
// Si caricano all'apertura della battaglia, per non appesantire il resto del sito. Una carta di un set non presente
// qui resta riconoscibile da rango e seme.
export type BattleNames = { cards: Map<string, string>; abilities: Map<string, string> }

export const NO_NAMES: BattleNames = { cards: new Map(), abilities: new Map() }

type Named = Record<string, { name?: string }>

export async function loadBattleNames(): Promise<BattleNames> {
  const [set, keywords, evolutions] = await Promise.all([
    import('../../../QUORMA-backend-engine/data/sets/BASE.json'),
    import('../../../QUORMA-backend-engine/data/keyword.json'),
    import('../../../QUORMA-backend-engine/data/evolutions.json'),
  ])
  const texts = (set.default as unknown as { texts: { it: Record<string, string> } }).texts.it
  const cards = new Map<string, string>()
  for (const [key, text] of Object.entries(texts)) {
    const m = /^card\.(.+)\.name$/.exec(key)
    if (m) cards.set(m[1], text)
  }
  const abilities = new Map<string, string>()
  for (const source of [keywords.default, evolutions.default] as unknown as Named[]) {
    for (const [id, value] of Object.entries(source)) if (value?.name) abilities.set(id, value.name)
  }
  return { cards, abilities }
}
