// Le due valute in alto a destra.
type CurrencyProps = { label: string; value: string; icon: string }

function Currency({ label, value, icon }: CurrencyProps) {
  return (
    <div className="currency">
      <span className="currency-icon">{icon}</span>
      <div>
        <div className="currency-label">{label}</div>
        <div className="currency-value">{value}</div>
      </div>
    </div>
  )
}

export default function CurrencyBar() {
  return (
    <div className="currency-bar">
      <Currency icon="◎" label="VALUTA 1" value="12.500" />
      <Currency icon="◆" label="VALUTA 2" value="250" />
    </div>
  )
}
