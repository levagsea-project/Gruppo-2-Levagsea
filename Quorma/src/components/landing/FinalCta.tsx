import { Link } from 'react-router'

// Invito finale a registrarsi.
export default function FinalCta() {
  return (
    <section className="l-sez l-entra" aria-labelledby="h-entra">
      <div className="l-wrap">
        <h2 id="h-entra" className="l-h2" style={{ margin: 0 }}>Entra in QUORMA</h2>
        <p className="l-lead">Crea il tuo account, costruisci il tuo primo mazzo e sfida gli altri giocatori.</p>
        <div className="l-entra__azioni">
          <Link to="/registrati" preventScrollReset className="q-btn q-btn--argento">
            <span className="q-btn__in">Registrati</span>
          </Link>
          <Link to="/accedi" preventScrollReset className="q-btn q-btn--acciaio">
            <span className="q-btn__in">Accedi</span>
          </Link>
        </div>
      </div>
    </section>
  )
}
