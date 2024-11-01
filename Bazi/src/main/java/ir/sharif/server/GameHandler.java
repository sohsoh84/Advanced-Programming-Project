package ir.sharif.server;

import com.almasb.fxgl.net.Server;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ir.sharif.enums.ResultCode;
import ir.sharif.messages.FinishGameMessage;
import ir.sharif.messages.Game.*;
import ir.sharif.messages.GetLiveGamesMessage;
import ir.sharif.messages.ServerMessage;
import ir.sharif.messages.StartNewGameMessage;
import ir.sharif.model.GameHistory;
import ir.sharif.model.Pair;
import ir.sharif.model.User;
import ir.sharif.model.server.GameRecord;
import ir.sharif.model.server.Tournament;
import ir.sharif.service.storage.Database;
import ir.sharif.utils.Random;
import ir.sharif.view.GameGraphics;
import ir.sharif.view.controllers.Game;

import javax.xml.transform.Result;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

public class GameHandler {

    private Gson gson;

    private GameHandler(){
        GsonBuilder builder = new GsonBuilder();
        gson = builder.create();
    }

    private static GameHandler instance = null;

    public static GameHandler getInstance(){
        if(instance == null) instance = new GameHandler();
        return instance;
    }

    HashMap<String, String> queuedGame = new HashMap<>(); // key username, value gameToken
    HashMap<String, GameRecord> pendingGames = new HashMap<>(); // key gameToken, value gameRecord
	HashMap<String, GameRecord> liveGames = new HashMap<>(); // key gameToken, value gameRecord

    HashMap<String, User> matchedRandom = new HashMap<>();
    ArrayList<Pair<User, String>> matchedRandomQueue = new ArrayList<>();
    HashMap<String, Boolean> userInGame = new HashMap<>();

    public synchronized ServerMessage startNewGame(StartNewGameMessage startNewGameMessage) {
        Database database = Database.getInstance();
        String gameToken = Random.generateNewToken();

        userInGame.put(startNewGameMessage.getUser1().getUsername(), true);
        userInGame.put(startNewGameMessage.getUser2().getUsername(), true);

        if(startNewGameMessage.getUser1().getUsername().compareTo(startNewGameMessage.getUser2().getUsername()) > 0){
            User tmp = startNewGameMessage.getUser1();
            startNewGameMessage.setUser1(startNewGameMessage.getUser2());
            startNewGameMessage.setUser2(tmp);
        }

        GameRecord gameRecord = new GameRecord(startNewGameMessage.getUser1(), startNewGameMessage.getUser2(),
                gameToken, startNewGameMessage.getTournamentToken(), startNewGameMessage.isPrivate());

        liveGames.put(gameToken, gameRecord);

        return new ServerMessage(ResultCode.ACCEPT, gameToken);
    }

    public synchronized ServerMessage randomGameRequest(RandomGameRequestMessage randomGameRequestMessage){
        User user = randomGameRequestMessage.getUser();
        String gameToken;
        if(matchedRandomQueue.isEmpty()){
            gameToken = Random.generateNewToken();
            matchedRandomQueue.add(new Pair<>(user, gameToken));
        } else{
            Pair<User, String> match = matchedRandomQueue.get(0);
            matchedRandomQueue.remove(match);
            User user2 = match.getFirst();
            gameToken = match.getSecond();
            matchedRandom.put(user.getUsername(), user2);
            matchedRandom.put(user2.getUsername(), user);

            if(user.getUsername().compareTo(user2.getUsername()) > 0){
                User tmp = user;
                user = user2;
                user2 = tmp;
            }

            userInGame.put(user.getUsername(), true);
            userInGame.put(user2.getUsername(), true);
            GameRecord gameRecord = new GameRecord(user, user2, gameToken, null, false);
            liveGames.put(gameToken, gameRecord);
        }
        System.err.println(user.getUsername() + " sss " + gameToken);
        return new ServerMessage(ResultCode.ACCEPT, gameToken);
    }

    public synchronized ServerMessage randomGameIsAccepted(RandomGameIsAcceptedMessage randomGameIsAcceptedMessage){
        String username = randomGameIsAcceptedMessage.getUsername();
        String gameToken = randomGameIsAcceptedMessage.getGameToken();
        System.err.println(username + " sss " + gameToken + "   " +matchedRandom.containsKey(username));
        if(matchedRandom.containsKey(username)){
            User opponentUser = matchedRandom.get(username);
            matchedRandom.remove(username);
            return new ServerMessage(ResultCode.ACCEPT, gson.toJson(opponentUser));
        }
        return new ServerMessage(ResultCode.NOT_FOUND, "no opponent founded!");
    }

    public synchronized ServerMessage gameRequest(GameRequestMessage gameRequestMessage) {
//        if(userInGame.getOrDefault(gameRequestMessage.getReceiver(), false)){
//            return new ServerMessage(ResultCode.FAILED, gameRequestMessage.getReceiver() + " is in the middle of a game!");
//        }
//        if(UserHandler.getInstance().getUserStatus().getOrDefault(gameRequestMessage.getReceiver(), false)){
//            return new ServerMessage(ResultCode.FAILED, gameRequestMessage.getReceiver() + " is not available!");
//        }

        String token = Random.generateNewToken();
        System.err.println(token);
        GameRecord gameRecord = new GameRecord(gameRequestMessage.getUser(), null, token, null, gameRequestMessage.isPrivate());
        pendingGames.put(token, gameRecord);
        queuedGame.put(gameRequestMessage.getReceiver(), token);
        return new ServerMessage(
                ResultCode.ACCEPT,
                token
        );
    }

