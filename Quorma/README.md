# QUORMA – frontend

Sito del gioco di carte collezionabili QUORMA: dark fantasy post-apocalittico, Resistenza Umana contro Potenze Artificiali.
React 19 + TypeScript, Vite e React Router (modalità framework).

Il sito ha due parti:

- **landing pubblica** (`/`): presentazione del gioco, pensata per la SEO. Durante la build diventa HTML statico.
- **area di gioco** (`/app`): l'app vera e propria, visibile solo dopo l'accesso.

## Requisiti

- **Node.js 22.22 o più recente** (richiesto da React Router)
- npm
- Docker serve **solo** per usare il backend vero. Senza Docker si lavora con l'[accesso demo](#accesso-senza-backend-modalità-demo).

## Avvio rapido

```bash
cd Quorma
npm install
cp .env.example .env      # su Windows: copy .env.example .env
npm run dev
```

Il sito è su http://localhost:5173.

## Accesso senza backend (modalità demo)

Senza Docker il backend non è raggiungibile, quindi accesso e registrazione non funzionerebbero.
Per lavorare comunque sull'area di gioco:

1. nel file `.env` imposta `VITE_AUTH_DEMO=true`
2. riavvia `npm run dev`
3. vai su http://localhost:5173/accedi e scrivi **qualsiasi nome e password**

La finestra mostra "Modalità demo attiva" e porta su `/app` senza chiamare il backend.

- Funziona **solo con `npm run dev`**: nella build di produzione la modalità demo non esiste.
- Per uscire cancella la sessione: dagli strumenti del browser (Application → Local Storage → `quorma.session`) oppure con `localStorage.clear()` nella console.
- Il token demo non vale per il backend: le future chiamate alle API protette risponderanno 401.

## Accesso con il backend vero (Docker)

Il backend è in `QUORMA-backend-engine` (non va modificato da qui). Per avviarlo:

```bash
cd QUORMA-backend-engine
docker compose up -d --build     # postgres + engine + backend sulla porta 8080
```

Con `VITE_AUTH_DEMO=false` il frontend usa il backend:

