package ir.sharif.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ir.sharif.messages.Game.*;
import ir.sharif.messages.chat.*;
import ir.sharif.messages.ClientMessage;
import ir.sharif.messages.friends.*;
import ir.sharif.messages.ServerMessage;
import ir.sharif.model.Message;
import ir.sharif.enums.ResultCode;
import ir.sharif.messages.*;
import ir.sharif.model.GameHistory;
import ir.sharif.model.User;
import ir.sharif.service.GameHistoryService;
import ir.sharif.utils.ConstantsLoader;
import ir.sharif.view.controllers.Game;

import javax.xml.catalog.CatalogManager;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

public class TCPServerWorker extends Thread {
	private static ServerSocket server;
	private static Gson gsonAgent;

	private static final String INTERNAL_ERROR = "internal server error";
	private static final String INVALID_USERNAME = "no user exists with such username";
	private static final String USERNAME_TAKEN = "this username is taken";
	private static final String INVALID_TOKEN = "this token belongs to no user";
	private static final String WRONG_PASSWORD = "wrong password";
	private static final String BUSY_USER = "user is already logged in";

	private static int WORKERS;

	private static ArrayList<Socket> connections;

	private DataOutputStream sendBuffer;
	private DataInputStream recieveBuffer;

	private AuthHandler authHandler = new AuthHandler();
	private GameHistoryHandler gameHistoryHandler = new GameHistoryHandler();
	private GameHandler gameHandler;

