import { writeFileSync } from 'node:fs'
import { join } from 'node:path'
import type { Config } from '@react-router/dev/config'
import { loadEnv } from 'vite'

// Pagine pubbliche: diventano file HTML già pronti durante la build (buone per la SEO).
// Il resto (l'area di gioco su /app) resta un'app che si carica nel browser.
const PUBLIC_PAGES = ['/']
// Accesso e registrazione: anche loro HTML statico, ma fuori dalla sitemap (sono noindex).
const AUTH_PAGES = ['/accedi', '/registrati']

const SITE_URL = (loadEnv('production', process.cwd(), 'VITE_').VITE_SITE_URL || '').replace(/\/+$/, '')

export default {
  appDirectory: 'src',
  ssr: false,
  prerender: [...PUBLIC_PAGES, ...AUTH_PAGES],

  // Dopo la build: sitemap.xml e robots.txt. La sitemap richiede indirizzi completi,
  // quindi viene creata solo quando VITE_SITE_URL è impostato (vedi .env.example).
  buildEnd({ reactRouterConfig }) {
    const out = join(reactRouterConfig.buildDirectory, 'client')
    // /accedi e /registrati restano visitabili: è il loro meta "noindex" a tenerle fuori da Google.
    const robots = ['User-agent: *', 'Allow: /', 'Disallow: /app']

    if (SITE_URL) {
      const urls = PUBLIC_PAGES.map((p) => `  <url><loc>${SITE_URL}${p}</loc></url>`).join('\n')
      writeFileSync(
        join(out, 'sitemap.xml'),
        `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls}\n</urlset>\n`,
      )
      robots.push('', `Sitemap: ${SITE_URL}/sitemap.xml`)
    }
    writeFileSync(join(out, 'robots.txt'), robots.join('\n') + '\n')
  },
} satisfies Config
