package it.polito.extgol;

import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class GameRepository  extends GenericExtGOLRepository<Game, Long> {
    public GameRepository()  { super(Game.class);  }

    public Optional<Game> load(Long id) {
        Objects.requireNonNull(id);

        EntityManager em = JPAUtil.getEntityManager();
        TypedQuery<Game> query = em.createQuery(
            "SELECT g FROM Game g " +
            "WHERE g.id =: id", 
            Game.class);
        query.setParameter("id", id);
        
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    public Optional<Game> loadComplete(Long id) {
        Objects.requireNonNull(id);

        EntityManager em = JPAUtil.getEntityManager();
        try {
            Game game = em.find(Game.class, id);
            if (game == null)
                return Optional.empty();

            if (game.getBoard() == null)
                return Optional.of(game);

            BoardRepository boardRepository = new BoardRepository();
            Optional<Board> boardOpt = boardRepository.load(game.getBoard().getId(), em);
            boardOpt.ifPresent(game::setBoard);

            return Optional.of(game);
        } finally {
            em.close();
        } 
    }
}