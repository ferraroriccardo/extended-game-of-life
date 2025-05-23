package it.polito.extgol;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

/**
 * Facade coordinating the core operations of the Extended Game of Life simulation.
 *
 * This class provides high-level methods to:
 *   - Evolve a single generation or advance multiple steps.
 *   - Visualize the board state and retrieve alive cells by coordinate.
 *   - Persist and reload entire game instances.
 */
public class ExtendedGameOfLife {
    private Game game;
    private Game game;

    /**
     * Computes and returns the next generation based on the current one.
     *
     * The method follows these steps:
     *   1. Validates that the current generation has an associated Board and Game.
     *   2. Computes the next alive/dead state for each cell based solely on the current state.
     *   3. Creates a new Generation object representing the next simulation step.
     *   4. Applies all calculated state changes simultaneously, ensuring consistency.
     *   5. Captures a snapshot of all cells' states into the persistent map for future retrieval.
     *
     * @param current The current generation snapshot used for evolving to the next state.
     * @return A new Generation object reflecting the evolved board state.
     * @throws IllegalStateException If Generation is not properly initialized.
     */
    public Generation evolve(Generation current) {
        Objects.requireNonNull(current, "Current generation cannot be null");
        Board board = current.getBoard();
        Game game = current.getGame();
        
        // Ensure current generation is properly initialized
        if (board == null || game == null) {
            throw new IllegalStateException(
                "Generation must have associated Board and Game!");
        }

        // Step 1: Compute next state for each cell based only on current generation state
        Map<Cell, Boolean> nextStates = new HashMap<>();
        for (Tile tile : board.getTiles()) {
            Cell c = tile.getCell();
            if (c == null) {
                throw new IllegalStateException("Missing cell on tile " + tile);
            }

            int aliveNeighbors = c.countAliveNeighbors();
            boolean nextState = c.evolve(aliveNeighbors);

            nextStates.put(c, nextState);
        }

        // Step 2: Instantiate the next Generation based on current
        Generation nextGen = Generation.createNextGeneration(current);

        // Step 3: Apply all computed states simultaneously to avoid intermediate inconsistencies
        for (Map.Entry<Cell, Boolean> e : nextStates.entrySet()) {
            Cell c = e.getKey();
            c.setAlive(e.getValue());
            c.addGeneration(nextGen);  // register cell with new generation
        }

        // Step 4: Persist snapshot of the next generation state
        nextGen.snapCells();
        return nextGen;
    }

    /**
     * Advances the simulation by evolving the game state through a given number of steps.
     *
     * Starting from the game's initial generation, this method repeatedly computes the next
     * generation and appends it to the game's history.
     *
     * @param game  The Game instance whose generations will be advanced.
     * @param steps The number of evolution steps (generations) to perform.
     * @return The same Game instance, updated with the new generation.
     */
    public Game run(Game game, int steps) {
        Generation current = game.getStart();
        for (int i = 0; i < steps; i++) {
            Generation next = evolve(current);
            current = next;
        }
        this.game = game;        return game;
    }

    /**
     * Advances the simulation by evolving the game state through a given number of steps.
     *
     * Starting from the game's initial generation, this method repeatedly computes the next
     * generation and appends it to the game's history. 
     * 
     * It applies any events at their scheduled generations.
     *
     * At each step:
     *   1. If an event is scheduled for the current step (according to eventMap), the
     *      corresponding event is applied to all tiles before evolution.
     *   2. The board then evolves to the next generation, which is added to the game.
     *
     * @param game      The Game instance to run and update.
     * @param steps     The total number of generations to simulate.
     * @param eventMap  A map from generation index (0-based) to the EventType to trigger;
     *                  if a step is not present in the map, no event is applied that step.
     * @return          The same Game instance, now containing the extended generation history.
     */
    public Game run(Game game, int steps, Map<Integer, EventType> eventMap) {
    Generation current = game.getStart();
    for (int i = 0; i < steps; i++) {
        final int step = i;
        if (eventMap.containsKey(step)) {
            game.getBoard().getCellSet()
                .forEach(cell -> game.unrollEvent(eventMap.get(step), cell));
        }
        Generation next = evolve(current);
        game.addGeneration(next);
        current = next;
    }
    this.game = game;
    return game;
}

    /**
     * Builds and returns a map associating each coordinate with its alive Cell 
     * instance for the specified generation.
     *
     * Iterates over all alive cells present in the given generation and constructs 
     * a coordinate-based map, facilitating cell access.
     *
     * @param generation The generation whose alive cells are mapped.
     * @return A Map from Coord (coordinates) to Cell instances representing all alive cells.
    */
    public Map<Coord, Cell> getAliveCells(Generation generation) {
        Map<Coord, Cell> alive = new HashMap<>();
        for (Cell c : generation.getAliveCells()) {
            alive.put(c.getCoordinates(), c);
        }
        return alive;
    }

    /**
     * Generates a visual string representation of the specified generation's board state.
     *
     * It produces a multi-line textual snapshot showing cells and their status.
     * "C" -> alive cell
     * "0" -> dead cell
     *
     * @param generation The Generation instance to visualize.
     * @return A multi-line String-based representiion of the board's current state.
    */
    public String visualize(Generation generation) {
        return generation.getBoard().visualize(generation);
    }

    /**
     * Persists the complete state of the provided Game instance, including its Board, Tiles,
     * Cells, and all associated Generations.
     *
     * If the Game is new, it will be created and persisted.
     * Otherwise, its state will be updated (merged) in the database. Ensures transactional 
     * safety and consistency through commit and rollback handling.
     *
     * @param game The Game instance to persist or update.
     */
    public void saveGame(Game game) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (game.getId() == null) {
                em.persist(game);
            } else {
                em.merge(game);
            }
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Loads and returns a persisted map of game events keyed by generation step.
     *
     * Delegates retrieval to the corresponding repository class, which in turn implements 
     * the provided generic repository class for persistence. This method reconstructs 
     * the event timeline for inspection or replay.
     *
     * @return A Map<Integer, EventType> mapping generation steps to associated events.
     */
    public Map<Integer, EventType> loadEvents() {
        return Game.loadEvents(this.game);
        return Game.loadEvents(this.game);
    }
}