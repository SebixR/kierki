package com.example.kierki;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class GameController {

    private Client client;
    private HashMap<Card, ImageView> cardsOnScreen = new HashMap<>();
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
    private Pane playerPlayedCardPane;
    @FXML
    private Pane otherPlayedCardPane1;
    @FXML
    private Pane otherPlayedCardPane2;
    @FXML
    private Pane otherPlayedCardPane3;
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

    /**
     * Generates the player's hand, after the cards have been delt
     */
    public void generatePlayerCards() {

        List<Card> clientCards = client.getCardsInHand();

        int i = 0;
        for (Card card : clientCards)
        {
            String path = "cards/";
            if (card.getValue() <= 10) path += card.getValue() + "_of_";
            else if (card.getValue() == 11) path += "jack_of_";
            else if (card.getValue() == 12) path += "queen_of_";
            else if (card.getValue() == 13) path += "king_of_";
            else path += "ace_of_";

            if (card.getSuit() == Suit.HEART) path += "hearts";
            else if (card.getSuit() == Suit.SPADE) path += "spades";
            else if (card.getSuit() == Suit.CLUB) path += "clubs";
            else path += "diamonds";

            path += ".png";

            Image cardImage = new Image(Objects.requireNonNull(this.getClass().getResource(path)).toString());
            ImageView imageView = new ImageView(cardImage);
            imageView.setPreserveRatio(true);
            imageView.setFitHeight(100);
            imageView.setFitWidth(50);
            imageView.setTranslateX((i) * (-25));

            int finalI = i;
            imageView.setOnMouseClicked(e -> {
                try {
                    client.playCard(finalI);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });

            cardsOnScreen.put(clientCards.get(i), imageView);
            playerCardsBox.getChildren().add(imageView);

            i++;
        }
    }

    /**
     * Removes the played card from the player's hand, and places it in the middle of the screen
     */
    public void playCard(Card card) {
        playerPlayedCardPane.getChildren().add(cardsOnScreen.get(card));

        playerCardsBox.getChildren().remove(cardsOnScreen.get(card));
        cardsOnScreen.remove(card);
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
