package com.example.kierki;


import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 * Class representing the client.
 * <p>
 *     The Client class is responsible for most client-server communication, and thus holds all the
 *     information the client received. In addition it also has access to multiple javaFx loaders
 *     and controllers, so that it can update the GUI, which in this case i the HeartsApp application.
 * </p>
 * <p>
 *     The GUI's elements like loaders and scenes are only created once. Their contents are cleared
 *     and generated anew whenever that is necessary.
 * </p>
 */
public class Client {
    private Socket clientSocket = null;
    private int clientId;
    private String username = null;
    private int currentRoomId = 0;
    private final List<Card> cardsInHand = new ArrayList<>();
    private HashMap<Integer, Room> rooms;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    private Stage primaryStage;
    private UsernameController usernameController;
    private RoomsController roomsController;
    private WaitingController waitingController;
    private GameController gameController;
    private Scene roomsScene;
    private Scene waitingScene;
    private Scene gameScene;

    /**
     * The Client class constructor.
     * @param socket the socket used to communicate with the corresponding instance of ClientHandler
     * @param primaryStage main javaFX stage, i.e. the application's main window
     * @param usernameController javaFX controller for the login scene
     * @param roomsController javaFX controller for the lobby scene
     * @param roomsLoader javaFX loader for the lobby scene
     * @param waitingController javaFX controller for the waiting for players scene
     * @param waitingLoader javaFX loader for the waiting for players scene
     * @param gameController javaFX controller for the game scene
     * @param gameLoader javaFX loader for the game scene
     */
    public Client(Socket socket, Stage primaryStage,  UsernameController usernameController, RoomsController roomsController, FXMLLoader roomsLoader, WaitingController waitingController, FXMLLoader waitingLoader, GameController gameController, FXMLLoader gameLoader){
        try {
            this.clientSocket = socket;
            this.primaryStage = primaryStage;
            this.usernameController = usernameController;
            this.roomsController = roomsController;
            this.waitingController = waitingController;
            this.gameController = gameController;

            this.roomsScene = new Scene(roomsLoader.getRoot());
            this.waitingScene = new Scene(waitingLoader.getRoot());
            this.gameScene = new Scene(gameLoader.getRoot());

            this.out = new ObjectOutputStream(clientSocket.getOutputStream());
            this.in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            closeEverything();
        }
    }

    /**
     * The first method used by the client after establishing connection with the server.
     * Used to get a unique ID, given by the server application.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    public void receiveID() throws IOException, ClassNotFoundException {
        clientId = (int) in.readObject();
    }

    /**
     * Method used by the client after logging in, responsible for receiving the currently open rooms.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    public void receiveRooms() throws IOException, ClassNotFoundException {
        rooms = (HashMap<Integer, Room>) in.readObject();

        for (Map.Entry<Integer, Room> entry : rooms.entrySet()) {
            roomsController.addRoom(entry.getValue());
        }
    }

    /**
     * Requests setting a username. The server receives it, and checks if the username is taken or not.
     * @param username the requested username
     * @throws IOException in/out communication exception
     */
    public void requestUsername(String username) throws IOException {
        out.reset();
        out.writeObject(Request.REQUEST_USERNAME);
        out.writeObject(username);
        out.flush();
    }

    /**
     * Requests the creation of a new room.
     * @throws IOException in/out communication exception
     */
    public void requestRoomAdd() throws IOException {
        out.reset();
        out.writeObject(Request.CREATE_ROOM);
        out.flush();
    }

    /**
     * Updates the GUI after the client joins the room they themselves created.
     */
    public void joinCreatedRoom() {
        primaryStage.setScene(waitingScene);
        waitingController.showInvitePane();
    }

    /**
     * Requests joining an existing room.
     * @param roomId the ID of the room the client wants to join.
     * @throws IOException in/out communication exception
     */
    public void joinExistingRoom(int roomId) throws IOException {
        out.reset();
        out.writeObject(Request.JOIN_ROOM);
        out.writeInt(roomId);
        out.flush();
    }

