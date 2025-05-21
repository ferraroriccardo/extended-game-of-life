package it.polito.extgol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static it.polito.extgol.CellMood.HEALER;
import static it.polito.extgol.CellMood.NAIVE;
import static it.polito.extgol.CellMood.VAMPIRE;
import static it.polito.extgol.CellType.BASIC;
import static it.polito.extgol.CellType.HIGHLANDER;
import static it.polito.extgol.CellType.LONER;
import static it.polito.extgol.CellType.SOCIAL;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

/**
 * Entity representing a cell in the Extended Game of Life.
 *
 * Serves as the base class for all cell types, embedding its board coordinates,
 * alive/dead state, energy budget (lifePoints), and interaction mood.
 * Each Cell is linked to a Board, Game, Tile, and a history of Generations.
 * Implements Evolvable to apply Conway’s rules plus energy checks each
 * generation,
 * and Interactable to model cell–cell energy exchanges.
 */
@Entity
public class Cell implements Evolvable, Interactable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * In-memory coordinates, persisted as two columns cell_x and cell_y.
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "x", column = @Column(name = "cell_x", nullable = false)),
            @AttributeOverride(name = "y", column = @Column(name = "cell_y", nullable = false))
    })
    private Coord cellCoord;

    /** Persisted alive/dead state */
    @Column(name = "is_alive", nullable = false)
    protected Boolean isAlive = false;

    /** Persisted lifepoints (default 0) */
    @Column(name = "lifepoints", nullable = false)
    protected Integer lifepoints = 0;

    private CellMood mood;
    private CellType type;

    /** Reference to the parent board (read-only). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false, updatable = false)
    protected Board board;

    /** Reference to the owning game (read-only). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false, updatable = false)
    protected Game game;

    /** Transient list tracking generations this cell belongs to. */
    @Transient
    protected List<Generation> generations = new ArrayList<>();

    /** Back-reference: Tile owns the foreign key mapping. */
    @OneToOne(mappedBy = "cell", fetch = FetchType.LAZY)
    protected Tile tile;

    //TODO
    /** Default setting */
    protected int minThreshold = 2;

    //TODO
    /** Default setting */
    protected int maxThreshold = 3;

    //TODO check if it has to be saved
    /** Default setting as NAIVE */
    protected CellMood cellMood;

    //TODO check if it has to be saved
    /** Default setting as BASIC */
    protected CellType cellType;

    //TODO check if it has to be saved
    /** Number of skipped generations */
    protected int skippedGen = 0;

    /** Default constructor for JPA compliance. */
    public Cell() {
    }

    /**
     * Constructs a new Cell at given coordinates, defaulting to dead.
     * 
     * @param coord the cell's coordinates
     */
    public Cell(Coord tileCoord) {
        this.cellCoord = tileCoord;
        this.isAlive = false;
        this.cellMood = NAIVE;
        this.cellType = BASIC;
    }

    /**
     * Constructs a new Cell with its tile, board, and game context.
     * 
     * @param coord the cell's coordinates
     * @param tile  the owning Tile
     * @param board the Board context
     * @param game  the owning Game
     */
    public Cell(Coord tileCoord, Tile t, Board b, Game g) {
        this.cellCoord = tileCoord;
        this.isAlive = false;
        this.tile = t;
        this.board = b;
        this.game = g;
        this.cellMood = NAIVE;
        this.cellType = BASIC;
    }

    /**
     * Applies the classic Conway’s Game of Life rules to calculate the cell’s next
     * alive/dead state.
     *
     * Rules:
     * - Underpopulation: A live cell with fewer than 2 neighbors dies.
     * - Overpopulation: A live cell with more than 3 neighbors dies.
     * - Respawn: A dead cell with exactly 3 neighbors becomes alive.
     * - Survival: A live cell with 2 or 3 neighbors stays alive.
     *
     * @param aliveNeighbors the count of alive neighboring cells
     * @return true if the cell will live, false otherwise
     */
    @Override
    public Boolean evolve(int aliveNeighbors) {
        // Start by assuming the cell retains its current state
        Boolean willLive = this.isAlive;

        // Overpopulation: more than 3 neighbors kills a live cell
        if (aliveNeighbors > maxThreshold) {
            if( this.cellType.equals(HIGHLANDER) ) {
                if (this.skippedGen < 3 && this.skippedGen != -1) {
                    willLive = true;
                    this.skippedGen ++;
                }
                else {
                    this.skippedGen = -1; 
                    willLive = false;
                }
            }
            else {
                willLive = false;
            }
        }
        // Underpopulation: fewer than 2 neighbors kills a live cell
        else if (aliveNeighbors < minThreshold) {
            if( this.cellType.equals(HIGHLANDER) ) {
                if (this.skippedGen < 3 && this.skippedGen != -1) {
                    willLive = true;
                    this.skippedGen ++;
                }
                else {
                    this.skippedGen = -1;
                    willLive = false;
                }
            }
            else {
                willLive = false;
            }
        }
        // Respawn: exactly 3 neighbors brings a dead cell to life
        else if (!this.isAlive && aliveNeighbors == 3) {
            willLive = true;
        }
        // Otherwise (2 or 3 neighbors on a live cell) nothing changes and willLive
        // remains true

        return willLive;
    }

    /**
     * Retrieves all tiles adjacent to this cell's tile.
     *
     * This method returns a copy of the underlying neighbor list to ensure
     * external code cannot modify the board topology.
     *
     * @return an immutable List of neighboring Tile instances
     */
    public List<Tile> getNeighbors() {
        return List.copyOf(tile.getNeighbors());
    }

    /**
     * Counts the number of live cells adjacent to this cell’s tile.
     *
     * Iterates over all neighboring tiles and increments the count for each
     * tile that hosts an alive Cell.
     *
     * @return the total number of alive neighboring cells
     */
    public int countAliveNeighbors() {
        int count = 0;
        for (Tile t : tile.getNeighbors()) {
            if (t.getCell() != null && t.getCell().isAlive())
                count++;
        }
        return count;
    }

    /**
     * Registers this cell in the specified generation’s back-reference list.
     *
     * Used internally by the ORM to maintain the relationship between
     * cells and the generations they belong to. Adds the given generation
     * to the cell’s internal history.
     *
     * @param gen the Generation instance to associate with this cell
     */
    void addGeneration(Generation gen) {
        generations.add(gen);
    }

    /**
     * Provides an unmodifiable history of all generations in which this cell has
     * appeared.
     *
     * Returns a copy of the internal list to prevent external modification
     * of the cell’s generation history.
     *
     * @return an immutable List of Generation instances tracking this cell’s
     *         lineage
     */
    public List<Generation> getGenerations() {
        return List.copyOf(generations);
    }

    /**
     * Returns the X coordinate of this cell on the board.
     *
     * @return the cell’s X position
     */
    public int getX() {
        return this.cellCoord.getX();
    }

    /**
     * Returns the Y coordinate of this cell on the board.
     *
     * @return the cell’s Y position
     */
    public int getY() {
        return this.cellCoord.getY();
    }

    /**
     * Retrieves the full coordinate object for this cell.
     *
     * @return a Coord instance representing this cell’s position
     */
    public Coord getCoordinates() {
        return this.cellCoord;
    }

    /**
     * Checks whether this cell is currently alive.
     *
     * @return true if the cell is alive; false if it is dead
     */
    public boolean isAlive() {
        return isAlive;
    }

    /**
     * Updates the alive/dead state of this cell.
     *
     * @param isAlive true to mark the cell as alive; false to mark it as dead
     */
    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }

    /**
     * Returns a string representation of this cell’s position in the format "x,y".
     *
     * Overrides Object.toString() to provide a concise coordinate-based
     * representation.
     * 
     * @return a comma-separated string of the cell’s X and Y coordinates
     */
    @Override
    public String toString() {
        return getX() + "," + getY();
    }

    // EXTENDED BEHAVIORS

    /**
     * Set the new minimum threshold for this specific cell
     * 
     * @param value the number of new minimum threshold
     */
    public void setMinThreshold(int value) {

        this.minThreshold = value;
    }

    /**
     * @return the value of the minimum threshold for this specific cell
     */
    public int getMinThreshold() {

        return this.minThreshold;
    }

    /**
     * Set the new maximum threshold for this specific cell
     * 
     * @param value the number of new maximum threshold
     */
    public void setMaxThreshold(int value) {

        this.maxThreshold = value;
    }

    /**
     * @return the value of the maximum threshold for this specific cell
     */
    public int getMaxThreshold() {

        return this.maxThreshold;
    }
    
    /**
     * Retrieves the current energy level of this cell.
     *
     * @return the number of life points the cell currently has
     */
    public int getLifePoints() {
        return this.lifepoints;
    }

    /**
     * Updates the energy level of this cell.
     *
     * @param lifePoints the new number of life points to assign to the cell
     */
    public void setLifePoints(int lifePoints) {

        this.lifepoints = lifePoints;
    }

    /**
     * Implements the interact() method of Interactable to
     * define the interaction between this cell and another cell.
     * Implementations will adjust life points, mood, or other state based on the
     * interaction rules.
     *
     * @param cell the Cell object to interact with
     */
    @Override
    public void interact(Cell otherCell) {

        Objects.requireNonNull(otherCell,"Interaction need cells. 'otherCell' cannot be null");
        
        CellMood thisType = this.cellMood;
        CellMood otherType = otherCell.cellMood;

        switch(thisType) {
            case NAIVE: 
                switch (otherType) {
                    case NAIVE:
                        break;
                    case HEALER:
                        this.setLifePoints(this.getLifePoints() + 1);
                        break;
                    case VAMPIRE:
                        otherCell.setLifePoints(otherCell.getLifePoints() + 1);
                        this.setLifePoints(this.getLifePoints() - 1);
                        this.setMood(VAMPIRE);
                        break;
                    default:
                }
                break;
            case HEALER: 
                switch (otherType) {
                    case NAIVE:
                        otherCell.setLifePoints(otherCell.getLifePoints() + 1);
                        break;
                    case HEALER:
                        break;
                    case VAMPIRE:
                        otherCell.setLifePoints(otherCell.getLifePoints() + 1);
                        this.setLifePoints(this.getLifePoints() - 1);
                        break;
                    default:
                    }
                break;
            case VAMPIRE:
                    switch (otherType) {
                        case NAIVE:
                            otherCell.setLifePoints(otherCell.getLifePoints() - 1);
                            otherCell.setMood(VAMPIRE);
                            this.setLifePoints(this.getLifePoints() + 1);
                            break;
                        case HEALER:
                            otherCell.setLifePoints(otherCell.getLifePoints() - 1);
                            this.setLifePoints(this.getLifePoints() + 1);
                            break;
                        case VAMPIRE:
                            break;
                        default:
                    }    
                break;    
        }
    }

    /**
     * Assigns a specific cell type to this cell, influencing its behavior.
     *
     * @param t the CellType to set (e.g., BASIC, HIGHLANDER, LONER, SOCIAL)
     */
    public void setType(CellType t) {

        Objects.requireNonNull(t,"Cell type null");

        switch(t) {
            case BASIC:
                this.cellType = BASIC;
                break;
            case HIGHLANDER: 
                this.cellType = HIGHLANDER;
                this.skippedGen = 0;
                break;
            case LONER: 
                this.cellType = LONER;
                setMinThreshold(1);
                break;
            case SOCIAL: 
                this.cellType = SOCIAL;
                setMaxThreshold(8);
                break;
            default: 
                break;
        }
    }

    /**
     * Retrieves the current type of this cell.
     *
     * @return the CellType representing the cell’s influencing behavior.
     */
    public CellType getType() {
        
        return this.cellType;
    }

    /**
     * Sets the current mood of this cell, impacting how it interacts with others.
     *
     * @param mood the CellMood to assign (NAIVE, HEALER, or VAMPIRE)
     */
    public void setMood(CellMood mood) {
        
        Objects.requireNonNull(mood,"Cell mood null");

        switch(mood) {
            case NAIVE: 
                this.cellMood = NAIVE;
                break;
            case HEALER: 
                this.cellMood = HEALER;
                break;
            case VAMPIRE: 
                this.cellMood = VAMPIRE;
                break;
            default: 
                break;
        }
    }

    /**
     * Retrieves the current mood of this cell.
     *
     * @return the CellMood representing the cell’s interaction style
     */
    public CellMood getMood() {
        
        return this.cellMood;
    }
}
