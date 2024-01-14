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
    private int currentRoomId = 0;
    private List<Card> cardsInHand = new ArrayList<>();
    private HashMap<Integer, Room> rooms;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    private Stage primaryStage;
    private RoomsController roomsController;
    private WaitingController waitingController;
    private FXMLLoader waitingLoader;
    private GameController gameController;
    private FXMLLoader gameLoader;

    public Client(Socket socket, Stage primaryStage, RoomsController roomsController, WaitingController waitingController, FXMLLoader waitingLoader, GameController gameController, FXMLLoader gameLoader){
        try {
            this.clientSocket = socket;
            this.primaryStage = primaryStage;
            this.roomsController = roomsController;
            this.waitingController = waitingController;
            this.waitingLoader = waitingLoader;
            this.gameController = gameController;
            this.gameLoader = gameLoader;

            this.out = new ObjectOutputStream(clientSocket.getOutputStream());
            this.in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void receiveID() throws IOException {
        clientId = in.readInt();
    }

    public void receiveRooms() throws IOException, ClassNotFoundException {
        rooms = (HashMap<Integer, Room>) in.readObject();

        for (Map.Entry<Integer, Room> entry : rooms.entrySet()) {
            roomsController.addRoom(entry.getValue());
        }
    }

    public void requestRoomAdd() throws IOException {
        out.reset();
        out.writeObject(Request.CREATE_ROOM);
        out.flush();
    }

    public void joinCreatedRoom() {
        Scene gameScene = new Scene(waitingLoader.getRoot());
        primaryStage.setScene(gameScene);
    }

    public void joinExistingRoom(int roomId) throws IOException {
        out.reset();
        out.writeObject(Request.JOIN_ROOM);
        out.writeInt(roomId);
        out.flush();
    }

    public void startGame() throws IOException {
        out.reset();
        out.writeObject(Request.START_GAME);
        out.writeObject(currentRoomId);

        gameController.setLabels(rooms.get(currentRoomId).getConnectedPlayers());

        Scene gameScene = new Scene(gameLoader.getRoot());
        primaryStage.setScene(gameScene);
    }

    public void requestPlayerInvite(int playerId) throws IOException {
        out.writeObject(Request.INVITE_PLAYER);
        out.writeInt(playerId);
        out.flush();

        System.out.println("Invited: " + playerId);
    }

    // turn the individual if's into methods
    public void listen(){
        new Thread(() -> {
            while (clientSocket.isConnected())
            {
                try {
                    Response response = (Response) in.readObject();

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
                                for (int player : room.getConnectedPlayers()) {
                                    waitingController.addPlayerLabel(String.valueOf(player));
                                }

                                if (room.isFull()) {
                                    try {
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

                        Platform.runLater(() -> waitingController.addPlayerLabel(String.valueOf(clientId)));

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
                            Scene gameScene = new Scene(waitingLoader.getRoot());
                            primaryStage.setScene(gameScene);
                            waitingController.addPlayerLabel(String.valueOf(this.clientId));
                            waitingController.hideInvitePane();

                            waitingController.getConnectedPlayersVBox().getChildren().clear();
                            for (int player : room.getConnectedPlayers()) {
                                waitingController.addPlayerLabel(String.valueOf(player));
                            }

                            if (room.isFull()) {
                                try {
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

                        System.out.println("Received cards:");
                        for (Card card : cardsInHand) {
                            System.out.println(card.getValue() + " " + card.getSuit());
                        }
                    }

                } catch (Exception e) {
                    closeEverything();
                }
            }
        }).start();
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

    public Integer getClientId() {
        return clientId;
    }

    public HashMap<Integer, Room> getRooms() {
        return rooms;
    }

}
