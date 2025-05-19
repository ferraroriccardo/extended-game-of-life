# Extended Persistence-Based Conway’s Game of Life

---

## Panoramica

Il sistema modella una versione estesa del [Game of Life (GOL)](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life) come una serie di configurazioni di cellule in evoluzione. Il progetto fornisce un’implementazione base del GOL, progettata per essere estesa.

# Cellule

Nel gioco base fornito, le cellule seguono le regole standard del GOL. L’implementazione estesa arricchisce questo modello introducendo tipi specializzati di cellule con comportamenti metabolici distinti. Alcune cellule possono ritardare la morte in scenari di sotto- o sovrappopolazione, sopravvivendo a più generazioni sotto stress. Altre cellule tollerano condizioni di vicinato differenti, prosperando in isolamento o in ambienti affollati. Inoltre, le cellule scambiano energia con i vicini, rappresentando punti vita o risorse che influenzano la loro evoluzione.

# Board

La board base consiste in una griglia che contiene un insieme di tile, ciascuna delle quali ospita una cellula. Nella versione estesa, la board è composta da tile interattive, in grado di assorbire o donare energia alle cellule. Queste interazioni possono influenzare il comportamento delle cellule, ed è necessario effettuare analisi per valutare le proprietà e gli stati evolutivi della board.

# Game

Nella versione base, il gioco simula l’evoluzione standard del GOL, gestendo le interazioni tra cellule in base allo stato dei vicini a partire da una configurazione iniziale. Ogni tile contiene una cellula che rimane la stessa per tutta la simulazione, alternando il proprio stato tra viva e morta. L’implementazione estesa introduce eventi globali che influenzano tutte le tile e le cellule della board in generazioni specifiche. Questi eventi, che rappresentano scenari differenti, alterano in modo significativo lo stato della board e le dinamiche cellulari.

# Persistenza

Lo stato del GOL esteso deve essere memorizzato in modo persistente e recuperabile utilizzando `JPA`/`Hibernate` con un database `H2` in-memory. Le entità vengono salvate e possono essere ricaricate successivamente, permettendo l’ispezione o la ripresa della simulazione in una generazione qualsiasi.

---

## Requisiti Dettagliati

Il progetto Extended GOL fornisce fin dall’inizio un’implementazione base del GOL, che deve essere estesa.

### Codice fornito

Classi del GOL:

- `ExtendedGameOfLife`: classe facade con metodi richiesti per i test implementati come stub.
- `Cell`: rappresenta una cellula in una generazione.
- `Tile`: rappresenta una tile sulla board.
- `Board`: rappresenta una board statica semplice.
- `Game`: definisce il contesto della simulazione.
- `Generation`: rappresentazione dello stato del gioco.
- `Interactable` e `Evolvable`: interfacce che alcune entità devono implementare.
- `CellType`, `CellMood` e `EventType`: enum che includono rispettivamente i tipi previsti di cellule, umori e eventi nel gioco esteso.
- `Coord`: oggetto valore `JPA` embeddable e hashable che incapsula coordinate intere `(x, y)`, con override corretti di `equals()` e `hashCode()` per l’uso come chiave in mappe e set.
- `ExtendedGameOfLife`: classe di eccezione personalizzata per il GOL esteso.

- Persistenza:

  - `JPAUtil`: utility singleton per gestire l’`EntityManagerFactory`.
  - `GenericExtGOLRepository`: repository generico da implementare per ciascuna entità.

- Configurazione:
  - `persistence.xml`: configura Hibernate con database `H2` in-memory.
  - `pom.xml`: include le dipendenze per `Hibernate ORM`, `JPA`, `H2` e `JUnit 4`.

---

## R1 Cellule

### Comportamento Base

Nel gioco base, le cellule seguono le regole classiche del GOL:

- **Sopravvivenza:** una cellula viva con due o tre vicine vive sopravvive alla generazione successiva.
- **Morte per sottopopolazione:** una cellula viva con meno di due vicine vive muore.
- **Morte per sovrappopolazione:** una cellula viva con più di tre vicine vive muore.
- **Rinascita:** una cellula morta torna viva quando ha esattamente tre vicine vive.

La board è considerata finita: le cellule agli angoli hanno 3 vicini, quelle sui bordi ne hanno 5, quelle centrali 8.

