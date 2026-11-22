// News del gioco. Il backend non ha ancora una chiamata per le news: la lista è vuota e la card
// mostra lo stato vuoto. Quando arriverà (es. GET /news), basta riempire questa lista con lo stesso formato,
// oppure sostituirla con un hook che la legge dal backend: NewsCard non cambia.

export type NewsItem = {
  id: string
  title: string // titolo breve, una riga
  date: string // data di pubblicazione in formato ISO, es. "2026-11-19"
  summary?: string // una frase di riassunto (facoltativa)
}

export const news: NewsItem[] = []
