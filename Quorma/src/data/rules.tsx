// Testo del regolamento (v1.0-alpha), una voce per sezione.
// I numeri tra parentesi quadre sono segnaposto che cambieranno con il playtesting.
import type { ReactNode } from 'react'
import { Ol, Table } from '../components/rules/RuleBlocks'
import SuitCycle from '../components/rules/SuitCycle'

export type RuleSection = { id: string; num: string; title: string; body: ReactNode }

export const ruleSections: RuleSection[] = [
  {
    id: 'oro',
    num: '0',
    title: "Regole d'Oro",
    body: (
      <Ol>
        <li>Il gioco non prevede decisioni da parte dei giocatori durante la partita. Il gioco è completamente deterministico e si svolgerà sempre allo stesso modo, a parità di mazzi e di seed (seme) casuale.</li>
        <li>Se il testo di una carta contraddice il regolamento, il testo della carta ha la precedenza.</li>
        <li>Quando vengono applicati più modificatori, quelli additivi vengono applicati per primi, poi quelli moltiplicativi in per-mille, infine il numero viene arrotondato per difetto all'intero più vicino. Se un effetto deve impostare un valore, viene applicato dopo tutti i modificatori. Se più effetti impostassero lo stesso valore, l'effetto che si verifica per ultimo sostituisce quello precedente.</li>
      </Ol>
    ),
  },
  {
    id: 'obiettivi',
    num: '1',
    title: 'Obiettivi',
    body: <p>Ridurre la vita del proprio avversario a 0 o meno, oppure lasciarlo senza creature.</p>,
  },
  {
    id: 'fazioni',
    num: '2',
    title: 'Fazioni',
    body: (
      <>
        <Ol>
          <li>Ci sono due fazioni nel gioco: la Resistenza Umana e le Potenze Artificiali. Ogni fazione ha il proprio pool unico di carte.</li>
          <li>
            Ogni fazione è divisa in sottofazioni. Ogni carta creatura appartiene a una sottofazione. Le sottofazioni sono le seguenti:
            <ul className="r-ul">
              <li><b>Resistenza Umana:</b> Adattati, Spezzasigilli, Portanome, Segnaterra</li>
              <li><b>Potenze Artificiali:</b> SEKTAC / Senzaeco, MEROZYN / Franti, EFFRIX / Malscritti, YLMOR / Incompiuti</li>
            </ul>
          </li>
          <li>Ogni carta creatura appartiene a un seme. I semi sono condivisi tra le fazioni. Ci sono quattro semi nel gioco: Vincolo, Frattura, Spira, Vestigio.</li>
          <li>Ogni sottofazione è forte contro una sottofazione e debole contro un'altra. Le relazioni sono reciproche e formano due cicli tematici: il Ciclo del Mondo e il Ciclo del Sé. Le relazioni sono le seguenti:</li>
        </Ol>
        <Table
          head={['Sottofazione', 'Forte contro', 'Debole contro']}
          rows={[
            ['Adattati', 'MEROZYN', 'EFFRIX'],
            ['Spezzasigilli', 'SEKTAC', 'YLMOR'],
            ['Portanome', 'EFFRIX', 'MEROZYN'],
            ['Segnaterra', 'YLMOR', 'SEKTAC'],
            ['SEKTAC', 'Segnaterra', 'Spezzasigilli'],
            ['MEROZYN', 'Portanome', 'Adattati'],
            ['EFFRIX', 'Adattati', 'Portanome'],
            ['YLMOR', 'Spezzasigilli', 'Segnaterra'],
          ]}
        />
        <div className="r-cicli">
          <SuitCycle
            nodes={[
              { short: 'AD', name: 'Adattati', color: '#C9785A' },
              { short: 'MZ', name: 'MEROZYN', color: '#D16FB4' },
              { short: 'PN', name: 'Portanome', color: '#E3D3A1' },
              { short: 'EF', name: 'EFFRIX', color: '#E2B254' },
            ]}
          />
          <SuitCycle
            nodes={[
              { short: 'SP', name: 'Spezzasigilli', color: '#A98BE6' },
              { short: 'SK', name: 'SEKTAC', color: '#6FA9E0' },
              { short: 'SG', name: 'Segnaterra', color: '#9DBF6E' },
              { short: 'YL', name: 'YLMOR', color: '#4FD1B0' },
            ]}
          />
        </div>
        <p className="r-nota">La freccia va dalla sottofazione forte a quella contro cui è forte.</p>
      </>
    ),
  },
  {
    id: 'carte',
    num: '3',
    title: 'Carte',
    body: (
      <>
        <h3 className="r-h3">3.1 Mazzo</h3>
        <Ol>
          <li>Un mazzo ha esattamente 32 carte.</li>
          <li>Un mazzo è costruito da esattamente un pool di una fazione principale.</li>
          <li>A seconda della fazione principale, un mazzo deve rispettare i seguenti limiti:</li>
        </Ol>
        <Table
          head={['Fazione principale', 'Creature', 'Magie / Equipaggiamenti']}
          rows={[
            ['Resistenza Umana', '20–23', '9–12'],
            ['Potenze Artificiali', '24–27', '5–8'],
          ]}
        />
        <Ol from={4}>
          <li>Un mazzo contiene al massimo una carta creatura per ogni combinazione di rango e seme.</li>
          <li>Un mazzo può contenere solo una singola copia di ogni carta magia o equipaggiamento.</li>
        </Ol>

        <h3 className="r-h3">3.2 Carte Creatura</h3>
        <Ol>
          <li>Una carta creatura ha i seguenti attributi: Nome, Fazione (Resistenza Umana o Potenze Artificiali), Sottofazione, Rango (Asso, 2, 3, 4, 5, 6, 7, 8, 9, 10, Jack, Queen, King), Seme (Vincolo, Frattura, Spira, Vestigio), Attacco (ATK), Difesa (DEF), Abilità Speciale (se presente).</li>
          <li>
            Ogni carta ha un livello di rarità. Il Tier (livello) della carta è determinato dal suo rango:
            <ul className="r-ul">
              <li>2–6: Tier 1</li>
              <li>7–10: Tier 2</li>
              <li>Jack, Queen, King: Tier 3</li>
              <li>Asso: Tier 4</li>
            </ul>
            Il Tier non ha effetto sulle statistiche o sulla forza della carta, ma le carte di Tier più alto hanno un impatto maggiore sul gameplay e hanno più probabilità di avere abilità speciali.
          </li>
          <li>L'Attacco base non cambia mai. L'Attacco attuale di una creatura è pari al suo Attacco base più eventuali modificatori, nell'ordine indicato dalle Regole d'Oro.</li>
          <li>L'Attacco non può mai scendere sotto 0. Se l'Attacco di una creatura viene ridotto a 0, può comunque attaccare ma non infliggerà alcun danno.</li>
          <li>La Difesa è la salute di una creatura. Quando una creatura subisce danni, la sua Difesa viene ridotta della quantità di danni subiti.</li>
          <li>Se la Difesa di una creatura raggiunge 0 o meno, viene distrutta e sarà scartata durante la fase di Rifornimento. I danni rimangono tra i turni, riducendo permanentemente la Difesa della creatura.</li>
          <li>Se una creatura viene curata, la sua Difesa aumenta della quantità di cura. La cura non può mai portare la Difesa al di sopra della DEF base della creatura (la sua DEF stampata). Un effetto che aumenta la DEF base di una creatura aumenta la Difesa massima che può avere, ma non la cura.</li>
          <li>Una creatura può avere effetti keyword. Una keyword è una regola con un nome stampata sulla carta.</li>
          <li>Un effetto Evoluzione è un effetto che può essere applicato a qualsiasi creatura, qualunque sia il suo seme. Conferisce alla creatura un effetto extra se la sua condizione di innesco (trigger) è soddisfatta. Un'Evoluzione viene innescata quando la sua condizione di innesco è soddisfatta, oppure quando un effetto stabilisce che viene innescata indipendentemente dalla sua condizione.</li>
        </Ol>

        <h3 className="r-h3">3.3 Carte Magia</h3>
        <Ol>
          <li>Una carta magia ha i seguenti attributi: Nome, Fazione (Resistenza Umana o Potenze Artificiali), Effetto.</li>
          <li>Quando viene pescata una magia, risolvi immediatamente l'effetto della carta. Eventuali effetti innescati vengono aggiunti alla pila e si risolvono prima che venga pescata la carta successiva.</li>
        </Ol>

        <h3 className="r-h3">3.4 Carte Equipaggiamento</h3>
        <Ol>
          <li>Una carta equipaggiamento ha i seguenti attributi: Nome, Fazione (Resistenza Umana o Potenze Artificiali), Effetto.</li>
          <li>Quando viene pescato un equipaggiamento, risolvi immediatamente l'effetto della carta. Eventuali effetti innescati vengono aggiunti alla pila e si risolvono prima che venga pescata la carta successiva.</li>
          <li>Gli effetti dell'equipaggiamento vengono applicati alla carta sostitutiva pescata. Se la carta sostitutiva è una magia o un equipaggiamento, l'effetto viene applicato alla successiva carta sostitutiva pescata.</li>
          <li>Gli effetti degli equipaggiamenti sono additivi con tutti gli altri modificatori. Se due effetti di equipaggiamento impostassero lo stesso valore, l'effetto pescato per ultimo sostituisce quello precedente. Altrimenti, tutti gli effetti di equipaggiamento in sospeso si applicano.</li>
        </Ol>
      </>
    ),
  },
  {
    id: 'pila',
    num: '4',
    title: 'Pila (Stack)',
    body: (
      <Ol>
        <li>La pila è un elenco di effetti in attesa di essere risolti. Gli effetti vengono aggiunti alla pila quando vengono innescati.</li>
        <li>Gli effetti vengono risolti in ordine LIFO (Last-In-First-Out): l'ultimo effetto aggiunto alla pila è il primo a essere risolto.</li>
        <li>Se due effetti vengono innescati contemporaneamente, vengono aggiunti alla pila in ordine di corsia (da 1 a 5), aggiungendo prima gli effetti delle Potenze Artificiali e poi quelli della Resistenza Umana (il che significa che gli effetti della Resistenza Umana si risolveranno per primi).</li>
      </Ol>
    ),
  },
  {
    id: 'setup',
    num: '5',
    title: 'Setup',
    body: (
      <Ol>
        <li>Ogni mazzo viene mescolato.</li>
        <li>Ogni giocatore inizia con [12 000] punti vita.</li>
        <li>Le carte vengono pescate dalla cima del mazzo per riempire ogni corsia con una carta. Ogni giocatore ha 5 corsie, numerate da 1 a 5 da sinistra a destra.</li>
        <li>Durante il setup, solo le carte creatura vengono piazzate nelle corsie. Le carte magia ed equipaggiamento vengono messe da parte senza alcun effetto; verranno rimescolate nel mazzo al termine del setup.</li>
      </Ol>
    ),
  },
  {
    id: 'turno',
    num: '6',
    title: 'Struttura del Turno',
    body: (
      <>
        <p>I turni sono simultanei. Ogni turno consiste nelle seguenti fasi:</p>
        <Ol>
          <li>Valutazione (Evaluate)</li>
          <li>Dichiarazione (Declare)</li>
          <li>Combattimento (Inizio del combattimento, Ciclo 1, Ciclo 2, Fine del combattimento)</li>
          <li>Rifornimento (Refill)</li>
          <li>Fine del turno</li>
        </Ol>
      </>
    ),
  },
  {
    id: 'valutazione',
    num: '7',
    title: 'Fase di Valutazione',
    body: (
      <>
        <Ol>
          <li>Le creature in gioco di ogni giocatore formano una mano di poker usando i ranghi e i semi delle creature.</li>
          <li>L'Asso conta come alto o basso. La scala più bassa è A-2-3-4-5 e la più alta è 10-J-Q-K-A (la scala reale, se dello stesso seme). Le scale non si avvolgono: Q-K-A-2-3 non è una scala.</li>
          <li>Se un giocatore ha meno di 5 creature, usa la migliore mano che quelle creature possono formare. Scale, colori, full, scale colore e five of a kind richiedono 5 creature. Una carta alta (high card) è una mano valida. Un giocatore senza creature non ha mano, il che conta come la mano peggiore.</li>
          <li>Le classifiche delle mani sono le seguenti (dalla migliore alla peggiore): Five of a kind (5 carte dello stesso rango), Scala reale, Scala colore, Poker (4 carte dello stesso rango), Full, Colore, Scala, Tris, Quattro dello stesso seme (Four of a suit), Doppia coppia, Coppia, Carta alta.</li>
          <li>Le mani dello stesso tipo vengono confrontate con le regole standard del poker: prima per i ranghi dei gruppi (ad esempio il rango della coppia, poi i kicker dal più alto al più basso). Una scala viene confrontata per la sua carta più alta (A-2-3-4-5 è una scala con 5 alto). Un "Quattro dello stesso seme" viene confrontato per i ranghi delle sue quattro creature, dal più alto al più basso. Un Five of a kind viene confrontato per il suo rango. I semi non rompono mai i pareggi.</li>
          <li>Un Five of a kind è composto da cinque creature dello stesso rango. Un mazzo ha una creatura per rango e seme, quindi può verificarsi solo quando gli effetti permettono alle creature di contare come un altro rango. Senza tali effetti, la migliore mano con lo stesso rango è un Poker.</li>
          <li>Se la migliore mano di un giocatore corrisponde a più mani di poker, viene usata la mano migliore in classifica.</li>
          <li>Ogni mano di poker ha un moltiplicatore per il danno da traboccamento (spill) e un numero di attaccanti.</li>
        </Ol>
        <Table
          head={['Mano', 'Attaccanti', 'Moltiplicatore']}
          rows={[
            ['Carta alta', 'La creatura con il rango più alto', '[1.000]'],
            ['Coppia', 'Le 2 creature accoppiate', '[1.100]'],
            ['Doppia coppia', 'Le 4 creature accoppiate', '[1.300]'],
            ['Quattro dello stesso seme', 'Le 4 creature corrispondenti', '[1.350]'],
            ['Tris', 'Le 3 creature corrispondenti', '[1.400]'],
            ['Scala', 'Tutte e 5', '[1.600]'],
            ['Colore', 'Tutte e 5', '[1.700]'],
            ['Full', 'Tutte e 5', '[1.800]'],
            ['Poker', 'Le 4 creature corrispondenti', '[2.200]'],
            ['Scala colore', 'Tutte e 5', '[2.600]'],
            ['Scala reale', 'Tutte e 5', '[3.000]'],
            ['Five of a kind', 'Tutte e 5', '[3.500]'],
          ]}
        />
        <Ol from={9}>
          <li>La mano e il moltiplicatore sono fissi per il turno. Se una creatura viene distrutta durante il combattimento, conta comunque come parte della mano per quel turno.</li>
        </Ol>

        <h3 className="r-h3">Sinergie dei semi</h3>
        <p>Durante la fase di Valutazione, le creature di ogni giocatore vengono conteggiate per seme. Un seme con 3, 4 o 5 creature di un giocatore in gioco ha una sinergia. Gli effetti seguenti si applicano alle creature di quel giocatore di quel seme, a meno che il testo non dica diversamente.</p>
        <Ol>
          <li>Gli effetti di sinergia sono cumulativi: con N creature di un seme in gioco, si applica ogni Tier dal 3 fino a N.</li>
          <li>Se un effetto dice "Sostituisce l'effetto del Tier X", l'effetto del Tier X smette di applicarsi mentre il Tier sostitutivo è attivo. Un Tier è attivo quando il suo conteggio di creature è soddisfatto, indipendentemente dal fatto che il suo effetto sia stato usato.</li>
          <li>Il conteggio di ogni seme è fisso per il turno, come la mano di poker. Una creatura distrutta durante il combattimento conta ancora per il Tier fino alla successiva fase di Valutazione. Le condizioni scritte all'interno di un effetto (adiacente, non attaccante, ecc.) vengono controllate in tempo reale, e una creatura distrutta durante il combattimento non soddisfa più tali condizioni.</li>
          <li>
            Definizioni usate dagli effetti:
            <ul className="r-ul">
              <li><b>Adiacente:</b> due creature dello stesso giocatore in corsie i cui numeri differiscono esattamente di 1. Le corsie 1 e 5 non sono adiacenti.</li>
              <li><b>Attaccante:</b> una creatura selezionata dalla mano di poker. Una creatura non attaccante è qualsiasi creatura dello stesso giocatore che non è stata selezionata.</li>
              <li><b>In gioco:</b> in una corsia di uno dei due giocatori, a meno che il testo non dica "tua" o "dell'avversario".</li>
              <li><b>DEF base:</b> la DEF stampata di una creatura.</li>
            </ul>
          </li>
          <li>Tutti i numeri tra parentesi quadre sono segnaposto da impostare in fase di playtesting.</li>
          <li>Gli effetti sono i seguenti:</li>
        </Ol>
        <Table
          head={['Seme', 'Creature', 'Effetto']}
          rows={[
            ['Vincolo', '3', 'Ogni creatura Vincolo guadagna +[300] ATK mentre è adiacente a un\'altra creatura Vincolo.'],
            ['Vincolo', '4', 'Ogni creatura Vincolo concede +[200] ATK a ogni attaccante adiacente, anche se è a sua volta un attaccante.'],
            ['Vincolo', '5', 'Ogni creatura Vincolo guadagna +[500] ATK. Le creature Vincolo sono sempre considerate adiacenti tra loro per gli effetti delle carte.'],
            ['Frattura', '3', 'Un attaccante Frattura guadagna +[300] ATK quando attacca un bersaglio la cui DEF attuale è superiore all\'ATK attuale dell\'attaccante (senza questo bonus). Entrambi i valori vengono presi all\'inizio del ciclo in cui si risolve l\'attacco.'],
            ['Frattura', '4', 'Quando un attaccante Frattura uccide un bersaglio, il moltiplicatore della mano usato per il suo spill viene moltiplicato per [1.2] per quell\'attacco.'],
            ['Frattura', '5', 'Quando un attaccante Frattura uccide un bersaglio, il suo spill tratta la DEF del bersaglio prima del colpo mortale come il [50]% del suo valore. L\'ATK attuale delle creature Frattura non può essere ridotto dagli effetti. Sostituisce l\'effetto del Tier 4.'],
            ['Spira', '3', 'Per ogni Evoluzione innescata in gioco, ogni creatura Spira guadagna +[100] ATK.'],
            ['Spira', '4', 'Ogni valore numerico sugli effetti Evoluzione delle creature Spira viene moltiplicato per [1.2] prima di essere usato.'],
            ['Spira', '5', 'La keyword Evoluzione di ogni creatura Spira viene innescata anche se la sua condizione di innesco non è soddisfatta.'],
            ['Vestigio', '3', 'Quando una delle tue creature Vestigio viene distrutta, la creatura che la sostituisce in quella corsia durante la fase di rifornimento guadagna +[500] ATK fino a quando non distrugge una creatura.'],
            ['Vestigio', '4', 'All\'inizio di ogni fase di rifornimento, ognuna delle tue creature Vestigio cura [700] DEF, fino alla sua DEF base. Le creature distrutte non vengono curate.'],
            ['Vestigio', '5', 'Quando una delle tue creature Vestigio sta per essere distrutta, non viene distrutta e rientra in gioco con la difesa completa. Durante la fase di rifornimento, se la carta è ancora lì, distruggila. Questo effetto viene quindi disattivato fino a quando non torni ad avere meno di 5 creature Vestigio in gioco dopo la fase di rifornimento. Sostituisce l\'effetto del Tier 3.'],
          ]}
        />
      </>
    ),
  },
  {
    id: 'dichiarazione',
    num: '8',
    title: 'Fase di Dichiarazione',
    body: (
      <Ol>
        <li>Ogni creatura selezionata dalla mano di poker dichiara un bersaglio per il suo attacco. Ogni creatura attacca la creatura avversaria nella stessa corsia. Se la corsia opposta è vuota, l'attaccante parte dalla propria corsia e si muove verso l'alto in modo circolare (la corsia 4 controlla 4, 5, 1, 2, 3). La prima corsia che contiene una creatura nemica è il bersaglio.</li>
        <li>I bersagli vengono fissati una volta dichiarati e non vengono cambiati durante il combattimento.</li>
      </Ol>
    ),
  },
  {
    id: 'combattimento',
    num: '9',
    title: 'Fase di Combattimento',
    body: (
      <Ol>
        <li>
          L'Attacco effettivo di una creatura è pari al suo Attacco attuale moltiplicato per il modificatore di vantaggio della sottofazione:
          <ul className="r-ul">
            <li>Se l'attaccante è forte contro il difensore, l'Attacco effettivo viene moltiplicato per 1.1.</li>
            <li>Se l'attaccante è debole contro il difensore, l'Attacco effettivo viene moltiplicato per 0.9.</li>
            <li>Se l'attaccante non è né forte né debole contro il difensore, l'Attacco effettivo è pari al suo Attacco attuale.</li>
          </ul>
        </li>
        <li>Durante il Ciclo 1, ogni attaccante il cui bersaglio è un nemico nella corsia opposta attacca simultaneamente. Una creatura può essere attaccata solo una volta durante questo ciclo.</li>
        <li>Durante il Ciclo 2, ogni attaccante sopravvissuto al Ciclo 1 e che non ha ancora attaccato può attaccare il suo bersaglio. Gli attacchi vengono eseguiti in ordine di numero di corsia, dal più basso al più alto. Se il bersaglio è già stato distrutto, l'attaccante non attacca.</li>
        <li>Se un effetto reindirizza un attacco, l'attacco reindirizzato viene risolto nello stesso ciclo dell'attacco originale. Gli attacchi reindirizzati nel Ciclo 1 vengono risolti dopo che tutti gli attacchi simultanei sono stati risolti. Gli attacchi reindirizzati nel Ciclo 2 vengono risolti quando l'attacco originale sarebbe stato risolto.</li>
        <li>Un attaccante infligge danni pari al suo ATK effettivo al difensore. La DEF del difensore viene ridotta della quantità di danni inflitti.</li>
        <li>Se l'attacco uccide il bersaglio, l'attaccante trabocca (spill) sul giocatore avversario. Il danno da spill è pari a (ATK Effettivo − DEF del bersaglio prima del colpo mortale) moltiplicato per il moltiplicatore della mano, arrotondato per difetto. Le sinergie dei semi possono cambiare il valore DEF usato e il moltiplicatore della mano per un dato attacco (vedi 7.10). I modificatori al moltiplicatore della mano sono moltiplicativi e vengono applicati in per-mille.</li>
        <li>Se due creature si uccidono a vicenda nel Ciclo 1, non viene inflitto alcun danno da spill a nessuno dei due giocatori. Il danno da spill viene inflitto normalmente nel Ciclo 2.</li>
      </Ol>
    ),
  },
  {
    id: 'rifornimento',
    num: '10',
    title: 'Fase di Rifornimento',
    body: (
      <Ol>
        <li>All'inizio della fase di Rifornimento, gli effetti che si innescano all'inizio della fase di rifornimento si risolvono per primi, prima che le creature distrutte vengano scartate. Le creature distrutte non sono influenzate da questi effetti.</li>
        <li>Tutte le creature distrutte vanno nella pila degli scarti del loro proprietario. Una creatura salvata dall'effetto Vestigio Tier 5 che è ancora nella sua corsia viene distrutta ora e va nella pila degli scarti.</li>
        <li>Ogni giocatore pesca i sostituti per le proprie corsie vuote in ordine di corsia, da 1 a 5. Le Potenze Artificiali pescano per prime, poi la Resistenza Umana. Se un giocatore non ha più carte nel suo mazzo, non pesca un sostituto per quella corsia.</li>
        <li>Un sostituto pescato come creatura viene piazzato nella corsia vuota.</li>
        <li>Una magia pescata ha effetto immediatamente, poi il giocatore pesca un'altra carta.</li>
        <li>Un equipaggiamento pescato si applica immediatamente alla successiva creatura pescata in questa fase, poi il giocatore pesca un'altra carta.</li>
        <li>Le carte magia ed equipaggiamento vengono scartate dopo l'uso.</li>
        <li>Se il mazzo è vuoto, le corsie vuote rimangono vuote e il giocatore continua con meno creature. Un equipaggiamento in sospeso senza una creatura che lo riceva viene perso.</li>
        <li>Se un effetto uccide una creatura durante il Rifornimento, quella corsia viene rifornita prima che la fase termini, sempre in ordine di corsia.</li>
        <li>Alla fine della fase di Rifornimento, vengono controllati gli effetti che dipendono dal numero di creature in gioco dopo la fase di rifornimento (ad esempio la riattivazione dell'effetto Vestigio Tier 5).</li>
      </Ol>
    ),
  },
  {
    id: 'fine',
    num: '11',
    title: 'Fase di Fine Turno',
    body: (
      <Ol>
        <li>Se un giocatore non ha creature in gioco, perde la partita.</li>
        <li>Se la vita di un giocatore viene ridotta a 0 o meno, perde la partita.</li>
        <li>Se entrambi i giocatori perdono contemporaneamente, la partita è un pareggio.</li>
        <li>Alla fine del turno 50 vince il giocatore con più vita. In caso di pareggio, viene usata la migliore mano di poker per rompere il pareggio. Se il pareggio persiste, la partita è un pareggio.</li>
      </Ol>
    ),
  },
  {
    id: 'tecniche',
    num: '12',
    title: 'Regole Tecniche',
    body: (
      <Ol>
        <li>Tutti i calcoli usano matematica intera o a virgola fissa. Le frazioni vengono arrotondate per difetto.</li>
        <li>Ogni partita registra la versione del regolamento, le versioni del pool di carte, la versione del motore (engine version), il seed di mescolamento e il nome del PRNG e dell'algoritmo di shuffle usati, con un ordine fisso di pesca casuale (mescola il mazzo del giocatore 1, poi quello del giocatore 2, poi eventuali rimescolamenti di setup nello stesso ordine), in modo che possa essere riprodotta esattamente.</li>
        <li>Ogni ordine di risoluzione nel regolamento è fisso (ordine di corsia, o simultaneo), mai casuale.</li>
      </Ol>
    ),
  },
]

// Colonna di destra: riferimento rapido.
export const pokerHands: [string, string][] = [
  ['Five of a kind', '[3.500]'],
  ['Scala reale', '[3.000]'],
  ['Scala colore', '[2.600]'],
  ['Poker', '[2.200]'],
  ['Full', '[1.800]'],
  ['Colore', '[1.700]'],
  ['Scala', '[1.600]'],
  ['Tris', '[1.400]'],
  ['Quattro dello stesso seme', '[1.350]'],
  ['Doppia coppia', '[1.300]'],
  ['Coppia', '[1.100]'],
  ['Carta alta', '[1.000]'],
]

export const advantage: [string, string][] = [
  ['Forte contro il difensore', '× 1.1'],
  ['Debole contro il difensore', '× 0.9'],
]

export const matchFacts: [string, string][] = [
  ['Punti vita iniziali', '[12 000]'],
  ['Corsie per giocatore', '5'],
  ['Turni massimi', '50'],
  ['Carte nel mazzo', '32'],
]
