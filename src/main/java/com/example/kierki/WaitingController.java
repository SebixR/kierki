package com.example.kierki;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
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
    private AnchorPane waitingAnchorPane;

    @FXML
    public void invitePlayerButton() throws IOException {
        if (inviteIdField.getText() != null)
        {
            client.requestPlayerInvite(inviteIdField.getText());
        }
    }

    public void stylize() {
        waitingAnchorPane.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 100%, #b7b7b7, #f1f1f1);");

        invitePane.setStyle("-fx-background-radius: 10, 10, 10, 10; " +
                "-fx-background-color: #fcfcfc; ");

        connectedPlayersVBox.setStyle("-fx-background-radius: 10, 10, 10, 10; " +
                "-fx-background-color: #fcfcfc; " +
                "-fx-padding: 10px;");
    }

    public void addPlayerLabel(String player) {
        Label playerName = new Label(player);
        connectedPlayersVBox.getChildren().add(playerName);
    }

    public void clearLabels() {
        connectedPlayersVBox.getChildren().clear();
    }

    public void hideInvitePane() {
        invitePane.setVisible(false);
    }
    public void showInvitePane() {
        invitePane.setVisible(true);
    }

    public VBox getConnectedPlayersVBox() {
        return connectedPlayersVBox;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
