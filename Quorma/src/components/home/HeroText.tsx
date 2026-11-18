import PlayButton from './PlayButton'

// Titolo, descrizione e bottone: la parte di testo a sinistra.
export default function HeroText({ onPlay }: { onPlay: () => void }) {
  return (
    <div className="hero-text">
      <span className="hero-line" />
      <h1>
        Pronto a
        <br />
        giocare?
      </h1>
      <p className="hero-desc">
        Un ordine artificiale che si proclama perfetto. Una frattura al suo
        interno. Un'umanità che sopravvive.
      </p>
      <PlayButton onClick={onPlay} />
    </div>
  )
}
