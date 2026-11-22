import path from 'node:path'
import { reactRouter } from '@react-router/dev/vite'
import { defineConfig, loadEnv, type ProxyOptions } from 'vite'

// Le chiamate al backend (docker compose, porta 8080) passano da qui.
// Il backend ha il CORS, ma ammette solo le origini di cors.allowed-origins (di default la porta 5173).
// Togliendo Origin la richiesta diventa "stesso sito" e funziona su qualsiasi porta, anche con
// "npm run preview" (4173). Per chiamare il backend direttamente, senza proxy, imposta VITE_API_URL.
// Ogni nuovo percorso del backend usato dal frontend va aggiunto a questa lista, altrimenti Vite risponde 404.
const BACKEND_PATHS = ['/auth', '/decks', '/collection', '/matchmaking', '/matches', '/challenges', '/rooms', '/packs', '/wallet']

function backendProxy(backend: string): Record<string, ProxyOptions> {
  const options: ProxyOptions = {
    target: backend,
    changeOrigin: true,
    configure: (proxy) => {
      proxy.on('proxyReq', (req) => {
        req.removeHeader('origin')
        req.removeHeader('referer')
      })
    },
  }
  return Object.fromEntries(BACKEND_PATHS.map((p) => [p, options]))
}

// I messaggi d'errore (src/lib/api.ts) e i nomi di carte e abilità nella battaglia (src/lib/battleNames.ts) vengono da
// QUORMA-backend-engine/data: la cartella è fuori dal progetto, quindi va permessa esplicitamente al server di sviluppo.
const GAME_DATA_DIR = path.resolve(import.meta.dirname, '../QUORMA-backend-engine/data')

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxy = backendProxy(env.BACKEND_URL || 'http://localhost:8080')

  return {
    plugins: [reactRouter()],
    server: { proxy, fs: { allow: [import.meta.dirname, GAME_DATA_DIR] } },
    preview: { proxy },
  }
})
