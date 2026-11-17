import type { MetaDescriptor } from 'react-router'

// Dati del sito usati da titoli, descrizioni e anteprime social.
// L'indirizzo del sito arriva da VITE_SITE_URL (file .env): finché non c'è un dominio resta vuoto
// e i tag che richiedono un indirizzo completo (canonical, og:url, og:image) non vengono scritti.
export const SITE_NAME = 'QUORMA'
export const SITE_URL = (import.meta.env.VITE_SITE_URL ?? '').replace(/\/+$/, '')
export const OG_IMAGE = '/images/og-image.jpg'

type PageMeta = {
  title: string
  description: string
  path: string
  noindex?: boolean
}

// Crea i meta di una pagina: titolo, descrizione, robots, canonical, Open Graph e Twitter.
export function pageMeta({ title, description, path, noindex }: PageMeta): MetaDescriptor[] {
  const meta: MetaDescriptor[] = [
    { title },
    { name: 'description', content: description },
    { name: 'robots', content: noindex ? 'noindex, follow' : 'index, follow' },
    { property: 'og:type', content: 'website' },
    { property: 'og:site_name', content: SITE_NAME },
    { property: 'og:locale', content: 'it_IT' },
    { property: 'og:title', content: title },
    { property: 'og:description', content: description },
    { name: 'twitter:card', content: 'summary_large_image' },
    { name: 'twitter:title', content: title },
    { name: 'twitter:description', content: description },
  ]

  if (SITE_URL) {
    meta.push(
      { tagName: 'link', rel: 'canonical', href: SITE_URL + path },
      { property: 'og:url', content: SITE_URL + path },
      { property: 'og:image', content: SITE_URL + OG_IMAGE },
      { property: 'og:image:width', content: '1200' },
      { property: 'og:image:height', content: '630' },
      { property: 'og:image:alt', content: 'Due colossi delle IA si scontrano sopra una città in rovina, osservati da sopravvissuti con le torce' },
      { name: 'twitter:image', content: SITE_URL + OG_IMAGE },
    )
  }
  return meta
}
