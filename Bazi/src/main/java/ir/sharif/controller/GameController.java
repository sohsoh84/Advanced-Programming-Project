package ir.sharif.controller;

import ir.sharif.enums.ResultCode;
import ir.sharif.model.CommandResult;
import ir.sharif.model.game.*;
import ir.sharif.model.game.abilities.Spy;
import ir.sharif.service.GameService;
import javafx.beans.binding.StringBinding;

import java.util.Random;

public class GameController {
    private MatchTable matchTable;

    public GameController() {
        this.matchTable = GameService.getInstance().getMatchTable();
    }

    public int getRandomNumber(int n) {
        Random random = new Random();
        return random.nextInt(n);
    }

    public CardPosition getCardPositionByRowNumber(int rowNumber) {
        return switch (rowNumber) {
            case 0 -> CardPosition.CLOSE_COMBAT_UNIT;
            case 1 -> CardPosition.RANGED_UNIT;
            case 2 -> CardPosition.SIEGE_UNIT;
            case -1 -> CardPosition.WEATHER;
            case -2 -> CardPosition.SPELL;
            default -> null;
        };
    }

    public Row getRowByPosition(int player, CardPosition cardPosition) {
        return switch (cardPosition) {
            case CLOSE_COMBAT_UNIT -> matchTable.getUserTable(player).getCloseCombat();
            case RANGED_UNIT -> matchTable.getUserTable(player).getRanged();
            case SIEGE_UNIT -> matchTable.getUserTable(player).getSiege();
            default -> null;
        };
    }

    public int weatherCardsOnTable() {
        int answer = 0;
        for(Card card : matchTable.getWeatherCards()) {
            if(card.getName().equals("Torrential rain")) {
                answer |= 4;
            }
            if(card.getName().equals("Impenetrable fog")) {
                answer |= 2;
            }
            if(card.getName().equals("Biting frost")) {
                answer |= 1;
            }
        }
        return answer;
    }

    public int calculatePower(int player, int rowNumber, Card card) {
        double cofficient = 1;
        int weather = weatherCardsOnTable();
        if(card.isHero()) {
            return card.getPower();
        }
        if(((1 << rowNumber) & weather) != 0) {
            if(matchTable.getUserTable(player).getLeader().getName().equals("King Bran"))
        }

        return (int)(cofficient * card.getPower());
    }

    public CommandResult vetoCard(int cardNumber) {
        int player = matchTable.getTurn();
        if(matchTable.getVetoesLeft(player) == 0) {
            return new CommandResult(ResultCode.FAILED, "You don't have any vetoes left");
        }
        Card card = matchTable.getUserTable(player).getHand().get(cardNumber);
        matchTable.getUserTable(player).getHand().remove(cardNumber);
        matchTable.getUserTable(player).getDeck().add(card);
        int randomNumber = getRandomNumber(matchTable.getUserTable(player).getDeck().size());
        Card randomCard = matchTable.getUserTable(player).getDeck().get(randomNumber);
        matchTable.getUserTable(player).getDeck().remove(randomNumber);
        matchTable.getUserTable(player).getHand().add(randomCard);
        matchTable.decreaseVetoesLeft(player);
        return new CommandResult(ResultCode.ACCEPT, "Card vetoed successfully");
    }

    public CommandResult inHandDeck(int cardNumber) {
        // Implement the logic for handling in-hand deck
        return null;
    }

    public CommandResult remainingCardsToPlay() {
        int player = matchTable.getTurn();
        return new CommandResult(ResultCode.ACCEPT, "Remaining cards to play: " + matchTable.getUserTable(player).getHand().size());
    }

    public CommandResult outOfPlayCards() {
        // Implement the logic for out of play cards
        return null;
    }

    public CommandResult cardsInRow(int rowNumber) {
        // Implement the logic for cards in a specific row
        return null;
    }

    public CommandResult spellsInPlay() {
        // Implement the logic for spells in play
        return null;
    }

    public CommandResult placeCard(int cardNumber, int rowNumber) {
        if(matchTable.getUserTable(matchTable.getTurn()).getHand().size() <= cardNumber) {
            return new CommandResult(ResultCode.FAILED, "Invalid card number");
        }
        if(getCardPositionByRowNumber(rowNumber) == null) {
            return new CommandResult(ResultCode.FAILED, "Invalid row number");
        }
        int player = matchTable.getTurn();
        CardPosition cardPosition = getCardPositionByRowNumber(rowNumber);
        Card card = matchTable.getUserTable(player).getHand().get(cardNumber);
        matchTable.getUserTable(player).getHand().remove(cardNumber);
        if(cardPosition == CardPosition.WEATHER) {
            return placeWeatherCard(card);
        }
        if(cardPosition == CardPosition.SPELL) {
            return placeSpellCard(card, rowNumber);
        }
        if(card.getAbility() instanceof Spy) {
            return placeSpyCard(card, rowNumber);
        }
        return placeUnitCard(card, rowNumber);
    }

