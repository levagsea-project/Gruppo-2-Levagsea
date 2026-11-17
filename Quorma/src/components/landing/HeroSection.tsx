import { Link } from 'react-router'

// Apertura: immagine a tutto schermo, titolo, motto e invito a giocare.
export default function HeroSection() {
  return (
    <section id="top" className="l-hero" aria-labelledby="titolo">
      <div className="l-hero__bg">
        {/* È l'immagine più grande della pagina: si carica per prima (fetchPriority). */}
        <img
          src="/images/sfondi/hero-home.webp"
          srcSet="/images/sfondi/hero-home-960.webp 960w, /images/sfondi/hero-home.webp 1920w"
          sizes="100vw"
          width={1920}
          height={1088}
          fetchPriority="high"
          alt="Due colossi delle IA si scontrano sopra una città gotica in rovina, mentre alcuni sopravvissuti incappucciati con le torce osservano da una rupe"
        />
      </div>
      <div className="l-wrap">
        <div className="l-hero__testo">
          {/* Il titolo contiene testo vero, non solo il logo: è quello che legge Google. */}
          <h1 id="titolo">
            <span className="l-kicker" style={{ margin: 0 }}>Gioco di carte collezionabili</span>
            <img src="/images/ui/logo-quorma.png" alt="QUORMA" width={640} height={179} />
          </h1>
          <p className="l-hero__motto">
            Un ordine artificiale che si proclama perfetto. Una frattura al suo interno. Un'umanità che sopravvive.
          </p>
          <div className="l-hero__azioni">
            <Link to="/registrati" preventScrollReset className="q-btn q-btn--sigillo">
              <span className="q-btn__in">
                <svg className="q-stella" aria-hidden="true" viewBox="0 0 20 28">
                  <path d="M10 0l2.2 11.6L20 14l-7.8 2.4L10 28l-2.2-11.6L0 14l7.8-2.4z" fill="#8FD6DB" />
                </svg>
                Gioca ora
              </span>
              <span className="q-borchia q-borchia--w" />
              <span className="q-borchia q-borchia--e" />
            </Link>
            <Link to="/accedi" preventScrollReset className="l-testo-link">Hai già un account? Accedi</Link>
          </div>
        </div>
      </div>
      <a href="#il-gioco" className="l-link l-scopri">
        Scopri il gioco
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
          <path d="m6 9 6 6 6-6" />
        </svg>
      </a>
    </section>
  )
}
