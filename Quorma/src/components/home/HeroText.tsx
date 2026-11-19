import type { PlayNotice } from '../../hooks/usePlayStatus'
import PlayButton from './PlayButton'

const TITLE = 'Pronto a giocare?'

type Props = {
  onPlay: () => void
  notice: PlayNotice
  onNoticeAction: (action: NonNullable<PlayNotice['action']>) => void
}

// Titolo, descrizione e sigillo GIOCA: la parte di testo a sinistra.
// data-text serve all'effetto "glitch" del titolo (due copie colorate che ogni tanto sfasano).
export default function HeroText({ onPlay, notice, onNoticeAction }: Props) {
  return (
    <div className="hero-text">
      <span className="hero-line" aria-hidden="true" />
      <h1 className="hero-title" data-text={TITLE}>{TITLE}</h1>
      <p className="hero-desc">
        Un ordine artificiale che si proclama perfetto. Una frattura al suo interno. Un'umanità che sopravvive.
      </p>
      <PlayButton onClick={onPlay} notice={notice} onNoticeAction={onNoticeAction} />
    </div>
  )
}
