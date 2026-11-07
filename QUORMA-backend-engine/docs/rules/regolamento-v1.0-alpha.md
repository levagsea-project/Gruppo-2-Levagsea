
# Regolamento Base (v1.0-alpha)

## 0. Regole d'Oro

0. Il gioco non prevede decisioni da parte dei giocatori durante la partita. Il gioco è completamente deterministico e si svolgerà sempre allo stesso modo, a parità di mazzi e di seed (seme) casuale.
1. Se il testo di una carta contraddice il regolamento, il testo della carta ha la precedenza.
2. Quando vengono applicati più modificatori, quelli additivi vengono applicati per primi, poi quelli moltiplicativi in per-mille, infine il numero viene arrotondato per difetto all'intero più vicino. Se un effetto deve impostare un valore, viene applicato dopo tutti i modificatori. Se più effetti impostassero lo stesso valore, l'effetto che si verifica per ultimo sostituisce quello precedente.

## 1. Obiettivi

Ridurre la vita del proprio avversario a 0 o meno, oppure lasciarlo senza creature.

## 2. Fazioni

1. Ci sono due fazioni nel gioco: la Resistenza Umana e le Potenze Artificiali. Ogni fazione ha il proprio pool unico di carte.
2. Ogni fazione è divisa in sottofazioni. Ogni carta creatura appartiene a una sottofazione. Le sottofazioni sono le seguenti:
    - Resistenza Umana:
        - Adattati
        - Spezzasigilli
        - Portanome
        - Segnaterra
    - Potenze Artificiali:
        - SEKTAC / Senzaeco
        - MEROZYN / Franti
        - EFFRIX / Malscritti
        - YLMOR / Incompiuti
3. Ogni carta creatura appartiene a un seme. I semi sono condivisi tra le fazioni. Ci sono quattro semi nel gioco:
    - Vincolo
    - Frattura
    - Spira
    - Vestigio
4. Ogni sottofazione è forte contro una sottofazione e debole contro un'altra. Le relazioni sono reciproche e formano due cicli tematici: il Ciclo del Mondo e il Ciclo del Sé. Le relazioni sono le seguenti:

| Sottofazione | Forte Contro | Debole Contro |
|---|---|---|
| Adattati | MEROZYN | EFFRIX |
| Spezzasigilli | SEKTAC | YLMOR |
| Portanome | EFFRIX | MEROZYN |
| Segnaterra | YLMOR | SEKTAC |
| SEKTAC | Segnaterra | Spezzasigilli |
| MEROZYN | Portanome | Adattati |
| EFFRIX | Adattati | Portanome |
| YLMOR | Spezzasigilli | Segnaterra |

## 3. Carte

### 3.1 Mazzo

1. Un mazzo ha esattamente 32 carte.
2. Un mazzo è costruito da esattamente un pool di una fazione principale.
3. A seconda della fazione principale, un mazzo deve rispettare i seguenti limiti:

| Fazione Principale | Creature | Magie/Equipaggiamenti |
|---|---|---|
| Resistenza Umana | 20-23 | 9-12 |
| Potenze Artificiali | 24-27 | 5-8 |

4. Un mazzo contiene al massimo una carta creatura per ogni combinazione di rango e seme.
5. Un mazzo può contenere solo una singola copia di ogni carta magia o equipaggiamento.

### 3.2 Carte Creatura

1. Una carta creatura ha i seguenti attributi:
    - Nome
    - Fazione (Resistenza Umana o Potenze Artificiali)
    - Sottofazione
    - Rango (Asso, 2, 3, 4, 5, 6, 7, 8, 9, 10, Jack, Queen, King)
    - Seme (Vincolo, Frattura, Spira, Vestigio)
    - Attacco (ATK)
    - Difesa (DEF)
    - Abilità Speciale (se presente)
2. Ogni carta ha un livello di rarità. Il Tier (livello) della carta è determinato dal suo rango:
    - 2-6: Tier 1
    - 7-10: Tier 2
    - Jack, Queen, King: Tier 3
    - Asso: Tier 4
    Il Tier non ha effetto sulle statistiche o sulla forza della carta, ma le carte di Tier più alto hanno un impatto maggiore sul gameplay e hanno più probabilità di avere abilità speciali.