    public CommandResult placeWeatherCard(Card card) {
        matchTable.addWeatherCard(card);
        return new CommandResult(ResultCode.ACCEPT, "Weather card placed successfully");
        //done here
    }

    public CommandResult placeSpellCard(Card card, int rowNumber) {
        int player = matchTable.getTurn();
        matchTable.getUserTable(player).getRowByNumber(rowNumber).setSpecialCard(card);
        return new CommandResult(ResultCode.ACCEPT, "Spell card placed successfully");
        //done here
    }

    public CommandResult placeSpyCard(Card card, int rowNumber) {
        // Implement the logic for placing a spy card
        //TODO: complete this
        return null;
    }

    public CommandResult placeUnitCard(Card card, int rowNumber) {
        // Implement the logic for placing a unit card
        //TODO: complete this
        return null;
    }

    public CommandResult showCommander() {
        // Implement the logic for showing the commander
        return null;
    }

    public CommandResult commanderPowerPlay() {
        // Implement the logic for playing the commander's power
        return null;
    }

    public CommandResult showPlayersInfo() {
        String playersInfo = matchTable.getUser(0).getUsername() +
                " ~ Fraction: " +
                matchTable.getUserTable(0).getLeader().getFaction().name() +
                "\n" +
                matchTable.getUser(1).getUsername() +
                " ~ Fraction: " +
                matchTable.getUserTable(1).getLeader().getFaction().name();
        return new CommandResult(ResultCode.ACCEPT, playersInfo);
    }

    public CommandResult showPlayersLives() {
        // Implement the logic for showing players' lives
        String playersLives = matchTable.getUser(0).getUsername() +
                " ~ Lives: " +
                matchTable.getLife(0) +
                "\n" +
                matchTable.getUser(1).getUsername() +
                " ~ Lives: " +
                matchTable.getLife(1);
        return null;
    }

    public CommandResult showNumberOfCardsInHand() {
        // Implement the logic for showing the number of cards in hand
        int numberOfCardsInHand = matchTable.getUserTable(matchTable.getTurn()).getHand().size();
        int numberofCardsInOpponentHand = matchTable.getUserTable(1 - matchTable.getTurn()).getHand().size();
        return new CommandResult(ResultCode.ACCEPT, "Number of cards in hand: "
                + numberOfCardsInHand + "\nNumber of cards in opponent's hand: " + numberofCardsInOpponentHand);
    }

    public CommandResult showTurnInfo() {
        // Implement the logic for showing turn info
        String response = "It's " + matchTable.getUser(matchTable.getTurn()).getUsername() + "'s turn";
        return new CommandResult(ResultCode.ACCEPT, response);
    }

    public int getScoreOfRow(int player, int rowNumber) {
        Row row = getRowByPosition(player, getCardPositionByRowNumber(rowNumber));
        int score = 0;
        for(Card card : row.getCards()) {
            score += calculatePower(player, rowNumber, card);
        }
        return score;
    }

    public CommandResult showTotalScore() {
        String response = "Total score of " + matchTable.getUser(0).getUsername() + ": " +
                (getScoreOfRow(0, 0) + getScoreOfRow(0, 1) +
                        getScoreOfRow(0, 2)) +
                "\n" +
                "Total score of " + matchTable.getUser(1).getUsername() + ": " +
                (getScoreOfRow(1, 0) + getScoreOfRow(1, 1) +
                        getScoreOfRow(1, 2));
        return new CommandResult(ResultCode.ACCEPT, response);
    }

    public CommandResult showTotalScoreOfRow(int rowNumber) {
        String response = "Total score of row " + rowNumber + " of " + matchTable.getUser(0).getUsername() + ": " +
                getScoreOfRow(0, rowNumber) +
                "\n" +
                "Total score of row " + rowNumber + " of " + matchTable.getUser(1).getUsername() + ": " +
                getScoreOfRow(1, rowNumber);
        return new CommandResult(ResultCode.ACCEPT, response);
    }

    public CommandResult passTurn() {
        // Implement the logic for passing the turn
        return null;
    }
}