    /**
     * Starts the game; asks the server for cards and updates the GUI.
     * @throws IOException in/out communication exception
     */
    public void startGame() throws IOException {
        out.reset();
        out.writeObject(Request.DEAL_CARDS);
        out.writeObject(currentRoomId);

        gameController.setLabels(rooms.get(currentRoomId).getConnectedPlayers(), rooms.get(currentRoomId).getConnectedPlayersNames());

        primaryStage.setScene(gameScene);
    }

    /**
     * Asks the server to play a card, but only if it's this client's turn to do so.
     * This method is called when a javaFX ImageView representing a card is clicked.
     * @param cardIndex the index of the card the user wants to play (0 - 12).
     * @throws IOException in/out communication exception
     */
    public void playCard(int cardIndex) throws IOException {
        if (rooms.get(currentRoomId).getCurrentTurn() == this.clientId)
        {
            out.reset();
            out.writeObject(Request.PLAY_CARD);
            out.writeObject(cardsInHand.get(cardIndex));
            out.writeObject(currentRoomId);
            out.flush();
            System.out.println("Sent request to play card: " + cardsInHand.get(cardIndex).getValue() + " " + cardsInHand.get(cardIndex).getSuit());
        }
    }

    /**
     * Asks the server to invite a player. Only the host can invite players.
     * @param username the username of the player the host wants to invite.
     * @throws IOException IOException in/out communication exception
     */
    public void requestPlayerInvite(String username) throws IOException {
        out.writeObject(Request.INVITE_PLAYER);
        out.writeObject(username);
        out.flush();

        System.out.println("Invited: " + username);
    }

    /**
     * Notifies the server the user has exited the application.
     * @throws IOException OException in/out communication exception
     */
    public void exitGame() throws IOException {
        out.writeObject(Request.EXIT_GAME);
    }


    /**
     * Main method used to receive information from the server.
     * Starts a new thread which handles various server messages,
     * depending on the received Request object
     */
    public void listen(){
        new Thread(() -> {
            while (clientSocket.isConnected())
            {
                try {
                    Response response = (Response) in.readObject();

                    if (response == Response.SET_USERNAME) {
                        handleSetUsername();
                    }
                    if (response == Response.ROOMS_UPDATE) {
                        handleRoomsUpdate();
                    }
                    else if (response == Response.ROOM_CREATED) {
                        handleRoomCreated();
                    }
                    else if (response == Response.INVITATION) {
                        handleInvitation();
                    }
                    else if (response == Response.JOINED_ROOM) {
                        handleJoinedRoom();
                    }
                    else if (response == Response.DEALT_CARDS) {
                        handleDealtCards();
                    }
                    else if (response == Response.PLAYED_CARD) {
                        handlePlayedCard();
                    }
                    else if (response == Response.CARDS_UPDATE) {
                        handleCardsUpdate();
                    }
                    else if (response == Response.TURN_OVER) {
                        handleTurnOver();
                    }
                    else if (response == Response.ROUND_OVER) {
                        handleRoundOver();
                    }
                    else if (response == Response.GAME_OVER) {
                        handleGameOver();
                    }

                } catch (Exception e) {
                    closeEverything();
                }
            }
        }).start();
    }