3. L'Attacco base non cambia mai. L'Attacco attuale di una creatura è pari al suo Attacco base più eventuali modificatori, nell'ordine indicato dalle Regole d'Oro.
4. L'Attacco non può mai scendere sotto 0. Se l'Attacco di una creatura viene ridotto a 0, può comunque attaccare ma non infliggerà alcun danno.
5. La Difesa è la salute di una creatura. Quando una creatura subisce danni, la sua Difesa viene ridotta della quantità di danni subiti.
6. Se la Difesa di una creatura raggiunge 0 o meno, viene distrutta e sarà scartata durante la fase di Rifornimento. I danni rimangono tra i turni, riducendo permanentemente la Difesa della creatura.
7. Se una creatura viene curata, la sua Difesa aumenta della quantità di cura. La cura non può mai portare la Difesa al di sopra della DEF base della creatura (la sua DEF stampata). Un effetto che aumenta la DEF base di una creatura aumenta la Difesa massima che può avere, ma non la cura.
8. Una creatura può avere effetti keyword. Una keyword è una regola con un nome stampata sulla carta.
9. Un effetto Evoluzione è un effetto che può essere applicato a qualsiasi creatura, qualunque sia il suo seme. Conferisce alla creatura un effetto extra se la sua condizione di innesco (trigger) è soddisfatta. Un'Evoluzione viene innescata quando la sua condizione di innesco è soddisfatta, oppure quando un effetto stabilisce che viene innescata indipendentemente dalla sua condizione.

### 3.3 Carte Magia

1. Una carta magia ha i seguenti attributi:
    - Nome
    - Fazione (Resistenza Umana o Potenze Artificiali)
    - Effetto
2. Quando viene pescata una magia, risolvi immediatamente l'effetto della carta. Eventuali effetti innescati vengono aggiunti alla pila e si risolvono prima che venga pescata la carta successiva.

### 3.4 Carte Equipaggiamento

1. Una carta equipaggiamento ha i seguenti attributi:
    - Nome
    - Fazione (Resistenza Umana o Potenze Artificiali)
    - Effetto
2. Quando viene pescato un equipaggiamento, risolvi immediatamente l'effetto della carta. Eventuali effetti innescati vengono aggiunti alla pila e si risolvono prima che venga pescata la carta successiva.
3. Gli effetti dell'equipaggiamento vengono applicati alla carta sostitutiva pescata. Se la carta sostitutiva è una magia o un equipaggiamento, l'effetto viene applicato alla successiva carta sostitutiva pescata.
4. Gli effetti degli equipaggiamenti sono additivi con tutti gli altri modificatori. Se due effetti di equipaggiamento impostassero lo stesso valore, l'effetto pescato per ultimo sostituisce quello precedente. Altrimenti, tutti gli effetti di equipaggiamento in sospeso si applicano.

## 4. Pila (Stack)

1. La pila è un elenco di effetti in attesa di essere risolti. Gli effetti vengono aggiunti alla pila quando vengono innescati.
2. Gli effetti vengono risolti in ordine LIFO (Last-In-First-Out): l'ultimo effetto aggiunto alla pila è il primo a essere risolto.
3. Se due effetti vengono innescati contemporaneamente, vengono aggiunti alla pila in ordine di corsia (da 1 a 5), aggiungendo prima gli effetti delle Potenze Artificiali e poi quelli della Resistenza Umana (il che significa che gli effetti della Resistenza Umana si risolveranno per primi).

## 5. Setup

1. Ogni mazzo viene mescolato.
2. Ogni giocatore inizia con [12 000] punti vita.
3. Le carte vengono pescate dalla cima del mazzo per riempire ogni corsia con una carta. Ogni giocatore ha 5 corsie, numerate da 1 a 5 da sinistra a destra.
4. Durante il setup, solo le carte creatura vengono piazzate nelle corsie. Le carte magia ed equipaggiamento vengono messe da parte senza alcun effetto; verranno rimescolate nel mazzo al termine del setup.

## 6. Struttura del Turno

1. I turni sono simultanei. Ogni turno consiste nelle seguenti fasi:
    1. Valutazione (Evaluate)
    2. Dichiarazione (Declare)
    3. Combattimento (Inizio del combattimento, Ciclo 1, Ciclo 2, Fine del combattimento)
    4. Rifornimento (Refill)
    5. Fine del turno

## 7. Fase di Valutazione

