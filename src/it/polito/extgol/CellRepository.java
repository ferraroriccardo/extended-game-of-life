package it.polito.extgol;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class CellRepository  extends GenericExtGOLRepository<Cell, Long> {

    public CellRepository() { 
        super(Cell.class);  
    }

    /**
     * loads a single cell from db using its id
     * @param id the id of the cell
     * @return the cell object
     */
    public Cell load(Long id) {
        Objects.requireNonNull(id);

        EntityManager em = JPAUtil.getEntityManager();

        try {
            return em.find(Cell.class, id);
        } finally {
            em.close();
        }
    }

    /**
     * loads all alive cells of a board
     * @param boardId the id of the board
     * @return the list of alive cells of the board
     */
    public Optional<List<Cell>> loadAliveCells(Integer boardId) {
        Objects.requireNonNull(boardId);
        
        EntityManager em = JPAUtil.getEntityManager();
        TypedQuery<Cell> query = em.createQuery(
                "SELECT c FROM Cell c WHERE c.isAlive = TRUE AND c.board.id = :boardId", 
                Cell.class);
                query.setParameter("boardId", boardId);
        try {
            return Optional.of(query.getResultList());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    /**
     * loads all the cells of a board
     * @param boardId the id of the board
     * @return the list of cells of the board
     */
    public Optional<List<Cell>> loadCells(Integer boardId) {
        Objects.requireNonNull(boardId);

        EntityManager em = JPAUtil.getEntityManager();
        TypedQuery<Cell> query = em.createQuery(
                "SELECT c FROM Cell c WHERE c.board.id = :boardId", 
                Cell.class);
                query.setParameter("boardId", boardId);
        try {
            return Optional.of(query.getResultList());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    Optional<List<Cell>> loadCells(int boardId, EntityManager em) {
        Objects.requireNonNull(em);
        
        TypedQuery<Cell> query = em.createQuery(
            "SELECT c FROM Cell c WHERE c.board.id = :boardId",
            Cell.class);
        query.setParameter("boardId", boardId);
        try {
            return Optional.of(query.getResultList());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}