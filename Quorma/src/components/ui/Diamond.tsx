// Il piccolo rombo azzurro che si vede sopra le card.
export default function Diamond({ className = '' }: { className?: string }) {
  return <span className={`diamond ${className}`} aria-hidden="true" />
}