Per modellare le interazioni con altre cellule e con l’ambiente, le cellule devono implementare l’interfaccia `Evolvable`. Il progetto fornisce la versione base delle cellule tramite la classe `Cell`, che implementa `Evolvable` e contiene un override del metodo `evolve()` secondo le regole classiche del GOL.

### Comportamenti Estesi

#### Tipi di Cellule Specializzate

Ogni cellula ha un attributo `lifePoints`, che rappresenta il suo livello energetico. La classe base `Cell` ha questo attributo impostato al valore predefinito `0`.  
Il GOL esteso implementa tre diversi tipi di cellule come sottoclassi di `Cell`:

- `Highlander`: può sopravvivere per tre generazioni anche in condizioni che normalmente causerebbero la morte secondo le regole del GOL.
- `Loner`: prospera in isolamento, abbassando la soglia minima di sopravvivenza a **1 sola** vicina.
- `Social`: alza la soglia massima di sopravvivenza fino a **8** vicine.

Tutte le cellule hanno un attributo `cellType`, e le cellule base sono marcate come `BASIC`.

A ogni generazione, il metodo `evolve()` aggiorna i `lifePoints` della cellula in base al vicinato e alle interazioni:

- In caso di morte, diminuisce di 1
- In caso di sopravvivenza, aumenta di 1
- In caso di rinascita, viene azzerato a 0

Oltre al rispetto delle regole base del GOL, una cellula deve avere `lifePoints` maggiori o uguali a 0 per essere considerata viva. Tuttavia, le cellule che si trovano in condizioni di morte secondo il GOL muoiono comunque anche se hanno `lifePoints` positivi. Le cellule morte non aggiornano i propri punti vita.

#### Vampiri e guaritori

Ogni cellula può trovarsi in uno dei seguenti `mood`: `NAIVE`, `HEALER` oppure `VAMPIRE`. Quando due cellule interagiscono (implementando `Interactable`), il risultato dipende dai rispettivi `mood`:

- `HEALER` + `NAIVE`: il `HEALER` dona 1 `lifePoint` al `NAIVE`
- `HEALER` + `HEALER`: nessun effetto
- `HEALER` + `VAMPIRE`: il `VAMPIRE` assorbe 1 `lifePoint` dal `HEALER`
- `VAMPIRE` + `VAMPIRE`: nessun effetto
- `VAMPIRE` + `NAIVE`: il `VAMPIRE` assorbe 1 `lifePoint` dal `NAIVE` e lo trasforma in un altro `VAMPIRE`
- `NAIVE` + `NAIVE`: nessun effetto

Tutte le cellule hanno un attributo `cellMood`. Il `mood` può cambiare più volte per una stessa cellula, in base alle interazioni con altre cellule sulla board e agli eventi che si verificano durante il `Game`.

#### Persistenza

Tutte le classi derivate da `Cell`, inclusa la classe base, devono essere annotate come entità `JPA`, in modo che l'intera gerarchia possa essere salvata e ricaricata tramite i repository `JPA`.  
La classe generica `GenericExtGOLRepository<E,I>` deve essere implementata parametrizzandola per `Cell`, al fine di fornire le operazioni CRUD e un metodo `load(...)` che consenta di memorizzare e recuperare lo stato persistente. Esempio:

```java
public class CellRepository  extends GenericExtGOLRepository<Cell, Long> {
    public CellRepository()  { super(Cell.class);  }
}
```

Ogni repository deve implementare `load(...)` (e eventuali query personalizzate) affinché lo stato completo della board e del game sia salvabile e ricaricabile tramite `JPA`.

---

## R2 Board

### Comportamenti Base

La board è una griglia di dimensioni fisse (`M×N`), composta da `M*N` oggetti `Tile`, ognuno con coordinate `x` e `y`, contenente una singola `Cell`.

### Comportamenti Estesi

#### Tile Interattive

Ogni `Tile` ha un attributo `lifePointModifier`, con valore predefinito `0`, e implementa l’interfaccia `Interactable`. Le `Tile` possono modificare i `lifePoints` della cellula contenuta, in base al proprio valore:

- Se `> 0`: aggiunge quel valore ai `lifePoints` della cellula
- Se `< 0`: lo sottrae
- Se `0`: nessun effetto

Questa interazione avviene all’inizio di ogni generazione.

#### Visualizzazione

La board supporta una visualizzazione testuale tramite il metodo `visualize()`. Ogni `Tile` che ospita una cellula morta viene rappresentata con `0`. Le cellule vive vengono rappresentate così:

