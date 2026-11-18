import type { ReactNode } from 'react'

// Tabella del regolamento: intestazioni + righe di testo.
export function Table({ head, rows }: { head: string[]; rows: string[][] }) {
  return (
    <div className="r-tabwrap">
      <table className="r-tab">
        <thead>
          <tr>
            {head.map((h) => (
              <th key={h} scope="col">{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i}>
              {r.map((c, j) => (
                <td key={j}>{c}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

// Elenco numerato che riprende da un numero diverso da 1 (es. from={4} parte da 4).
export function Ol({ from = 1, children }: { from?: number; children: ReactNode }) {
  return (
    <ol className="r-ol" start={from} style={{ counterReset: `r ${from - 1}` }}>
      {children}
    </ol>
  )
}
