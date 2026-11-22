import { index, route, type RouteConfig } from '@react-router/dev/routes'

// Tutti gli indirizzi del sito. Le pagine pre-generate sono elencate in react-router.config.ts.
export default [
  index('routes/landing.tsx'), // /            landing pubblica (SEO)
  route('accedi', 'routes/accedi.tsx'), // /accedi      landing + finestra di accesso
  route('registrati', 'routes/registrati.tsx'), // /registrati  landing + finestra di registrazione
  route('app/*', 'routes/app.tsx'), // /app         area di gioco, solo dopo l'accesso
] satisfies RouteConfig
