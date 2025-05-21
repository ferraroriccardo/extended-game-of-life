package it.polito.extgol;

import java.util.ArrayList;
import java.util.List;

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

    private CellType type;
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
        if (aliveNeighbors > 3) {
            willLive = false;
        }
        // Underpopulation: fewer than 2 neighbors kills a live cell
        else if (aliveNeighbors < 2) {
            willLive = false;
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
        // TODO Auto-generated method stub
    }

    /**
     * Assigns a specific cell type to this cell, influencing its behavior.
     *
     * @param t the CellType to set (e.g., BASIC, HIGHLANDER, LONER, SOCIAL)
     */
    public void setType(CellType t) {
        type = t;
    }

    /**
     * Returns the type of the cell
     * @return type
     */
    public CellType getType() {
        return type;
    }
    
    /**
     * Sets the current mood of this cell, impacting how it interacts with others.
     *
     * @param mood the CellMood to assign (NAIVE, HEALER, or VAMPIRE)
     */
    public void setMood(CellMood mood) {
        // TODO Auto-generated method stub
    }

    /**
     * Retrieves the current mood of this cell.
     *
     * @return the CellMood representing the cell’s interaction style
     */
    public CellMood getMood() {
        // TODO Auto-generated method stub
        return null;
    }

}
