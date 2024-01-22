package com.example.kierki;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * The JavaFX controller responsible for generating and updating the login, which is the first scene
 * shown to a newly connected client.
 * <p>
 *     It contains a Client object, so that the client can call methods using the GUI.
 *     It also houses all FXML objects, which connect to the fx IDs in the game.fxml file.
 * </p>
 */
public class UsernameController {

    private Client client;
    @FXML
    private TextField userNameInput;
    @FXML
    private Label errorLabel;

    /**
     * A JavaFX method assigned to a button's onAction field.
     * When the button is clicked, the client requests that the server assigns them the username,
     * given in the TextField userNameInput.
     * @throws IOException in/out communication exception.
     */
    @FXML
    public void acceptUsername() throws IOException {
        if (!userNameInput.getText().isEmpty()) {
            client.requestUsername(userNameInput.getText());
        }
    }

    /**
     * Displays the error label, if the username's already taken.
     */
    public void showErrorLabel() {
        errorLabel.setVisible(true);
    }

    /**
     * Hides the error label. Called when initializing the window.
     */
    public void hideErrorLabel() {
        errorLabel.setVisible(false);
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
