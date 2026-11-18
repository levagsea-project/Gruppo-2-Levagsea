import type { CSSProperties, ReactNode } from 'react'
import Hearth from '../layout/Hearth'

// Ogiva (zoccolo ad arco) con icona. active = fuoco azzurro, alert = braci arancioni con scintille.
export default function Arch({ children, active, alert }: { children: ReactNode; active?: boolean; alert?: boolean }) {
  return (
    <span className={alert ? 'q-ogiva-wrap q-braci' : 'q-ogiva-wrap'} aria-hidden="true">
      <span className="q-ogiva">
        <span className="q-ogiva__in">
          {active && (
            <Hearth
              flames={[{ left: '38%' }, { left: '58%', delay: '0.5s', duration: '1.3s' }, { left: '48%', delay: '0.95s', duration: '1.7s' }]}
              sparks={[{ left: '40%', dx: '-3px', delay: '0.2s' }, { left: '62%', dx: '4px', delay: '1.3s', duration: '2.8s' }]}
            />
          )}
          {children}
        </span>
      </span>
      {alert && (
        <>
          <span className="q-scintilla" style={{ left: '30%', '--dx': '-4px' } as CSSProperties} />
          <span className="q-scintilla" style={{ left: '62%', '--dx': '5px', animationDelay: '1.3s' } as CSSProperties} />
        </>
      )}
    </span>
  )
}
