package it.polito.extgol;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class BoardRepository  extends GenericExtGOLRepository<Board, Long> {
    
    public BoardRepository()  { 
        super(Board.class);  
    }
 

    public Optional<Board> load(Integer boardId) {
        Objects.requireNonNull(boardId);

        EntityManager em = JPAUtil.getEntityManager();
        TypedQuery<Board> query = em.createQuery(
            "SELECT b FROM Board b " +
            "JOIN FETCH b.tiles t " +
            "JOIN FETCH t.cell c " +
            "WHERE b.id =: boardId", 
            Board.class);
        query.setParameter("boardId", boardId);
        
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    public Optional<Board> load(Long gameId) {
        Objects.requireNonNull(gameId);

        EntityManager em = JPAUtil.getEntityManager();
        TypedQuery<Board> query = em.createQuery(
            "SELECT b FROM Board b " +
            "JOIN FETCH b.tiles t " +
            "JOIN FETCH t.cell c " +
            "WHERE b.game.id =: gameId", 
            Board.class);
        query.setParameter("gameId", gameId);
        
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }


    public Optional<Board> load(int id, EntityManager em) {
        Objects.requireNonNull(em);

        Board board = em.find(Board.class, id);
        if (board == null)
            return Optional.empty();

        CellRepository cellRepo = new CellRepository();
        Optional<List<Cell>> cellsOpt = cellRepo.loadCells(board.getId(), em);
        if (cellsOpt.isPresent()) {
            List<Cell> cells = cellsOpt.get();
            for (Cell c : cells) {
                Coord co = c.getCoordinates();
                Tile t = board.getTile(co);
                if (t != null) {
                    t.setCell(c);
                }
            }
        }
        return Optional.of(board);
    }
}