1. Le creature in gioco di ogni giocatore formano una mano di poker usando i ranghi e i semi delle creature.
2. L'Asso conta come alto o basso. La scala più bassa è A-2-3-4-5 e la più alta è 10-J-Q-K-A (la scala reale, se dello stesso seme). Le scale non si avvolgono: Q-K-A-2-3 non è una scala.
3. Se un giocatore ha meno di 5 creature, usa la migliore mano che quelle creature possono formare. Scale, colori, full, scale colore e five of a kind richiedono 5 creature. Una carta alta (high card) è una mano valida. Un giocatore senza creature non ha mano, il che conta come la mano peggiore.
4. Le classifiche delle mani sono le seguenti (dalla migliore alla peggiore):
    1. Five of a kind (5 carte dello stesso rango)
    2. Scala reale
    3. Scala colore
    4. Poker (4 carte dello stesso rango)
    5. Full
    6. Colore
    7. Scala
    8. Tris
    9. Quattro dello stesso seme (Four of a suit)
    10. Doppia coppia
    11. Coppia
    12. Carta alta
5. Le mani dello stesso tipo vengono confrontate con le regole standard del poker: prima per i ranghi dei gruppi (ad esempio il rango della coppia, poi i kicker dal più alto al più basso). Una scala viene confrontata per la sua carta più alta (A-2-3-4-5 è una scala con 5 alto). Un "Quattro dello stesso seme" viene confrontato per i ranghi delle sue quattro creature, dal più alto al più basso. Un Five of a kind viene confrontato per il suo rango. I semi non rompono mai i pareggi.
6. Un Five of a kind è composto da cinque creature dello stesso rango. Un mazzo ha una creatura per rango e seme, quindi può verificarsi solo quando gli effetti permettono alle creature di contare come un altro rango. Senza tali effetti, la migliore mano con lo stesso rango è un Poker.
7. Se la migliore mano di un giocatore corrisponde a più mani di poker, viene usata la mano migliore in classifica.
8. Ogni mano di poker ha un moltiplicatore per il danno da traboccamento (spill) e un numero di attaccanti.

| Mano | Attaccanti | Moltiplicatore |
|---|---|---|
| Carta alta | La creatura con il rango più alto | [1.000] |
| Coppia | Le 2 creature accoppiate | [1.100] |
| Doppia coppia | Le 4 creature accoppiate | [1.300] |
| Quattro dello stesso seme | Le 4 creature corrispondenti | [1.350] |
| Tris | Le 3 creature corrispondenti | [1.400] |
| Scala | Tutte e 5 | [1.600] |
| Colore | Tutte e 5 | [1.700] |
| Full | Tutte e 5 | [1.800] |
| Poker | Le 4 creature corrispondenti | [2.200] |
| Scala colore | Tutte e 5 | [2.600] |
| Scala reale | Tutte e 5 | [3.000] |
| Five of a kind | Tutte e 5 | [3.500] |

9. La mano e il moltiplicatore sono fissi per il turno. Se una creatura viene distrutta durante il combattimento, conta comunque come parte della mano per quel turno.
10. Sinergie dei semi. Durante la fase di Valutazione, le creature di ogni giocatore vengono conteggiate per seme. Un seme con 3, 4 o 5 creature di un giocatore in gioco ha una sinergia. Gli effetti seguenti si applicano alle creature di quel giocatore di quel seme, a meno che il testo non dica diversamente.
    1. Gli effetti di sinergia sono cumulativi: con N creature di un seme in gioco, si applica ogni Tier dal 3 fino a N.
    2. Se un effetto dice "Sostituisce l'effetto del Tier X", l'effetto del Tier X smette di applicarsi mentre il Tier sostitutivo è attivo. Un Tier è attivo quando il suo conteggio di creature è soddisfatto, indipendentemente dal fatto che il suo effetto sia stato usato.
    3. Il conteggio di ogni seme è fisso per il turno, come la mano di poker. Una creatura distrutta durante il combattimento conta ancora per il Tier fino alla successiva fase di Valutazione. Le condizioni scritte all'interno di un effetto (adiacente, non attaccante, ecc.) vengono controllate in tempo reale, e una creatura distrutta durante il combattimento non soddisfa più tali condizioni.
    4. Definizioni usate dagli effetti:
        - Adiacente: due creature dello stesso giocatore in corsie i cui numeri differiscono esattamente di 1. Le corsie 1 e 5 non sono adiacenti.
        - Attaccante: una creatura selezionata dalla mano di poker. Una creatura non attaccante è qualsiasi creatura dello stesso giocatore che non è stata selezionata.
        - In gioco: in una corsia di uno dei due giocatori, a meno che il testo non dica "tua" o "dell'avversario".
        - DEF base: la DEF stampata di una creatura.
    5. Tutti i numeri tra parentesi quadre sono segnaposto da impostare in fase di playtesting.
    6. Gli effetti sono i seguenti:

