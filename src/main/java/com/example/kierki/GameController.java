package com.example.kierki;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class GameController {

    private Client client;
    private HashMap<Integer, ImageView> cardsOnScreen = new HashMap<>();
    private int round;
    private List<Integer> otherPlayerIds = new ArrayList<>();
    @FXML
    private Label currentPlayerLabel;
    @FXML
    private Label otherPlayerLabel1;
    @FXML
    private Label otherPlayerLabel2;
    @FXML
    private Label otherPlayerLabel3;
    @FXML
    private ImageView playerPlayedCardView;
    @FXML
    private ImageView otherPlayedCardView1;
    @FXML
    private ImageView otherPlayedCardView2;
    @FXML
    private ImageView otherPlayedCardView3;
    @FXML
    private HBox playerCardsBox;
    private int cardsOffset = -25;

    public void setLabels(List<Integer> players) {
        currentPlayerLabel.setText(String.valueOf(client.getClientId()));
        int playerIndex = players.indexOf(client.getClientId());

        int counter = 0;
        for (int i = playerIndex; counter < 3; i++) {
            if (i == 4) i = 0;

            if (!Objects.equals(players.get(i), client.getClientId())) {
                otherPlayerIds.add(players.get(i));

                if (counter == 0) otherPlayerLabel1.setText(String.valueOf(players.get(i)));
                else if (counter == 1) otherPlayerLabel2.setText(String.valueOf(players.get(i)));
                else if (counter == 2) otherPlayerLabel3.setText(String.valueOf(players.get(i)));
                counter++;
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
            ImageView imageView = new ImageView(setCardImage(card));
            imageView.setPreserveRatio(true);
            imageView.setFitHeight(100);
            imageView.setFitWidth(50);
            //imageView.setTranslateX(i * (cardsOffset));

            int finalI = i;
            imageView.setOnMouseClicked(e -> {
                try {
                    client.playCard(finalI);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });

            cardsOnScreen.put(i, imageView);
            playerCardsBox.getChildren().add(imageView);

            i++;
        }
    }

    /**
     * Removes the played card from the player's hand, and places it in the middle of the screen
     */
    public void playCard(int cardIndex) {

        cardsOffset -= 2;
        for (int i = 0; i < cardsOnScreen.size(); i++) {
            //cardsOnScreen.get(i).setTranslateX(i * (cardsOffset));
        }

        cardsOnScreen.get(cardIndex).setTranslateX(0);
        //playerPlayedCardView.getChildren().add(cardsOnScreen.get(cardIndex));
        playerPlayedCardView.setImage(cardsOnScreen.get(cardIndex).getImage());

        playerCardsBox.getChildren().remove(cardsOnScreen.get(cardIndex));
        cardsOnScreen.remove(cardIndex);
    }

    public void placeOtherPlayersCard(Card playedCard, int clientId) {
        if (clientId == otherPlayerIds.get(0)) otherPlayedCardView1.setImage(setCardImage(playedCard));
        else if (clientId == otherPlayerIds.get(1)) otherPlayedCardView2.setImage(setCardImage(playedCard));
        else otherPlayedCardView3.setImage(setCardImage(playedCard));
    }

    public Image setCardImage(Card card) {
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

        return new Image(Objects.requireNonNull(this.getClass().getResource(path)).toString());
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