- registrazione: `POST /auth/register` con `{ username, email, password }`
- accesso: `POST /auth/login` con `{ username, password }` (si accede con il **nome del giocatore**, non con l'email)
- regole: nome 3–32 caratteri (lettere, cifre, `_ . -`), password 8–72 caratteri

Il database parte vuoto: non ci sono account di prova, ognuno si registra da `/registrati`.

### Perché c'è un proxy

Il backend non ha CORS, quindi il browser non può chiamarlo direttamente da un'altra porta.
In sviluppo Vite inoltra `/auth/*` al backend togliendo l'header `Origin` (vedi `vite.config.ts`).
L'indirizzo del backend si cambia con `BACKEND_URL` nel file `.env`.

> **Da risolvere prima della pubblicazione:** online il proxy di Vite non c'è. Serve che il backend abiliti CORS
> per il dominio del sito, oppure che sito e backend stiano sotto lo stesso dominio. È compito del team backend.

## Comandi

| Comando | Cosa fa |
|---|---|
| `npm run dev` | server di sviluppo con ricaricamento automatico |
| `npm run build` | genera i tipi, controlla TypeScript e crea la build in `build/client` |
| `npm run preview` | serve la build appena creata, per provarla in locale |
| `npm run typecheck` | solo controllo dei tipi, senza build |
| `npm run lint` | oxlint |

## Variabili d'ambiente (`.env`)

| Variabile | Default | A cosa serve |
|---|---|---|
| `VITE_SITE_URL` | vuoto | indirizzo pubblico del sito, senza `/` finale. Quando c'è, la build aggiunge canonical, immagine per le anteprime social e `sitemap.xml` |
| `BACKEND_URL` | `http://localhost:8080` | dove il proxy di sviluppo manda `/auth` |
| `VITE_AUTH_DEMO` | `false` | `true` = accesso demo senza backend (solo `npm run dev`) |

Il file `.env` non va su git; `.env.example` sì.

## Pagine

| Indirizzo | File | Note |
|---|---|---|
| `/` | `src/routes/landing.tsx` | landing, indicizzata |
| `/accedi` | `src/routes/accedi.tsx` | landing con la finestra di accesso, `noindex` |
| `/registrati` | `src/routes/registrati.tsx` | landing con la finestra di registrazione, `noindex` |
| `/app/*` | `src/routes/app.tsx` | area di gioco; senza sessione rimanda a `/accedi` |

Gli indirizzi sono in `src/routes.ts`. L'area di gioco per ora cambia schermata con lo stato interno di `App.tsx`
(home, mazzi, nuovo mazzo): le singole schermate non hanno ancora un loro indirizzo.

## Struttura

```
Quorma/
├─ public/images/
│  ├─ sfondi/          hero e banner delle fazioni (WebP)
│  ├─ ui/              logo e simboli dei semi
│  └─ og-image.jpg     immagine per le anteprime social (1200×630)
├─ src/
│  ├─ root.tsx         cornice HTML di tutte le pagine (head, font, errori)
│  ├─ routes.ts        elenco degli indirizzi
│  ├─ routes/          una route per pagina (meta SEO + componente)
│  ├─ components/
│  │  ├─ landing/      sezioni della landing e finestra di accesso
│  │  └─ …             componenti dell'area di gioco
│  ├─ data/landing.ts  TUTTI i testi della landing
│  ├─ lib/auth.ts      chiamate al backend, validazione, sessione, modalità demo
│  ├─ lib/site.ts      nome del sito e meta per SEO e social
│  └─ styles/landing/  stile della landing e componenti del design system
├─ react-router.config.ts   pagine pre-generate, robots.txt e sitemap.xml
└─ vite.config.ts           proxy verso il backend
```

## SEO

- Le pagine in `PUBLIC_PAGES` (`react-router.config.ts`) diventano HTML completo durante la build:
  i motori di ricerca leggono i testi senza eseguire JavaScript.
- Titolo, descrizione, Open Graph e Twitter di ogni pagina: export `meta` della route, con `pageMeta()` da `src/lib/site.ts`.
- La landing contiene i dati strutturati `VideoGame` (JSON-LD) in `LandingPage.tsx`.
- `robots.txt` viene generato dalla build: `/app` è escluso. `sitemap.xml` solo se `VITE_SITE_URL` è impostato.

**Aggiungere una pagina pubblica** (es. `/regole`):

1. crea `src/routes/regole.tsx` con `export const meta = () => pageMeta({ … })` e il componente
2. aggiungi `route('regole', 'routes/regole.tsx')` in `src/routes.ts`
3. aggiungi `'/regole'` a `PUBLIC_PAGES` in `react-router.config.ts`

**Testi della landing:** sono tutti in `src/data/landing.ts`. Le fonti sono il regolamento v1.0-alpha e il dossier lore v2.0
in `QUORMA-backend-engine/docs`: nomi di fazioni e sottofazioni vanno scritti esattamente come lì
(SEKTAC, MEROZYN, EFFRIX, YLMOR, Adattati, Spezzasigilli, Portanome, Segnaterra).

## Immagini

- Sono generate con ComfyUI (modello Z-Image) a partire da un workflow comune, per avere tutte lo stesso stile.
  Il workflow e la procedura sono in `tools/` e `CLAUDE.md`, che restano in locale e non sono su git.
- Formati: hero 1920×1088, banner 1536×640, carte 832×1216. Sul sito vanno in WebP (l'hero da 2,5 MB in PNG scende sotto i 100 KB).
- Niente testo dentro le immagini; ogni immagine ha un `alt` descrittivo in italiano.
- Il logo ufficiale (`public/images/ui/logo-quorma.png`) è fornito dal team: non va rigenerato.

## Stile

Colori, materiali e componenti (`q-tela`, `q-btn`, `q-borchia`…) vengono dal QUORMA Design System; la landing riprende il design
"Sito di presentazione" del canvas del progetto. Le classi del design system usate dalla landing sono copiate in
`src/styles/landing/quorma-ds.css`: se cambiano nel design system, vanno aggiornate lì.

## Pubblicazione

`npm run build` crea in `build/client` un sito statico, pubblicabile su qualsiasi hosting statico (es. GitHub Pages):

- `index.html`, `accedi/index.html`, `registrati/index.html`: pagine già pronte
- `__spa-fallback.html`: va servito per tutti gli altri indirizzi (es. `/app`). Su GitHub Pages copialo come `404.html`.

## Da fare

- CORS del backend per la pubblicazione (vedi sopra)
- pagine Termini, Privacy, Chi siamo, FAQ completa: il modulo di registrazione e il piè di pagina non hanno ancora i link
- recupero password: il backend non ha ancora l'endpoint
- favicon: è ancora quella di Vite
- indirizzi propri per le schermate dell'area di gioco (`/app/mazzi`, …)
