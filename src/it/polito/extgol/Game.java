package it.polito.extgol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Entity representing a Game of Life simulation instance.
 * 
 * Maintains the board, generation history, and global event schedule for each
 * simulation. Provides factory methods for classic and extended game setups,
 * as well as operations for evolving and querying game state.
 */
@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable name for this game instance.
     */
     @Column(nullable = false, unique = true)
    private String name;

    /**
     * The board on which this game is played.
     *
     * One board per game; cascade so the board is persisted/removed along with the game.  
     * Stored in TILE table as board_id FK.  
     */
    @OneToOne(
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY,
        optional = false
        )
    @JoinColumn(name = "board_id", nullable = false, unique = true)
    private Board board;

    /**
     * List of all generations in this game, including the initial.
     * 
     * All generations (including initial) in time order.  
     * Uses an ORDER_COLUMN so the DB keeps the sequence.
     */
    @OneToMany(
      mappedBy       = "game",
      cascade        = CascadeType.ALL,
      orphanRemoval  = true,
      fetch          = FetchType.LAZY
    )
    @OrderColumn(name = "generation_index")
    private List<Generation> generations = new ArrayList<>();
    
    @ElementCollection
    @MapKeyColumn(name = "generation_id")
    private Map<Integer, EventType> eventMapInternal = new HashMap<>();

    /** 
     * Default constructor for JPA.
     */
    protected Game() {
    }

    /**
     * Constructs a Game with the specified name.
     * Creates a default 5×5 Board and defers initialization of the first generation
     * to the associated factory or caller.
     *
     * @param name the human-readable name for this game instance
     */
    protected Game(String name) {
        this.name = name;
    }

    /**
     * Constructs a Game with the specified name and board dimensions.
     * Creates a new Board of the given size, associates it with this Game,
     * and initializes the initial Generation on that board.
     *
     * @param name   the human-readable name for this game instance
     * @param width  the number of columns for the game board
     * @param height the number of rows for the game board
     */
    public Game(String name, int width, int height) {
        this.name = name;
        this.board = new Board(width, height, this);
        Generation.createInitial(this, board);
    }

    /**
     * Factory method to create and fully initialize a Game instance.
     * Uses a protected constructor to set the name, builds a Board of the given size,
     * sets up the first Generation, and returns the ready-to-run Game.
     *
     * @param name   the human-readable name for this game instance
     * @param width  the number of columns for the game board
     * @param height the number of rows for the game board
     * @return a new Game configured with its board and initial generation
     */
    public static Game create(String name, int width, int height) {
        Game game = new Game(name);
        Board board = new Board(width, height, game);
        game.setBoard(board);
        Generation.createInitial(game, board);
        
        return game;
    }

    /**
     * Factory method to create and fully initialize an extended Game instance.
     * Creates a Game with the given name, constructs an extended Board using
     * specialized tiles and default cell settings, and initializes the first Generation.
     *
     * @param name   the human-readable name for this game instance
     * @param width  the number of columns for the game board
     * @param height the number of rows for the game board
     * @return a new Game configured with its extended board and initial generation
     */
    public static Game createExtended(String name, int width, int height) {
        Game game = new Game(name);
        Board board = Board.createExtended(width, height, game);
        game.setBoard(board);
        Generation.createInitial(game, board);
        
        return game;
    }

     /**
     * Appends a new Generation to the end of this game’s timeline.
     * Sets the generation’s back-reference to this Game before adding.
     *
     * @param generation the Generation instance to add to the sequence
     */
    public void addGeneration(Generation generation) {
        generation.setGame(this);
        generations.add(generation);
    }

    /**
     * Inserts a Generation at the specified index in the game’s timeline.
     * Shifts subsequent generations to higher indices. Sets the generation’s
     * back-reference to this Game before insertion.
     *
     * @param generation the Generation instance to insert
     * @param step       the zero-based index at which to insert this generation
     */
    public void addGeneration(Generation generation, Integer step) {
        generation.setGame(this);
        generations.add(step, generation);
    }

    /**
     * Removes all generations from this Game’s history.
     * After clearing, the game will have no recorded generations until new ones are added.
     */
    public void clearGenerations() {
        generations.clear();
    }

    /**
     * Retrieves the full history of generations in this game, in chronological order.
     *
     * @return a List of Generation instances representing each step in the simulation
     */
    public List<Generation> getGenerations() {
        return generations;
    }

    /**
     * Returns the unique identifier for this Game.
     *
     * @return the database ID of this game instance
     */
    public Long getId() {
        return id;
    }

    /**
     * Retrieves the human-readable name of this Game.
     *
     * @return the name assigned to this game
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the name of this Game.
     *
     * @param name the new human-readable name to assign
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the Board associated with this Game.
     *
     * @return the Board instance on which this game is played
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Associates a Board with this Game.
     *
     * @param b the Board instance to assign to this game
     */
    public void setBoard(Board b) {
        this.board = b;
    }

    /**
     * Returns the initial Generation of the game (step 0).
     *
     * @return the first Generation in the game’s sequence
     */
    public Generation getStart() {
        return this.generations.get(0);
    }

    /**
     * Applies the specified event to a single cell.
     *
     * @param event the EventType to apply
     * @param cell  the Cell instance to which the event should be applied
     */
    public void unrollEvent(EventType event, Cell cell) {
        //this method should not be called, to let Game.class interact only with tiles and not directly to cells
        Tile tile = this.board.getTile(cell.getCoordinates());
        
        Objects.requireNonNull(event);
        switch(event){
            case CATACLYSM -> tile.setLifePointModifier(-1 * tile.getCell().getLifePoints());

            case FAMINE -> tile.setLifePointModifier(-1);

            case BLOOM -> tile.setLifePointModifier(+2);

            case BLOOD_MOON -> tile.setEnableSuperVampire(true);

            case SANCTUARY -> {
                Cell c = tile.getCell();
                if (c.getMood() == CellMood.VAMPIRE)
                    c.setMood(CellMood.NAIVE);

                if (c.getMood() == CellMood.HEALER)
                    tile.setLifePointModifier(+1);
            }
            default -> {}
        }
    }

    /**
     * Sets the given mood for all cells at the specified coordinates.
     * Useful for scenarios like converting a batch of cells to VAMPIRE or HEALER.
     *
     * @param mood              the CellMood to assign (NAIVE, HEALER, or VAMPIRE)
     * @param targetCoordinates the list of coordinates of cells to update
     */
    public void setMood(CellMood mood, List<Coord> targetCoordinates) {
        setMoods(mood, targetCoordinates);
    }

    /**
     * Assigns a common mood to multiple cells in one operation.
     * Currently unimplemented; will throw an exception until provided.
     *
     * @param mood        the CellMood to set (e.g., VAMPIRE)
     * @param coordinates the list of cell coordinates to update
     */
    public void setMoods(CellMood mood, List<Coord> coordinates) {
        Objects.requireNonNull(mood);
        Objects.requireNonNull(coordinates);

        for (Coord c : coordinates){
            Objects.requireNonNull(c);
            board.getTile(c).getCell().setMood(mood);
        }
    }

    /**
     * Retrieves the internal mapping of scheduled events for this game.
     * Each entry maps a generation index to an EventType.
     *
     * @return a mutable Map from generation step to EventType
     */
    public Map<Integer, EventType> getEventMapInternal() {
        return this.eventMapInternal;
    }

    /**
     * Loads the persisted event schedule for the given Game instance.
     * Delegates to the repository classes implementing
     * GenericExtGOLRepository to fetch the map of events
     * from the database, then returns it as an immutable map.
     *
     * @param game the detached Game instance whose events should be reloaded
     * @return an immutable Map from generation step to EventType
     */
    public static Map<Integer, EventType> loadEvents(Game game) {
    EntityManager em = JPAUtil.getEntityManager();
    try {
        Game persistentGame = em.find(Game.class, game.getId());
        if (persistentGame == null) {
            throw new IllegalArgumentException("Game with id " + game.getId() + " not found.");
        }
        return Map.copyOf(persistentGame.getEventMapInternal());
    } catch(Exception e) {
        throw e;
    } finally {
        em.close();
        }
    }           
}
