package it.polito.extgol;

public class GameRepository  extends GenericExtGOLRepository<Game, Long> {
    public GameRepository()  { super(Game.class);  }
}