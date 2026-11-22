// La carta "Nuovo mazzo": arco vuoto con il più, porta alla schermata di creazione.
export default function NewDeckCard({ onClick }: { onClick: () => void }) {
  return (
    <button type="button" className="pick deck-pick deck-pick--nuovo" onClick={onClick}>
      <span className="q-tela q-tela--arco">
        <span className="q-tela__in">
          <svg aria-hidden="true" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round">
            <path d="M12 5v14M5 12h14" />
          </svg>
          <span className="deck-pick__nuovo">Nuovo mazzo</span>
          <span className="deck-pick__carte">32 carte</span>
        </span>
        <span className="q-borchia q-borchia--n" aria-hidden="true" />
      </span>
    </button>
  )
}
