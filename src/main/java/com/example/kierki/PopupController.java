package com.example.kierki;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The JavaFX controller responsible for a popup window appearing when a player has been invited
 * to a game. Its displays information about who invited the player, and holds the ID of the
 * room they've been invited to.
 */
public class PopupController {

    Stage popupStage;
    private Client client;
    private int roomId;
    @FXML
    private Label inviterLabel;

    /**
     * A JavaFX onAction function bound to a button that accepts the invitation. Calls a method
     * that notifies the server a client wants to join a room.
     * @throws IOException in/out communication exception
     */
    @FXML
    public void acceptInviteButton() throws IOException {
        client.joinExistingRoom(roomId);
        popupStage.close();
    }

    public void setInviterLabel(String player) {
        inviterLabel.setText(player);
    }
    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }
    public void setClient(Client client) {
        this.client = client;
    }
    public void setPopupStage(Stage popupStage) {
        this.popupStage = popupStage;
    }
}
