import { useEffect, useRef, useState, useSyncExternalStore, type CSSProperties } from 'react'
import { AnimatePresence, animate, m, useMotionValue, useTransform } from 'motion/react'
import { steps } from '../../data/landing'

// Esempio di tavolo: le corsie dell'avversario sopra, le tue sotto. Tre Queen = Tris, attaccano le corsie 1, 3 e 5.
const opponent = [
  { rank: '3', hit: '−4.500', over: '+1.500' },
  { rank: 'K' },
  { rank: '7', hit: '−4.200', over: '+900' },
  { rank: '9' },
  { rank: '2', hit: '−3.900' },
]
const mine = [
  { rank: 'Q', suit: 'vincolo', attacks: true },
  { rank: '4', suit: 'spira', attacks: false },
  { rank: 'Q', suit: 'frattura', attacks: true },
  { rank: '8', suit: 'vestigio', attacks: false },
  { rank: 'Q', suit: 'spira', attacks: true },
]
// Cosa succede sul tavolo a ogni passo (la didascalia sotto) e la vita dell'avversario.
const CAPTIONS = [
  ['Il tuo mazzo da 32 carte viene mescolato: ', '5 creature', ' entrano nelle tue corsie, coperte.'],
  ['Le creature si scoprono. Tre Queen formano un ', 'Tris', ': sono loro ad attaccare.'],
  ['Ogni Queen colpisce la creatura nella corsia opposta e ne riduce la difesa.', '', ''],
  ['Il danno in eccesso ', 'trabocca', ' sull’avversario, moltiplicato per il Tris.'],
]
const LIFE = [12000, 12000, 12000, 8640]
const STEP_MS = 3400
const migliaia = (n: number) => String(Math.round(n)).replace(/\B(?=(\d{3})+(?!\d))/g, '.')

// La vita dell'avversario scala cifra per cifra insieme alla barra (stessi tempi della transizione in landing.css),
// così il danno che trabocca si legge come numero. Con il movimento ridotto il valore cambia subito.
function LifeCount({ value, reduced }: { value: number; reduced: boolean }) {
  const mv = useMotionValue(value)
  const text = useTransform(mv, migliaia)
  useEffect(() => {
    if (reduced) {
      mv.set(value)
      return
    }
    const a = animate(mv, value, { duration: 1.1, delay: 0.5, ease: [0.3, 0.7, 0.2, 1] })
    return () => a.stop()
  }, [mv, value, reduced])
  return <m.b>{text}</m.b>
}

// Movimento ridotto del sistema: letto dal vivo (e false nell'HTML statico, dove la demo non scorre comunque).
const REDUCED = '(prefers-reduced-motion: reduce)'
const onReducedChange = (cb: () => void) => {
  const mq = window.matchMedia(REDUCED)
  mq.addEventListener('change', cb)
  return () => mq.removeEventListener('change', cb)
}

