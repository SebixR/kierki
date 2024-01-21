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


public class Client {
    private Socket clientSocket = null;
    private int clientId;
    private String username = null;
    private int currentRoomId = 0;
    private List<Card> cardsInHand = new ArrayList<>();
    private HashMap<Integer, Room> rooms;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    private Stage primaryStage;
    private UsernameController usernameController;
    private RoomsController roomsController;
    private FXMLLoader roomsLoader;
    private WaitingController waitingController;
    private FXMLLoader waitingLoader;
    private GameController gameController;
    private FXMLLoader gameLoader;
    private Scene roomsScene;
    private Scene waitingScene;
    private Scene gameScene;

    public Client(Socket socket, Stage primaryStage,  UsernameController usernameController, RoomsController roomsController, FXMLLoader roomsLoader, WaitingController waitingController, FXMLLoader waitingLoader, GameController gameController, FXMLLoader gameLoader){
        try {
            this.clientSocket = socket;
            this.primaryStage = primaryStage;
            this.usernameController = usernameController;
            this.roomsController = roomsController;
            this.roomsLoader = roomsLoader;
            this.waitingController = waitingController;
            this.waitingLoader = waitingLoader;
            this.gameController = gameController;
            this.gameLoader = gameLoader;

            this.roomsScene = new Scene(roomsLoader.getRoot());
            this.waitingScene = new Scene(waitingLoader.getRoot());
            this.gameScene = new Scene(gameLoader.getRoot());

            this.out = new ObjectOutputStream(clientSocket.getOutputStream());
            this.in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void receiveID() throws IOException, ClassNotFoundException {
        clientId = (int) in.readObject();
    }

    public void receiveRooms() throws IOException, ClassNotFoundException {
        rooms = (HashMap<Integer, Room>) in.readObject();

        for (Map.Entry<Integer, Room> entry : rooms.entrySet()) {
            roomsController.addRoom(entry.getValue());
        }
    }

    public void requestUsername(String username) throws IOException {
        out.reset();
        out.writeObject(Request.REQUEST_USERNAME);
        out.writeObject(username);
        out.flush();
    }

    public void requestRoomAdd() throws IOException {
        out.reset();
        out.writeObject(Request.CREATE_ROOM);
        out.flush();
    }

    public void joinCreatedRoom() {
        primaryStage.setScene(waitingScene);
    }

    public void joinExistingRoom(int roomId) throws IOException {
        out.reset();
        out.writeObject(Request.JOIN_ROOM);
        out.writeInt(roomId);
        out.flush();
    }

    public void startGame() throws IOException {
        out.reset();
        out.writeObject(Request.DEAL_CARDS);
        out.writeObject(currentRoomId);

        gameController.setLabels(rooms.get(currentRoomId).getConnectedPlayers(), rooms.get(currentRoomId).getConnectedPlayersNames());

        primaryStage.setScene(gameScene);
    }

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

    public void requestPlayerInvite(int playerId) throws IOException {
        out.writeObject(Request.INVITE_PLAYER);
        out.writeInt(playerId);
        out.flush();

        System.out.println("Invited: " + playerId);
    }

    public void exitGame() throws IOException {
        out.writeObject(Request.EXIT_GAME);
    }

    // turn the individual if's into methods
    public void listen(){
        new Thread(() -> {
            while (clientSocket.isConnected())
            {
                try {
                    Response response = (Response) in.readObject();

                    if (response == Response.SET_USERNAME) {
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
                    if (response == Response.ROOMS_UPDATE) {
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
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        });

                        rooms.put(roomId, room);//won't add a new one if the key's already there
                    }
                    else if (response == Response.ROOM_CREATED) {
                        Room room = (Room) in.readObject();

                        Platform.runLater(() -> waitingController.addPlayerLabel(username));

                        currentRoomId = room.getRoomId();
                        rooms.put(room.getRoomId(), room);
                    }
                    else if (response == Response.INVITATION) {
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

                            popupController.setInviterLabel(String.valueOf(invitation.getInviterId()));
                            popupController.setClient(this);
                            popupController.setRoomId(invitation.getRoomId());
                            popupController.setPopupStage(popupStage);

                            Scene popupScene = new Scene(popupLoader.getRoot());

                            popupStage.setScene(popupScene);
                            popupStage.showAndWait();
                        });
                    }
                    else if (response == Response.JOINED_ROOM) {
                        Room room = (Room) in.readObject();
                        currentRoomId = room.getRoomId();

                        rooms.put(room.getRoomId(), room); //won't add a new one if the key's already there

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
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }
                    else if (response == Response.DEALT_CARDS) {
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
                    else if (response == Response.PLAYED_CARD) {
                        Card playedCard = (Card) in.readObject();
                        Room room = (Room) in.readObject();
                        rooms.put(room.getRoomId(), room); //updates the turn
                        System.out.println("Client succesfully played card: " + playedCard.getValue() + " " + playedCard.getSuit());

                        int cardIndex = findCard(playedCard);
                        cardsInHand.set(cardIndex, playedCard);

                        Platform.runLater(() -> gameController.playCard(cardIndex));
                    }
                    else if (response == Response.CARDS_UPDATE) {
                        Card playedCard = (Card) in.readObject();
                        Room room = (Room) in.readObject();
                        rooms.put(room.getRoomId(), room);
                        System.out.println("Player: " + playedCard.getClientId() + " played: " + playedCard.getValue() + " " + playedCard.getSuit());

                        Platform.runLater(() -> gameController.placeOtherPlayersCard(playedCard,  playedCard.getClientId()));
                    }
                    else if (response == Response.TURN_OVER) {
                        int takerClientId = (int) in.readObject();
                        int receivedPoints = (int) in.readObject();
                        Room room = (Room) in.readObject();
                        rooms.put(room.getRoomId(), room);
                        System.out.println("Player: " + takerClientId + " received: " + receivedPoints + " points");

                        Platform.runLater(() ->{
                            gameController.clearTable();
                            gameController.updatePoints(takerClientId, room.getPlayerPoints().get(takerClientId));
                        });
                    }
                    else if (response == Response.ROUND_OVER) {
                        Room room = (Room) in.readObject();
                        rooms.put(room.getRoomId(), room);

                        out.writeObject(Request.DEAL_CARDS);
                        out.writeObject(currentRoomId);

                        Platform.runLater(() -> gameController.updateRound(room.getCurrentRound()));
                    }
                    else if (response == Response.GAME_OVER) {
                        String winnerName = (String) in.readObject();
                        Room room = (Room) in.readObject();

                        rooms.remove(room.getRoomId());

                        Platform.runLater(() -> {
                            gameController.showWinner(winnerName);
                            waitingController.clearLabels();
                            roomsController.removeRoomButton(room.getRoomId());
                        });
                    }

                } catch (Exception e) {
                    closeEverything();
                }
            }
        }).start();
    }

    /**
     * Function to find the given card in the player's hand, after its fields have been changed by the server
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

    public void disconnectClient() throws IOException {
        out.writeObject(Request.DISCONNECT);
        out.writeObject(currentRoomId);
    }

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
    public FXMLLoader getRoomsLoader() {
        return roomsLoader;
    }
    public Scene getRoomsScene() {
        return this.roomsScene;
    }
    public void setRoomsController(RoomsController roomsController) {
        this.roomsController = roomsController;
    }
}
