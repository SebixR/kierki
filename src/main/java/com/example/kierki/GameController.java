package com.example.kierki;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class GameController {

    private Client client;
    private final HashMap<Integer, ImageView> cardsOnScreen = new HashMap<>();
    private int round;
    private final List<Integer> otherPlayerIds = new ArrayList<>();
    private final HashMap<Integer, Label> scoreboardScoreLabels = new HashMap<>();
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
    private VBox scoreboardNamesVBox;
    @FXML
    private VBox scoreboardScoresVBox;
    @FXML
    private HBox playerCardsBox;
    @FXML
    private Label roundLabel;
    @FXML
    private Pane winnerPane;
    @FXML
    private Label winnerLabel;
    @FXML
    private Button exitButton;
    private int cardsOffset = -25;

    /**
     * Sets all the player's username labels, and initializes the scoreboard
     * @param players a list of the players' id's
     */
    public void setLabels(List<Integer> players) {
        winnerPane.setVisible(false);

        currentPlayerLabel.setText(String.valueOf(client.getClientId()));
        scoreboardScoreLabels.put(client.getClientId(), new Label("0"));
        int playerIndex = players.indexOf(client.getClientId());

        int counter = 0;
        for (int i = playerIndex; counter < 3; i++) {
            if (i == 4) i = 0;

            if (!Objects.equals(players.get(i), client.getClientId())) {
                otherPlayerIds.add(players.get(i));
                scoreboardScoreLabels.put(players.get(i), new Label("0"));

                if (counter == 0) otherPlayerLabel1.setText(String.valueOf(players.get(i)));
                else if (counter == 1) otherPlayerLabel2.setText(String.valueOf(players.get(i)));
                else if (counter == 2) otherPlayerLabel3.setText(String.valueOf(players.get(i)));
                counter++;
            }
        }

        scoreboardNamesVBox.getChildren().add(new Label(currentPlayerLabel.getText()));
        scoreboardNamesVBox.getChildren().add(new Label(otherPlayerLabel1.getText()));
        scoreboardNamesVBox.getChildren().add(new Label(otherPlayerLabel2.getText()));
        scoreboardNamesVBox.getChildren().add(new Label(otherPlayerLabel3.getText()));

        scoreboardScoresVBox.getChildren().addAll(new Label("0"), new Label("0"), new Label("0"), new Label("0"));

        updateRound(1);
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

    /**
     * Places a different player's card on the correct spot on the table
     * @param playedCard the card being places
     * @param clientId the client who played the card
     */
    public void placeOtherPlayersCard(Card playedCard, int clientId) {
        if (clientId == otherPlayerIds.get(0)) otherPlayedCardView1.setImage(setCardImage(playedCard));
        else if (clientId == otherPlayerIds.get(1)) otherPlayedCardView2.setImage(setCardImage(playedCard));
        else otherPlayedCardView3.setImage(setCardImage(playedCard));
    }

    /**
     * Clears the scoreboard values and fills it up again with the updated scores. The order of the scores is the
     * same as the order of players
     * @param clientId the client who received points
     * @param points the sum amount of point the client now has
     */
    public void updatePoints(int clientId, int points) {
        scoreboardScoresVBox.getChildren().clear();
        scoreboardScoreLabels.put(clientId, new Label(String.valueOf(points)));

        scoreboardScoresVBox.getChildren().add(scoreboardScoreLabels.get(client.getClientId()));
        scoreboardScoresVBox.getChildren().add(scoreboardScoreLabels.get(otherPlayerIds.get(0)));
        scoreboardScoresVBox.getChildren().add(scoreboardScoreLabels.get(otherPlayerIds.get(1)));
        scoreboardScoresVBox.getChildren().add(scoreboardScoreLabels.get(otherPlayerIds.get(2)));
    }

    /**
     * Removes cards from the table
     */
    public void clearTable() {
        playerPlayedCardView.setImage(null);
        otherPlayedCardView1.setImage(null);
        otherPlayedCardView2.setImage(null);
        otherPlayedCardView3.setImage(null);
    }

    public void updateRound(int round) {
        this.round = round;
        roundLabel.setText("Round: " + round);
    }

    public void showWinner(int winnerId) {
        winnerLabel.setText("Winner: " + winnerId + "!");
        winnerPane.setVisible(true);
    }

    /**
     * Creates the path for image for the given card
     * @param card the given card
     * @return an Image of the given card
     */
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

    /**
     * Sets the client
     * @param client the client being set
     */
    public void setClient(Client client) {
        this.client = client;
    }
}
