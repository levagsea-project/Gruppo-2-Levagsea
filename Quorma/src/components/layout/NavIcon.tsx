// Icone del menu laterale (dal design system QUORMA). Prendono il colore del testo (currentColor).
const PATHS = {
  home: ['M6 21V11.5C6 7.5 8.5 4.8 12 3c3.5 1.8 6 4.5 6 8.5V21', 'M4.5 21h15', 'M12 12.2a1.3 1.3 0 1 0 0.01 0'],
  gioca: ['M12 2 22 12 12 22 2 12z', 'M8.5 8.5l7 7', 'M15.5 8.5l-7 7'],
  sfide: ['M6 21V3', 'M6 4h12l-3 4.5 3 4.5H6', 'M11 7l.7 1.5 1.5.7-1.5.7L11 11.4l-.7-1.5-1.5-.7 1.5-.7z'],
  collezione: ['M9.5 3.5h5v13h-5z', 'M9.5 6 6 7l2.6 10', 'M14.5 6 18 7l-2.6 10', 'M12 19v2.5'],
  mazzi: ['M6.5 2.5h11v13h-11z', 'M8 18.5h8M10 21.5h4', 'M12 6l2.5 3L12 12l-2.5-3z'],
  scambi: ['M5 10a7.5 7.5 0 0 1 12.8-3.6', 'M18.5 3v4h-4', 'M19 14a7.5 7.5 0 0 1-12.8 3.6', 'M5.5 21v-4h4'],
  negozio: ['M12 3.5l5 6.5-5 10.5L7 10z', 'M7 10h10', 'M10 10l2 10.5 2-10.5'],
  regole: ['M6 3h12v18H6z', 'M12 7a3 3 0 1 0 0.01 0', 'M12 6v6M9 9h6', 'M9 16.5h6'],
}

export type NavIconName = keyof typeof PATHS

export default function NavIcon({ name }: { name: NavIconName }) {
  return (
    <svg
      className="q-navata__ico"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {PATHS[name].map((d) => (
        <path key={d} d={d} />
      ))}
    </svg>
  )
}
