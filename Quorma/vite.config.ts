import { reactRouter } from '@react-router/dev/vite'
import { defineConfig, loadEnv, type ProxyOptions } from 'vite'

// Le chiamate a /auth/* passano da qui e arrivano al backend (docker compose, porta 8080).
// Il backend non ha CORS: togliamo l'header Origin così tratta la richiesta come
// "stesso sito", come fa WMD/Test_Auth/server.js. Il backend non va modificato.
function authProxy(backend: string): Record<string, ProxyOptions> {
  return {
    '/auth': {
      target: backend,
      changeOrigin: true,
      configure: (proxy) => {
        proxy.on('proxyReq', (req) => {
          req.removeHeader('origin')
          req.removeHeader('referer')
        })
      },
    },
  }
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxy = authProxy(env.BACKEND_URL || 'http://localhost:8080')

  return {
    plugins: [reactRouter()],
    server: { proxy },
    preview: { proxy },
  }
})
