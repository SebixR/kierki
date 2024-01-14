package com.example.kierki;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class WaitingController {

    private Client client;
    @FXML
    private TextField inviteIdField;
    @FXML
    private VBox connectedPlayersVBox;
    @FXML
    private Pane invitePane;
    @FXML
    private Label waitingLabel;


    @FXML
    public void invitePlayerButton() throws IOException {
        if (inviteIdField.getText() != null)
        {
            client.requestPlayerInvite(Integer.parseInt(inviteIdField.getText()));
        }
    }


    public void addPlayerLabel(String player) {
        Label playerName = new Label(player);
        connectedPlayersVBox.getChildren().add(playerName);
    }

    public void hideInvitePane() {
        invitePane.setVisible(false);
    }

    public void hideWaitingLabel() {
        waitingLabel.setVisible(false);
    }

    public VBox getConnectedPlayersVBox() {
        return connectedPlayersVBox;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