| Seme | Creature | Effetto |
|---|---|---|
| Vincolo | 3 | Ogni creatura Vincolo guadagna +[600] ATK mentre è adiacente a un'altra creatura Vincolo. |
| Vincolo | 4 | Ogni creatura Vincolo concede +[400] ATK a ogni attaccante adiacente, anche se è a sua volta un attaccante. |
| Vincolo | 5 | Ogni creatura Vincolo guadagna +[1000] ATK. Le creature Vincolo sono sempre considerate adiacenti tra loro per gli effetti delle carte. |
| Frattura | 3 | Un attaccante Frattura guadagna +[1200] ATK quando attacca un bersaglio la cui DEF attuale è superiore all'ATK attuale dell'attaccante (senza questo bonus). Entrambi i valori vengono presi all'inizio del ciclo in cui si risolve l'attacco. |
| Frattura | 4 | Quando un attaccante Frattura uccide un bersaglio, il moltiplicatore della mano usato per il suo spill viene moltiplicato per [1.2] per quell'attacco e il suo spill tratta la DEF del bersaglio prima del colpo mortale come l'[80]% del suo valore. |
| Frattura | 5 | Quando un attaccante Frattura uccide un bersaglio, il moltiplicatore della mano usato per il suo spill viene moltiplicato per [1.2] per quell'attacco e il suo spill tratta la DEF del bersaglio prima del colpo mortale come il [50]% del suo valore. L'ATK attuale delle creature Frattura non può essere ridotto dagli effetti. Sostituisce l'effetto del Tier 4. |
| Spira | 3 | Per ogni Evoluzione innescata su una tua creatura, ogni tua creatura Spira guadagna +[200] ATK. |
| Spira | 4 | Ogni valore numerico sugli effetti Evoluzione delle creature Spira viene moltiplicato per [1.2] prima di essere usato. |
| Spira | 5 | La keyword Evoluzione di ogni creatura Spira viene innescata anche se la sua condizione di innesco non è soddisfatta. |
| Vestigio | 3 | Quando una delle tue creature Vestigio viene distrutta, la creatura (di qualsiasi seme) che la sostituisce in quella corsia durante la fase di rifornimento guadagna +[1000] ATK fino a quando non distrugge una creatura. |
| Vestigio | 4 | All'inizio di ogni fase di rifornimento, ognuna delle tue creature Vestigio cura [1400] DEF, fino alla sua DEF base. Le creature distrutte non vengono curate. |
| Vestigio | 5 | Quando una delle tue creature Vestigio sta per essere distrutta, non viene distrutta e rientra in gioco con la difesa completa. Durante la fase di rifornimento, se la carta è ancora lì, distruggila. Questo effetto viene quindi disattivato fino a quando non torni ad avere meno di 5 creature Vestigio in gioco dopo la fase di rifornimento. Sostituisce l'effetto del Tier 3. |

## 8. Fase di Dichiarazione

1. Ogni creatura selezionata dalla mano di poker dichiara un bersaglio per il suo attacco. Ogni creatura attacca la creatura avversaria nella stessa corsia. Se la corsia opposta è vuota, l'attaccante parte dalla propria corsia e si muove verso l'alto in modo circolare (la corsia 4 controlla 4, 5, 1, 2, 3). La prima corsia che contiene una creatura nemica è il bersaglio.
2. I bersagli vengono fissati una volta dichiarati e non vengono cambiati durante il combattimento.

## 9. Fase di Combattimento

1. L'Attacco effettivo di una creatura è pari al suo Attacco attuale moltiplicato per il modificatore di vantaggio della sottofazione:
    - Se l'attaccante è forte contro il difensore, l'Attacco effettivo viene moltiplicato per 1.1.
    - Se l'attaccante è debole contro il difensore, l'Attacco effettivo viene moltiplicato per 0.9.
    - Se l'attaccante non è né forte né debole contro il difensore, l'Attacco effettivo è pari al suo Attacco attuale.
