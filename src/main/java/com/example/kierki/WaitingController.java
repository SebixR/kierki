package com.example.kierki;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * The JavaFX controller responsible for generating and updating the scene visible when players are waiting
 * for the game to start.
 * <p>
 *     It contains a Client object, so that the client can call methods using the GUI.
 *     It also houses all FXML objects, which connect to the fx IDs in the game.fxml file.
 * </p>
 */
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

    /**
     * A JavaFX method assigned to a button's onAction field.
     * When the button is clicked, the client requests to invite a player whose username is
     * specified in the inviteIdField.
     * @throws IOException in/out communication exception
     */
    @FXML
    public void invitePlayerButton() throws IOException {
        if (inviteIdField.getText() != null)
        {
            client.requestPlayerInvite(inviteIdField.getText());
        }
    }

    /**
     * Used to stylize the waiting scene, instead of using a CSS stylesheet.
     */
    public void stylize() {
        waitingAnchorPane.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 100%, #b7b7b7, #f1f1f1);");

        invitePane.setStyle("-fx-background-radius: 10, 10, 10, 10; " +
                "-fx-background-color: #fcfcfc; ");

        connectedPlayersVBox.setStyle("-fx-background-radius: 10, 10, 10, 10; " +
                "-fx-background-color: #fcfcfc; " +
                "-fx-padding: 10px;");
    }

    /**
     * Adds a player's username to the list of connected players when they join.
     * @param player the username of the new player.
     */
    public void addPlayerLabel(String player) {
        Label playerName = new Label(player);
        connectedPlayersVBox.getChildren().add(playerName);
    }

    /**
     * Clears the list of players, so that the scene can be used again.
     */
    public void clearLabels() {
        connectedPlayersVBox.getChildren().clear();
    }

    /**
     * Hides the pane responsible for inviting a player to all players except for the host.
     */
    public void hideInvitePane() {
        invitePane.setVisible(false);
    }

    /**
     * Shows the pane responsible for inviting a player to the host.
     */
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