	private static boolean setupServer(int portNumber, int workerNum) {
		try {
			server = new ServerSocket(portNumber);
			connections = new ArrayList <Socket>();
			WORKERS = workerNum;
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public TCPServerWorker() {
		GsonBuilder builder = new GsonBuilder();
		gsonAgent = builder.create();

		gameHandler = GameHandler.getInstance();
	}

	public void listen() throws IOException {
		Socket socket;
		while (true) {
			socket = server.accept();
			synchronized (connections) {
				connections.add(socket);
				connections.notify();
			}
		}
	}

	@Override
	public void run() {
		Socket socket;
		while (true) {
			socket = null;
			synchronized (connections) {
				while (connections.isEmpty()) {
					try {
						connections.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				socket = connections.get(0);
				connections.remove(0);
			}
			if (socket != null) {
				handleConnection(socket);
			}
		}
	}

	private String generateNewToken() {
		Random random = new Random();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 256; i++)
			sb.append((char) (random.nextInt(128)));
		return sb.toString();
	}

    private ClientMessage extractClientMessage(String clientStr) {
        try {
            ClientMessage clientMessage = gsonAgent.fromJson(clientStr, ClientMessage.class);
            switch (clientMessage.getType()) {
                case CHAT_SEND_MESSAGE:
                    return gsonAgent.fromJson(clientStr, ChatSendMessage.class);
                case LOGIN_MESSAGE:
                    return gsonAgent.fromJson(clientStr, LoginMessage.class);
                case REGISTER_MESSAGE:
                    return gsonAgent.fromJson(clientStr, RegisterMessage.class);
                case ADD_GAME_HISTORY_MESSAGE:
                    return gsonAgent.fromJson(clientStr, AddGameHistoryMessage.class);
                case GET_GAME_HISTORIES_MESSAGE:
                    return gsonAgent.fromJson(clientStr, GetGameHistoriesMessage.class);
                case CHAT_ALL_MESSAGES:
                    return gsonAgent.fromJson(clientStr, ChatAllMessage.class);
                case GET_FRIENDS_MESSAGE:
                    return gsonAgent.fromJson(clientStr, GetFriendsMessage.class);
                case FRIEND_REQUEST_CREATE_MESSAGE:
                    return gsonAgent.fromJson(clientStr, FriendRequestCreateMessage.class);
	            case PENDING_FRIEND_REQUESTS:
					return gsonAgent.fromJson(clientStr, PendingFriendRequests.class);
				case ACCEPT_FRIEND_REQUEST_MESSAGE:
                    return gsonAgent.fromJson(clientStr, AcceptFriendRequestMessage.class);
				case START_NEW_GAME_MESSAGE:
					return gsonAgent.fromJson(clientStr, StartNewGameMessage.class);
				case GAME_REQUEST_MESSAGE:
						return gsonAgent.fromJson(clientStr, GameRequestMessage.class);
				case GAME_IS_ACCEPTED_MESSAGE:
					return gsonAgent.fromJson(clientStr, GameIsAcceptedMessage.class);
				case GET_QUEUED_GAME_MESSAGE:
					return gsonAgent.fromJson(clientStr, GetQueuedGameMessage.class);
				case GAME_ACCEPT_REQUEST_MESSAGE:
					return gsonAgent.fromJson(clientStr, GameAcceptRequestMessage.class);
				case GET_GAME_RECORD_MESSAGE:
					return gsonAgent.fromJson(clientStr, GetGameRecordMessage.class);
				case GAME_ACTION_MESSAGE:
					return gsonAgent.fromJson(clientStr, GameActionMessage.class);
				case FINISH_GAME_MESSAGE:
					return gsonAgent.fromJson(clientStr, FinishGameMessage.class);
                default:
                    System.err.println("wtf: " + clientStr);
                    return null;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean sendMessage(ResultCode resultCode, String problem) {
        return sendMessage(new ServerMessage(resultCode, problem));
    }


    private boolean sendMessage(ServerMessage serverMessage){
        String failureString = gsonAgent.toJson(serverMessage);
        try {
            sendBuffer.writeUTF(failureString);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

	private boolean sendFailure(String problem) {
		return sendMessage(ResultCode.FAILED, problem);
	}

	private boolean sendSuccess(String info) {
		return sendMessage(ResultCode.ACCEPT, info);
	}


	private void handleConnection(Socket socket) {
		String clientRequest;

		try {
			recieveBuffer = new DataInputStream(
				new BufferedInputStream(socket.getInputStream())
			);
			sendBuffer = new DataOutputStream(
				new BufferedOutputStream(socket.getOutputStream())
			);


			clientRequest = recieveBuffer.readUTF();
			ClientMessage msg = extractClientMessage(clientRequest);
            splitHandler(msg);


			sendBuffer.close();
			recieveBuffer.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    void splitHandler(ClientMessage msg){
        if (msg instanceof ChatSendMessage) {
			ChatSendMessage chatSendMessage = (ChatSendMessage) msg;
			Message message = new Message(chatSendMessage.getSenderUsername(), chatSendMessage.getMessage());
			ChatService.getInstance().addMessage(message);
			sendSuccess("Message received");
        } else if(msg instanceof LoginMessage){
            LoginMessage loginMessage = (LoginMessage) msg;
            sendMessage(authHandler.login(loginMessage));
        } else if(msg instanceof RegisterMessage) {
            RegisterMessage registerMessage = (RegisterMessage) msg;
            sendMessage(authHandler.register(registerMessage));
        } else if(msg instanceof AddGameHistoryMessage) {
            AddGameHistoryMessage addGameHistoryMessage = (AddGameHistoryMessage) msg;
            sendMessage(gameHistoryHandler.addGameHistory(addGameHistoryMessage));
        } else if(msg instanceof GetGameHistoriesMessage){
            GetGameHistoriesMessage getGameHistoriesMessage = (GetGameHistoriesMessage) msg;
            sendMessage(gameHistoryHandler.getGameHistories(getGameHistoriesMessage));
        } else if (msg instanceof ChatAllMessage) {
            sendSuccess(gsonAgent.toJson(ChatService.getInstance().getMessages()));
        } else if (msg instanceof FriendRequestCreateMessage) {
            FriendRequestCreateMessage request = (FriendRequestCreateMessage) msg;
            FriendRequestService.getInstance().createFriendRequest(request.getFromUsername(), request.getTargetUsername());
            sendSuccess("Friend request sent");
        } else if (msg instanceof GetFriendsMessage) {
            sendSuccess(gsonAgent.toJson(FriendRequestService.getInstance().getFriends(((GetFriendsMessage) msg).getUsername())));
        } else if (msg instanceof AcceptFriendRequestMessage) {
            AcceptFriendRequestMessage acceptMessage = (AcceptFriendRequestMessage) msg;
            FriendRequestService.getInstance().acceptFriendRequest(acceptMessage.getFromUsername(), acceptMessage.getTargetUsername());
            sendSuccess("Friend request accepted");
        } else if (msg instanceof PendingFriendRequests) {
			sendSuccess(gsonAgent.toJson(FriendRequestService.getInstance().getPendingFriends(((PendingFriendRequests) msg).getUsername())));
        } else if(msg instanceof StartNewGameMessage){
            StartNewGameMessage startNewGameMessage = (StartNewGameMessage) msg;
            sendMessage(gameHandler.startNewGame(startNewGameMessage));
        } else if(msg instanceof GameAcceptRequestMessage) {
            GameAcceptRequestMessage gameAcceptRequestMessage = (GameAcceptRequestMessage) msg;
            sendMessage(gameHandler.gameAcceptRequest(gameAcceptRequestMessage));
        } else if(msg instanceof GameIsAcceptedMessage){
            GameIsAcceptedMessage gameIsAcceptedMessage = (GameIsAcceptedMessage) msg;
            sendMessage(gameHandler.gameIsAccepted(gameIsAcceptedMessage));
        } else if(msg instanceof GameRequestMessage){
			GameRequestMessage gameRequestMessage = (GameRequestMessage) msg;
			sendMessage(gameHandler.gameRequest(gameRequestMessage));
		} else if(msg instanceof GetQueuedGameMessage){
            GetQueuedGameMessage getQueuedGameMessage = (GetQueuedGameMessage) msg;
            sendMessage(gameHandler.getQueuedGame(getQueuedGameMessage));
        } else if(msg instanceof GetGameRecordMessage){
            GetGameRecordMessage getGameRecordMessage = (GetGameRecordMessage) msg;
            sendMessage(gameHandler.getGameRecord(getGameRecordMessage));
        } else if(msg instanceof GameActionMessage){
			GameActionMessage gameActionMessage = (GameActionMessage) msg;
			sendMessage(gameHandler.gameAction(gameActionMessage));
		} else if(msg instanceof FinishGameMessage){
			FinishGameMessage finishGameMessage = (FinishGameMessage) msg;
			sendMessage(gameHandler.finishGame(finishGameMessage));
		} else if(msg instanceof GetActionsMessage) {
			GetActionsMessage getActionsMessage = (GetActionsMessage) msg;
			sendMessage(gameHandler.getActions(getActionsMessage));
		}
		else {
	        System.err.println("invalid client command :)");
            sendFailure("Invalid message type");
        }
    }

	public static void main(String[] args) {
		try {
			TCPServerWorker.setupServer(Integer.parseInt(ConstantsLoader.getInstance().getProperty("server.port")), 10);
			for (int i = 0; i < WORKERS; i++) {
				new TCPServerWorker().start();
			}
			new TCPServerWorker().listen();
		} catch (Exception e) {
			System.out.println("Server encountered a problem!");
			e.printStackTrace();
		}
	}
}