2. Durante il Ciclo 1, ogni attaccante il cui bersaglio è un nemico nella corsia opposta attacca simultaneamente. Una creatura può essere attaccata solo una volta durante questo ciclo.
3. Durante il Ciclo 2, ogni attaccante sopravvissuto al Ciclo 1 e che non ha ancora attaccato può attaccare il suo bersaglio. Gli attacchi vengono eseguiti in ordine di numero di corsia, dal più basso al più alto. Se il bersaglio è già stato distrutto, l'attaccante non attacca.
4. Se un effetto reindirizza un attacco, l'attacco reindirizzato viene risolto nello stesso ciclo dell'attacco originale. Gli attacchi reindirizzati nel Ciclo 1 vengono risolti dopo che tutti gli attacchi simultanei sono stati risolti. Gli attacchi reindirizzati nel Ciclo 2 vengono risolti quando l'attacco originale sarebbe stato risolto.
5. Un attaccante infligge danni pari al suo ATK effettivo al difensore. La DEF del difensore viene ridotta della quantità di danni inflitti.
6. Se l'attacco uccide il bersaglio, l'attaccante trabocca (spill) sul giocatore avversario. Il danno da spill è pari a (ATK Effettivo − DEF del bersaglio prima del colpo mortale), con minimo 0, moltiplicato per il moltiplicatore della mano, arrotondato per difetto. Le sinergie dei semi possono cambiare il valore DEF usato e il moltiplicatore della mano per un dato attacco (vedi 7.10). I modificatori al moltiplicatore della mano sono moltiplicativi e vengono applicati in per-mille.
7. Se due creature si uccidono a vicenda nel Ciclo 1, non viene inflitto alcun danno da spill a nessuno dei due giocatori. Il danno da spill viene inflitto normalmente nel Ciclo 2.

## 10. Fase di Rifornimento

1. All'inizio della fase di Rifornimento, gli effetti che si innescano all'inizio della fase di rifornimento si risolvono per primi, prima che le creature distrutte vengano scartate. Le creature distrutte non sono influenzate da questi effetti.
2. Tutte le creature distrutte vanno nella pila degli scarti del loro proprietario. Una creatura salvata dall'effetto Vestigio Tier 5 che è ancora nella sua corsia viene distrutta ora e va nella pila degli scarti.
3. Ogni giocatore pesca i sostituti per le proprie corsie vuote in ordine di corsia, da 1 a 5. Le Potenze Artificiali pescano per prime, poi la Resistenza Umana. Se un giocatore non ha più carte nel suo mazzo, non pesca un sostituto per quella corsia.
4. Un sostituto pescato come creatura viene piazzato nella corsia vuota.
5. Una magia pescata ha effetto immediatamente, poi il giocatore pesca un'altra carta.
6. Un equipaggiamento pescato si applica immediatamente alla successiva creatura pescata in questa fase, poi il giocatore pesca un'altra carta.
7. Le carte magia ed equipaggiamento vengono scartate dopo l'uso.
8. Se il mazzo è vuoto, le corsie vuote rimangono vuote e il giocatore continua con meno creature. Un equipaggiamento in sospeso senza una creatura che lo riceva viene perso.
9. Se un effetto uccide una creatura durante il Rifornimento, quella corsia viene rifornita prima che la fase termini, sempre in ordine di corsia.
10. Alla fine della fase di Rifornimento, vengono controllati gli effetti che dipendono dal numero di creature in gioco dopo la fase di rifornimento (ad esempio la riattivazione dell'effetto Vestigio Tier 5).

## 11. Fase di Fine Turno

1. Se un giocatore non ha creature in gioco, perde la partita.
2. Se la vita di un giocatore viene ridotta a 0 o meno, perde la partita.
3. Se entrambi i giocatori perdono contemporaneamente, la partita è un pareggio.
4. Alla fine del turno 50 vince il giocatore con più vita. In caso di pareggio, vince il giocatore con la mano di poker migliore nell'ultima fase di Valutazione. Se il pareggio persiste, la partita è un pareggio.

## 12. Regole Tecniche

1. Tutti i calcoli usano matematica intera o a virgola fissa. Le frazioni vengono arrotondate per difetto.
2. Ogni partita registra la versione del regolamento, le versioni del pool di carte, la versione del motore (engine version), il seed di mescolamento e il nome del PRNG e dell'algoritmo di shuffle usati, con un ordine fisso di pesca casuale (mescola il mazzo del giocatore 1, poi quello del giocatore 2, poi eventuali rimescolamenti di setup nello stesso ordine), in modo che possa essere riprodotta esattamente.
3. Ogni ordine di risoluzione nel regolamento è fisso (ordine di corsia, o simultaneo), mai casuale.