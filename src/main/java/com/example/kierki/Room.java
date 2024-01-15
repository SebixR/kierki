package com.example.kierki;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Room implements Serializable {

    private final int roomId;
    private final int hostId;
    private final List<Integer> connectedPlayers = new ArrayList<>();
    private HashMap<Integer, Integer> playerPoints = new HashMap<>(); //key - clientId, value - points
    private boolean isFull;
    private List<Card> cards;
    private int currentTurn;

    public Room(int hostId, int roomId){
        this.hostId = hostId;
        this.roomId = roomId;
        this.isFull = false;
        this.currentTurn = hostId; //the host starts the game (technically the person on the LEFT of the host should start)
        connectedPlayers.add(hostId);

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
    public void addPlayer(int playerId){
        int index = 0;
        while (index < connectedPlayers.size() && connectedPlayers.get(index) < playerId) {
            index++;
        }
        connectedPlayers.add(index, playerId);

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

    public int getCurrentTurn() {
        return this.currentTurn;
    }

    public void setFull(){
        isFull = true;
    }
}