// "Come si gioca" (design: Landing-Quorma, Demo della partita): i quattro passi del turno e un tavolo che li mostra.
// La dimostrazione scorre da sola solo mentre la sezione è sullo schermo; premere un passo o Indietro/Avanti la ferma.
// Con il movimento ridotto non parte da sola e le carte cambiano senza animazione (landing.css).
export default function HowToPlaySection() {
  const [step, setStep] = useState(0)
  const [auto, setAuto] = useState<boolean | null>(null) // null = nessuna scelta: decide il movimento ridotto
  const reduced = useSyncExternalStore(onReducedChange, () => window.matchMedia(REDUCED).matches, () => false)
  const playing = auto ?? !reduced // con il movimento ridotto non scorre da sola, ma si può avviare con Riproduci
  const [visible, setVisible] = useState(false)
  const section = useRef<HTMLElement>(null)

  useEffect(() => {
    const el = section.current
    if (!el || !('IntersectionObserver' in window)) return
    const io = new IntersectionObserver(([e]) => setVisible(e.isIntersecting), { threshold: 0.25 })
    io.observe(el)
    return () => io.disconnect()
  }, [])

  useEffect(() => {
    if (!playing || !visible) return
    const timer = window.setInterval(() => setStep((s) => (s + 1) % steps.length), STEP_MS)
    return () => window.clearInterval(timer)
  }, [playing, visible])

  const go = (n: number) => {
    setStep(n)
    setAuto(false)
  }
  const [before, bold, after] = CAPTIONS[step]

  return (
    <section ref={section} id="come-si-gioca" className="l-sez" aria-labelledby="h-come">
      <div className="l-wrap l-come">
        <div>
          <p className="l-kicker l-rivela">Come si gioca</p>
          <h2 id="h-come" className="l-h2 l-rivela">Quattro passi</h2>
          <p className="l-lead l-come__lead l-rivela">Premi un passo per vederlo sul tavolo, oppure lascia scorrere la dimostrazione.</p>
          <ol className={`l-passi ${playing ? 'l-auto' : ''}`} aria-label="Passi della partita">
            {steps.map((s, i) => (
              <li key={s.title} className="l-rivela">
                <button type="button" className="l-passo-btn" aria-pressed={step === i} aria-controls="l-tavolo" onClick={() => go(i)}>
                  <span className="q-marker__num" aria-hidden="true"><span>{i + 1}</span></span>
                  <span className="l-passo-btn__titolo">{s.title}</span>
                  <span className="l-passo-btn__testo">{s.text}</span>
                  {/* la barra si riempie nel tempo del passo, solo mentre la dimostrazione scorre */}
                  <span key={step === i ? `on-${step}` : 'off'} className="l-avanz" aria-hidden="true"><i /></span>
                </button>
              </li>
            ))}
          </ol>
        </div>

        <figure className="q-tela l-tavolo-box l-rivela">
          <div id="l-tavolo" className={`q-tela__in l-tavolo l-p${step}`}>
            <span className="l-kicker">Avversario</span>
            <div className="l-vita" role="img" aria-label={`Vita dell'avversario: ${migliaia(LIFE[step])}`}>
              <i style={{ '--v': step === 3 ? '72%' : '100%' } as CSSProperties} />
              <LifeCount value={LIFE[step]} reduced={reduced} />
            </div>
            <div className="l-riga l-avv" aria-hidden="true">
              {opponent.map((c, i) => (
                <span key={i} className={`l-c ${c.hit ? 'l-bersaglio' : ''}`}>
                  <span className="l-c__in"><span className="l-c__f"><span>{c.rank}</span></span><span className="l-c__r" /></span>
                  {c.hit && <span className="l-danno">{c.hit}</span>}
                  {c.over && <span className="l-trab">{c.over}</span>}
                </span>
              ))}
            </div>
            <div className="l-riga" aria-hidden="true">
              {mine.map((c, i) => <span key={i} className={`l-fr ${c.attacks ? 'l-si' : ''}`} />)}
            </div>
            <div className="l-riga l-mio" aria-hidden="true">
              {mine.map((c, i) => (
                <span key={i} className={`l-c ${c.attacks ? 'l-q' : ''}`}>
                  <span className="l-c__in">
                    <span className="l-c__f"><span>{c.rank}</span><i className={`l-seme l-seme--${c.suit}`} /></span>
                    <span className="l-c__r" />
                  </span>
                </span>
              ))}
            </div>
            <div className="l-riga" aria-hidden="true">
              {mine.map((_, i) => <span key={i} className="l-num">{i + 1}</span>)}
            </div>
            <div className="l-mano" aria-hidden={step === 0}><b>Tris</b><span>× 1.400</span></div>
            {/* annunciata solo quando la si guida a mano: in automatico cambierebbe ogni 3 secondi */}
            <figcaption className="l-didascalia" aria-live={playing ? 'off' : 'polite'}>
              {/* la didascalia nuova sostituisce la vecchia con una breve dissolvenza (uscita più rapida dell'entrata) */}
              <AnimatePresence mode="wait" initial={false}>
                <m.span
                  key={step}
                  className="l-didascalia__testo"
                  initial={{ opacity: 0, y: 5 }}
                  animate={{ opacity: 1, y: 0, transition: { duration: 0.24, ease: [0.16, 1, 0.3, 1] } }}
                  exit={{ opacity: 0, y: -3, transition: { duration: 0.12, ease: 'easeIn' } }}
                >
                  {before}{bold && <b>{bold}</b>}{after}
                </m.span>
              </AnimatePresence>
            </figcaption>
            <div className="l-comandi">
              <button type="button" aria-disabled={step === 0} onClick={() => step > 0 && go(step - 1)}>
                <svg viewBox="0 0 12 12" aria-hidden="true"><path d="M7.5 2 3.5 6l4 4" /></svg>Indietro
              </button>
              <button type="button" aria-pressed={playing} onClick={() => setAuto(!playing)}>{playing ? 'Pausa' : 'Riproduci'}</button>
              <button type="button" aria-disabled={step === steps.length - 1} onClick={() => step < steps.length - 1 && go(step + 1)}>
                Avanti<svg viewBox="0 0 12 12" aria-hidden="true"><path d="m4.5 2 4 4-4 4" /></svg>
              </button>
            </div>
          </div>
        </figure>
      </div>
    </section>
  )
}
