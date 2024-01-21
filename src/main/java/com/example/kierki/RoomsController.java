package com.example.kierki;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.HashMap;

public class RoomsController {

    private Client client;
    private HashMap<Integer, Button> roomButtons = new HashMap<>();
    @FXML
    private VBox availableRoomsList;

    @FXML
    void createRoomButton() throws IOException {
        client.requestRoomAdd();
        client.joinCreatedRoom();
    }

    public void addRoom(Room room){
        Platform.runLater(() -> {
            Button button = new Button();
            if (room.isFull())
            {
                button.setText("Full! 4/4");
            }
            else
            {
                button.setText("Join " + room.getPlayerAmount() + "/4");
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

    public void updateRoomButton(int roomId, int playerCount) {
        if (roomButtons.get(roomId) != null)
        {
            Platform.runLater(() -> {
                if (playerCount >= 4) {
                    roomButtons.get(roomId).setText("Full! 4/4");
                }
                else {
                    roomButtons.get(roomId).setText("Join " + playerCount + "/4");
                }
            });
        }
    }

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