    /**
     * Handles setting the client's username. If the username is unique, and thus allowed,
     * the client receives a HashMap containing currently open rooms, and updates the GUI.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handleSetUsername() throws IOException, ClassNotFoundException {
        boolean usernameSet = (boolean) in.readObject();

        if (usernameSet) {
            this.username = (String) in.readObject();
            receiveRooms();
            System.out.println("Available rooms:");
            for (Map.Entry<Integer, Room> entry : rooms.entrySet()) {
                System.out.println("Id: " + entry.getValue().getRoomId() + " players: " + entry.getValue().getPlayerAmount() + "/4\n");
            }

            Platform.runLater(() -> {
                primaryStage.setScene(roomsScene);
            });
        }
        else {
            Platform.runLater(() -> usernameController.showErrorLabel());
        }
    }

    /**
     * Handles an update to the HashMap containing rooms. Updates the list of rooms in the GUI.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handleRoomsUpdate() throws IOException, ClassNotFoundException {
        Room room = (Room) in.readObject();
        int roomId = room.getRoomId();

        if (rooms.get(roomId) == null){
            roomsController.addRoom(room);
        }

        System.out.println("Room size: " + room.getPlayerAmount());

        Platform.runLater(() -> {
            roomsController.updateRoomButton(room.getRoomId(), room.getPlayerAmount());

            if (currentRoomId == room.getRoomId())
            {
                waitingController.getConnectedPlayersVBox().getChildren().clear();
                for (int i = 0; i < room.getPlayerAmount(); i++) {
                    waitingController.addPlayerLabel(room.getConnectedPlayersNames().get(i));
                }

                if (room.isFull()) {
                    try {
                        gameController.clear();
                        startGame();
                        gameController.highlightPlayer(room.getHostId());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        rooms.put(roomId, room);//won't add a new one if the key's already there
    }

    /**
     * Handles a situation where a new room has been created by this client.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handleRoomCreated() throws IOException, ClassNotFoundException {
        Room room = (Room) in.readObject();

        Platform.runLater(() -> waitingController.addPlayerLabel(username));

        currentRoomId = room.getRoomId();
        rooms.put(room.getRoomId(), room);
    }

    /**
     * Handles receiving an invitation from another client. Creates a popup window on the
     * client's screen, asking them to accept the invite. Updates the GUI to show the main game scene,
     * if the client accepted the invitation.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handleInvitation() throws IOException, ClassNotFoundException {
        Invitation invitation = (Invitation) in.readObject();
        System.out.println("Received invitation");

        Platform.runLater(() -> {
            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL); //blocks other events until is closed
            popupStage.setTitle("Invitation");
            FXMLLoader popupLoader = new FXMLLoader(getClass().getResource("popup.fxml"));
            try {
                popupLoader.load();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            PopupController popupController = popupLoader.getController();

            popupController.setInviterLabel(invitation.getInviterName());
            popupController.setClient(this);
            popupController.setRoomId(invitation.getRoomId());
            popupController.setPopupStage(popupStage);

            Scene popupScene = new Scene(popupLoader.getRoot());

            popupStage.setScene(popupScene);
            popupStage.showAndWait();
        });
    }

    /**
     * Handles joining another player's room. Updates the GUI accordingly.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handleJoinedRoom() throws IOException, ClassNotFoundException {
        Room room = (Room) in.readObject();
        currentRoomId = room.getRoomId();

        rooms.put(room.getRoomId(), room);

        Platform.runLater(() -> {
            primaryStage.setScene(waitingScene);
            waitingController.addPlayerLabel(this.username);
            waitingController.hideInvitePane();

            waitingController.getConnectedPlayersVBox().getChildren().clear();
            for (int i = 0; i < room.getPlayerAmount(); i++) {
                waitingController.addPlayerLabel(room.getConnectedPlayersNames().get(i));
            }

            if (room.isFull()) {
                try {
                    gameController.clear();
                    startGame();
                    gameController.highlightPlayer(room.getHostId());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Handles receiving cards from the server at the start of every round.
     * Updates the GUI accordingly.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handleDealtCards() throws IOException, ClassNotFoundException {
        cardsInHand.clear();

        for (int i = 0; i < 13; i++)
        {
            Card card = (Card) in.readObject();
            cardsInHand.add(card);
        }

        cardsInHand.sort(new Card.CardComparator());

        System.out.println("Received cards:");
        for (Card card : cardsInHand) {
            System.out.println(card.getValue() + " " + card.getSuit());
        }

        Platform.runLater(() -> gameController.generatePlayerCards());
    }

    /**
     * Handles a situation when this client was allowed to play a card they chose,
     * using the playCard() method.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handlePlayedCard() throws IOException, ClassNotFoundException {
        Card playedCard = (Card) in.readObject();
        Room room = (Room) in.readObject();
        rooms.put(room.getRoomId(), room); //updates the turn
        System.out.println("Client succesfully played card: " + playedCard.getValue() + " " + playedCard.getSuit());

        int cardIndex = findCard(playedCard);
        cardsInHand.set(cardIndex, playedCard);

        Platform.runLater(() -> {
            gameController.playCard(cardIndex);
            gameController.highlightPlayer(room.getCurrentTurn());
        });
    }

    /**
     * Handles an update to the cards on the table. The client receives a card another client played,
     * and updates the GUI accordingly.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handleCardsUpdate() throws IOException, ClassNotFoundException {
        Card playedCard = (Card) in.readObject();
        Room room = (Room) in.readObject();
        rooms.put(room.getRoomId(), room);
        System.out.println("Player: " + playedCard.getClientId() + " played: " + playedCard.getValue() + " " + playedCard.getSuit());

        Platform.runLater(() -> {
            gameController.placeOtherPlayersCard(playedCard,  playedCard.getClientId());
            gameController.highlightPlayer(room.getCurrentTurn());
        });
    }

    /**
     * Handles the end of the turn, that is, all four players having played a card, and one of them
     * having received points. Updates the GUI accordingly.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handleTurnOver() throws IOException, ClassNotFoundException {
        int takerClientId = (int) in.readObject();
        int receivedPoints = (int) in.readObject();
        Room room = (Room) in.readObject();
        rooms.put(room.getRoomId(), room);
        System.out.println("Player: " + takerClientId + " received: " + receivedPoints + " points");

        Platform.runLater(() ->{
            gameController.clearTable();
            gameController.updatePoints(takerClientId, room.getPlayerPoints().get(takerClientId));
            gameController.highlightPlayer(room.getCurrentTurn());
        });
    }

    /**
     * Handles ending a round. This means all cards have been played,
     * and it's time to move on to the next round. Asks the server to deal cards again.
     * Updates the GUI  accordingly.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handleRoundOver() throws IOException, ClassNotFoundException {
        Room room = (Room) in.readObject();
        rooms.put(room.getRoomId(), room);

        out.writeObject(Request.DEAL_CARDS);
        out.writeObject(currentRoomId);

        Platform.runLater(() -> {
            gameController.updateRound(room.getCurrentRound());
            gameController.highlightPlayer(room.getCurrentTurn());
        });
    }

    /**
     * Handles the game ending. The client receives the winner's name, and displays a popup with it inside.
     * Also removes the room in which the game was taking place from this client's local HashMap of rooms.
     * @throws IOException in/out communication exception
     * @throws ClassNotFoundException if the class can't be identified after deserialization
     */
    private void handleGameOver() throws IOException, ClassNotFoundException {
        String winnerName = (String) in.readObject();
        Room room = (Room) in.readObject();

        rooms.remove(room.getRoomId());

        Platform.runLater(() -> {
            gameController.showWinner(winnerName);
            waitingController.clearLabels();
            roomsController.removeRoomButton(room.getRoomId());
        });
    }

    /**
     * Function to find the given card in the player's hand, after its properties have been
     * changed by the server.
     * @param updatedCard the card it looks for
     */
    public int findCard(Card updatedCard) {
        int index = 0;
        for (Card card : this.cardsInHand) {
            if (card.getSuit() == updatedCard.getSuit() && card.getValue() == updatedCard.getValue()) {
                break;
            }
            index++;
        }
        return index;
    }

    /**
     * Used to inform the server that the client has disconnected. The server then ends the game.
     * @throws IOException in/out communication exception
     */
    public void disconnectClient() throws IOException {
        out.writeObject(Request.DISCONNECT);
        out.writeObject(currentRoomId);
    }

    /**
     * Severs al client-server communication.
     */
    public void closeEverything() {
        try {
            if (in != null)
            {
                in.close();
            }
            if (out != null)
            {
                out.close();
            }
            if (clientSocket != null){
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public List<Card> getCardsInHand() {
        return this.cardsInHand;
    }
    public Integer getClientId() {
        return clientId;
    }
    public String getUsername() {
        return this.username;
    }
    public Stage getPrimaryStage() {
        return this.primaryStage;
    }
    public Scene getRoomsScene() {
        return this.roomsScene;
    }
}
