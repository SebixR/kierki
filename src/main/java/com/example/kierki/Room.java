package com.example.kierki;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Room implements Serializable {

    private final int roomId;
    private final int hostId;
    private final List<Integer> connectedPlayers = new ArrayList<>();
    private boolean isFull;
    private List<Card> cards;

    public Room(int hostId, int roomId){
        this.hostId = hostId;
        this.roomId = roomId;
        this.isFull = false;
        connectedPlayers.add(hostId);

        initializeDeck();
    }

    public void initializeDeck() {
        cards = new ArrayList<>();

        Suit[] suits = {Suit.HEART, Suit.CLUB, Suit.DIAMOND, Suit.SPADE};
        for (Suit suit : suits) {
            for (int i = 2; i < 15; i++)
            {
//                String path = "cards/";
//                if (i <= 10) path += i + "_of_";
//                else if (i == 11) path += "jack_of_";
//                else if (i == 12) path += "queen_of_";
//                else if (i == 13) path += "king_of_";
//                else path += "ace_of_";
//
//                if (suit == Suit.HEART) path += "hearts";
//                else if (suit == Suit.SPADE) path += "spades";
//                else if (suit == Suit.CLUB) path += "clubs";
//                else path += "diamonds";
//
//                path += ".png";
//
//                Image image = new Image(Objects.requireNonNull(this.getClass().getResource(path)).toString());

                Card card = new Card(suit, i);
                cards.add(card);
            }
        }
    }

    /**
     * Adds a new client to the room, always in ascending order
     * @param playerId the player to add
     */
    public void addPlayer(int playerId){
        int index = 0;
        while (index < connectedPlayers.size() && connectedPlayers.get(index) < playerId) {
            index++;
        }
        connectedPlayers.add(index, playerId);
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

    public void setFull(){
        isFull = true;
    }
}
