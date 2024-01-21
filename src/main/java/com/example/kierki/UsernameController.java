package com.example.kierki;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class UsernameController {

    private Client client;
    @FXML
    private TextField userNameInput;
    @FXML
    private Label errorLabel;

    @FXML
    public void acceptUsername() throws IOException {
        if (!userNameInput.getText().isEmpty()) {
            client.requestUsername(userNameInput.getText());
        }
    }

    public void showErrorLabel() {
        errorLabel.setVisible(true);
    }

    public void hideErrorLabel() {
        errorLabel.setVisible(false);
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
