package com.example.kierki;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.HashMap;

/**
 * The JavaFX controller responsible for generating and updating the lobby scene.
 * <p>
 *     It contains a Client object, so that the client can call methods using the GUI.
 *     It also houses all FXML objects, which connect to the fx IDs in the game.fxml file.
 * </p>
 */
public class RoomsController {

    private Client client;
    private final HashMap<Integer, Button> roomButtons = new HashMap<>();
    @FXML
    private VBox availableRoomsList;
    @FXML
    private AnchorPane roomsAnchorPane;

    /**
     * A JavaFX method assigned to a button's onAction field.
     * When the button is clicked, the client requests creating a new room, and joins it.
     * @throws IOException in/out communication exception
     */
    @FXML
    public void createRoomButton() throws IOException {
        client.requestRoomAdd();
        client.joinCreatedRoom();
    }

    /**
     * A JavaFX method assigned to a button's onAction field.
     * When the button is clicked, the application shuts down, after having notified the server of the
     * client's disconnection.
     * @throws IOException in/out communication exception
     */
    @FXML
    public void exit() throws IOException {
        client.disconnectClient();
        Platform.exit();
    }

    /**
     * Used to stylize the lobby scene, instead of using a CSS stylesheet.
     */
    public void stylize() {
        roomsAnchorPane.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 100%, #b7b7b7, #f1f1f1);");
    }

    /**
     * Updates the list of currently open rooms whenever a new room has been created,
     * setting the correct labels and creating a new onClick
     * event, which calls a method that requests the server to join the room.
     * @param room a newly created room, to be added to the list of buttons.
     */
    public void addRoom(Room room){
        Platform.runLater(() -> {
            Button button = new Button();
            if (room.isFull())
            {
                button.setText("Full! 4/4");
            }
            else
            {
                button.setText("Room ID: " + room.getRoomId() +  " Players " + room.getPlayerAmount() + "/4");
            }

            availableRoomsList.getChildren().add(button);
            roomButtons.put(room.getRoomId(), button);

            button.setOnAction(e -> {
                try {
                    client.joinExistingRoom(room.getRoomId());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        });
    }

    /**
     * Updates the list of room buttons whenever a player joins a room.
     * @param roomId the ID of the room which changed.
     * @param playerCount the amount of players now in the room.
     */
    public void updateRoomButton(int roomId, int playerCount) {
        if (roomButtons.get(roomId) != null)
        {
            Platform.runLater(() -> {
                if (playerCount >= 4) {
                    roomButtons.get(roomId).setText("Full! 4/4");
                }
                else {
                    roomButtons.get(roomId).setText("Room Id: " + roomId + " Players " + playerCount + "/4");
                }
            });
        }
    }

    /**
     * Removes a room button from the list, when the game ends.
     * @param roomId the ID of the room getting removed.
     */
    public void removeRoomButton(int roomId) {
        if (roomButtons.get(roomId) != null) {
            Platform.runLater(() -> {
                availableRoomsList.getChildren().remove(roomButtons.get(roomId));
                roomButtons.remove(roomId);
            });
        }
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
