import { packCover } from '../../data/cardArt'
import type { FactionId } from '../../types/deck'

type Props = {
  code: string // tipo di busta: sceglie la copertina e quanto brilla
  faction: FactionId
  size: number // carte nella busta
  setName?: string
  featured?: boolean
  count?: number // quante buste uguali: da 2 in su si vede una pila
}

// La bustina sigillata, uguale in Negozio e in Buste da aprire (stili in collection.css).
// L'ombra sta sul contenitore perché la maschera dei bordi dentellati la taglierebbe.
// Il riflesso cresce con il valore: Standard solo al passaggio, Avanzato più acceso, Élite sempre vivo.
export default function SealedPack({ code, faction, size, setName, featured = false, count = 1 }: Props) {
  const cover = packCover(code, faction)
  return (
    <span className={`shop-busta__ombra shop-busta__ombra--${code} ${count > 1 ? 'shop-busta__ombra--pila' : ''}`}>
      {count > 1 && (
        <>
          <span className="shop-busta__corpo shop-busta__strato shop-busta__strato--2" style={cover} aria-hidden="true" />
          <span className="shop-busta__corpo shop-busta__strato" style={cover} aria-hidden="true" />
        </>
      )}
      <span className="shop-busta__corpo" style={cover}>
        <span className="shop-busta__vignetta" aria-hidden="true" />
        <span className="shop-busta__riflesso" aria-hidden="true" />
        <img className="shop-busta__logo" src="/images/ui/logo-quorma.png" alt="" />
        {featured && <em className="shop-busta__tag">In evidenza</em>}
        <span className="shop-busta__info">
          <b><span className="shop-busta__da">Pacchetto da </span>{size} carte</b>
          {setName && <small>{setName}</small>}
        </span>
      </span>
      {count > 1 && <span className="shop-busta__conta">× {count}</span>}
    </span>
  )
}
