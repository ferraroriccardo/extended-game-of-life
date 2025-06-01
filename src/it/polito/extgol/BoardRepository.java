package it.polito.extgol;

import java.util.Optional;


import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class BoardRepository  extends GenericExtGOLRepository<Board, Long> {
    
    public BoardRepository()  { 
        super(Board.class);  
    }
 

    public Optional<Board> load(Integer id) {
        EntityManager em = JPAUtil.getEntityManager();
        TypedQuery<Board> query = em.createQuery(
            "SELECT b FROM Board b " +
            "JOIN b.tile t " +
            "JOIN t.cell c " +
            "WHERE b.id =: id", 
            Board.class);
        query.setParameter("id", id);
        
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }

}
