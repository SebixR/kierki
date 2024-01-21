package com.example.kierki;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Room implements Serializable {

    private final int roomId;
    private final int hostId;
    private final List<Integer> connectedPlayers = new ArrayList<>();
    private List<String> connectedPlayersNames = new ArrayList<>();
    private final HashMap<Integer, Integer> playerPoints = new HashMap<>(); //key - clientId, value - points
    private boolean isFull;
    private List<Card> cards;
    private final HashMap<Integer, Card> cardsOnTable = new HashMap<>();
    private Card firstCardOnTable;
    private int currentTurn;
    private int currentRound;
    private int turnCounter = 1;
    private boolean gameOver = false;

    public Room(int hostId, int roomId, String username){
        this.hostId = hostId;
        this.roomId = roomId;
        this.isFull = false;
        this.currentTurn = hostId;
        this.currentRound = 7;
        connectedPlayers.add(hostId);
        connectedPlayersNames.add(username);

        initializeDeck();
    }

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
     * Adds a new client to the room, always in ascending order
     * Sets the isFull parameter, and initializes the hashMap containing players' points
     * @param playerId the player to add
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
     * Updates the player's amount of points
     * @param clientId the player whose points will be updated
     * @param points the amount of received points
     */
    public void givePoints(int clientId, int points) {
        int currentPoints = playerPoints.get(clientId);
        playerPoints.put(clientId, currentPoints + points);
    }

    /**
     * Update whose turn is it to play. The turn order is dictated by ids, not the order the players joined
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

    public void setFull(){
        isFull = true;
    }
    public void setFirstCardOnTable(Card card) {
        this.firstCardOnTable = card;
    }

    public void incrementRound() {
        this.currentRound++;
    }
}