- `Cell`: `C`
- `Highlander`: `H`
- `Loner`: `L`
- `Social`: `S`

Esempio:

```
0C00H
L00CS
0H000
C0000
```

### Metodi Analitici

| Signature metodo                                                                 | Descrizione                                                                                                    |
| -------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| `public Integer countCells(Generation gen)`                                      | Restituisce il numero **totale** di cellule vive nella generazione `gen`.                                      |
| `public Cell getHighestEnergyCell(Generation gen)`                               | Restituisce la cellula con il `lifePoints` più alto. In caso di parità, sceglie quella più in alto a sinistra. |
| `public Map<Integer, List<Cell>> getCellsByEnergyLevel(Generation gen)`          | Raggruppa le **cellule vive** in base al loro `lifePoints` corrente.                                           |
| `public Map<CellType, Integer> countCellsByType(Generation gen)`                 | Conta le cellule vive per ciascun `CellType`.                                                                  |
| `public List<Cell> topEnergyCells(Generation gen, int n)`                        | Restituisce le **prime `n`** cellule ordinate per `lifePoints` in ordine decrescente.                          |
| `public Map<Integer, List<Cell>> groupByAliveNeighborCount(Generation gen)`      | Raggruppa le cellule in base al numero di vicine vive.                                                         |
| `public IntSummaryStatistics energyStatistics(Generation gen)`                   | Calcola statistiche riassuntive (`count`, `min`, `max`, `sum`, `average`) sui `lifePoints`.                    |
| `public Map<Integer, IntSummaryStatistics> getTimeSeriesStats(int from, int to)` | Restituisce una **serie temporale** delle statistiche energetiche per ogni generazione da `from` a `to`.       |

#### Persistenza

La configurazione completa della board deve essere persistente: tutte le entità devono essere annotate con ID e mapping JPA appropriati.

La classe `GenericExtGOLRepository<E,I>` deve essere parametrizzata per `Board`, ad esempio:

```java
public class BoardRepository  extends GenericExtGOLRepository<Board, Long> {
    public BoardRepository()  { super(Board.class);  }
}
```

Ogni repository deve implementare `load(...)` (e eventuali query personalizzate) in modo che lo stato completo della board e del gioco sia salvabile e ricaricabile.

---

## R3 Game

### Comportamenti Base

La classe `Game` gestisce l’evoluzione della `Board` attraverso le `Generation`, applicando le regole standard del GOL e i comportamenti base delle cellule.

L’esecuzione avviene così:

1. **Rilevamento vicini**: inizializzazione dei vicini di ogni cellula sulla board
2. **Valutazione del vicinato**: ogni cellula valuta lo stato delle vicine nella generazione corrente
3. **Evoluzione**: ogni cellula applica le regole del GOL e produce il proprio stato per la generazione successiva

### Comportamenti Estesi

#### Eventi

Il GOL esteso può attivare eventi globali che influenzano tutte le `Tile` per una singola generazione. Gli eventi possibili sono:

- **Cataclysm**: tutte le `Tile` azzerano i `lifePoints` delle cellule che contengono
- **Famine**: tutte le `Tile` assorbono esattamente 1 `lifePoint` dalla cellula
- **Bloom**: le `Tile` donano esattamente 2 `lifePoints` alla cellula
- **Blood Moon**: ogni `VAMPIRE` assorbe 1 `lifePoint` da ogni vicino `NAIVE` o `HEALER` e li trasforma in `VAMPIRE`
- **Sanctuary**: tutte le `Tile` donano 1 `lifePoint` agli `HEALER`, e trasformano i `VAMPIRE` in `NAIVE`

Ogni evento ha effetto su una singola generazione. Si può verificare al massimo un evento per generazione. Gli eventi si applicano all’inizio della generazione.

### Persistenza

Il sistema di persistenza deve catturare ogni snapshot di `Generation` (layout della board e stato delle cellule), insieme alle entità `Game` associate.

Alla ricarica, il metodo `loadEvents()` è responsabile del collegamento di ogni evento alla sua `Generation`, per permettere la ricostruzione e l’ispezione della cronologia della simulazione.

Esempio:

```java
public class GameRepository  extends GenericExtGOLRepository<Game, Long> {
    public GameRepository()  { super(Game.class);  }
}
```

Ogni repository deve implementare `load(...)` (e query personalizzate) per permettere il salvataggio e il recupero completo dello stato del `Game`, incluse tutte le generazioni e i loro eventi.
