package com.example.kierki;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Objects;

public class GameController {

    private Client client;
    private int round;
    @FXML
    private Label currentPlayerLabel;
    @FXML
    private Label otherPlayerLabel1;
    @FXML
    private Label otherPlayerLabel2;
    @FXML
    private Label otherPlayerLabel3;
    @FXML
    private HBox playerCardsBox;

    public void setLabels(List<Integer> players) {
        int counter = 0;
        for (Integer player : players) {
            if (Objects.equals(player, client.getClientId())) currentPlayerLabel.setText(String.valueOf(player));
            else
            {
                counter++;
                if (counter == 1) otherPlayerLabel1.setText(String.valueOf(player));
                else if (counter == 2) otherPlayerLabel2.setText(String.valueOf(player));
                else if (counter == 3) otherPlayerLabel3.setText(String.valueOf(player));
            }
        }
    }

    public void generatePlayerCards(List<Card> cards) {

    }

    public void setClient(Client client) {
        this.client = client;
    }
}
