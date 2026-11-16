// Bottone "a pillola" con bordo, usato dentro le card (es. VAI AGLI SCAMBI →).
export default function OutlineButton({ children }: { children: string }) {
  return (
    <button className="outline-btn">
      {children} <span>→</span>
    </button>
  )
}
