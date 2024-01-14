package com.example.kierki;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class PopupController {

    Stage popupStage;
    private Client client;
    private int roomId;

    @FXML
    private Label inviterLabel;

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
