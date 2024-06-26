package ir.sharif.model.game.abilities;

import ir.sharif.model.game.Ability;
import ir.sharif.model.game.Card;
import ir.sharif.model.game.Row;
import ir.sharif.model.game.UserTable;
import ir.sharif.service.GameService;
import ir.sharif.utils.Random;

public class DestroyerOfWorlds implements Ability {
    @Override
    public void execute(Object... objs) {
        UserTable userTable = GameService.getInstance().getMatchTable().getCurrentUserTable();
        for (int cardIndex = 0; cardIndex < 2 && !userTable.getHand().isEmpty(); cardIndex++) {
            Card card = userTable.getHand().get(Random.getRandomInt(userTable.getHand().size()));
            userTable.getHand().remove(card);
            userTable.addOutOfPlay(card);
        }
        if(!userTable.getDeck().isEmpty()){
            Card card = userTable.getDeck().get(Random.getRandomInt(userTable.getDeck().size()));
            userTable.getDeck().remove(card);
            userTable.addHand(card);
        }
    }

}
