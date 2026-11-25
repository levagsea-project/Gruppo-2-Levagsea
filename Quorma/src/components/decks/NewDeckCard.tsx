// La scatola "Nuovo mazzo": vuota, con il più, porta alla schermata di creazione.
export default function NewDeckCard({ onClick }: { onClick: () => void }) {
  return (
    <button type="button" className="k-mazzo" onClick={onClick}>
      <span className="k-scatola k-vuota" aria-hidden="true">
        <span className="k-fronte">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round">
            <path d="M12 5v14M5 12h14" />
          </svg>
        </span>
      </span>
      <span className="k-targa">
        <span className="k-nome">Nuovo mazzo</span>
      </span>
    </button>
  )
}
