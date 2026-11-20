import type { ReactNode } from 'react'
import { isRouteErrorResponse, Links, Meta, Outlet, Scripts, ScrollRestoration } from 'react-router'
import './index.css'

// La cornice HTML di ogni pagina. I meta di titolo e descrizione arrivano dalle singole route.
export function Layout({ children }: { children: ReactNode }) {
  return (
    <html lang="it">
      <head>
        <meta charSet="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <meta name="theme-color" content="#0D171D" />
        <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="" />
        <link
          rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=Cinzel:wght@500;600;700;800&family=Inter:wght@400;500;600;700&family=Spectral:ital,wght@1,400&display=swap"
        />
        <Meta />
        <Links />
      </head>
      <body>
        {children}
        <ScrollRestoration />
        <Scripts />
      </body>
    </html>
  )
}

export default function Root() {
  return <Outlet />
}

// Mostrato mentre si carica una pagina non pre-generata (es. /app).
export function HydrateFallback() {
  return null
}

export function ErrorBoundary({ error }: { error: unknown }) {
  const notFound = isRouteErrorResponse(error) && error.status === 404
  return (
    <main style={{ padding: 48, textAlign: 'center' }}>
      <h1 style={{ fontFamily: 'var(--serif)', marginBottom: 16 }}>{notFound ? 'Pagina non trovata' : 'Errore'}</h1>
      <p style={{ marginBottom: 24 }}>{notFound ? 'Questa pagina non esiste.' : 'Qualcosa è andato storto.'}</p>
      <a href="/" style={{ textDecoration: 'underline' }}>Torna alla home</a>
    </main>
  )
}