    public synchronized ServerMessage gameIsAccepted(GameIsAcceptedMessage gameIsAcceptedMessage){
        String token = gameIsAcceptedMessage.getGameToken();
        if(liveGames.containsKey(token)){
            return new ServerMessage(ResultCode.ACCEPT, gson.toJson(liveGames.get(token).getUser2()));
        }
        return new ServerMessage(ResultCode.FAILED, "game doesnt accepted");
    }

    public synchronized ServerMessage getQueuedGame(GetQueuedGameMessage getQueuedGameMessage){
        if(queuedGame.containsKey(getQueuedGameMessage.getUsername())){
            return new ServerMessage(ResultCode.ACCEPT, queuedGame.get(getQueuedGameMessage.getUsername()));
        }
        return new ServerMessage(ResultCode.NOT_FOUND, "not pending game exits for this user");
    }

    public synchronized ServerMessage gameAcceptRequest(GameAcceptRequestMessage gameAcceptRequestMessage) {
        String token = gameAcceptRequestMessage.getGameToken();
        User user2 = gameAcceptRequestMessage.getUser();
        System.err.println(token);
        if(!pendingGames.containsKey(token)){
            return new ServerMessage(ResultCode.NOT_FOUND, "game with this token not exist");
        }
        GameRecord gameRecord = pendingGames.get(token);
        pendingGames.remove(token);
        queuedGame.remove(user2.getUsername());
        gameRecord.setUser2(user2);

        userInGame.put(gameRecord.getUser1().getUsername(), true);
        userInGame.put(gameRecord.getUser2().getUsername(), true);

        liveGames.put(token, gameRecord);
        return new ServerMessage(ResultCode.ACCEPT, gson.toJson(gameRecord.getUser1()));
    }

    public synchronized ServerMessage getGameRecord(GetGameRecordMessage getGameRecordMessage) {
        String token = getGameRecordMessage.getGameToken();
	    if(pendingGames.containsKey(token)){
		    return new ServerMessage(ResultCode.ACCEPT, gson.toJson(pendingGames.get(token)));
	    }

        if(liveGames.containsKey(token)){
            return new ServerMessage(ResultCode.ACCEPT, gson.toJson(liveGames.get(token)));
        }
        GameRecord gameRecord = Database.getInstance().getGameRecordWithId(token);
        if(gameRecord != null){
            return new ServerMessage(ResultCode.ACCEPT, gson.toJson(gameRecord));
        }
        return new ServerMessage(ResultCode.NOT_FOUND, "dont exist such game with this token = " + token);
    }

    public synchronized ServerMessage gameAction(GameActionMessage gameActionMessage) {
        String token = gameActionMessage.getGameToken();
        String action = gameActionMessage.getAction();

        System.err.println(token + " " + action);

        if(!liveGames.containsKey(token)){
            return new ServerMessage(ResultCode.FAILED, "game with this token not exist");
        }

        GameRecord gameRecord = liveGames.get(token);
        gameRecord.getCommands().add(action);
        return new ServerMessage(ResultCode.ACCEPT, "action added successfully");
    }

    public synchronized ServerMessage finishGame(FinishGameMessage finishGameMessage) {
        String token = finishGameMessage.getGameToken();
        GameHistory gameHistory = finishGameMessage.getGameHistory();
        if(!liveGames.containsKey(token)){
            return new ServerMessage(ResultCode.FAILED, "game with this token not exist");
        }
        GameRecord gameRecord = liveGames.get(token);
        liveGames.remove(token);

        Database.getInstance().addGameRecord(gameRecord);
        Database.getInstance().addGameHistories(gameHistory);

        userInGame.put(gameRecord.getUser1().getUsername(), false);
        userInGame.put(gameRecord.getUser2().getUsername(), false);

        TournamentHandler.getInstance().finishGame(gameHistory);

        return new ServerMessage(ResultCode.ACCEPT, "game finished successfully");
    }


    public synchronized ServerMessage getActions(GetActionsMessage getActionsMessage) {
        int buffer = getActionsMessage.getBuffer();
        String token = getActionsMessage.getGameToken();

        GameRecord gameRecord;
        if(!liveGames.containsKey(token)){
            gameRecord = Database.getInstance().getGameRecordWithId(token);
        } else {
            gameRecord = liveGames.get(token);
        }

        ArrayList<String> newActions = new ArrayList<>();

		try {
			for (int newActionIndex = buffer; newActionIndex < gameRecord.getCommands().size(); newActionIndex++) {
				newActions.add(gameRecord.getCommands().get(newActionIndex));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

        return new ServerMessage(ResultCode.ACCEPT, gson.toJson(newActions));
    }
    public synchronized ServerMessage getLiveGames(GetLiveGamesMessage getLiveGamesMessage){
        ArrayList<GameRecord> result = new ArrayList<>();
        liveGames.forEach((key, value) -> {
            result.add(value);
        });
        return new ServerMessage(ResultCode.ACCEPT, gson.toJson(result));
    }
}
