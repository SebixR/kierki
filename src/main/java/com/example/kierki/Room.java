package com.example.kierki;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The Room class holds most of the information about an ongoing game and its players.
 * <p>
 *     This class stores a list of IDs of clients who are currently in the room, a list of their names
 *     in the same order as the IDs, as well as a HashMap where the keys are clients' IDs, and the
 *     values are the amounts of points they currently have.
 * </p>
 * <p>
 *     The Room class is also responsible for initializing and storing the deck of cards used in throughout
 *     the game, and various information ion the current state of the game, like whose turn is is to play
 *     a card, how many turns have passed, which round it is, and what card was the first one placed on the
 *     table.
 * </p>
 */
public class Room implements Serializable {

    private final int roomId;
    private final int hostId;
    private final List<Integer> connectedPlayers = new ArrayList<>();
    private final List<String> connectedPlayersNames = new ArrayList<>();
    private final HashMap<Integer, Integer> playerPoints = new HashMap<>(); //key - clientId, value - points
    private boolean isFull;
    private List<Card> cards;
    private final HashMap<Integer, Card> cardsOnTable = new HashMap<>();
    private Card firstCardOnTable;
    private int currentTurn;
    private int currentRound;
    private int turnCounter = 1;
    private boolean gameOver = false;

    /**
     * Room class constructor.
     * @param hostId the ID of the player who created the room.
     * @param roomId a unique ID given by the server.
     * @param username the username of the host, to be put in the list of usernames.
     */
    public Room(int hostId, int roomId, String username){
        this.hostId = hostId;
        this.roomId = roomId;
        this.isFull = false;
        this.currentTurn = hostId;
        this.currentRound = 1;
        connectedPlayers.add(hostId);
        connectedPlayersNames.add(username);

        initializeDeck();
    }

    /**
     * Initializes a deck of cards. It creates 52 cards in total, and assigns them values
     * and suits.
     */
    public void initializeDeck() {
        cards = new ArrayList<>();

        Suit[] suits = {Suit.HEART, Suit.CLUB, Suit.DIAMOND, Suit.SPADE};
        for (Suit suit : suits) {
            for (int i = 2; i < 15; i++)
            {
                Card card = new Card(suit, i);
                cards.add(card);
            }
        }
    }

    /**
     * Adds a new client to the room, always in ascending order, as to determine the order of turns
     * and simplify generating the GUI correctly.
     * Sets the isFull parameter, and initializes the hashMap containing players' points.
     * @param playerId the ID of the player to add.
     */
    public void addPlayer(int playerId, String username){
        int index = 0;
        while (index < connectedPlayers.size() && connectedPlayers.get(index) < playerId) {
            index++;
        }
        connectedPlayers.add(index, playerId);
        connectedPlayersNames.add(index, username);

        if (connectedPlayers.size() == 4){
            isFull = true;
            for (Integer player : connectedPlayers) {
                playerPoints.put(player, 0);
            }
        }
    }

    /**
     * Updates the player's amount of points.
     * @param clientId the player whose points will be updated.
     * @param points the amount of received points.
     */
    public void givePoints(int clientId, int points) {
        int currentPoints = playerPoints.get(clientId);
        playerPoints.put(clientId, currentPoints + points);
    }

    /**
     * Updates whose turn is it to play. The turn order is dictated by ids, not the order the players joined.
     */
    public void changeTurn() {
        int index = connectedPlayers.indexOf(currentTurn);
        if (index == 3) currentTurn = connectedPlayers.get(0);
        else currentTurn = connectedPlayers.get(index + 1);
    }

    /**
     * Setter for currentTurn, used at the end of the turn, and at the end of the round
     * @param clientId the client who took the cards, and thus will start the next turn
     */
    public void setCurrentTurn(int clientId) {
        this.currentTurn = clientId;
    }

    /**
     * Increments the current turn number, for the 6th and 7th rounds
     */
    public void incrementTurnCounter() {
        this.turnCounter++;
    }

    public void toggleGameOver() {
        this.gameOver = !this.gameOver;
    }

    public void incrementCurrentRound() {
        this.currentRound++;
    }

    public void resetTurnCounter() {
        this.currentTurn = 1;
    }

    public boolean isFull(){
        return isFull;
    }

    public int getPlayerAmount() {
        return connectedPlayers.size();
    }

    public int getRoomId(){
        return roomId;
    }

    public List<Integer> getConnectedPlayers() {
        return connectedPlayers;
    }

    public int getHostId() {
        return hostId;
    }

    public List<Card> getCards() {
        return this.cards;
    }

    public HashMap<Integer, Card> getCardsOnTable() {
        return this.cardsOnTable;
    }
    public int getCurrentRound() {
        return this.currentRound;
    }

    public int getCurrentTurn() {
        return this.currentTurn;
    }

    public int getTurnCounter() {
        return this.turnCounter;
    }
    public Card getFirstCardOnTable() {
        return this.firstCardOnTable;
    }

    public HashMap<Integer, Integer> getPlayerPoints() {
        return playerPoints;
    }

    public boolean getGameOver() {
        return this.gameOver;
    }

    public List<String> getConnectedPlayersNames() {
        return connectedPlayersNames;
    }

    public void setFirstCardOnTable(Card card) {
        this.firstCardOnTable = card;
    }
}
